#!/usr/bin/env python3
"""Verify the built candidate and copy artifacts with unambiguous names."""
from pathlib import Path
from project_version import VERSION, VERSION_CODE
from zipfile import ZipFile
import argparse
import hashlib
import json
import os
import re
import shutil
import struct
import subprocess
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--sdk', type=Path, default=Path(os.environ.get('ANDROID_SDK_ROOT') or os.environ.get('ANDROID_HOME') or root / '.android-sdk'))
parser.add_argument("--test-release", action="store_true", help="Sign optimized APK with the existing Android debug test key, never a publisher key")
args = parser.parse_args()
tools = args.sdk / 'build-tools/35.0.0'
artifacts = root / 'artifacts'
artifacts.mkdir(exist_ok=True)

def command(arguments):
    return subprocess.run([str(a) for a in arguments], capture_output=True, text=True, check=True).stdout

def certificate(apk):
    output = command([tools / 'apksigner', 'verify', '--print-certs', apk])
    found = re.search(r'Signer #1 certificate SHA-256 digest: (\w+)', output)
    if not found:
        raise RuntimeError('APK signer missing')
    return found.group(1)

reports = []
for path in sorted((root / 'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml')):
    suite = ET.parse(path).getroot().attrib
    reports.append({key: suite[key] for key in ['name', 'tests', 'failures', 'errors', 'skipped']})
expected_suites = {'com.scanly.app.tools.DocumentToolsTest', 'com.scanly.app.tools.DocumentToolsUiTest', 'com.scanly.app.tools.DocumentToolsModelTest', 'com.scanly.app.LibraryFlowTest', 'com.scanly.app.data.DocumentCatalogTest', 'com.scanly.app.ocr.PdfOcrEngineTest', 'com.scanly.app.scanner.ScannerLauncherTest', 'com.scanly.app.data.PdfStoreTest', 'com.scanly.app.ScanlyFlowTest', 'com.scanly.app.data.DocumentRepositoryTest'}
if expected_suites != {report['name'] for report in reports}:
    raise RuntimeError('Run the full suite, not a filtered test selection')
if not reports or any(int(report['failures']) or int(report['errors']) or int(report['skipped']) for report in reports):
    raise RuntimeError('Tests missing, failing or skipped')
lint = {}
for variant in ['debug', 'release']:
    issues = ET.parse(root / f'app/build/reports/lint-results-{variant}.xml').getroot().findall('issue')
    lint[variant] = {'errors': sum(item.attrib['severity'] in ('Error', 'Fatal') for item in issues),
                     'warnings': sum(item.attrib['severity'] == 'Warning' for item in issues),
                     'issues': [{'id': item.attrib['id'], 'severity': item.attrib['severity']} for item in issues]}
    if lint[variant]['errors']:
        raise RuntimeError('Lint errors: ' + variant)

debug = root / 'app/build/outputs/apk/debug/app-debug.apk'
release = root / 'app/build/outputs/apk/release/app-release-unsigned.apk'
bundle = root / 'app/build/outputs/bundle/release/app-release.aab'
signer = certificate(debug)
for previous_version in ['1.0.0', '1.1.0', '1.2.0', '1.3.0', '1.4.0', '1.5.0', '1.6.0']:
    previous = artifacts / f'Scanly-{previous_version}-debug.apk'
    if previous.exists() and signer != certificate(previous):
        raise RuntimeError('Debug update signing certificate changed: ' + previous_version)

