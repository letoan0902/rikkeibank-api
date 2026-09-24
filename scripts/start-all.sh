#!/usr/bin/env bash
# Chạy toàn bộ service theo thứ tự, log ghi vào run/
cd "$(dirname "$0")/.." || exit 1
mkdir -p run

JAVA_OPTS="-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"

http_code() {
  curl -s -o /dev/null -w "%{http_code}" --max-time 3 "$1"
}

wait_url() {
  local name="$1" url="$2" max="${3:-90}"
  echo -n "Chờ $name ($url) "
  for ((i = 1; i <= max; i++)); do
    if [ "$(http_code "$url")" = "200" ]; then
      echo " OK"
      return 0
    fi
    echo -n "."
    sleep 2
  done
  echo " LỖI, xem log trong run/"
  return 1
}

# start_service <module> <cổng> <tên log> [tham số thêm]
start_service() {
  local module="$1" port="$2" logname="$3"; shift 3
  local jar="$module/build/libs/$module.jar"
  if [ ! -f "$jar" ]; then
    echo "Không thấy $jar, hãy chạy scripts/build-all.sh trước"
    exit 1
  fi
  if [ "$(http_code "http://localhost:$port/actuator/health")" = "200" ]; then
    echo "$module:$port đang chạy, bỏ qua"
    return 0
  fi
  echo "Khởi động $module cổng $port -> run/$logname.log"
  nohup java $JAVA_OPTS -jar "$jar" "$@" > "run/$logname.log" 2>&1 &
}

# 1. config-server
if [ "$(http_code http://localhost:8888/application/default)" != "200" ]; then
  echo "Khởi động config-server cổng 8888 -> run/config-server.log"
  nohup java $JAVA_OPTS -jar config-server/build/libs/config-server.jar > run/config-server.log 2>&1 &
fi
wait_url "config-server" "http://localhost:8888/application/default" || exit 1

# 2. eureka-server
start_service eureka-server 8761 eureka-server
wait_url "eureka-server" "http://localhost:8761/actuator/health" || exit 1

# 3. các service nghiệp vụ
start_service identity-service 8081 identity-service
wait_url "identity-service" "http://localhost:8081/actuator/health" || exit 1

start_service customer-service 8082 customer-service
wait_url "customer-service" "http://localhost:8082/actuator/health" || exit 1

start_service account-service 8083 account-service-8083
wait_url "account-service:8083" "http://localhost:8083/actuator/health" || exit 1

start_service account-service 8093 account-service-8093 --server.port=8093
wait_url "account-service:8093" "http://localhost:8093/actuator/health" || exit 1

start_service transaction-service 8084 transaction-service
wait_url "transaction-service" "http://localhost:8084/actuator/health" || exit 1

start_service notification-service 8085 notification-service
wait_url "notification-service" "http://localhost:8085/actuator/health" || exit 1

# 4. gateway
start_service api-gateway 8080 api-gateway
wait_url "api-gateway" "http://localhost:8080/actuator/health" || exit 1

# 5. chờ Eureka có đủ 7 instance
echo -n "Chờ Eureka đủ 7 instance "
for ((i = 1; i <= 60; i++)); do
  count=$(curl -s --max-time 3 -H "Accept: application/json" http://localhost:8761/eureka/apps \
          | grep -o '"instanceId"' | wc -l)
  if [ "$count" -ge 7 ]; then
    echo " OK ($count instance)"
    echo "Hệ thống đã sẵn sàng tại http://localhost:8080"
    exit 0
  fi
  echo -n "."
  sleep 2
done
echo " chỉ có $count instance, kiểm tra http://localhost:8761"
exit 1
