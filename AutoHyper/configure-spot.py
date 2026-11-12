"""Write Linux Spot executable paths from the active environment to paths.json."""
import argparse
import json
import shutil
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--app-dir", type=Path, default=Path(__file__).resolve().parent / "app")
    args = parser.parse_args()
    paths = {name: shutil.which(name) for name in ("ltl2tgba", "autfilt")}
    missing = [name for name, path in paths.items() if not path]
    if missing:
        parser.error("Missing on PATH: " + ", ".join(missing) + "; activate your Spot environment first")
    target = args.app_dir / "paths.json"
    if not args.app_dir.is_dir():
        parser.error("App directory does not exist; publish AutoHyper first")
    # Preserve optional inclusion-checker settings when updating Spot paths.
    config = json.loads(target.read_text(encoding="utf-8-sig")) if target.exists() else {}
    config.update({name: str(Path(path).absolute()) for name, path in paths.items()})
    target.write_text(json.dumps(config, indent=2) + "\n", encoding="utf-8")
    print(target)
    print(json.dumps(config, indent=2))


if __name__ == "__main__":
    main()
