"""Allowlisted public source; local release history, SDKs and signing/data files stay private."""
from pathlib import Path
import re

ROOT_FILES = ['README.md', 'LICENSE', 'CONTRIBUTING.md', '.gitignore', '.gitattributes',
              'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties', 'gradlew', 'gradlew.bat',
              'app/build.gradle.kts', 'app/proguard-rules.pro']
PRIVATE_MARKERS = [re.compile(rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----'),
                   re.compile(rb'gh[pousr]_[A-Za-z0-9_]{20,}'),
                   re.compile(rb'github_pat_[A-Za-z0-9_]{20,}')]

def public_files(root: Path):
    files = [root / name for name in ROOT_FILES]
    for folder in ['.github', 'app/src', 'gradle/wrapper', 'docs', 'scripts']:
        for path in (root / folder).rglob('*'):
            if not path.is_file() or path.is_symlink() or '__pycache__' in path.parts:
                continue
            relative = path.relative_to(root)
            if relative.parts[:2] in [('docs', 'releases'), ('docs', 'store')]:
                continue
            if relative.parts[:2] == ('docs', 'screenshots') and path.name.startswith(('camera-', 'editor-')):
                continue
            files.append(path)
    for path in sorted(set(files)):
        if not path.is_file() or path.is_symlink():
            raise RuntimeError(f'Missing/unsafe public source: {path.relative_to(root)}')
        if path.suffix in ('.jks', '.keystore', '.p12', '.key', '.hprof', '.apk', '.aab', '.pyc') or path.name in ['local.properties', 'release-signing.properties', '.env']:
            raise RuntimeError('Private/generated file in public source: ' + str(path.relative_to(root)))
        data = path.read_bytes()
        if any(marker.search(data) for marker in PRIVATE_MARKERS):
            raise RuntimeError('Possible private key/token in public source: ' + str(path.relative_to(root)))
        yield path

if __name__ == '__main__':
    import hashlib
    import json
    root = Path(__file__).resolve().parents[1]
    print(json.dumps([{'path': str(path.relative_to(root)), 'bytes': path.stat().st_size,
                      'mode': '100755' if path.stat().st_mode & 0o111 else '100644',
                      'git_sha': hashlib.sha1(b'blob ' + str(path.stat().st_size).encode() + b'\0' + path.read_bytes()).hexdigest()}
                     for path in public_files(root)]))
