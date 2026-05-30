# Hệ Thống Đấu Giá Trực Tuyến (Online Bidding System)

## 1. Mô tả chi tiết bài toán và phạm vi hệ thống
Hệ thống Đấu Giá Trực Tuyến là một phần mềm Client-Server toàn diện, cho phép người dùng tham gia đấu giá sản phẩm theo thời gian thực với cơ chế cạnh tranh minh bạch.
- **Mục tiêu cốt lõi:** Áp dụng toàn diện OOP, xây dựng hệ thống giao tiếp mạng bằng Java Socket thuần túy (không dùng framework HTTP), và xử lý đồng thời an toàn (chống race condition, lost update) khi nhiều người đặt giá cùng lúc.
- **Đối tượng sử dụng (3 vai trò):** Admin (Quản trị viên có toàn quyền kiểm duyệt), User đóng vai trò linh hoạt: Seller (Người bán tạo phiên) và Bidder (Người mua đặt giá).
- **Kiến trúc phân tầng (6 lớp):** Hệ thống được tổ chức thành 6 lớp rõ ràng: Client (MVC nội bộ: FXML -> Controller -> Service) -> Network (TCP/UDP) -> Server (Business Logic theo luồng Thread-per-client) -> DAO (11 class) -> Database (11 bảng) -> và Module Common dùng chung.

## 2. Công nghệ sử dụng, môi trường chạy và yêu cầu cài đặt
- **Ngôn ngữ & Nền tảng:** Java 21 (JDK 21), quản lý dự án bằng Maven.
- **Giao tiếp mạng:**
    - **TCP Socket:** Duy trì kết nối liên tục (Persistent Connection) cho toàn bộ phiên làm việc.
    - **UDP Socket:** Cơ chế Auto-Discovery (Server broadcast, Client tự động bắt IP trong mạng LAN).
- **Giao diện & UI:** JavaFX 21 (với 15 màn hình FXML) và **JavaFX LineChart** (vẽ biểu đồ).
- **Cơ sở dữ liệu:** MySQL / TiDB. Hệ thống kết nối thẳng tới Database đám mây (TiDB Cloud) qua `HikariCP` (Connection Pool tối đa 20 kết nối).
- **Thiết kế & Tối ưu:**
    - Design Patterns: Singleton, Factory Method, Observer, Strategy.
    - Xử lý đồng thời (Concurrency): `ReentrantLock`, `ConcurrentHashMap`.
- **Dịch vụ Cloud (Third-party):** Cloudinary (Lưu trữ ảnh), Jakarta Mail (Gửi thông báo).
- **Chất lượng mã (CI/CD):** Unit Test với JUnit 5, tích hợp GitHub Actions tự động test & build khi có Push/Pull Request.
- **Yêu cầu cài đặt:** Chỉ cần cài JDK 21 và Maven. **Không cần cài đặt CSDL cục bộ** (hệ thống đã trỏ đến TiDB Cloud). Yêu cầu có kết nối Internet.

## 3. Cấu trúc thư mục hoặc các module chính
Dự án áp dụng thiết kế đa module (Multi-module Maven) chia thành 3 phần chính, phục vụ cho kiến trúc 6 lớp:
- **`Common` (Shared Module):** Chứa các tài nguyên dùng chung: Abstract class (BaseEntity, Item), Models (User, Auction,...), DTO, Enums, gói mạng tuần tự hóa (Request/Response), giao diện Observer và Factory.
- **`Server` (Business & Data Access):**
- **Network / Business:** Lắng nghe kết nối TCP, UDP Broadcast. Quản lý mỗi client bằng một luồng `ClientHandler` riêng (Thread-per-client). Xử lý logic vòng đời phiên đấu giá, thuật toán Auto-bid, Anti-sniping.
- **DAO:** Gồm 11 class đảm nhiệm việc đọc/ghi SQL tập trung (UserDAO, BidDAO...).
- **Database:** Quản lý kết nối HikariCP, thao tác trực tiếp với TiDB.
- **`Client` (JavaFX MVC):** Xây dựng theo mô hình MVC nội bộ. Không chứa logic nghiệp vụ và không kết nối trực tiếp CSDL. Chứa 15 màn hình FXML, Controllers xử lý sự kiện UI, Service đóng gói gói tin gửi lên Server, và cơ chế tự động cập nhật UI qua `Platform.runLater()`.

