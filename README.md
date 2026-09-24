# RikkeiBank API - Ngân hàng số theo kiến trúc Microservice

Mini project môn Microservice. Hệ thống Backend cho ngân hàng bán lẻ RikkeiBank, chuyển từ mô hình
Monolithic sang Microservice: mỗi nghiệp vụ là một service độc lập, có cơ sở dữ liệu riêng, giao tiếp
qua API Gateway, Feign và Kafka.

## Thành viên và phân công

| Thành viên | Phụ trách |
|---|---|
| Lê Phú Toàn (trưởng nhóm) | Phân công, chốt quy ước chung, review và merge, tích hợp và kiểm thử toàn hệ thống |
| Lê Duy Minh | `identity-service`: đăng nhập, JWT, làm mới token, ADMIN ép đăng xuất |
| Phạm Phương Anh | `customer-service`: Khách hàng, Nhân viên, Redis cache |
| Trần Đăng Việt | `account-service`: Loại tài khoản, Tài khoản, trừ / cộng / hoàn tiền |
| Lê Phương Linh | `transaction-service`: chuyển khoản bằng Saga, hoàn tác, Circuit Breaker |
| Phùng Văn Vượng | Config Server, Eureka, Gateway, Docker, `notification-service` (WebFlux + Kafka) |

## Kiến trúc

| Service | Cổng | Cơ sở dữ liệu riêng |
|---|---|---|
| config-server | 8888 | - |
| eureka-server | 8761 | - |
| api-gateway | 8080 | - |
| identity-service | 8081 | rikkeibank_identity_db |
| customer-service | 8082 | rikkeibank_customer_db |
| account-service | 8083, 8093 (2 bản) | rikkeibank_account_db |
| transaction-service | 8084 | rikkeibank_transaction_db |
| notification-service | 8085 | rikkeibank_notification_db |

Hạ tầng chạy bằng Docker: MySQL (cổng 3307), Kafka + Zookeeper, Redis.

## Đáp ứng yêu cầu kỹ thuật

| Yêu cầu | Cách làm |
|---|---|
| Config Server | Cấu hình dùng chung đặt trong `config-repo/`, mọi service nạp khi khởi động |
| Eureka | Mọi service tự đăng ký; account-service chạy 2 bản để demo cân bằng tải |
| Gateway và LoadBalancer | Cổng vào duy nhất 8080, route `lb://`, kiểm tra JWT, chặn đường `/internal/**` |
| Feign | transaction-service gọi account-service, account-service gọi customer-service theo tên service |
| Circuit Breaker | Resilience4j trên các lời gọi Feign, đủ 3 trạng thái CLOSED, OPEN, HALF_OPEN, có fallback |
| Kafka + WebFlux | Topic `transaction-events`, `account-events`; notification-service nhận sự kiện bằng Reactor Kafka |
| Saga | Orchestrator cho chuyển khoản (transaction-service), Choreography cho thông báo (notification-service) |
| Redis Cache-Aside | `@Cacheable`, `@CachePut`, `@CacheEvict` cho khách hàng và loại tài khoản |
| JWT và phân quyền | identity-service cấp token; `@PreAuthorize` theo vai trò; sai token 401, sai quyền 403 |
| Xử lý lỗi | `@RestControllerAdvice` trả một cấu trúc lỗi JSON thống nhất |
| Kiểm thử | Unit test cho Service/Controller, Jacoco đo độ bao phủ |

## Luồng chuyển khoản (Saga Orchestrator)

```
1. Tạo giao dịch PENDING
2. Kiểm tra tài khoản nguồn thuộc đúng khách hàng, tài khoản đích tồn tại
3. Trừ tiền tài khoản nguồn
4. Cộng tiền tài khoản đích
   - Bước 4 lỗi -> HOÀN TÁC: cộng lại tiền cho tài khoản nguồn, giao dịch COMPENSATED,
     phát sự kiện TRANSFER_COMPENSATED lên Kafka
5. Thành công -> COMPLETED, ghi bút toán DEBIT / CREDIT, phát TRANSFER_COMPLETED
6. notification-service nhận sự kiện, gửi thông báo biến động số dư
```

