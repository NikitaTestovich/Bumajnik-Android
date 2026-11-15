import os
import subprocess
import unittest
from pathlib import Path


@unittest.skipUnless(os.name == 'nt', 'Windows PowerShell and where.exe required')
class BiometricDriverTests(unittest.TestCase):
    def test_empty_instrumentation_output_is_failure(self):
        root = Path(__file__).resolve().parents[2]
        driver = root / 'scripts' / 'test-biometric-prompt.ps1'
        result = subprocess.run(['powershell.exe', '-NoProfile', '-ExecutionPolicy', 'Bypass',
                                 '-File', str(driver), '-Adb', r'C:\Windows\System32\where.exe',
                                 '-Serial', 'emulator-5554', '-DevicePin', '4826'],
                                capture_output=True, text=True, timeout=30)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('Biometric prompt acceptance did not pass', result.stderr)


if __name__ == '__main__':
    unittest.main()