metadata = {}
for variant, path in [('debug', debug), ('release-unsigned', release)]:
    output = command([tools / 'aapt', 'dump', 'badging', path])
    if f"name='com.scanly.app' versionCode='{VERSION_CODE}' versionName='{VERSION}'" not in output.splitlines()[0]:
        raise RuntimeError('Unexpected APK identity/version')
    permissions = command([tools / 'aapt', 'dump', 'permissions', path])
    if 'android.permission.CAMERA' in permissions:
        raise RuntimeError('Google scanner must not request app camera permission')
    if 'android.permission.RECORD_AUDIO' in permissions:
        raise RuntimeError('Unexpected microphone permission')
    debuggable = 'application-debuggable' in output
    if debuggable != (variant == 'debug'):
        raise RuntimeError('Unexpected debuggable flag')
    command([tools / 'zipalign', '-c', '-P', '16', '4', path])
    alignments = []
    with ZipFile(path) as archive:
        if archive.testzip() is not None:
            raise RuntimeError('Invalid APK ZIP')
        for asset in ['privacy.txt', 'licenses.txt']:
            if archive.read('assets/' + asset) != (root / 'app/src/main/assets' / asset).read_bytes():
                raise RuntimeError('APK contains outdated asset: ' + asset)
        ocr_models=[name for name in archive.namelist() if name.startswith('assets/mlkit-google-ocr-models/') and not name.endswith('/')]
        if not any('Latn_ctc' in name for name in ocr_models):
            raise RuntimeError('Bundled Latin OCR model missing')
        for name in archive.namelist():
            if not name.endswith('.so'):
                continue
            data = archive.read(name)
            if data[:4] != b'\x7fELF':
                raise RuntimeError('Non-ELF native library')
            endian = '<' if data[5] == 1 else '>'
            wide = data[4] == 2
            phoff = struct.unpack_from(endian + ('Q' if wide else 'I'), data, 32 if wide else 28)[0]
            entry_size, count = struct.unpack_from(endian + 'HH', data, 54 if wide else 42)
            for index in range(count):
                offset = phoff + entry_size * index
                segment = struct.unpack_from(endian + 'I', data, offset)[0]
                if segment != 1:
                    continue
                alignment = struct.unpack_from(endian + ('Q' if wide else 'I'), data, offset + (48 if wide else 28))[0]
                required_alignment=16384 if wide else 4096
                if alignment < required_alignment:
                    raise RuntimeError(f'Native LOAD alignment below {required_alignment}: {name}')
                alignments.append(alignment)
    metadata[variant] = {'debuggable': debuggable, 'bundled_ocr_model_files':len(ocr_models),
                         'elf_alignment_requirement':'ELF64 >=16 KB; ELF32 >=4 KB; runtime not tested', 'elf_load_segments_checked': len(alignments),
                         'zip_16kb_alignment_checked': True, 'package': output.splitlines()[0], 'permissions': permissions.splitlines()}

with ZipFile(bundle) as archive:
    if archive.testzip() is not None:
        raise RuntimeError('Invalid AAB ZIP')
    if any(re.search(r'^META-INF/.*\.(RSA|DSA|EC|SF)$', name, re.IGNORECASE) for name in archive.namelist()):
        raise RuntimeError('Bundle is signed; do not label it unsigned')
    assert archive.read('base/assets/privacy.txt') == (root / 'app/src/main/assets/privacy.txt').read_bytes()

copied = []
for source, name in [(debug, f'Scanly-{VERSION}-debug.apk'), (release, f'Scanly-{VERSION}-release-unsigned.apk'),
                     (bundle, f'Scanly-{VERSION}-release-unsigned.aab')]:
    target = artifacts / name
    shutil.copy2(source, target)
    copied.append({'name': name, 'bytes': target.stat().st_size,
                   'sha256': hashlib.sha256(target.read_bytes()).hexdigest()})
if args.test_release:
    test_apk=artifacts / f'Scanly-{VERSION}-release-test.apk'
    key_candidates=[Path.home()/'.android/debug.keystore',Path(os.environ.get('XDG_CONFIG_HOME',str(Path.home()/'.config')))/'.android/debug.keystore']
    if os.environ.get('ANDROID_USER_HOME'):
        key_candidates.insert(0,Path(os.environ['ANDROID_USER_HOME'])/'debug.keystore')
    debug_keystore=next((key for key in key_candidates if key.is_file()),None)
    if debug_keystore is None:
        raise RuntimeError('Existing Android debug test keystore missing; do not create a publisher key here')
    command([tools / 'apksigner','sign','--ks',debug_keystore,'--ks-key-alias','androiddebugkey',
             '--ks-pass','pass:android','--key-pass','pass:android','--v4-signing-enabled','false','--out',test_apk,release])
    if certificate(test_apk)!=signer:
        raise RuntimeError('Optimized test APK must use the same debug update certificate')
    command([tools / 'zipalign','-c','-P','16','4',test_apk])
    badging=command([tools / 'aapt','dump','badging',test_apk])
    if 'application-debuggable' in badging:
        raise RuntimeError('Optimized test APK must not be debuggable')
    metadata['release-test']={**metadata['release-unsigned'],'signing':'Android debug test key; not a publisher release'}
    copied.append({'name':test_apk.name,'bytes':test_apk.stat().st_size,'sha256':hashlib.sha256(test_apk.read_bytes()).hexdigest()})

summary = {'version': VERSION, 'versionCode': VERSION_CODE, 'test_suites': reports, 'lint': lint,
           'apk_checks': metadata, 'debug_certificate_sha256': signer, 'artifacts': copied,
           'limitations': ['Robolectric UI tests use a test-only PDFBox renderer shadow; native Android PDF device test is compiled but not run.',
                           'Google scanner UI and native OCR accuracy/offline behavior require real-device validation; unit tests inject recognition results.',
                           'Unsigned release APK/AAB are not publication-ready; publisher signing and real-device validation remain.']}
(artifacts / 'validation-summary.json').write_text(json.dumps(summary, indent=2) + '\n')
for artifact in copied:
    print(artifact['name'], artifact['bytes'], 'bytes')
print('Tests:', sum(int(report['tests']) for report in reports), 'passed; lint debug/release: 0 errors.')
