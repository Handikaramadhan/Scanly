#!/usr/bin/env python3
"""Package the same reviewed public source used for GitHub, without private workspace history."""
from pathlib import Path
from project_version import VERSION
from public_source import public_files
from zipfile import ZipFile, ZIP_DEFLATED

root = Path(__file__).resolve().parents[1]
files = list(public_files(root))
output = root / f'artifacts/Scanly-{VERSION}-source.zip'
output.parent.mkdir(exist_ok=True)
with ZipFile(output, 'w', ZIP_DEFLATED) as archive:
    for path in files:
        archive.write(path, 'Scanly/' + str(path.relative_to(root)))
with ZipFile(output) as archive:
    assert archive.testzip() is None
print(f'Packaged {len(files)} public source files: {output.name}')
