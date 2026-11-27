"""Prepare FieldSQL native libraries from the upstream b12774 Android arm64 archive.
Usage: pip install lief==1.0.0
       python tools/prepare_native.py path/to/llama-b12774-bin-android-arm64.tar.gz
"""
from pathlib import Path
import sys
import tarfile
import lief

DEST=Path(__file__).resolve().parents[1]/"src/main/jniLibs/arm64-v8a"
FILES={"llama-completion":"libfieldsql-runner.so", **{name:name for name in (
    "libllama-completion-impl.so","libllama-common.so","libllama.so","libggml.so","libggml-base.so",
    "libggml-cpu-android_armv8.0_1.so","libggml-cpu-android_armv8.2_1.so","libggml-cpu-android_armv8.2_2.so"
)}}
if __name__=="__main__":
    DEST.mkdir(parents=True,exist_ok=True)
    with tarfile.open(sys.argv[1],mode="r:gz") as ar:
        for origin,name in FILES.items():
            data=ar.extractfile("llama-b12774/"+origin).read()
            elf=lief.ELF.parse(data)
            elf.add(lief.ELF.DynamicEntryRunPath("$ORIGIN"))
            (DEST/name).write_bytes(bytes(elf.write_to_bytes()))
