import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).with_name('summarize-test-results.py')


class SummaryTests(unittest.TestCase):
    def run_case(self, xml, allowed=None):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            if xml is not None:
                (root / 'results.xml').write_text(xml, encoding='utf-8')
            allowed_file = root / 'allow.json'
            allowed_file.write_text(json.dumps(allowed or []), encoding='utf-8')
            return subprocess.run([sys.executable, str(SCRIPT), '--results', str(root),
                                   '--allowed-skips', str(allowed_file), '--profile', 'pr',
                                   '--summary', str(root / 'summary.md')], capture_output=True, text=True)

    def test_success(self):
        self.assertEqual(0, self.run_case('<testsuite><testcase classname="A" name="ok"/></testsuite>').returncode)

    def test_failure_and_empty_or_broken_results_fail(self):
        for xml in ('<testsuite><testcase classname="A" name="bad"><failure/></testcase></testsuite>', None, '<oops'):
            self.assertNotEqual(0, self.run_case(xml).returncode)

    def test_skip_requires_exact_match_and_profile(self):
        xml = '<testsuite><testcase classname="A" name="special"><skipped/></testcase></testsuite>'
        self.assertNotEqual(0, self.run_case(xml).returncode)
        self.assertEqual(0, self.run_case(xml, [{'classname': 'A', 'name': 'special', 'profile': 'pr', 'reason': 'requires setup'}]).returncode)
        self.assertNotEqual(0, self.run_case(xml, [{'classname': 'A', 'name': 'special', 'profile': 'other', 'reason': 'requires setup'}]).returncode)

    def test_multiple_suites_and_error(self):
        xml = '<testsuites><testsuite><testcase classname="A" name="ok"/></testsuite><testsuite><testcase classname="B" name="bad"><error/></testcase></testsuite></testsuites>'
        self.assertNotEqual(0, self.run_case(xml).returncode)

    def test_android_assumption_is_allowed_only_for_exact_test(self):
        xml = '<testsuite><testcase classname="A" name="conditional"><failure>org.junit.AssumptionViolatedException: requires setup</failure></testcase></testsuite>'
        self.assertNotEqual(0, self.run_case(xml).returncode)
        self.assertEqual(0, self.run_case(xml, [{'classname': 'A', 'name': 'conditional', 'profile': 'pr', 'reason': 'setup'}]).returncode)
        real_failure = '<testsuite><testcase classname="A" name="conditional"><failure>java.lang.AssertionError: org.junit.AssumptionViolatedException</failure></testcase></testsuite>'
        self.assertNotEqual(0, self.run_case(real_failure, [{'classname': 'A', 'name': 'conditional', 'profile': 'pr', 'reason': 'setup'}]).returncode)

    def test_wrong_class_does_not_count_as_expected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / 'report.xml').write_text('<testsuite><testcase classname="Wrong" name="ok"/></testsuite>')
            (root / 'allowed.json').write_text('[]')
            result = subprocess.run([sys.executable, str(SCRIPT), '--results', str(root), '--allowed-skips',
                                     str(root / 'allowed.json'), '--profile', 'pr', '--summary',
                                     str(root / 'summary.md'), '--expected-class', 'Right'], capture_output=True)
            self.assertNotEqual(0, result.returncode)
            (root / 'report.xml').write_text('<testsuite><testcase classname="Right" name="ok"/></testsuite>')
            result = subprocess.run([sys.executable, str(SCRIPT), '--results', str(root), '--allowed-skips',
                                     str(root / 'allowed.json'), '--profile', 'pr', '--summary',
                                     str(root / 'summary.md'), '--expected-class', 'Right'], capture_output=True)
            self.assertEqual(0, result.returncode)


if __name__ == '__main__':
    unittest.main()
