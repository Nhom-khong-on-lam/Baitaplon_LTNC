#  Auction System — Hệ Thống Đấu Giá Trực Tuyến

> Bài tập lớn môn Lập Trình Nâng Cao — Mô hình Client–Server, giao tiếp TCP Socket, giao diện JavaFX.

---

## Mô Tả Bài Toán

Hệ thống cho phép người dùng đăng ký tài khoản, đăng sản phẩm lên đấu giá (Electronics, Art, Vehicle), đặt giá thầu theo thời gian thực, và thanh toán sau khi phiên kết thúc. Admin duyệt phiên đấu giá và quản lý người dùng.

Giao tiếp qua **TCP Socket** (port `8080`) để truyền dữ liệu và **UDP Broadcast** (port `8888`) để Client tự phát hiện Server trong mạng LAN.

---

## Công Nghệ & Môi Trường

| Thành phần       | Chi tiết                                      |
|------------------|-----------------------------------------------|
| Ngôn ngữ         | Java 21                                       |
| Giao diện        | JavaFX 21.0.6 + FXML                          |
| Build tool       | Apache Maven (multi-module)                   |
| Cơ sở dữ liệu    | MySQL — TiDB Cloud (`auction_db`)             |
| Connection pool  | HikariCP 5.1.0                                |
| Lưu ảnh          | Cloudinary (cloudinary-http44 1.36.0)         |
| Mã hóa mật khẩu  | jBCrypt 0.4                                   |
| Kiểm thử         | JUnit 5.10 + Mockito 5.x                      |

**Yêu cầu cài đặt:**
- JDK 21+
- Apache Maven 3.8+
- Kết nối Internet (TiDB Cloud) hoặc MySQL local

---

## Cấu Trúc Thư Mục

```text
Baitaplon_LTNC/
├── pom.xml                          ← Parent POM (multi-module)
│
├── Common/                          ← Dùng chung giữa Server & Client
│   └── src/main/java/com/auction/common/
│       ├── dto/                     ← AuctionDTO, BidDTO, UserDTO, PaymentDTO, ...
│       ├── enums/                   ← AuctionStatus, Category, SystemRole, ...
│       ├── model/                   ← Auction, Item, User, BidTransaction, ...
│       ├── network/                 ← Request.java, Response.java
│       └── observer/                ← Observer pattern (AuctionObserver)
│
├── Server/                          ← Xử lý logic & database
│   └── src/main/java/server/
│       ├── network/
│       │   ├── AuctionServer.java   ← Điểm khởi động Server
│       │   └── ClientHandler.java   ← Xử lý từng kết nối Client
│       ├── repository/              ← AuctionDAO, BidDAO, UserDAO, PaymentDAO, ...
│       ├── database/DBConnection.java
│       └── resources/db.properties  ← Cấu hình kết nối DB
│
├── Client/                          ← Giao diện JavaFX
│   └── src/main/java/com/auction/client/
│       ├── Launcher.java            ← Entry point
│       ├── MainApp.java
│       ├── controller/              ← Controller cho từng màn hình
│       ├── service/                 ← AuctionService, AuthService, ServerConnection, ...
│       ├── factory/                 ← ItemFactory (Art, Electronics, Vehicle)
│       └── manager/                 ← AuctionManager, SceneManager, SessionManager
│   └── src/main/resources/com/auction/client/
│       ├── *.fxml                   ← Giao diện từng màn hình
│       └── global.css
│
├── dist/                            ← Thư mục chứa file thực thi (.jar)
│   ├── Client-1.0-SNAPSHOT.jar
│   ├── Common-1.0-SNAPSHOT.jar
│   └── Server-1.0-SNAPSHOT.jar
│
└── diagram/
    ├── database.sql                 ← Script tạo CSDL
    ├── auction_db_diagram.png       ← Sơ đồ quan hệ bảng
    ├── ClientDiagram.png            ← Sơ đồ kiến trúc Client
    └── ServerDiagram.png            ← Sơ đồ kiến trúc Server
```

---

## Vị Trí File `.jar`

Sau khi build bằng Maven, các file `.jar` thành phẩm nằm tại thư mục `dist/` ở gốc dự án:

| Module    | Đường dẫn                               |
|-----------|-----------------------------------------|
| `Common`  | `dist/Common-1.0-SNAPSHOT.jar`          |
| `Server`  | `dist/Server-1.0-SNAPSHOT.jar`          |
| `Client`  | `dist/Client-1.0-SNAPSHOT.jar`          |

---

##  Hướng Dẫn Chạy

### Chạy Server *(chạy trước)*

Từ thư mục gốc dự án, chạy:

```bash
java -jar dist/Server-1.0-SNAPSHOT.jar
```

Khi thành công, console hiển thị:
```
? [TCP Server] Listening on port: 8080
? [UDP Beacon] Broadcasting for background clients every second...
```

---

### Chạy Client *(sau khi Server sẵn sàng)*

Mở terminal khác từ thư mục gốc, chạy:

```bash
java -jar dist/Client-1.0-SNAPSHOT.jar
```

> Client tự phát hiện IP Server qua UDP Broadcast trong mạng LAN.

---

## Danh Sách Chức Năng Đã Hoàn Thành

**Người dùng**
- [x] Đăng ký / Đăng nhập / Đăng xuất
- [x] Xem & chỉnh sửa thông tin cá nhân
- [x] Cập nhật thông tin ngân hàng (số tài khoản, tên ngân hàng)
- [x] Nạp số dư tài khoản

**Sản phẩm & Đấu giá**
- [x] Đăng sản phẩm lên đấu giá (3 loại: Electronics, Art, Vehicle)
- [x] Xem danh sách phiên đấu giá đang mở / đang diễn ra
- [x] Xem chi tiết phiên đấu giá (giá hiện tại, thời gian còn lại, lịch sử đặt giá)
- [x] Đặt giá thầu thủ công (manual bid)
- [x] Đặt giá thầu tự động (auto bid — tự động tăng đến mức tối đa)
- [x] Theo dõi phiên đấu giá (watch/unwatch)
- [x] Xem danh sách sản phẩm đã đăng & giá thầu của tôi

**Thanh toán & Thông báo**
- [x] Xử lý thanh toán sau khi phiên đấu giá kết thúc
- [x] Hệ thống thông báo trong app


**Quản trị (Admin)**
- [x] Dashboard thống kê (người dùng, phiên đấu giá, doanh thu)
- [x] Duyệt / Từ chối phiên đấu giá chờ duyệt
- [x] Quản lý toàn bộ phiên đấu giá
- [x] Quản lý người dùng (khoá / mở tài khoản)

**Kiến trúc**
- [x] TCP Socket + UDP Broadcast tự phát hiện Server trong LAN
- [x] Design Pattern: Singleton, Factory Method, Observer
- [x] Connection Pool với HikariCP
- [x] Upload ảnh sản phẩm lên Cloudinary
- [x] Unit Test: JUnit 5 + Mockito

---

## Báo Cáo & Demo

| Tài nguyên   | Link                          |
|--------------|-------------------------------|
|  Báo cáo PDF | *(https://drive.google.com/file/d/1bkRsQyDM5F3pwcHMTEQO43wso0gMvH-i/view?usp=sharing)*     |
|  Video Demo  | *(https://drive.google.com/file/d/1e8WuRbYqmkW8xjRAt-6LvrtfisDIK732/view?usp=sharing)*     |