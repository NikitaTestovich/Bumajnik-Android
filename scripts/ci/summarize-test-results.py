#!/usr/bin/env python3
"""Summarize JUnit XML and reject failures, unexpected skips, and missing results."""
import argparse
import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--results', required=True, type=Path)
    parser.add_argument('--allowed-skips', required=True, type=Path)
    parser.add_argument('--profile', required=True)
    parser.add_argument('--expected-class')
    parser.add_argument('--summary', required=True, type=Path)
    args = parser.parse_args()
    try:
        allowed = json.loads(args.allowed_skips.read_text(encoding='utf-8'))
        if not isinstance(allowed, list):
            raise ValueError('allow list must be a list')
        permitted = set()
        for entry in allowed:
            if not all(isinstance(entry.get(key), str) and entry[key].strip() for key in ('classname', 'name', 'profile', 'reason')):
                raise ValueError('allow list entry requires classname, name, profile and reason')
            permitted.add((entry['classname'], entry['name'], entry['profile']))
        files = list(args.results.rglob('*.xml'))
        if not files:
            raise ValueError('no XML reports found')
        count = failures = skips = expected_count = 0
        issues = []
        for path in files:
            root = ET.parse(path).getroot()
            if root.tag not in ('testsuite', 'testsuites'):
                raise ValueError(f'{path}: not a JUnit report')
            for case in root.iter('testcase'):
                count += 1
                cls, name = case.get('classname'), case.get('name')
                if not cls or not name:
                    raise ValueError(f'{path}: testcase missing identity')
                if cls == args.expected_class:
                    expected_count += 1
                failure = case.find('failure')
                assumption = failure is not None and (failure.text or '').lstrip().startswith('org.junit.AssumptionViolatedException:')
                if (failure is not None and not assumption) or case.find('error') is not None:
                    failures += 1
                    issues.append(f'FAIL {cls}#{name}')
                if case.find('skipped') is not None or assumption:
                    skips += 1
                    if (cls, name, args.profile) not in permitted:
                        issues.append(f'Unexpected skip {cls}#{name}')
        if not count:
            raise ValueError('reports contain no test cases')
        if args.expected_class and not expected_count:
            raise ValueError(f'expected test class not found: {args.expected_class}')
        summary = f'Tests: {count}; failures: {failures}; skipped: {skips}\n' + '\n'.join(issues) + '\n'
        args.summary.write_text(summary, encoding='utf-8')
        print(summary)
        return 1 if issues else 0
    except (OSError, ValueError, TypeError, AttributeError, ET.ParseError) as exc:
        print(f'Invalid test results: {exc}', file=sys.stderr)
        return 2


if __name__ == '__main__':
    sys.exit(main())
