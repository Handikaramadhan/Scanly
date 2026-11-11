#!/usr/bin/env python3
"""Build notices from resolved runtime coordinates and cached Maven POM metadata."""
from pathlib import Path
from project_version import VERSION
import argparse
import json
import os
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--cache', type=Path, default=Path(os.environ.get('GRADLE_USER_HOME', str(Path.home() / '.gradle'))) / 'caches/modules-2/files-2.1')
args = parser.parse_args()
coordinates = (ROOT / 'app/build/reports/runtime-dependencies.txt').read_text().splitlines()

def licenses_for(coordinate, seen=None):
    seen = set() if seen is None else seen
    if coordinate in seen:
        raise RuntimeError('Cyclic POM parent: ' + coordinate)
    seen.add(coordinate)
    group, artifact, version = coordinate.split(':')
    paths = sorted((args.cache / group / artifact / version).glob('*/*.pom'))
    if not paths:
        raise RuntimeError('POM missing from cache: ' + coordinate)
    doc = ET.parse(paths[0]).getroot()
    licenses = [{'name': (item.findtext('{*}name') or '').strip(),
                 'url': (item.findtext('{*}url') or '').strip()}
                for item in doc.findall('{*}licenses/{*}license')]
    if licenses:
        return licenses
    parent = doc.find('{*}parent')
    if parent is not None:
        parent_coordinate = ':'.join(parent.findtext('{*}' + field) or ''
                                     for field in ['groupId', 'artifactId', 'version'])
        return licenses_for(parent_coordinate, seen)
    raise RuntimeError('License metadata missing: ' + coordinate)

inventory = [{'coordinate': coordinate, 'licenses': licenses_for(coordinate)}
             for coordinate in sorted(set(coordinates))]
(ROOT / 'docs/DEPENDENCIES.json').write_text(json.dumps(inventory, indent=2, ensure_ascii=False) + '\n')
header = f"""Scanly {VERSION} — kode aplikasi: Apache License 2.0

Kode Scanly terbuka (Apache-2.0). Kamera, deteksi tepi dan editor memakai ML Kit Document Scanner yang didistribusikan Google Play services; komponen scanner Google tersebut tidak open source. OCR Latin memakai model ML Kit yang disertakan di APK dan tunduk pada ketentuan Google. Ketentuan setiap dependency dideklarasikan di bawah.

Daftar berikut mencakup dependency runtime debug dan release yang diselesaikan Gradle, termasuk dependency transitif. Nama dan URL lisensi diambil dari metadata Maven POM (termasuk parent POM); daftar ini bukan audit hukum seluruh binary pihak ketiga.

"""
lines = []
for component in inventory:
    lines.append(component['coordinate'])
    lines.extend('  ' + license['name'] + ' — ' + license['url'] for license in component['licenses'])
    lines.append('')
(ROOT / 'app/src/main/assets/licenses.txt').write_text(
    header + '\n'.join(lines) + '\n\nApache License 2.0\n\n' + (ROOT / 'LICENSE').read_text())
print(f'Generated notices for {len(inventory)} resolved runtime modules.')
