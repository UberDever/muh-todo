#!/usr/bin/env bash
# Private archive: includes the debug signing key. Never publish to a registry.
set -euo pipefail
umask 077
repo_root=$(cd -- "$(dirname -- "$0")/.." && pwd)
archive_root=${1:-"$repo_root/artifacts"}
mkdir -p -- "$archive_root"
archive_root=$(cd -- "$archive_root" && pwd)
context_root=$(mktemp -d /tmp/muh-todo-image.XXXXXX)
docker_config=$(mktemp -d /tmp/muh-todo-docker.XXXXXX)
export DOCKER_CONFIG=${DOCKER_CONFIG:-"$docker_config"}
container_name="muh-todo-offline-$$"
cleanup() {
    docker rm --force "$container_name" >/dev/null 2>&1 || true
    rm -rf -- "$context_root" "$docker_config"
}
trap cleanup EXIT
python3 - "$repo_root" "$context_root" <<'PY'
import os, pathlib, shutil, sys
repo, context = map(pathlib.Path, sys.argv[1:])
# Only dependency caches, never daemon logs, user config or credentials.
cache = pathlib.Path(os.environ.get('GRADLE_USER_HOME', '/workspace/.gradle'))
sdk = pathlib.Path(os.environ.get('ANDROID_HOME', '/workspace/.android-sdk'))
key = pathlib.Path(os.environ.get('MUH_TODO_DEBUG_KEYSTORE', '/workspace/.muh-todo/debug.keystore'))
if not key.is_file():
    raise SystemExit('Existing signing key required; source the build environment first.')
skip_source = shutil.ignore_patterns('.git', '.gradle', '.kotlin', '.superpowers', 'build', 'artifacts', 'local.properties', '*.jks', '*.keystore')
shutil.copytree(repo, context / 'source', ignore=skip_source)
shutil.copytree(sdk, context / 'toolchain/sdk')
for name in ['caches', 'wrapper/dists', 'robolectric-home']:
    shutil.copytree(cache / name, context / 'toolchain/gradle' / name, ignore=shutil.ignore_patterns('*.lock', '*.lck'))
shutil.copy2(key, context / 'toolchain/debug.keystore')
shutil.copy2(repo / 'build-support/Dockerfile', context / 'Dockerfile')
shutil.copy2(repo / '.dockerignore', context / '.dockerignore')
PY
image_name=muh-todo-build:0.1.0
docker build --platform linux/amd64 --network=none --tag "$image_name" "$context_root"
# Fresh container; no build outputs in the copied source and no network access.
docker run --name "$container_name" --network=none "$image_name" clean :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
docker cp "$container_name:/workspace/muh-todo/app/build/outputs/apk/debug/app-debug.apk" "$archive_root/markdown-todo-0.1.0.apk"
docker cp "$container_name:/workspace/muh-todo/app/build/test-results/testDebugUnitTest/." - | gzip -1 > "$archive_root/unit-test-results.tar.gz"
docker cp "$container_name:/workspace/muh-todo/app/build/reports/lint-results-debug.xml" "$archive_root/lint-results-debug.xml"
docker rm "$container_name"
docker image inspect "$image_name" --format '{{.Id}}' > "$archive_root/image-id.txt"
docker save "$image_name" | gzip -1 > "$archive_root/muh-todo-build-0.1.0.tar.gz"
cp -- "$repo_root/README.md" "$archive_root/README.md"
(cd -- "$archive_root" && sha256sum muh-todo-build-0.1.0.tar.gz markdown-todo-0.1.0.apk image-id.txt README.md unit-test-results.tar.gz lint-results-debug.xml > SHA256SUMS)
printf 'Verified offline build and private archive saved to %s\n' "$archive_root"