Trừ và cộng tiền dùng câu lệnh cập nhật nguyên tử và chống xử lý trùng theo mã giao dịch, nên hai bản
account-service chạy song song vẫn không làm lệch số dư.

## Bảo mật và đăng nhập

- **Access token** sống 15 phút, **refresh token** sống 30 ngày: app khách hàng tự làm mới token, không
  bắt nhập lại mật khẩu.
- **ADMIN ép đăng xuất**: thu hồi mọi refresh token và ghi mốc thu hồi lên Redis; Gateway kiểm tra mốc
  này nên token cũ bị chặn ngay lập tức.
- **ADMIN** quản lý toàn bộ; **TELLER** chỉ xem giao dịch của chi nhánh mình và chỉ duyệt giao dịch được
  phân công cho mình; **CUSTOMER** chỉ xem tài khoản, giao dịch của chính mình; chưa đăng nhập thì 401.

## Kết quả

| Hạng mục | Kết quả |
|---|---|
| Build 8 module | Thành công |
| Unit test | 41 test, 0 lỗi |
| Postman qua Gateway | 82 request, 84 kiểm tra, 0 lỗi (chạy lại nhiều lần vẫn đạt) |
| Eureka | Đủ 7 instance, account-service có 2 bản |
| Chuyển khoản | Số dư tài khoản nguồn giảm đúng số tiền chuyển |
| Hoàn tác | 2 lần hoàn tác (tài khoản đích bị khóa, giả lập lỗi), số dư không lệch |
| Circuit Breaker | Mở sau 5 lần lỗi, từ chối ngay dưới 1 giây, tự về HALF_OPEN sau 10 giây, CLOSED sau 3 lần thăm dò |

Độ bao phủ Jacoco theo dòng: identity 72,6%, customer 72,4%, account 56,1%, transaction 54,6%,
notification 55,1%, gateway 87,0%. Báo cáo HTML tại `<module>/build/reports/jacoco/test/html/index.html`.

## Cách chạy

Yêu cầu: Java 17, Docker Desktop, Git Bash.

```bash
bash scripts/build-all.sh
bash scripts/start-infra.sh
bash scripts/start-all.sh
```

Dừng hệ thống:

```bash
bash scripts/stop-all.sh
bash scripts/stop-infra.sh
```

Dashboard Eureka: http://localhost:8761

## Demo bằng Postman

Import `postman/RikkeiBank-API.postman_collection.json`, chạy lần lượt các thư mục:

| Thư mục | Nội dung trình diễn |
|---|---|
| 1. Xác thực và người dùng | Đăng nhập, làm mới token, ADMIN ép đăng xuất khách hàng |
| 2. Khách hàng và nhân viên | CRUD, khách xem người khác bị 403, thống kê cache Redis |
| 3. Tài khoản | Loại tài khoản, mở / khóa / đóng tài khoản, cân bằng tải giữa 8083 và 8093 |
| 4. Chuyển khoản và Saga | Chuyển khoản thành công, 2 kịch bản hoàn tác, kiểm tra số dư không lệch |
| 5. Thông báo biến động | Thông báo nhận từ Kafka sau mỗi giao dịch |
| 6. Bảo mật và phân quyền | 401, 403, chặn API nội bộ |
| 7. Kháng lỗi Circuit Breaker | CLOSED, OPEN, HALF_OPEN, CLOSED khi tắt rồi bật lại account-service |

Tài khoản demo:

| Tên đăng nhập | Mật khẩu | Vai trò |
|---|---|---|
| admin | Admin@123 | ADMIN |
| teller.hn | Teller@123 | TELLER chi nhánh HN01 |
| an | Customer@123 | CUSTOMER, tài khoản 100000000001 |
| binh | Customer@123 | CUSTOMER, tài khoản 100000000002 |
