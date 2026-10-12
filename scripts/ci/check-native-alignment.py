#!/usr/bin/env python3
"""Verify 16 KB load-segment alignment of every packaged 64-bit native library."""
from pathlib import Path
import struct
import sys
from zipfile import ZipFile

apk = Path(sys.argv[1])
checked = 0
with ZipFile(apk) as package:
    for name in package.namelist():
        if not name.startswith(('lib/arm64-v8a/', 'lib/x86_64/')) or not name.endswith('.so'):
            continue
        data = package.read(name)
        assert data[:5] == b'\x7fELF\x02' and data[5] in (1, 2), f'{name}: expected ELF64'
        order = '<' if data[5] == 1 else '>'
        offset = struct.unpack_from(order + 'Q', data, 32)[0]
        entry_size, count = struct.unpack_from(order + 'HH', data, 54)
        assert entry_size >= 56 and count > 0, f'{name}: invalid program headers'
        alignments = [
            struct.unpack_from(order + 'Q', data, offset + i * entry_size + 48)[0]
            for i in range(count)
            if struct.unpack_from(order + 'I', data, offset + i * entry_size)[0] == 1
        ]
        assert alignments and all(alignment >= 16384 for alignment in alignments), f'{name}: not 16 KB aligned: {alignments}'
        checked += 1
print(f'16 KB ELF alignment verified for {checked} native libraries in {apk.name}')
