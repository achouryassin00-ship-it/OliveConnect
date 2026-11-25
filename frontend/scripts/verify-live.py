"""Run opt-in live checks against a dedicated MongoDB database; no production data touched.
Requires the packaged backend jar, JAVA_HOME (21+) and MongoDB on localhost:27017.
Credentials are generated per run in process memory. No tokens/passwords are logged.
"""
import os
from pathlib import Path
import secrets
import socket
import subprocess
import time
import urllib.request

frontend = Path(__file__).resolve().parents[1]
root = frontend.parent
java_home = os.environ.get('JAVA_HOME')
if not java_home:
    raise SystemExit('Set JAVA_HOME to JDK 21+ first.')
jar = root / 'backend/target/oliveconnect-backend-0.1.0-SNAPSHOT.jar'
if not jar.exists():
    raise SystemExit('Package backend first: mvnw.cmd -B -ntp package -DskipTests')
with socket.socket() as probe:
    if probe.connect_ex(('127.0.0.1', 8085)) == 0:
        raise SystemExit('Port 8085 is already in use; existing process left untouched.')
env = dict(os.environ)
database = 'oliveconnect_j04_qa_' + secrets.token_hex(4)
env.update(SERVER_PORT='8085', SPRING_PROFILES_ACTIVE='dev', SPRING_DATA_MONGODB_DATABASE=database, MONGODB_URI='mongodb://127.0.0.1:27017/' + database, SEED_ENABLED='true', JWT_SECRET=secrets.token_hex(32))
env['SEED_DEFAULT_PASSWORD'] = secrets.token_urlsafe(24)
print('Isolated QA database (retained for inspection): ' + database, flush=True)
env['J04_TEST_PASSWORD'] = env['SEED_DEFAULT_PASSWORD']
env['CORS_ALLOWED_ORIGINS'] = 'http://127.0.0.1:5174'
(root / 'tmp').mkdir(exist_ok=True)
with (root / 'tmp/j04-live-backend.log').open('w', encoding='utf-8') as log:
    proc = subprocess.Popen([str(Path(java_home) / 'bin/java.exe'), '-jar', str(jar)], cwd=root / 'backend', env=env, stdout=log, stderr=subprocess.STDOUT, creationflags=getattr(subprocess, 'CREATE_NO_WINDOW', 0))
    try:
        ready = False
        for _ in range(60):
            if proc.poll() is not None:
                break
            try:
                with urllib.request.urlopen('http://127.0.0.1:8085/api/health', timeout=1) as response:
                    ready = response.status == 200
                if ready:
                    break
            except OSError:
                pass
            time.sleep(1)
        if not ready:
            raise SystemExit('Backend unavailable. Inspect tmp/j04-live-backend.log (no credential output).')
        result = subprocess.run(['cmd.exe', '/c', 'npm.cmd', 'exec', '--', 'playwright', 'test', '--config', 'playwright.live.config.ts'], cwd=frontend, env=env)
        raise SystemExit(result.returncode)
    finally:
        proc.terminate()
        try:
            proc.wait(timeout=10)
        except subprocess.TimeoutExpired:
            proc.kill()
            proc.wait()
