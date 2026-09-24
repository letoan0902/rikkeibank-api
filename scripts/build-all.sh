#!/usr/bin/env bash
# Build toàn bộ module, tham số thêm được truyền cho gradle (vd: -x test)
cd "$(dirname "$0")/.." || exit 1

GRADLE_BIN="${GRADLE_BIN:-}"
if [ -z "$GRADLE_BIN" ]; then
  if command -v gradle > /dev/null 2>&1; then
    GRADLE_BIN=gradle
  else
    GRADLE_BIN=/c/Users/gigabyte/.gradle/wrapper/dists/gradle-8.14.3-bin/cv11ve7ro1n3o1j4so8xd9n66/gradle-8.14.3/bin/gradle
  fi
fi

"$GRADLE_BIN" clean build "$@" || exit 1

echo "Các jar đã build:"
ls -1 */build/libs/*.jar