## 4. Vị trí các file `.jar`
Để đóng gói ứng dụng thông qua quá trình CI/CD hoặc build thủ công, chạy lệnh sau tại thư mục gốc của dự án:
```bash
mvn clean install
```
Sau khi Maven chạy thành công (bao gồm cả việc pass toàn bộ Unit Test), các file `.jar` sẽ nằm ở:
- **Server:** `Server/target/Server-1.0-SNAPSHOT.jar`
- **Client:** `Client/target/Client-1.0-SNAPSHOT.jar`

## 5. Hướng dẫn chạy Server/Client theo thứ tự cụ thể
**Bắt buộc phải chạy Server trước để Client có thể phát hiện qua UDP.**

- **Bước 1: Khởi động Server**
    - Mở dự án bằng IDE. Tại module `Server`, chạy hàm `main` của lớp `server.network.AuctionServer`.
    - Console sẽ thông báo Server lắng nghe TCP và liên tục phát tín hiệu UDP Broadcast trên cổng 8888.
- **Bước 2: Khởi động Client**
    - Tại module `Client`, chạy lớp `com.auction.client.MainApp` (qua IDE) hoặc gõ terminal: `mvn javafx:run -pl Client`.
    - **Lưu ý:** Nhờ tính năng **UDP Auto-Discovery**, Client sẽ lắng nghe cổng 8888 và tự động lấy địa chỉ IP của Server trong mạng LAN mà không cần bạn cấu hình thủ công. (Nếu sau 3 giây không tìm thấy, hệ thống tự động fallback về `localhost`).

## 6. Danh sách chức năng đã hoàn thành
Hệ thống giải quyết trọn vẹn nghiệp vụ vòng đời đấu giá với các tính năng kỹ thuật ấn tượng:
- **Tự động tìm kiếm Server (Auto-Discovery):** Client tự nhận diện IP mạng LAN qua tín hiệu UDP broadcast từ Server.
- **Xử lý Đấu giá đồng thời an toàn:** Sử dụng `ReentrantLock` theo từng phiên, đảm bảo không xảy ra hiện tượng mất dữ liệu (Lost Update) hay ghi đè (Race Condition) khi nhiều người đặt giá cùng tíc tắc.
- **Cập nhật Live theo thời gian thực:** Áp dụng Observer Pattern qua TCP, đẩy thông báo giá mới, cảnh báo bị vượt giá tới toàn bộ Client ngay lập tức.
- **Các chức năng nâng cao (Nổi bật):**
    - **Auto-Bidding (Đấu giá tự động):** Ứng dụng *Strategy Pattern* để xử lý 2 chiến lược: 1 người dùng auto-bid độc lập, hoặc nhiều người cùng cài auto-bid cạnh tranh nhau, hệ thống tự động tính ra người chiến thắng với mức giá hợp lý nhất.
    - **Anti-sniping (Gia hạn phiên đấu giá):** Thuật toán chống chiến thuật bắn tỉa (sniping). Nếu có lượt bid trong 180 giây cuối cùng, thời gian kết thúc lập tức cộng thêm 180 giây, đảm bảo môi trường đấu giá công bằng.
    - **Bid History Visualization:** Nhúng biểu đồ đường (JavaFX LineChart) hiển thị biến động giá realtime theo thời gian, mỗi điểm tọa độ mới được vẽ lên ngay khi có bid hợp lệ nhờ cơ chế Observer.
- **Các chức năng nền tảng:** Phân quyền (Admin/Seller/Bidder), mã hóa mật khẩu BCrypt, upload ảnh lên Cloudinary, gửi email với Jakarta Mail.

## 7. Link báo cáo PDF và video demo
- **Link báo cáo chi tiết (PDF):** [Nhấn vào đây để xem báo cáo (https://drive.google.com/file/d/1bkRsQyDM5F3pwcHMTEQO43wso0gMvH-i/view?usp=sharing)]
- **Link Video Demo hệ thống:** [Nhấn vào đây để xem video demo (https://drive.google.com/file/d/1e8WuRbYqmkW8xjRAt-6LvrtfisDIK732/view?usp=sharing)]