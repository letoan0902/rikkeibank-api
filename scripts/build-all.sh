#!/usr/bin/env bash
# Build toàn bộ module bằng Gradle Wrapper, tham số thêm được truyền cho gradle (vd: -x test)
cd "$(dirname "$0")/.." || exit 1

./gradlew clean build "$@" || exit 1

echo "Các jar đã build:"
ls -1 */build/libs/*.jar
