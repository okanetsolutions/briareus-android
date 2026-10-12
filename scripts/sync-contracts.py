#!/usr/bin/env python3
"""Copy a reviewed contracts checkout, or verify the checked-in snapshot offline."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]
TOKEN = Path('app/src/main/kotlin/com/okanetsolutions/briareus/tokens/BriareusTokens.kt')


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, help='Reviewed briareus-contracts checkout; omit to check offline')
    options = parser.parse_args()
    lock_path = ROOT / 'contracts.lock.json'
    lock = json.loads(lock_path.read_text())
    if options.source:
        source = options.source.resolve()
        revision = subprocess.check_output(['git', '-C', str(source), 'rev-parse', 'HEAD'], text=True).strip()
        if subprocess.check_output(['git', '-C', str(source), 'status', '--porcelain'], text=True).strip():
            raise SystemExit('Commit the contracts changes before copying them.')
        shutil.copy2(source / 'dist/kotlin/BriareusTokens.kt', ROOT / TOKEN)
        wireframes = ROOT / 'docs/wireframes'
        if wireframes.exists():
            shutil.rmtree(wireframes)
        shutil.copytree(source / 'wireframes', wireframes)
        shutil.copy2(source / 'LICENSE', wireframes / 'LICENSE')
        tokens = json.loads((source / 'dist/json/briareus-tokens.json').read_text())
        resources = []
        for mode, folder in [('light', 'values'), ('dark', 'values-night')]:
            path = Path('app/src/main/res') / folder / 'colors.xml'
            (ROOT / path).parent.mkdir(parents=True, exist_ok=True)
            canvas = tokens['theme'][mode]['canvas']
            (ROOT / path).write_text(
                '<?xml version="1.0" encoding="utf-8"?>\n'
                '<!-- Generated from briareus-contracts by scripts/sync-contracts.py. -->\n'
                f'<resources>\n  <color name="canvas">{canvas}</color>\n</resources>\n'
            )
            resources.append(path)
        files = [TOKEN, *resources, *(p.relative_to(ROOT) for p in wireframes.iterdir() if p.is_file())]
        lock['contracts']['revision'] = revision
        lock['contracts'].pop('tokens_sha256', None)
        lock['contracts']['files'] = {str(p): digest(ROOT / p) for p in sorted(files)}
        lock_path.write_text(json.dumps(lock, indent=2) + '\n')
    for name, expected in lock['contracts']['files'].items():
        path = ROOT / name
        if not path.is_file() or digest(path) != expected:
            raise SystemExit(f'Contract snapshot changed: {name}. Run sync-contracts.py --source with a reviewed checkout.')
    print(f"Contracts {lock['contracts']['revision']}: {len(lock['contracts']['files'])} files verified")


if __name__ == '__main__':
    main()
