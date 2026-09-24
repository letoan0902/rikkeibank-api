#!/usr/bin/env bash
# Dừng các service theo cổng (Windows, Git Bash)
PORTS="8080 8085 8084 8093 8083 8082 8081 8761 8888"

find_pids() {
  netstat -ano | tr -d '\r' | awk -v p=":$1" '
    $1 == "TCP" && $4 == "LISTENING" && substr($2, length($2) - length(p) + 1) == p { print $5 }
  ' | sort -u
}

for port in $PORTS; do
  pids=$(find_pids "$port")
  if [ -z "$pids" ]; then
    echo "Cổng $port: không có tiến trình"
    continue
  fi
  for pid in $pids; do
    [ "$pid" = "0" ] && continue
    echo "Cổng $port: dừng PID $pid"
    powershell -Command "Stop-Process -Id $pid -Force" > /dev/null 2>&1
  done
done

sleep 2
still=""
for port in $PORTS; do
  [ -n "$(find_pids "$port")" ] && still="$still $port"
done
if [ -n "$still" ]; then
  echo "Vẫn còn cổng đang mở:$still"
  exit 1
fi
echo "Đã dừng toàn bộ service"
