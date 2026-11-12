#!/usr/bin/env python3
"""Sign the validated unsigned APK with the owner's private release key.

The signing properties stay outside public source. Passwords are passed through
the subprocess environment, never command arguments, reports or logs.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile

from project_version import VERSION, VERSION_CODE

ROOT = Path(__file__).resolve().parents[1]


def run(arguments, env=None):
    result = subprocess.run([str(arg) for arg in arguments], env=env,
                            capture_output=True, text=True)
    if result.returncode:
        # Signing tool errors can contain configuration; do not echo private input.
        raise RuntimeError('Release verification/signing command failed: ' + Path(arguments[0]).name)
    return result.stdout


def certificate(tool, apk):
    output = run([tool, 'verify', '--print-certs', apk])
    match = re.search(r'Signer #1 certificate SHA-256 digest: ([0-9a-f]{64})', output)
    if not match:
        raise RuntimeError('Expected one verifiable APK signer')
    return match.group(1)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--signing-properties', required=True, type=Path)
    parser.add_argument('--sdk', type=Path, default=Path(os.environ.get('ANDROID_SDK_ROOT') or
                        os.environ.get('ANDROID_HOME') or ROOT / '.android-sdk'))
    args = parser.parse_args()
    config = args.signing_properties.resolve()
    if config.is_relative_to(ROOT) or config.stat().st_mode & 0o077:
        raise RuntimeError('Keep signing properties outside the repository, mode 0600')
    properties = {}
    for line in config.read_text().splitlines():
        if line.strip() and not line.lstrip().startswith('#'):
            key, value = line.split('=', 1)
            properties[key.strip()] = value.strip()
    if not all(properties.get(key) for key in ['storeFile', 'storePassword', 'keyAlias', 'keyPassword']):
        raise RuntimeError('Signing properties incomplete')
    keystore = Path(properties['storeFile']).resolve()
    if keystore.is_relative_to(ROOT) or keystore.stat().st_mode & 0o077:
        raise RuntimeError('Keep keystore outside the repository, mode 0600')
    tools = args.sdk / 'build-tools/35.0.0'
    artifacts = ROOT / 'artifacts'
    summary = json.loads((artifacts / 'validation-summary.json').read_text())
    if summary['version'] != VERSION or summary['versionCode'] != VERSION_CODE:
        raise RuntimeError('Package current validation results first')
    source = artifacts / f'Scanly-{VERSION}-release-unsigned.apk'
    digest = hashlib.sha256(source.read_bytes()).hexdigest()
    if not any(item['name'] == source.name and item['sha256'] == digest for item in summary['artifacts']):
        raise RuntimeError('Unsigned APK differs from validated artifact')
    expected_path = ROOT / 'docs/release-certificate.sha256'
    expected = expected_path.read_text().strip()
    if not re.fullmatch(r'[0-9a-f]{64}', expected):
        raise RuntimeError('Missing pinned public release certificate fingerprint')
    env = dict(os.environ, SCANLY_STORE_PASSWORD=properties['storePassword'],
               SCANLY_KEY_PASSWORD=properties['keyPassword'])
    target = artifacts / f'Scanly-{VERSION}.apk'
    with tempfile.TemporaryDirectory(prefix='scanly-sign-', dir=artifacts) as temporary:
        staged = Path(temporary) / target.name
        run([tools / 'apksigner', 'sign', '--ks', keystore, '--ks-key-alias', properties['keyAlias'],
             '--ks-pass', 'env:SCANLY_STORE_PASSWORD', '--key-pass', 'env:SCANLY_KEY_PASSWORD',
             '--v4-signing-enabled', 'false', '--out', staged, source], env=env)
        actual = certificate(tools / 'apksigner', staged)
        if actual == summary['debug_certificate_sha256'] or actual != expected:
            raise RuntimeError('Release signer differs from pinned identity or uses debug key')
        run([tools / 'zipalign', '-c', '-P', '16', '4', staged])
        badging = run([tools / 'aapt', 'dump', 'badging', staged])
        identity = f"name='com.scanly.app' versionCode='{VERSION_CODE}' versionName='{VERSION}'"
        if identity not in badging.splitlines()[0] or 'application-debuggable' in badging:
            raise RuntimeError('Wrong release identity or debuggable APK')
        staged.replace(target)
    report = {
        'version': VERSION, 'versionCode': VERSION_CODE,
        'apk': target.name, 'bytes': target.stat().st_size,
        'sha256': hashlib.sha256(target.read_bytes()).hexdigest(),
        'certificate_sha256': actual, 'signing': 'Dedicated Scanly publisher key',
        'debuggable': False, 'zip_16kb_alignment_checked': True,
        'unsigned_apk_sha256': digest,
        'device_validation': 'Not run: no Android device connected; see docs/DEVICE_TESTS.md',
    }
    (artifacts / 'release-validation.json').write_text(json.dumps(report, indent=2) + '\n')
    print(target.name, report['bytes'], 'bytes; dedicated release certificate verified.')


if __name__ == '__main__':
    main()
