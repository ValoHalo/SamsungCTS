"""Check the shipped APK for the module artifacts intentionally removed by this fork."""
from pathlib import Path
import sys
import zipfile

FORBIDDEN = (
    b"org/lsposed/", b"org.lsposed.", b"io/github/libxposed/", b"io.github.libxposed.",
    b"de/robv/android/xposed/", b"de.robv.android.xposed.", b"META-INF/xposed/",
    b"HiddenApiBypass", b"XposedModule", b"com/topjohnwu/", b"com.topjohnwu.",
)

def inspect(path: Path) -> None:
    findings = []
    with zipfile.ZipFile(path) as archive:
        for info in archive.infolist():
            name = info.filename
            if any(marker.lower() in name.lower().encode() for marker in FORBIDDEN):
                findings.append(name)
            if name.endswith((".dex", ".xml", ".arsc", ".list", ".prop")):
                data = archive.read(info)
                for marker in FORBIDDEN:
                    if marker in data or marker.decode().encode("utf-16le") in data:
                        findings.append(f"{name}: {marker.decode()}")
    if findings:
        raise SystemExit("Removed artifacts found:\n" + "\n".join(findings))
    print(f"PASS: {path.name} contains none of the checked module artifacts")

if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("Usage: check_apk.py path/to/app.apk")
    inspect(Path(sys.argv[1]))
