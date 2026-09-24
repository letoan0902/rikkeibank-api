#!/usr/bin/env bash
# Dừng hạ tầng Docker (giữ volume MySQL)
cd "$(dirname "$0")/.." || exit 1
docker compose down
