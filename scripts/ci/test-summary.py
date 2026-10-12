#!/usr/bin/env python3
"""Fail on missing or failed JVM test reports and publish their actual totals."""
import os
from pathlib import Path
from xml.etree import ElementTree as E

reports = list(Path('core/build/test-results/test').glob('TEST-*.xml'))
assert reports, 'No core test reports were produced'
suites = [E.parse(path).getroot() for path in reports]
tests = sum(int(suite.get('tests', 0)) for suite in suites)
failed = sum(int(suite.get('failures', 0)) + int(suite.get('errors', 0)) for suite in suites)
assert tests > 0, 'No core tests ran'
summary = f'{tests - failed}/{tests} core tests passed.\n'
coverage = E.parse('core/build/reports/jacoco/test/jacocoTestReport.xml').getroot()
for counter in coverage.findall('counter'):
    if counter.get('type') in ('LINE', 'BRANCH'):
        covered, missed = int(counter.get('covered')), int(counter.get('missed'))
        summary += f"{counter.get('type').lower()} coverage: {covered}/{covered + missed}.\n"
print(summary)
if os.environ.get('GITHUB_STEP_SUMMARY'):
    with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as output:
        output.write(summary)
assert failed == 0, f'{failed} core tests failed'
