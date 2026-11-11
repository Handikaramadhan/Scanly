from pathlib import Path
import re

configuration=(Path(__file__).resolve().parents[1] / 'app/build.gradle.kts').read_text()
VERSION=re.search(r'versionName\s*=\s*"([^"]+)"',configuration).group(1)
VERSION_CODE=int(re.search(r'versionCode\s*=\s*(\d+)',configuration).group(1))
