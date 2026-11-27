"""Package just the rebuildable source; writer excludes caches, models and signing material by construction."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import re
root=Path(__file__).resolve().parents[1]; dist=root/'distribution'; dist.mkdir(exist_ok=True)
fixed=['README.md','DESIGN.md','VERIFICATION.md','gradle.properties','build.gradle','settings.gradle','build-local.ps1','gradlew','gradlew.bat','.gitignore','.gitattributes',
       'app/build.gradle','ai/build.gradle','ai/README.md','ai/models/NOTICE.txt','ai/models/LICENSE-Qwen-Apache-2.0.txt','db/build.gradle','db/consumer-rules.pro','db/README.md',
       'ui-kit/package.json','ui-kit/package-lock.json','ui-kit/editor.js','ui-kit/vendor.cjs']
trees=['gradle','app/src','db/src','db/checks','ai/src','ai/tests','ai/tools','checks','third-party','docs']
files=[root/n for n in fixed]+[p for d in trees for p in (root/d).rglob('*') if p.is_file() and '__pycache__' not in p.parts and p.name!='CLEANUP.md' and p.suffix not in {'.gguf','.apk','.jks','.p12','.dpapi'}]
version=re.search(r"versionName\s+[']([^']+)[']",(root/'app/build.gradle').read_text()).group(1)
out=dist/f'FieldSQL-{version}-source.zip'
with ZipFile(out,'w',compression=ZIP_DEFLATED,compresslevel=6) as z:
 for p in files:z.write(p,'FieldSQL-Android/'+p.relative_to(root).as_posix())
with ZipFile(out) as z:
 names=set(z.namelist())
 assert all('FieldSQL-Android/'+n in names for n in ['gradle/wrapper/gradle-wrapper.jar','ui-kit/editor.js','ai/src/main/jniLibs/arm64-v8a/libfieldsql-runner.so','app/src/main/assets/editor-kit.js'])
 assert not any(n.endswith(('.jks','.p12','.gguf','local.properties')) for n in names)
print(out)
