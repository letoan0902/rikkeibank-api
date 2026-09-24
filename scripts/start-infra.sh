#!/usr/bin/env bash
# Khởi động hạ tầng Docker và chờ sẵn sàng
export MSYS_NO_PATHCONV=1
cd "$(dirname "$0")/.." || exit 1

docker compose up -d || exit 1

wait_for() {
  local name="$1" max="$2"; shift 2
  echo -n "Chờ $name "
  for ((i = 1; i <= max; i++)); do
    if "$@" > /dev/null 2>&1; then
      echo " OK"
      return 0
    fi
    echo -n "."
    sleep 2
  done
  echo " HẾT THỜI GIAN"
  return 1
}

kafka_topics_ready() {
  local n
  n=$(docker exec rikkeibank-kafka kafka-topics --bootstrap-server localhost:29092 --list 2>/dev/null \
      | tr -d '\r' | grep -cE '^(transaction-events|account-events)$')
  [ "$n" -eq 2 ]
}

redis_ready() {
  docker exec rikkeibank-redis redis-cli ping 2>/dev/null | grep -q PONG
}

mysql_ready() {
  docker exec rikkeibank-mysql mysqladmin ping -h localhost -uroot -p123456 --silent
}

wait_for "Kafka (2 topic)" 90 kafka_topics_ready || exit 1
wait_for "Redis" 30 redis_ready || exit 1
wait_for "MySQL" 90 mysql_ready || exit 1

echo "Hạ tầng đã sẵn sàng"
docker compose ps
