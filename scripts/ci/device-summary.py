#!/usr/bin/env python3
"""Require completed native tests; adb itself can exit zero after runner failure."""
import os
from pathlib import Path
import re
import sys

result = Path(sys.argv[1] if len(sys.argv) > 1 else 'device-instrumentation.txt').read_text()
lines = result.splitlines()
codes = [int(line.partition(':')[2]) for line in lines if line.startswith('INSTRUMENTATION_STATUS_CODE:')]
assert codes and all(code in (0, 1) for code in codes), 'Instrumentation reported a failed or skipped test'
passed = codes.count(0)
assert passed >= 4, f'Expected at least four completed instrumentation tests, got {passed}'
assert 'INSTRUMENTATION_CODE: -1' in lines, 'Instrumentation did not finish successfully'
assert re.search(r'(?m)^OK \(\d+ tests?\)\s*$', result), 'JUnit did not report success'
summary = f'{passed} native instrumentation tests passed.\n'
print(summary)
if os.environ.get('GITHUB_STEP_SUMMARY'):
    with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as output:
        output.write(summary)
