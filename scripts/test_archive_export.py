#!/usr/bin/env python3
"""Regression check for archive APK provenance; Docker is simulated, not the build."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


class ArchiveExportTest(unittest.TestCase):
    def test_exports_verified_container_apk_even_without_host_outputs(self):
        for host_apk in (None, b'stale-host-apk'):
            with self.subTest(host_apk=host_apk), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                repo = root / 'repo'
                (repo / 'scripts').mkdir(parents=True)
                shutil.copy2(Path(__file__).with_name('archive-build-environment.sh'), repo / 'scripts/archive-build-environment.sh')
                (repo / 'build-support').mkdir()
                (repo / 'build-support/Dockerfile').write_text('FROM scratch\n')
                (repo / '.dockerignore').write_text('')
                (repo / 'README.md').write_text('fixture')
                if host_apk:
                    output = repo / 'app/build/outputs/apk/debug/app-debug.apk'
                    output.parent.mkdir(parents=True)
                    output.write_bytes(host_apk)
                sdk, cache, key = root / 'sdk', root / 'cache', root / 'test.keystore'
                sdk.mkdir()
                for name in ['caches', 'wrapper/dists', 'robolectric-home']:
                    (cache / name).mkdir(parents=True)
                key.write_text('test fixture, not a signing key')
                commands = root / 'bin'
                commands.mkdir()
                docker = commands / 'docker'
                docker.write_text('''#!/usr/bin/env python3
import pathlib, sys
args = sys.argv[1:]
if args[0] == 'cp':
    if args[1].endswith('/app/build/outputs/apk/debug/app-debug.apk'):
        pathlib.Path(args[2]).write_bytes(b'verified-container-apk')
    elif args[2] == '-':
        print('fixture test reports tar')
    else:
        pathlib.Path(args[2]).write_text('fixture lint report')
elif args[:2] == ['image', 'inspect']:
    print('sha256:fixture')
elif args[0] == 'save':
    print('fixture image archive')
''')
                docker.chmod(0o755)
                env = dict(os.environ, PATH=str(commands) + os.pathsep + os.environ['PATH'],
                           ANDROID_HOME=str(sdk), GRADLE_USER_HOME=str(cache), MUH_TODO_DEBUG_KEYSTORE=str(key))
                out = root / 'archive'
                result = subprocess.run([str(repo / 'scripts/archive-build-environment.sh'), str(out)],
                                        env=env, capture_output=True, text=True)
                self.assertEqual(0, result.returncode, result.stderr)
                self.assertEqual(b'verified-container-apk', (out / 'markdown-todo-0.1.0.apk').read_bytes())
                self.assertIn('markdown-todo-0.1.0.apk', (out / 'SHA256SUMS').read_text())


if __name__ == '__main__':
    unittest.main()
