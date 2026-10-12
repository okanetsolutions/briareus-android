#!/usr/bin/env python3
"""Require actual instrumentation results; AGP can succeed after an install failure."""
import os
from pathlib import Path
import sys
from xml.etree import ElementTree as E

results = Path(sys.argv[1] if len(sys.argv) > 1 else 'app/build/outputs/androidTest-results/connected/debug')
assert (results / 'test-result-exit-code.txt').read_text().strip() == '0', 'Android test runner failed'
reports = list(results.glob('TEST-*.xml'))
assert reports, 'No instrumentation XML reports were produced'
cases = [case for report in reports for case in E.parse(report).getroot().iter('testcase')]
assert len(cases) >= 4, f'Expected at least four instrumentation tests, got {len(cases)}'
for case in cases:
    assert all(case.find(tag) is None for tag in ('failure', 'error', 'skipped')), f"Device test did not pass: {case.get('name')}"
summary = f'{len(cases)} native instrumentation tests passed.\n'
print(summary)
if os.environ.get('GITHUB_STEP_SUMMARY'):
    with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as output:
        output.write(summary)
