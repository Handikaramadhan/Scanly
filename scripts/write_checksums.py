#!/usr/bin/env python3
"""Write checksums after source ZIP and binary artifacts are finalized."""
from pathlib import Path
from project_version import VERSION
import hashlib
import json

artifacts = Path(__file__).resolve().parents[1] / 'artifacts'
files = sorted(artifacts.glob(f'Scanly-{VERSION}-*'))
files.append(artifacts / 'validation-summary.json')
release_report = artifacts / 'release-validation.json'
if release_report.exists() and json.loads(release_report.read_text()).get('version') == VERSION:
    release_apk = artifacts / f'Scanly-{VERSION}.apk'
    report = json.loads(release_report.read_text())
    if hashlib.sha256(release_apk.read_bytes()).hexdigest() != report['sha256']:
        raise RuntimeError('Release APK differs from signing report')
    files.append(release_apk)
    files.append(release_report)
if not files or any(not file.is_file() for file in files):
    raise RuntimeError('Finalize artifact packaging first')
(artifacts / 'SHA256SUMS.txt').write_text(''.join(
    hashlib.sha256(file.read_bytes()).hexdigest() + '  ' + file.name + '\n' for file in files))
print('Checksums written for', len(files), 'artifacts.')
