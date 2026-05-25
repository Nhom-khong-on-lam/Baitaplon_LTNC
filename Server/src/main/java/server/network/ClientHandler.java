package server.network;

import com.auction.common.dto.AuctionDTO;
import com.auction.common.dto.AutoBidDTO;
import com.auction.common.dto.UserDTO;
import com.auction.common.enums.AccountStatus;
import com.auction.common.enums.AuctionStatus;
import com.auction.common.enums.SystemRole;
import com.auction.common.model.*;
import com.auction.common.network.Request;
import com.auction.common.network.Response;
import server.database.DBConnection;
import server.repository.*;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.out;

public class ClientHandler extends Thread {
    private Socket clientSocket;
    private ObjectOutputStream objectOut; // Đổi từ out -> objectOut
    private ObjectInputStream in;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            // Đổi tên ở đây luôn
            this.objectOut = new ObjectOutputStream(clientSocket.getOutputStream());
            this.in = new ObjectInputStream(clientSocket.getInputStream());

            while (true) {
                Object requestObj = in.readObject();
                if (requestObj == null) break;

                System.out.println("📩 [Server] Nhận được gói tin từ Client: " + requestObj.getClass().getSimpleName());

                Object responseObj = null;

                responseObj = handleRequest((com.auction.common.network.Request) requestObj);

                if (responseObj != null) {
                    System.out.println("📤 [Server] Gửi trả phản hồi thành công: " + responseObj.getClass().getSimpleName());
                } else {
                    System.out.println("⚠️ [Server] Cảnh báo: responseObj đang bị NULL! Kiểm tra lại hàm xử lý logic.");
                }

                objectOut.writeObject(responseObj);
                objectOut.flush();
                objectOut.reset();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { clientSocket.close(); } catch (Exception e) {}
        }
    }

    private Response handleRequest(Request req) {
        UserDAO userDAO = new UserDAO();

        switch (req.getAction()) {

            // ── AUTH ─────────────────────────────────────────────────────────

            case Request.LOGIN: {
                String[] creds = (String[]) req.getData();
                String username = creds[0];
                String rawPassword = creds[1]; // Mật khẩu thô từ Client gửi lên

                UserDTO user = userDAO.findByUsername(username);

                // ── VÁ LỖI TỰ ĐỘNG: Nếu đăng nhập bằng admin, tự băm lại pass bằng chính Java ──
                if (user != null && "admin".equals(user.getUsername())) {
                    String javaHashed = org.mindrot.jbcrypt.BCrypt.hashpw("123456", org.mindrot.jbcrypt.BCrypt.gensalt());
                    out.println("⚡ CHUỖI MẬT KHẨU MỚI DO JAVA TỰ SINH: " + javaHashed);

                    // Gán chuỗi mới tinh này vào user để tí nữa hàm checkpw chắc chắn qua
                    user.setPassword(javaHashed);

                    // Cập nhật đè trực tiếp vào TiDB luôn để lần sau không bị lỗi nữa
                    userDAO.update(user);
                }

                // SỬ DỤNG BCRYPT: So sánh mật khẩu thô và chuỗi băm trong DB
                if (user == null || !org.mindrot.jbcrypt.BCrypt.checkpw(rawPassword, user.getPassword())) {
                    return Response.error("Invalid username or password.");
                }
                if ("BANNED".equals(user.getAccountStatus())) {
                    return Response.error("Your account has been suspended. Please contact support.");
                }

                String token = UUID.randomUUID().toString();
                User clientUser = toClientUser(user);
                Response res = Response.ok(clientUser);
                res.setToken(token);
                return res;
            }

            case Request.LOGOUT: {
                return Response.ok(null);
            }

            case Request.CHECK_USER_EXISTS: {
                String username = (String) req.getData();
                boolean exists = userDAO.isExisted("username", username);
                return new Response(exists, exists ? "Username already taken." : "Username is available.", null);
            }
            case "CHECK_AUTOBID_STATUS": {
                Object[] data = (Object[]) req.getData();
                Long auctionId = (Long) data[0];
                Long bidderId = (Long) data[1];

                com.auction.common.dto.AutoBidDTO config = null;
                String sql = "SELECT * FROM auto_bid WHERE auction_id = ? AND bidder_id = ? LIMIT 1";
                try (java.sql.Connection conn = server.database.DBConnection.getConnection();
                     java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, auctionId);
                    ps.setLong(2, bidderId);
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            config = new com.auction.common.dto.AutoBidDTO();
                            config.setId(rs.getLong("id"));
                            config.setMaxPrice(rs.getDouble("max_price"));
                            config.setStepIncrement(rs.getDouble("step_increment"));
                            config.setActive(rs.getBoolean("active"));
                        }
                    }
                } catch (java.sql.SQLException e) {
                    System.out.println("❌ Lỗi check trạng thái AutoBid: " + e.getMessage());
                }

                // Trả về DTO
                return Response.ok(config);
            }

            case Request.SET_AUTO_BID: {
                // 1. Ép kiểu dữ liệu nhận từ Client
                AutoBidDTO autoBidDto = (AutoBidDTO) req.getData();
                AutoBidDAO autoBidDAO = new AutoBidDAO();

                Response responseToClient;

                // 2. Kiểm tra xem người dùng này đã từng có cấu hình nào trong phòng này chưa
                boolean isExisted = false;
                long existingId = -1;

                String sqlCheck = "SELECT id FROM auto_bid WHERE auction_id = ? AND bidder_id = ? LIMIT 1";
                try (java.sql.Connection conn = server.database.DBConnection.getConnection();
                     java.sql.PreparedStatement ps = conn.prepareStatement(sqlCheck)) {
                    ps.setLong(1, autoBidDto.getAuctionId());
                    ps.setLong(2, autoBidDto.getBidderId());
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            isExisted = true;
                            existingId = rs.getLong("id"); // Lấy được ID thực tế dưới DB
                        }
                    }
                } catch (java.sql.SQLException e) {
                    out.println("Lỗi kiểm tra cấu hình tồn tại: " + e.getMessage());
                }

                // 3. Nhận diện luồng bằng maxPrice để chống lỗi biến Boolean active
                if (autoBidDto.getMaxPrice() <= 0) {

                    // ── [LUỒNG HỦY AUTOBID] ──────────────────────────────────
                    if (isExisted && existingId != -1) {
                        // Gọi hàm updateActiveStatus(id, active) có sẵn trong DAO của bạn để tắt đi
                        boolean isUpdated = autoBidDAO.updateActiveStatus(existingId, false);
                        if (isUpdated) {
                            responseToClient = new Response(true, "Đã hủy chế độ tự động và lưu lịch sử thành công!", null);
                        } else {
                            responseToClient = new Response(false, "Lỗi: Không thể cập nhật trạng thái hủy vào CSDL!", null);
                        }
                    } else {
                        responseToClient = new Response(false, "Không tìm thấy cấu hình Auto Bid hoạt động để hủy!", null);
                    }

                } else {

                    // ── [LUỒNG KÍCH HOẠT MỚI HOẶC BẬT LẠI] ────────────────────
                    if (isExisted) {
                        // Nếu đã tồn tại dữ liệu cũ, chỉ việc UPDATE đè lên và bật active = 1 (true)
                        // Giúp giải quyết triệt để lỗi trùng lặp khóa ngoại khi INSERT
                        String sqlUpdateConfig = "UPDATE auto_bid SET max_price = ?, step_increment = ?, active = 1, registered_at = NOW() WHERE id = ?";
                        try (java.sql.Connection conn = server.database.DBConnection.getConnection();
                             java.sql.PreparedStatement ps = conn.prepareStatement(sqlUpdateConfig)) {
                            ps.setDouble(1, autoBidDto.getMaxPrice());
                            ps.setDouble(2, autoBidDto.getStepIncrement());
                            ps.setLong(3, existingId);

                            int rows = ps.executeUpdate();
                            if (rows > 0) {
                                responseToClient = new Response(true, "Cập nhật và kích hoạt Auto Bid thành công!", existingId);
                            } else {
                                responseToClient = new Response(false, "Lỗi khi cập nhật cấu hình Auto Bid!", null);
                            }
                        } catch (java.sql.SQLException e) {
                            responseToClient = new Response(false, "Lỗi SQL: " + e.getMessage(), null);
                        }
                    } else {
                        // Nếu hoàn toàn chưa có bản ghi nào, tiến hành INSERT mới từ đầu
                        autoBidDto.setActive(true); // Ép cứng trạng thái hoạt động trước khi lưu
                        long newId = autoBidDAO.insert(autoBidDto);
                        if (newId != -1) {
                            responseToClient = new Response(true, "Kích hoạt tự động đấu giá thành công!", newId);
                        } else {
                            responseToClient = new Response(false, "Lỗi hệ thống: Không thể lưu cấu hình mới!", null);
                        }
                    }
                }
                if (responseToClient != null && responseToClient.isSuccess() && autoBidDto.getMaxPrice() > 0) {
                    try {
                        synchronized (String.valueOf(autoBidDto.getAuctionId()).intern()) {
                            AuctionDAO auctionDAO = new AuctionDAO();
                            AuctionDTO auctionDto = auctionDAO.findById(autoBidDto.getAuctionId());
                            if (auctionDto != null) {
                                Long currentLeaderId = auctionDto.getHighestBidderId();
                                // Nếu phòng chưa có người đặt, hoặc người đặt cao nhất hiện tại KHÔNG phải là user này
                                if (currentLeaderId == null || !currentLeaderId.equals(autoBidDto.getBidderId())) {
                                    server.repository.BidDAO bidDAO = new server.repository.BidDAO();
                                    triggerAutoBidsLoop(auctionDto.getId(), auctionDto.getCurrentPrice(), currentLeaderId != null ? currentLeaderId : -1L, auctionDAO, bidDAO);
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Lỗi kích hoạt AutoBid ngay lập tức: " + e.getMessage());
                    }
                }

                // 4. Trả kết quả đồng bộ về cho Client qua Socket
                return responseToClient;
            }

            case "CHECK_EMAIL_EXISTS": {
                String email = (String) req.getData();
                boolean exists = userDAO.isExisted("email", email);
                return new Response(exists, exists ? "Email already registered." : "Email is available.", null);
            }

            case "REGISTER": {
                try {
                    // Kiểm tra xem dữ liệu gửi lên là gì để tránh lỗi ép kiểu (ClassCastException)
                    Object requestData = req.getData();
                    UserDTO newUser = null;

                    if (requestData instanceof com.auction.common.dto.UserDTO) {
                        newUser = (com.auction.common.dto.UserDTO) requestData;
                    } else if (requestData instanceof com.auction.common.model.User) {
                        // Phòng trường hợp Client gửi nhầm Object Model User sang, ta tự map lại thành DTO
                        com.auction.common.model.User modelUser = (com.auction.common.model.User) requestData;
                        newUser = new UserDTO();
                        newUser.setUsername(modelUser.getUsername());
                        newUser.setPassword(modelUser.getPasswordHash());
                        newUser.setEmail(modelUser.getEmail());
                    }

                    if (newUser == null) {
                        System.err.println("❌ LỖI ĐĂNG KÝ: Dữ liệu Client gửi lên không hợp lệ (Null hoặc sai kiểu Class)!");
                        return Response.error("Invalid registration data form.");
                    }

                    // 1. Kiểm tra trùng lặp tài khoản
                    if (userDAO.isExisted("username", newUser.getUsername())) {
                        return Response.error("Username already taken.");
                    }
                    if (newUser.getEmail() != null && userDAO.isExisted("email", newUser.getEmail())) {
                        return Response.error("Email already registered.");
                    }

                    // 2. Mã hóa mật khẩu bằng BCrypt
                    String rawPassword = newUser.getPassword();
                    if (rawPassword == null || rawPassword.isBlank()) {
                        return Response.error("Password cannot be empty.");
                    }
                    String hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(rawPassword, org.mindrot.jbcrypt.BCrypt.gensalt());
                    newUser.setPassword(hashedPassword);

                    // 3. Set thông tin mặc định
                    newUser.setSystemRole("USER");
                    newUser.setAccountStatus("ACTIVE");
                    newUser.setCreatedAt(java.time.LocalDateTime.now());

                    // 4. Lưu xuống cơ sở dữ liệu TiDB
                    long generatedId = userDAO.insert(newUser);
                    if (generatedId != -1) {
                        newUser.setId(generatedId);
                        out.println("🚀 [SUCCESS] Đăng ký thành công thành viên mới: " + newUser.getUsername());
                        return Response.ok(toClientUser(newUser));
                    } else {
                        return Response.error("Database error. Failed to save account.");
                    }

                } catch (Exception ex) {
                    System.err.println(" LỖI NGHIÊM TRỌNG TRONG LUỒNG REGISTER: " + ex.getMessage());
                    ex.printStackTrace();
                    return Response.error("Server internal error during registration.");
                }
            }

            case "CHECK_PASSWORD": {
                Object[] data = (Object[]) req.getData();
                Long userId = (Long) data[0];
                String rawPass = (String) data[1]; // Mật khẩu thô cần kiểm tra
                UserDTO user = userDAO.findById(userId);
                if (user == null) return Response.error("User not found.");

                // SỬ DỤNG BCRYPT: Kiểm tra mật khẩu cũ khi người dùng muốn đổi pass
                boolean match = org.mindrot.jbcrypt.BCrypt.checkpw(rawPass, user.getPassword());
                return new Response(match, match ? "Password is correct." : "Incorrect password.", null);
            }

            case "UPDATE_PASSWORD": {
                Object[] data = (Object[]) req.getData();
                String email = (String) data[0];
                String newRawPass = (String) data[1]; // Mật khẩu mới dạng thô
                UserDTO user = userDAO.findByField("email", email);
                if (user == null) return Response.error("User not found.");

                // SỬ DỤNG BCRYPT: Băm mật khẩu mới trước khi lưu
                String newHashedPass = org.mindrot.jbcrypt.BCrypt.hashpw(newRawPass, org.mindrot.jbcrypt.BCrypt.gensalt());
                user.setPassword(newHashedPass);

                boolean ok = userDAO.update(user);
                return ok ? Response.ok(null) : Response.error("Failed to update password.");
            }

            case "UPDATE_USER": {
                User incoming = (User) req.getData();
                UserDTO user = userDAO.findById(incoming.getId());
                if (user == null) return Response.error("User not found.");
                user.setEmail(incoming.getEmail());
                user.setUsername(incoming.getUsername());
                boolean ok = userDAO.update(user);
                return ok ? Response.ok(null) : Response.error("Failed to update user.");
            }

            // ── ADMIN ────────────────────────────────────────────────────────

            case Request.ADMIN_GET_USERS: {
                java.util.List<UserDTO> dtos = userDAO.findAll();
                java.util.List<User> users = new java.util.ArrayList<>();
                for (UserDTO dto : dtos) users.add(toClientUser(dto));
                return Response.ok(users);
            }

            case Request.PLACE_BID: {
                Object[] data = (Object[]) req.getData();
                Long auctionId = (Long) data[0];
                User bidder = (User) data[1];
                Double bidAmount = (Double) data[2];

                // Đảm bảo không có 2 người cùng đặt giá vào cùng 1 phòng ở cùng 1 phần nghìn giây (Race Condition)
                synchronized (String.valueOf(auctionId).intern()) {
                    AuctionDAO auctionDAO = new AuctionDAO();
                    AuctionDTO auctionDto = auctionDAO.findById(auctionId);

                    if (auctionDto == null) {
                        System.out.println("❌ [PLACE_BID ERROR] Phiên đấu giá ID=" + auctionId + " không tồn tại.");
                        return Response.error("Phiên đấu giá không tồn tại.");
                    }

                    if (auctionDto.getSellerId() == bidder.getId()) {
                        System.out.println("❌ [SECURITY VIOLATION] User ID=" + bidder.getId() + " cố tình đặt giá phòng của chính mình!");
                        return Response.error("Bạn không thể tự đặt giá phòng của mình.");
                    }

                    if (bidAmount <= auctionDto.getCurrentPrice()) {
                        System.out.println("❌ [PLACE_BID ERROR] Mức giá đặt (" + bidAmount + ") nhỏ hơn hoặc bằng giá hiện tại (" + auctionDto.getCurrentPrice() + ").");
                        return Response.error("Giá đặt của bạn đã bị người khác vượt lên trước đó. Hãy tải lại trang!");
                    }

                    auctionDto.setCurrentPrice(bidAmount);
                    auctionDto.setHighestBidderId(bidder.getId());

                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    java.time.LocalDateTime endTime = auctionDto.getEndTime();

                    if (endTime != null && now.isBefore(endTime)) {
                        long secondsRemaining = java.time.Duration.between(now, endTime).getSeconds();
                        if (secondsRemaining <= 180) {
                            auctionDto.setEndTime(endTime.plusSeconds(180));
                            System.out.println("⏳ [ANTI-SNIPING ACTIVATED] Phiên " + auctionId + " được gia hạn thêm 3 phút.");
                        }
                    }

                    boolean updateSuccess = auctionDAO.update(auctionDto);
                    if (!updateSuccess) {
                        System.out.println("❌ [DATABASE ERROR] Không thể cập nhật giá mới cho Auction ID=" + auctionId);
                        return Response.error("Lỗi kết nối cơ sở dữ liệu khi đặt giá.");
                    }

                    server.repository.BidDAO bidDAO = new server.repository.BidDAO();
                    com.auction.common.dto.BidDTO bidDto = new com.auction.common.dto.BidDTO();
                    bidDto.setAuctionId(auctionId);
                    bidDto.setBidderId(bidder.getId());
                    bidDto.setAmount(bidAmount);
                    bidDto.setBidTime(java.time.LocalDateTime.now());
                    bidDto.setAutoBid(false); // Đánh dấu lượt đặt giá thủ công từ Client
                    bidDAO.insert(bidDto);

                    System.out.println("💰 [BID SUCCESS] Người dùng [" + bidder.getUsername() + "] đặt giá thành công " + bidAmount + " tại phòng " + auctionId);

                    createBidNotifications(auctionId, bidder.getId(), bidAmount, false);

                    // ====================================================================
                    // CHUỖI PHẢN ỨNG CHẠY NỀN: Kích hoạt Robot AutoBid nâng giá đè lên luôn
                    // ====================================================================
                    triggerAutoBidsLoop(auctionId, bidAmount, bidder.getId(), auctionDAO, bidDAO);
                    // ====================================================================

                    return Response.ok("Đặt giá thành công!");
                }
            }
            case Request.ADMIN_DELETE_USER: {
                try {
                    Long userId = (Long) req.getData();

                    // Gọi xuống UserDAO thực thi lệnh SQL: DELETE FROM user WHERE id = ?
                    boolean success = userDAO.deleteUserById(userId);

                    if (success) {
                        // GIẢI PHÁP: Return trực tiếp gói tin về luồng xử lý trung tâm, không dùng out.writeObject và break nữa
                        return Response.ok("Đã xóa người dùng vĩnh viễn khỏi hệ thống!");
                    } else {
                        return Response.error("Xóa thất bại! Người dùng có thể đang vướng ràng buộc dữ liệu đấu giá.");
                    }
                } catch (Exception e) {
                    return Response.error("Lỗi Server khi xóa: " + e.getMessage());
                }
            }

            case Request.ADMIN_UPDATE_USER_STATUS: {
                try {
                    Object[] data = (Object[]) req.getData();
                    Long userId = (Long) data[0];
                    String newStatus = data[1].toString();

                    // Gọi xuống UserDAO để thực thi lệnh SQL: UPDATE user SET accountStatus = ? WHERE id = ?
                    boolean success = userDAO.updateStatus(userId, newStatus);

                    if (success) {
                        // GIẢI PHÁP: Dùng return chuẩn cấu trúc kiến trúc phần mềm
                        return Response.ok("Cập nhật trạng thái người dùng thành công!");
                    } else {
                        return Response.error("Không thể cập nhật trạng thái trong Database.");
                    }
                } catch (Exception e) {
                    return Response.error("Lỗi Server: " + e.getMessage());
                }
            }

            // ── AUCTION ──────────────────────────────────────────────────────

            case Request.GET_AUCTIONS: {
                AuctionDAO auctionDAO = new AuctionDAO();
                java.util.List<AuctionDTO> dtos = auctionDAO.findPublic();
                return Response.ok(toClientAuctions(dtos, userDAO));
            }

            case "GET_ACTIVE_AUCTIONS": {
                AuctionDAO auctionDAO = new AuctionDAO();
                java.util.List<AuctionDTO> dtos = auctionDAO.findActive();
                return Response.ok(toClientAuctions(dtos, userDAO));
            }

            case "UPDATE_RESERVE_PRICE": {
                try {
                    // 1. Bóc tách mảng dữ liệu do Client gửi sang qua Socket
                    Object[] data = (Object[]) req.getData();
                    Long auctionId = ((Number) data[0]).longValue();
                    Double reservePrice = ((Number) data[1]).doubleValue();

                    System.out.println("🚀 [Server] Nhận lệnh cập nhật Giá dự phòng cho Auction ID: " + auctionId + " | Giá mới: " + reservePrice);

                    // 2. Gọi lớp DAO có sẵn của Server để tương tác với Database
                    AuctionDAO auctionDAO = new AuctionDAO();
                    AuctionDTO existing = auctionDAO.findById(auctionId);

                    if (existing == null) {
                        return Response.error("Không tìm thấy phiên đấu giá tương ứng trên hệ thống.");
                    }

                    // 3. Thực thi cập nhật giá trị trực tiếp vào Database
                    boolean success = false;
                    String sql = "UPDATE auction SET reserve_price = ? WHERE id = ?";

                    // Sử dụng kết nối DBConnection chuẩn của Server bạn (server.database.DBConnection)
                    try (java.sql.Connection conn = server.database.DBConnection.getConnection();
                         java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setDouble(1, reservePrice);
                        ps.setLong(2, auctionId);
                        success = ps.executeUpdate() > 0;
                    }

                    if (success) {
                        System.out.println("✅ [Server] Đã lưu giá dự phòng vào DB thành công!");
                        return Response.ok("Cập nhật thành công");
                    } else {
                        return Response.error("Database từ chối cập nhật (Lỗi ràng buộc dữ liệu).");
                    }
                } catch (Exception e) {
                    System.err.println("❌ Lỗi xử lý UPDATE_RESERVE_PRICE: " + e.getMessage());
                    e.printStackTrace();
                    return Response.error("Lỗi nội bộ Server: " + e.getMessage());
                }
            }

            case Request.GET_AUCTION: {
                Long auctionId = (Long) req.getData();
                AuctionDAO auctionDAO = new AuctionDAO();
                AuctionDTO dto = auctionDAO.findById(auctionId);
                if (dto == null) return Response.error("Auction not found.");
                java.util.List<AuctionDTO> single = new java.util.ArrayList<>();
                single.add(dto);
                java.util.List<Auction> result = toClientAuctions(single, userDAO);
                return Response.ok(result.isEmpty() ? null : result.get(0));
            }

            case "GET_AUCTIONS_BY_SELLER": {
                Long sellerId = (Long) req.getData();
                AuctionDAO auctionDAO = new AuctionDAO();
                java.util.List<AuctionDTO> dtos = auctionDAO.findBySeller(sellerId);
                return Response.ok(toClientAuctions(dtos, userDAO));
            }

            case Request.GET_NOTIFICATIONS: {
                Long uid = (Long) req.getData();
                server.repository.NotificationDAO notifDAO = new server.repository.NotificationDAO();
                java.util.List<com.auction.common.dto.NotificationDTO> list = notifDAO.findByUserId(uid);
                return Response.ok(list);
            }

            case Request.GET_MY_BIDS: {
                Long userId = (Long) req.getData();
                AuctionDAO auctionDAO = new AuctionDAO();
                java.util.List<AuctionDTO> dtos = auctionDAO.findByBidder(userId);
                java.util.List<Auction> result = toClientAuctions(dtos, userDAO);

                server.repository.BidDAO bidDAO = new server.repository.BidDAO();
                for (Auction a : result) {
                    java.util.List<com.auction.common.dto.BidDTO> bids = bidDAO.getBidsByAuctionId(a.getId());
                    double maxBid = bids.stream()
                            .filter(b -> b.getBidderId() != null && b.getBidderId().equals(userId))
                            .mapToDouble(com.auction.common.dto.BidDTO::getAmount)
                            .max().orElse(0.0);
                    if (maxBid > 0) {
                        User fakeUser = new User(userId, "You", "", "", com.auction.common.enums.SystemRole.USER);
                        a.getBidHistory().add(new com.auction.common.model.BidTransaction(fakeUser, maxBid, false));
                    }
                }
                return Response.ok(result);
            }

            case "GET_WINNING_BIDS": {
                Long userId = (Long) req.getData();
                AuctionDAO auctionDAO = new AuctionDAO();
                java.util.List<AuctionDTO> dtos = auctionDAO.findWinningByUser(userId);
                return Response.ok(toClientAuctions(dtos, userDAO));
            }

            case "CREATE_AUCTION": {
                Object[] data = (Object[]) req.getData();

                // ĐÃ SỬA: Bốc ID người bán thay vì bốc cả Object User gây lệch byte
                Long sellerId = data[0] != null ? ((Number) data[0]).longValue() : 1L;

                String title = data.length > 1 && data[1] != null ? (String) data[1] : "Sản phẩm không tên";
                String description = data.length > 2 && data[2] != null ? (String) data[2] : "";
                String category = data.length > 3 && data[3] != null ? (String) data[3] : "Electronics";
                String condition = data.length > 4 && data[4] != null ? (String) data[4] : "New";

                double startPrice = 0.0;
                if (data.length > 5 && data[5] != null) {
                    startPrice = ((Number) data[5]).doubleValue();
                }

                java.time.LocalDateTime startTime = data.length > 8 && data[8] != null ? (java.time.LocalDateTime) data[8] : java.time.LocalDateTime.now();
                java.time.LocalDateTime endTime = data.length > 9 && data[9] != null ? (java.time.LocalDateTime) data[9] : java.time.LocalDateTime.now().plusDays(1);

                String imagePath = null;
                if (data.length > 10 && data[10] != null) {
                    imagePath = (String) data[10];
                }

                // ĐÃ SỬA: Đọc trực tiếp vị trí số 6, không lo vỡ mảng
                double reservePrice = 0.0;
                if (data.length > 6 && data[6] != null) {
                    try {
                        reservePrice = Double.parseDouble(data[6].toString());
                    } catch (Exception e) {
                        reservePrice = 0.0;
                    }
                }

                System.out.println("🔥 [SERVER CHỐT HẠ] Giá dự phòng nhận được: " + reservePrice);

                server.repository.ItemDAO itemDAO = new server.repository.ItemDAO();
                com.auction.common.dto.ItemDTO newItemDto = new com.auction.common.dto.ItemDTO();
                newItemDto.setName(title);
                newItemDto.setDescription(description);
                newItemDto.setCategory(category);
                newItemDto.setStartingPrice(startPrice);

                long generatedItemId = itemDAO.insert(newItemDto);
                if (generatedItemId == -1) {
                    return Response.error("Failed to create product item.");
                }

                if (imagePath != null && !imagePath.isBlank()) {
                    server.repository.ItemImageDAO imageDAO = new server.repository.ItemImageDAO();
                    com.auction.common.dto.ItemImageDTO imgDto = new com.auction.common.dto.ItemImageDTO();
                    imgDto.setItemId(generatedItemId);
                    imgDto.setImageUrl(imagePath.trim());
                    imageDAO.insert(imgDto);
                }

                AuctionDTO auction = new AuctionDTO();
                auction.setItemId(generatedItemId);
                auction.setSellerId(sellerId); // Gán ID trực tiếp cực chuẩn
                auction.setCurrentPrice(startPrice);
                auction.setReservePrice(reservePrice); // Gán giá dự phòng

                auction.setStartTime(startTime != null ? startTime : java.time.LocalDateTime.now());
                auction.setEndTime(endTime);
                auction.setStatus("PENDING_APPROVAL");

                AuctionDAO auctionDAO = new AuctionDAO();
                long newId = auctionDAO.insert(auction);
                if (newId == -1) {
                    return Response.error("Failed to create auction.");
                }

                return Response.ok(newId);
            }

            case Request.UPDATE_AUCTION: {
                Object[] data = (Object[]) req.getData();
                Long auctionId    = (Long)   data[0];
                String title      = (String) data[1];
                String desc       = (String) data[2];
                String category   = (String) data[3];
                double startPrice = (Double) data[4];
                java.time.LocalDateTime endTime = (java.time.LocalDateTime) data[5];
                String imagePath  = (data.length > 6) ? (String) data[6] : null;

                // THÊM TRƯỜNG HỢP AN TOÀN: Bóc tách giá dự phòng nếu Client gửi kèm khi sửa
                double reservePrice = 0.0;
                boolean hasReserveInUpdate = false;
                if (data.length > 7 && data[7] != null) {
                    reservePrice = ((Number) data[7]).doubleValue();
                    hasReserveInUpdate = true;
                }

                AuctionDAO auctionDAO = new AuctionDAO();
                AuctionDTO existing = auctionDAO.findById(auctionId);
                if (existing == null) return Response.error("Auction not found.");

                String currentStatus = existing.getStatus();
                if (!"PENDING_APPROVAL".equalsIgnoreCase(currentStatus)) {
                    return Response.error("Không thể chỉnh sửa: phiên đấu giá đã được duyệt hoặc đang diễn ra.");
                }

                existing.setCurrentPrice(startPrice);
                existing.setEndTime(endTime);

                // Nếu bóc tách được giá dự phòng mới, cập nhật vào đối tượng luôn
                if (hasReserveInUpdate) {
                    existing.setReservePrice(reservePrice);
                }

                boolean ok = auctionDAO.update(existing);
                if (!ok) return Response.error("Failed to update auction.");

                try (java.sql.Connection conn = server.database.DBConnection.getConnection()) {
                    long itemId = auctionDAO.getItemIdByAuctionId(auctionId);

                    String updateItemSql = "UPDATE item SET name=?, description=?, category=?, starting_price=? WHERE id=?";
                    try (java.sql.PreparedStatement ps = conn.prepareStatement(updateItemSql)) {
                        ps.setString(1, title);
                        ps.setString(2, desc);
                        ps.setString(3, category.toUpperCase());
                        ps.setDouble(4, startPrice);
                        ps.setLong(5, itemId);
                        ps.executeUpdate();
                    }

                    // BỔ SUNG: Cập nhật trực tiếp cột reserve_price vào bảng auction trong DB để đảm bảo không bị sót số
                    if (hasReserveInUpdate) {
                        String updateReserveSql = "UPDATE auction SET reserve_price=? WHERE id=?";
                        try (java.sql.PreparedStatement ps = conn.prepareStatement(updateReserveSql)) {
                            ps.setDouble(1, reservePrice);
                            ps.setLong(2, auctionId);
                            ps.executeUpdate();
                        }
                    }

                    if (imagePath != null && !imagePath.isBlank()) {
                        String checkSql = "SELECT COUNT(*) FROM item_image WHERE item_id=?";
                        boolean hasImage = false;
                        try (java.sql.PreparedStatement ps = conn.prepareStatement(checkSql)) {
                            ps.setLong(1, itemId);
                            try (java.sql.ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) hasImage = rs.getInt(1) > 0;
                            }
                        }
                        if (hasImage) {
                            String updateImgSql = "UPDATE item_image SET image_url=? WHERE item_id=?";
                            try (java.sql.PreparedStatement ps = conn.prepareStatement(updateImgSql)) {
                                ps.setString(1, imagePath);
                                ps.setLong(2, itemId);
                                ps.executeUpdate();
                            }
                        } else {
                            String insertImgSql = "INSERT INTO item_image (item_id, image_url) VALUES (?,?)";
                            try (java.sql.PreparedStatement ps = conn.prepareStatement(insertImgSql)) {
                                ps.setLong(1, itemId);
                                ps.setString(2, imagePath);
                                ps.executeUpdate();
                            }
                        }
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                return Response.ok(null);
            }
            case "DELETE_AUCTION": {
                Object rawData = req.getData();
                if (rawData == null) return Response.error("ID phiên đấu giá không được để trống!");

                long auctionId = -1;
                if (rawData instanceof Number) {
                    auctionId = ((Number) rawData).longValue();
                } else {
                    try {
                        auctionId = Long.parseLong(rawData.toString().trim());
                    } catch (Exception e) {
                        return Response.error("Định dạng ID không hợp lệ.");
                    }
                }

                AuctionDAO auctionDAO = new AuctionDAO();
                ItemDAO itemDAO = new ItemDAO();

                // 1. Lấy ra mã hàng hóa item_id trước khi bảng auction bị xóa sạch
                long itemId = auctionDAO.getItemIdByAuctionId(auctionId);
                System.out.println("🚀 [CLEAN] Tiến hành dọn dẹp chuỗi bản ghi liên kết cho Auction ID: " + auctionId);

                // 2. Thực thi chuỗi lệnh xóa bằng SQL thuần bọc cô lập để dọn sạch các bảng không có CASCADE
                try (Connection conn = server.database.DBConnection.getConnection()) {
                    conn.setAutoCommit(false); // Bật chế độ quản lý giao dịch để tránh xóa dở dang

                    try (PreparedStatement psAutoBid   = conn.prepareStatement("DELETE FROM auto_bid WHERE auction_id = ?");
                         PreparedStatement psPayment   = conn.prepareStatement("DELETE FROM payment WHERE auction_id = ?");
                         PreparedStatement psExtLog    = conn.prepareStatement("DELETE FROM auction_extension_log WHERE auction_id = ?");
                         PreparedStatement psBids      = conn.prepareStatement("DELETE FROM bid WHERE auction_id = ?");
                         PreparedStatement psAuction   = conn.prepareStatement("DELETE FROM auction WHERE id = ?");
                         PreparedStatement psItem      = conn.prepareStatement("DELETE FROM item WHERE id = ?")) {

                        // Bước A: Xóa sạch dữ liệu ở tất cả các bảng con đang giữ khóa ngoại cứng
                        psAutoBid.setLong(1, auctionId);
                        psAutoBid.executeUpdate();

                        psPayment.setLong(1, auctionId);
                        psPayment.executeUpdate();

                        psExtLog.setLong(1, auctionId);
                        psExtLog.executeUpdate();

                        psBids.setLong(1, auctionId);
                        psBids.executeUpdate();

                        // Bước B: Xóa tiêu đề phiên đấu giá trong bảng auction
                        psAuction.setLong(1, auctionId);
                        int auctionRows = psAuction.executeUpdate();

                        // Bước C: Xóa thực thể hàng hóa gốc trong bảng item
                        int itemRows = 0;
                        if (itemId != -1) {
                            psItem.setLong(1, itemId);
                            itemRows = psItem.executeUpdate();
                        }

                        // Kiểm tra: Nếu bản ghi auction chính đã được dọn khỏi bộ nhớ DB
                        if (auctionRows > 0) {
                            conn.commit(); // Hợp thức hóa, ghi dữ liệu xuống ổ cứng vĩnh viễn
                            System.out.println("🗑️ [SUCCESS] Đã xóa mất hút khỏi DB: 1 Auction và " + itemRows + " Item.");
                            return Response.ok("Xóa thành công");
                        } else {
                            conn.rollback();
                            return Response.error("Không tìm thấy phiên đấu giá tương ứng trong DB.");
                        }

                    } catch (SQLException e) {
                        conn.rollback(); // Hoàn tác dữ liệu ngay nếu có một bảng bị kẹt
                        System.err.println("❌ Lỗi thực thi SQL khi dọn rác DB: " + e.getMessage());
                        return Response.error("Lỗi ràng buộc dữ liệu: " + e.getMessage());
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    return Response.error("Lỗi kết nối cơ sở dữ liệu Server.");
                }
            }

            case "GET_DASHBOARD_DATA": {
                long userId = (Long) req.getData();
                AuctionDAO auctionDAO = new AuctionDAO();
                server.repository.UserDAO userRepo = new server.repository.UserDAO();

                java.util.List<com.auction.common.dto.AuctionDTO> allDtos = auctionDAO.findAll();
                java.util.List<com.auction.common.dto.AuctionDTO> bidDtos = auctionDAO.findByBidder(userId);
                java.util.List<com.auction.common.dto.AuctionDTO> winningDtos = auctionDAO.findWinningByUser(userId);
                java.util.List<com.auction.common.dto.AuctionDTO> productDtos = auctionDAO.findBySeller(userId);
                java.util.List<com.auction.common.dto.AuctionDTO> liveDtos = auctionDAO.findActive();

                java.util.List<Auction> all = toClientAuctions(allDtos, userRepo);
                java.util.List<Auction> bids = toClientAuctions(bidDtos, userRepo);
                java.util.List<Auction> winning = toClientAuctions(winningDtos, userRepo);
                java.util.List<Auction> products = toClientAuctions(productDtos, userRepo);
                java.util.List<Auction> live = toClientAuctions(liveDtos, userRepo);

                DashboardData dashboardData = new DashboardData(all, bids, winning, products, live);
                return Response.ok(dashboardData);
            }

            case Request.EXTEND_AUCTION: {
                try {
                    Object[] data = (Object[]) req.getData();
                    if (data == null || data.length < 2) {
                        return Response.error("Dữ liệu yêu cầu gia hạn không đầy đủ.");
                    }

                    long auctionId = -1;
                    if (data[0] instanceof Number) {
                        auctionId = ((Number) data[0]).longValue();
                    } else {
                        auctionId = Long.parseLong(data[0].toString());
                    }

                    int hours = 0;
                    if (data[1] instanceof Number) {
                        hours = ((Number) data[1]).intValue();
                    } else {
                        hours = Integer.parseInt(data[1].toString());
                    }

                    System.out.println("⏳ [PROCESS] Tiến hành gia hạn thêm " + hours + " giờ cho Auction ID: " + auctionId);

                    AuctionDAO auctionDAO = new AuctionDAO();
                    com.auction.common.dto.AuctionDTO existing = auctionDAO.findById(auctionId);

                    if (existing == null) {
                        return Response.error("Không tìm thấy phiên đấu giá tương ứng.");
                    }

                    // Tính toán thời gian kết thúc mới
                    java.time.LocalDateTime newEndTime = existing.getEndTime().plusHours(hours);

                    // GIẢI PHÁP DU KÍCH: Chạy lệnh UPDATE cô lập, chỉ sửa thời gian và trạng thái
                    // Tuyệt đối không chạm vào cột current_price hay highest_bidder_id trong DB!
                    String directSql = "UPDATE auction SET end_time = ? WHERE id = ?";

                    boolean ok = false;

                    try (java.sql.Connection conn = server.database.DBConnection.getConnection();
                         java.sql.PreparedStatement ps = conn.prepareStatement(directSql)) {
                        ps.setTimestamp(1, java.sql.Timestamp.valueOf(newEndTime));
                        ps.setLong(2, auctionId);
                        ok = ps.executeUpdate() > 0;
                    }

                    if (ok) {
                        System.out.println("🎉 [SUCCESS] Gia hạn thành công phiên ID " + auctionId + ". Thời gian mới: " + newEndTime);
                        return Response.ok(null);
                    } else {
                        return Response.error("Lỗi cập nhật thời gian mới vào Database.");
                    }

                } catch (Exception e) {
                    System.err.println("❌ Lỗi nghiêm trọng tại case EXTEND_AUCTION: " + e.getMessage());
                    e.printStackTrace();
                    return Response.error("Lỗi Server: " + e.getMessage());
                }
            }

            case "GET_ALL_APPROVAL_AUCTIONS": {
                AuctionDAO auctionDAO = new AuctionDAO();
                List<com.auction.common.dto.AuctionDTO> dtos = auctionDAO.findAll();
                List<Auction> clientAuctions = toClientAuctions(dtos, userDAO);
                return Response.ok(clientAuctions);
            }

            case Request.END_AUCTION_EARLY: {
                // 1. Ép kiểu dữ liệu an toàn tránh lỗi ClassCast từ Client gửi lên
                Long auctionId = null;
                if (req.getData() instanceof Number) {
                    auctionId = ((Number) req.getData()).longValue();
                } else {
                    auctionId = Long.valueOf(req.getData().toString());
                }

                AuctionDAO auctionDAO = new AuctionDAO();

                // ĐỌC DỮ LIỆU HIỆN TẠI: Lấy thông tin phiên đấu giá trước khi đóng sổ để biết ai đang dẫn đầu
                com.auction.common.dto.AuctionDTO existing = auctionDAO.findById(auctionId);
                if (existing == null) {
                    return Response.error("Không tìm thấy phiên đấu giá.");
                }

                // 2. Ép thời gian kết thúc (endTime) về HIỆN TẠI
                String sqlTime = "UPDATE auction SET end_time = ? WHERE id = ?";
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sqlTime)) {

                    ps.setTimestamp(1, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                    ps.setLong(2, auctionId);
                    ps.executeUpdate();

                } catch (SQLException e) {
                    System.err.println("Cảnh báo: Không thể cập nhật end_time: " + e.getMessage());
                }

                // 3. ĐỔI TRẠNG THÁI SANG FINISHED bằng hàm có sẵn của bạn
                boolean ok = auctionDAO.updateStatus(auctionId, "FINISHED");

                // 4. TỰ ĐỘNG THÊM VÀO BẢNG PAYMENT NẾU CÓ NGƯỜI ĐẶT GIÁ (HỢP NHẤT LOGIC)
                if (ok) {
                    // Kiểm tra xem phiên đấu giá này có ai đặt giá thắng cuộc không (highest_bidder_id != null)
                    if (existing.getHighestBidderId() != null) {
                        server.repository.PaymentDAO paymentDAO = new server.repository.PaymentDAO();

                        // Kiểm tra trước dưới DB xem hóa đơn cho phiên này đã tồn tại chưa để tránh bị insert trùng lặp
                        if (paymentDAO.getByAuctionId(auctionId) == null) {
                            com.auction.common.dto.PaymentDTO newPayment = new com.auction.common.dto.PaymentDTO();
                            newPayment.setAuctionId(auctionId);
                            newPayment.setBuyerId(existing.getHighestBidderId());
                            newPayment.setSellerId(existing.getSellerId());
                            newPayment.setAmount(existing.getCurrentPrice());
                            newPayment.setStatus("PENDING"); // Luôn mặc định là PENDING để chờ người thắng vào trả tiền

                            // Gọi hàm insert xịn trong PaymentDAO của bạn để đẩy xuống database
                            boolean isPaymentCreated = paymentDAO.insert(newPayment);
                            if (isPaymentCreated) {
                                System.out.println("🎉 [Server] Đã tạo thành công hóa đơn PENDING cho Buyer ID: "
                                        + existing.getHighestBidderId() + " tại Auction ID: " + auctionId);
                            } else {
                                System.err.println("❌ [Server] Lỗi: Không thể insert hóa đơn vào bảng payment.");
                            }
                        }
                    }

                    // Trả kết quả thành công về cho Client JavaFX
                    return Response.ok(null);
                } else {
                    return Response.error("Database refused to update status to FINISHED.");
                }
            }
            case Request.GET_BID_HISTORY: {
                Long targetAuctionId = (Long) req.getData();
                // Khởi tạo DAO lấy lịch sử đấu giá (Sửa theo đúng tên package DAO trong dự án của bạn)
                server.repository.BidDAO bidDAO = new server.repository.BidDAO();

                // Lấy danh sách lịch sử từ Database
                java.util.List<com.auction.common.dto.BidDTO> bidDtos = bidDAO.getBidsByAuctionId(targetAuctionId);
                java.util.List<BidTransaction> transactions = new java.util.ArrayList<>();

                if (bidDtos != null) {
                    for (com.auction.common.dto.BidDTO bDto : bidDtos) {
                        // Lấy thông tin User đã đặt giá
                        UserDTO bidderDto = userDAO.findById(bDto.getBidderId());
                        User bidderUser = bidderDto != null ? toClientUser(bidderDto)
                                : new User(bDto.getBidderId(), "Unknown", "", "", SystemRole.USER);

                        // Đóng gói thành Model gửi về Client
                        BidTransaction transaction = new BidTransaction(bidderUser, bDto.getAmount(), bDto.isAutoBid());

                        // Đồng bộ thời gian đặt giá chuẩn từ Database lên giao diện
                        if (bDto.getBidTime() != null) {
                            transaction.setBidTime(bDto.getBidTime());
                        }

                        transactions.add(transaction);
                    }
                }
                return Response.ok(transactions);
            }
            // BỔ SUNG VÀO TRONG HÀM handleRequest(Request req) CỦA ClientHandler.java
            case "DELETE_USER": {
                Long userId = (Long) req.getData();
                // Gọi xuống UserDAO để chạy lệnh DELETE SQL
                boolean ok = userDAO.deleteUserById(userId);
                return ok ? Response.ok(null) : Response.error("Không thể xóa người dùng này khỏi DB!");
            }

            case "UPDATE_USER_STATUS": {
                Object[] data = (Object[]) req.getData();
                Long userId = (Long) data[0];
                String newStatus = (String) data[1];
                // Gọi xuống UserDAO để chạy lệnh UPDATE user SET accountStatus = ? WHERE id = ?
                boolean ok = userDAO.updateStatus(userId, newStatus);
                return ok ? Response.ok(null) : Response.error("Không thể cập nhật trạng thái User!");
            }

            case "GET_PENDING_AUCTIONS": {
                try {
                    server.repository.AuctionDAO auctionDAO = new server.repository.AuctionDAO();
                    List<AuctionDTO> dtos = auctionDAO.getByStatus("PENDING_APPROVAL");
                    List<Auction> clientAuctions = toClientAuctions(dtos, userDAO);
                    return Response.ok(clientAuctions);
                } catch (Exception e) {
                    return Response.error("Lỗi lấy danh sách chờ duyệt: " + e.getMessage());
                }
            }

            case "GET_APPROVED_AUCTIONS": {
                try {
                    server.repository.AuctionDAO auctionDAO = new server.repository.AuctionDAO();
                    List<AuctionDTO> dtos = auctionDAO.getByStatus("RUNNING");
                    List<Auction> clientAuctions = toClientAuctions(dtos, userDAO);
                    return Response.ok(clientAuctions);
                } catch (Exception e) {
                    return Response.error("Lỗi lấy danh sách đã duyệt: " + e.getMessage());
                }
            }
            // ── THÊM VÀO CUỐI SWITCH CASE TRƯỚC DEFAULT ──────────────────────

            // ── THÊM CASE NÀY VÀO TRONG SWITCH (REQUEST.GETCOMMAND()) CỦA CLIENTHANDLER ──
            case "TOP_UP_BALANCE": {
                try {
                    // 1. Ép kiểu về mảng Object[] đúng như luồng Client truyền sang
                    Object[] data = (Object[]) req.getData();

                    // 2. Bóc tách chính xác UserId (Long) và Số tiền nạp (Double)
                    Long userId = ((Number) data[0]).longValue();
                    Double amount = ((Number) data[1]).doubleValue();

                    System.out.println("💰 [Server] Đang xử lý yêu cầu nạp tiền cho User ID: " + userId + " | Số tiền: " + amount);

                    // 3. Tìm thông tin UserDTO hiện tại trong Database thông qua userDAO
                    UserDTO userDto = userDAO.findById(userId);
                    if (userDto == null) {
                        return Response.error("Không tìm thấy tài khoản người dùng trên hệ thống!");
                    }

                    // 4. Cộng dồn số tiền nạp vào số dư hiện tại trong cơ sở dữ liệu
                    double newBalance = userDto.getBalance() + amount;
                    userDto.setBalance(newBalance);

                    // 5. Gọi câu lệnh UPDATE của UserDAO để ghi nhận số dư mới xuống DB
                    boolean success = userDAO.update(userDto);

                    if (success) {
                        System.out.println("✅ [Server] Nạp tiền THÀNH CÔNG! Tài khoản: " + userDto.getUsername() + " | Số dư mới: " + newBalance + " VND");
                        // Trả số dư mới tinh (newBalance) về để Client vẽ lại giao diện
                        return Response.ok(newBalance);
                    } else {
                        return Response.error("Lỗi hệ thống: Không thể lưu số dư mới vào Cơ sở dữ liệu.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return Response.error("Lỗi xử lý nạp tiền phía Server: " + e.getMessage());
                }
            }

            case "UPDATE_FINANCIAL_PROFILE": {
                try {
                    Object[] data = (Object[]) req.getData();
                    Long userId = ((Number) data[0]).longValue();
                    String bankName = (String) data[1];
                    String accountNumber = (String) data[2];

                    UserDTO user = userDAO.findById(userId);
                    if (user == null) return Response.error("Không tìm thấy người dùng.");

                    // Cập nhật thông tin liên kết ngân hàng
                    user.setBankName(bankName);
                    user.setAccountNumber(accountNumber);

                    boolean ok = userDAO.update(user);
                    if (ok) {
                        return Response.ok("Cập nhật thông tin tài chính thành công!");
                    } else {
                        return Response.error("Lỗi cập nhật dữ liệu ngân hàng.");
                    }
                } catch (Exception e) {
                    return Response.error("Lỗi Server: " + e.getMessage());
                }
            }

            case "GET_REJECTED_AUCTIONS": {
                try {
                    server.repository.AuctionDAO auctionDAO = new server.repository.AuctionDAO();
                    List<AuctionDTO> dtos = auctionDAO.getByStatus("REJECTED");
                    List<Auction> clientAuctions = toClientAuctions(dtos, userDAO);
                    return Response.ok(clientAuctions);
                } catch (Exception e) {
                    return Response.error("Lỗi lấy danh sách bị từ chối: " + e.getMessage());
                }
            }

            case "ADMIN_APPROVE_AUCTION": {
                try {
                    Long auctionId = ((Number) req.getData()).longValue();
                    server.repository.AuctionDAO auctionDAO = new server.repository.AuctionDAO();
                    boolean success = auctionDAO.updateStatusAndStartTime(auctionId, "RUNNING", java.time.LocalDateTime.now());
                    if (success) {
                        return Response.ok("Duyệt thành công");
                    } else {
                        return Response.error("Không thể cập nhật trạng thái");
                    }
                } catch (Exception e) {
                    return Response.error("Lỗi Server Approve: " + e.getMessage());
                }
            }

            case "ADMIN_REJECT_AUCTION": {
                try {
                    Object raw = req.getData();
                    Long auctionId;
                    if (raw instanceof Object[]) {
                        // Client gửi Object[] {auctionId, reason}
                        Object[] data = (Object[]) raw;
                        auctionId = ((Number) data[0]).longValue();
                        // reason = (String) data[1]; // có thể dùng sau nếu cần lưu lý do
                    } else {
                        // Fallback: gửi thẳng id
                        auctionId = ((Number) raw).longValue();
                    }
                    server.repository.AuctionDAO auctionDAO = new server.repository.AuctionDAO();
                    boolean success = auctionDAO.updateStatus(auctionId, "REJECTED");
                    if (success) {
                        return Response.ok("Từ chối thành công");
                    } else {
                        return Response.error("Không thể từ chối");
                    }
                } catch (Exception e) {
                    return Response.error("Lỗi Server Reject: " + e.getMessage());
                }
            }

            // ── PAYMENT ──────────────────────────────────────────────────────

            // ── PAYMENT ──────────────────────────────────────────────────────

            case "GET_PAYMENT_DETAIL": {
                try {
                    Long auctionId = (Long) req.getData();
                    AuctionDAO auctionDAO = new AuctionDAO();
                    AuctionDTO auction = auctionDAO.findById(auctionId);
                    if (auction == null) return Response.error("Không tìm thấy phiên đấu giá.");

                    server.repository.PaymentDAO paymentDAO = new server.repository.PaymentDAO();
                    com.auction.common.dto.PaymentDTO payment = paymentDAO.getByAuctionId(auctionId);

                    // Nếu chưa có bản ghi payment (auction vừa kết thúc), tự tạo một PENDING
                    if (payment == null) {
                        if (auction.getHighestBidderId() == null) {
                            return Response.error("Phiên này không có người đấu giá thắng cuộc.");
                        }
                        payment = new com.auction.common.dto.PaymentDTO();
                        payment.setAuctionId(auctionId);
                        payment.setBuyerId(auction.getHighestBidderId());
                        payment.setSellerId(auction.getSellerId());
                        payment.setAmount(auction.getCurrentPrice());
                        payment.setStatus("PENDING");

                        boolean inserted = paymentDAO.insert(payment);
                        if (!inserted) return Response.error("Không thể khởi tạo hóa đơn.");

                        // Fetch lại để lấy id chuẩn từ DB
                        payment = paymentDAO.getByAuctionId(auctionId);
                        if (payment == null) return Response.error("Lỗi nội bộ: không fetch lại được payment.");
                    }

                    // cho chạy tiếp xuống dưới để trả về Object kèm theo trạng thái "COMPLETED"
                    // cho Client tự biết đường mà xử lý đóng Popup hoặc ẩn nút.

                    // Gắn thông tin ngân hàng của Seller vào DTO phục vụ Client hiển thị lên UI Dialog
                    UserDTO seller = userDAO.findById(payment.getSellerId());
                    if (seller != null) {
                        payment.setSellerBankName(seller.getBankName());
                        payment.setSellerAccountNumber(seller.getAccountNumber());
                        payment.setSellerCardholderName(seller.getCardholderName() != null ? seller.getCardholderName() : seller.getUsername());
                    }

                    return Response.ok(payment); // Luôn trả về dữ liệu thành công
                } catch (Exception e) {
                    e.printStackTrace();
                    return Response.error("Lỗi Server khi tải thông tin thanh toán: " + e.getMessage());
                }
            }

            case "CREATE_PAYMENT": {
                try {
                    com.auction.common.dto.PaymentDTO p = (com.auction.common.dto.PaymentDTO) req.getData();
                    if (p == null || p.getAuctionId() == null) return Response.error("Dữ liệu thanh toán không hợp lệ.");

                    // 1. Kiểm tra ví tiền của người mua (Buyer) và người bán (Seller) xem có đủ điều kiện không
                    UserDTO buyer = userDAO.findById(p.getBuyerId());
                    UserDTO seller = userDAO.findById(p.getSellerId());

                    if (buyer == null || seller == null) {
                        return Response.error("Giao dịch thất bại: Thực thể tài khoản không hợp lệ.");
                    }

                    if (buyer.getBalance() < p.getAmount()) {
                        return Response.error("Số dư tài khoản ví AURUM không đủ! Vui lòng nạp thêm tiền tại trang Profile.");
                    }

                    server.repository.PaymentDAO paymentDAO = new server.repository.PaymentDAO();
                    com.auction.common.dto.PaymentDTO existing = paymentDAO.getByAuctionId(p.getAuctionId());

                    // 2. Mở Database Transaction cô lập tài chính
                    try (Connection conn = server.database.DBConnection.getConnection()) {
                        conn.setAutoCommit(false); // Bắt đầu chuỗi lệnh nguyên tử

                        // Trừ tiền người mua, cộng tiền người bán trực tiếp trên bộ nhớ DTO
                        buyer.setBalance(buyer.getBalance() - p.getAmount());
                        seller.setBalance(seller.getBalance() + p.getAmount());

                        // Đồng bộ số dư mới xuống cơ sở dữ liệu bảng user
                        userDAO.update(buyer);
                        userDAO.update(seller);

                        // Cập nhật trạng thái payment thành COMPLETED vào bảng payment
                        boolean ok;
                        if (existing != null) {
                            ok = paymentDAO.updateStatusWithConn(conn, existing.getId(), "COMPLETED");
                        } else {
                            p.setStatus("COMPLETED");
                            ok = paymentDAO.insertWithConn(conn, p);
                        }

                        if (!ok) {
                            conn.rollback();
                            return Response.error("Lưu thông tin thanh toán thất bại.");
                        }

                        // GIỮ NGUYÊN FINISHED: Tránh ghi đè chữ "PAID" lỗi khiến dữ liệu bị văng ngược về trạng thái "Live"
                        AuctionDAO auctionDAO = new AuctionDAO();
                        AuctionDTO auction = auctionDAO.findById(p.getAuctionId());
                        if (auction != null) {
                            auction.setStatus("FINISHED");
                            auctionDAO.update(auction);
                        }

                        conn.commit(); // Xác nhận hoàn tất chuỗi giao dịch ví thành công tuyệt đối!
                        return Response.ok("Thanh toán thành công!");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return Response.error("Lỗi Server khi xử lý thanh toán: " + e.getMessage());
                }
            }

            case "UPDATE_BANK_INFO": {
                try {
                    Object[] data = (Object[]) req.getData();
                    Long userId = (Long) data[0];
                    String bankName = (String) data[1];
                    String accountNumber = (String) data[2];
                    String cardholderName = (String) data[3];

                    UserDTO user = userDAO.findById(userId);
                    if (user == null) return Response.error("Không tìm thấy người dùng.");

                    user.setBankName(bankName);
                    user.setAccountNumber(accountNumber);
                    user.setCardholderName(cardholderName);
                    boolean ok = userDAO.update(user);
                    return ok ? Response.ok(null) : Response.error("Cập nhật thông tin ngân hàng thất bại.");
                } catch (Exception e) {
                    return Response.error("Lỗi Server: " + e.getMessage());
                }
            }

            default:
                out.println("WARNING: Unhandled command: " + req.getAction());
                return Response.error("Unknown command: " + req.getAction());
        }
        // return Response.error("Server internal processing error.");
    }


    private List<Auction> toClientAuctions(List<AuctionDTO> dtos, UserDAO userDAO) {

        java.util.List<Auction> result = new java.util.ArrayList<>();
        if (dtos == null || dtos.isEmpty()) return result;

        try {
            // 1. Khởi tạo các DAO kết nối dữ liệu
            server.repository.ItemDAO itemDAO = new server.repository.ItemDAO();
            server.repository.ItemImageDAO imageDAO = new server.repository.ItemImageDAO();

            // 2. BƯỚC THẦN TỐC: Gom toàn bộ bảng dữ liệu lên bộ nhớ tạm (RAM) của Server
            java.util.Map<Long, com.auction.common.dto.ItemDTO> itemMap = new java.util.HashMap<>();
            java.util.List<com.auction.common.dto.ItemDTO> allItems = itemDAO.getAll();
            if (allItems != null) {
                for (com.auction.common.dto.ItemDTO itemDto : allItems) {
                    itemMap.put(itemDto.getId(), itemDto);
                }
            }

            java.util.Map<Long, String> imageMap = new java.util.HashMap<>();
            java.util.List<com.auction.common.dto.ItemImageDTO> allImages = imageDAO.getAll();

            // Nạp tổng số lần đặt giá của TẤT CẢ phiên cùng lúc
            server.repository.BidDAO bidDAO = new server.repository.BidDAO();
            java.util.Map<Long, Integer> bidCountMap = bidDAO.countAllGroupedByAuction();

            if (allImages != null) {
                for (com.auction.common.dto.ItemImageDTO imgDto : allImages) {
                    if (!imageMap.containsKey(imgDto.getItemId())) {

                        String rawUrl = imgDto.getImageUrl();
                        if (rawUrl != null && !rawUrl.isBlank()) {
                            String safeUrl = rawUrl.trim().replace("\\", "/");

                            if (safeUrl.toLowerCase().startsWith("http://") || safeUrl.toLowerCase().startsWith("https://")) {
                                imageMap.put(imgDto.getItemId(), safeUrl);
                            }
                            else if (!safeUrl.toLowerCase().startsWith("file:")) {
                                safeUrl = "file:///" + safeUrl;
                                imageMap.put(imgDto.getItemId(), safeUrl);
                            } else {
                                imageMap.put(imgDto.getItemId(), safeUrl);
                            }
                        }
                    }
                }
            }
            java.util.Map<Long, UserDTO> userMap = new java.util.HashMap<>();
            java.util.List<UserDTO> allUsers = userDAO.findAll();
            if (allUsers != null) {
                for (UserDTO uDto : allUsers) {
                    userMap.put(uDto.getId(), uDto);
                }
            }

            // 3. Tiến hành xử lý lặp dữ liệu trên RAM
            for (AuctionDTO dto : dtos) {
                if (dto == null || dto.getItemId() <= 0) continue;

                UserDTO sellerDto = userMap.get(dto.getSellerId());
                User seller = sellerDto != null
                        ? toClientUser(sellerDto)
                        : new User(dto.getSellerId(), "Unknown", "", "", SystemRole.USER);

                com.auction.common.dto.ItemDTO itemDto = itemMap.get(dto.getItemId());
                Item item;

                if (itemDto != null) {
                    String categoryStr = itemDto.getCategory() != null ? itemDto.getCategory().toLowerCase().trim() : "";

                    item = switch (categoryStr) {
                        case "art" -> new com.auction.common.model.Art(
                                itemDto.getName(), itemDto.getDescription(), itemDto.getStartingPrice(),
                                itemDto.getArtist() != null ? itemDto.getArtist() : "",
                                itemDto.getProductionYear() != null ? itemDto.getProductionYear() : 0
                        );
                        case "vehicle" -> new com.auction.common.model.Vehicle(
                                itemDto.getName(), itemDto.getDescription(), itemDto.getStartingPrice(),
                                itemDto.getBrandMake() != null ? itemDto.getBrandMake() : "",
                                itemDto.getModel() != null ? itemDto.getModel() : "",
                                itemDto.getProductionYear() != null ? itemDto.getProductionYear() : 0
                        );
                        default -> new com.auction.common.model.Electronics(
                                itemDto.getName(), itemDto.getDescription(), itemDto.getStartingPrice(),
                                itemDto.getBrandMake() != null ? itemDto.getBrandMake() : "",
                                itemDto.getModel() != null ? itemDto.getModel() : ""
                        );
                    };
                    item.setId(dto.getItemId());

                    String cachedImgUrl = imageMap.get(dto.getItemId());
                    if (cachedImgUrl != null) {
                        item.setImageUrl(cachedImgUrl);
                    }

                } else {
                    continue;
                }

                // Gán dữ liệu phòng đấu giá hoàn chỉnh
                java.time.LocalDateTime endTime = dto.getEndTime() != null ? dto.getEndTime() : java.time.LocalDateTime.now().plusDays(1);
                Auction auction = new Auction(item, seller, endTime);
                auction.setId(dto.getId());
                auction.setCurrentPrice(dto.getCurrentPrice());

                // Đồng bộ Giá dự phòng từ DTO (Database) sang Model gửi về Client
                auction.setReservePrice(dto.getReservePrice());

                if (dto.getHighestBidderId() != null && dto.getHighestBidderId() > 0) {
                    UserDTO bidderDto = userMap.get(dto.getHighestBidderId());
                    if (bidderDto != null) {
                        auction.setHighestBidder(toClientUser(bidderDto));
                    }
                }

                try {
                    auction.setStatus(AuctionStatus.valueOf(
                            dto.getStatus() != null ? dto.getStatus().toUpperCase().trim() : "RUNNING"));
                } catch (Exception e) {
                    auction.setStatus(AuctionStatus.RUNNING);
                }
                auction.setBidCount(bidCountMap.getOrDefault(dto.getId(), 0));

                result.add(auction);
            }
        } catch (Exception e) {
            out.println("Lỗi nghiêm trọng khi tối ưu hóa bộ nhớ RAM cho phòng đấu giá: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    private User toClientUser(UserDTO dto) {
        SystemRole role;
        try {
            role = SystemRole.valueOf(
                    dto.getSystemRole() != null ? dto.getSystemRole().toUpperCase() : "USER");
        } catch (IllegalArgumentException e) {
            role = SystemRole.USER;
        }

        User u = new User(
                dto.getId(),
                dto.getUsername(),
                dto.getPassword(),
                dto.getEmail(),
                role
        );

        if (dto.getAccountStatus() != null
                && !dto.getAccountStatus().equalsIgnoreCase("ACTIVE")) {
            try {
                AccountStatus status = AccountStatus.valueOf(dto.getAccountStatus().toUpperCase());
                u.setAccountStatus(status);
            } catch (IllegalArgumentException ignored) {}
        }

        // Map banking fields
        u.setAccountNumber(dto.getAccountNumber());
        u.setBankName(dto.getBankName());
        u.setCardholderName(dto.getCardholderName());
        u.setJoinedDate(dto.getCreatedAt());        // ← fix ngày tham gia

        return u;
    }

    private void triggerAutoBidsLoop(Long auctionId, double currentPrice, Long lastBidderId, AuctionDAO auctionDAO, server.repository.BidDAO bidDAO) {
        try {
            server.repository.AutoBidDAO autoBidDAO = new server.repository.AutoBidDAO();

            // Truy vấn thẳng xuống Database tìm tất cả cấu hình AutoBid đang bật (active = 1)
            List<com.auction.common.dto.AutoBidDTO> configs = autoBidDAO.getActiveConfigsForAuction(auctionId);
            if (configs == null || configs.isEmpty()) return;

            for (com.auction.common.dto.AutoBidDTO config : configs) {
                // Nếu người giữ giá cao nhất hiện tại chính là chủ cấu hình này -> Bỏ qua không tự nâng đè chính mình
                if (config.getBidderId().equals(lastBidderId)) continue;

                // Tính toán mức giá tự động tiếp theo = Giá hiện tại + Bước nhảy
                double nextPrice = currentPrice + config.getStepIncrement();

                // Điều kiện: Nếu mức giá mới này nằm trong tầm chi trả (<= giá trần tối đa)
                if (nextPrice <= config.getMaxPrice()) {

                    AuctionDTO auctionDto = auctionDAO.findById(auctionId);
                    if (auctionDto == null) return;

                    // Cập nhật giá cao nhất mới cho phòng đấu giá
                    auctionDto.setCurrentPrice(nextPrice);
                    auctionDto.setHighestBidderId(config.getBidderId());

                    // Đồng bộ tính năng Anti-sniping (Gia hạn phòng 3 phút) cho robot đặt giá
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    java.time.LocalDateTime endTime = auctionDto.getEndTime();
                    if (endTime != null && now.isBefore(endTime)) {
                        long secondsRemaining = java.time.Duration.between(now, endTime).getSeconds();
                        if (secondsRemaining <= 180) {
                            auctionDto.setEndTime(endTime.plusSeconds(180));
                            out.println("Anti-sniping kích hoạt bởi AutoBid nền: Phiên " + auctionId + " được gia hạn thêm 3 phút.");
                        }
                    }

                    // 1. Ghi nhận giá mới của phòng đấu giá vào database
                    auctionDAO.update(auctionDto);

                    // 2. Chèn lịch sử đấu giá tự động của robot vào bảng bid (set auto_bid = true)
                    com.auction.common.dto.BidDTO autoBidDto = new com.auction.common.dto.BidDTO();
                    autoBidDto.setAuctionId(auctionId);
                    autoBidDto.setBidderId(config.getBidderId());
                    autoBidDto.setAmount(nextPrice);
                    autoBidDto.setBidTime(java.time.LocalDateTime.now());
                    autoBidDto.setAutoBid(true); // Đánh dấu robot hệ thống tự đặt hộ
                    bidDAO.insert(autoBidDto);

                    out.println("🤖 [AUTOBID REALTIME] Robot tự động nâng giá cho User ID [" + config.getBidderId() + "] lên " + nextPrice + " tại phòng " + auctionId);

                    createBidNotifications(auctionId, config.getBidderId(), nextPrice, true);

                    // CHUỖI PHẢN ỨNG ĐỆ QUY: Tự động gọi lại chính nó với mức giá mới
                    // Đảm bảo nếu trong phòng có nhiều robot cài AutoBid đè nhau, chúng sẽ tự nâng giá qua lại liên tục
                    triggerAutoBidsLoop(auctionId, nextPrice, config.getBidderId(), auctionDAO, bidDAO);
                    break; // Thoát vòng lặp hiện tại để nhường quyền xử lý cho luồng đệ quy mới
                } else {
                    // Nếu vượt quá số tiền tối đa chịu đựng được, tự động tắt trạng thái hoạt động trong DB
                    autoBidDAO.updateActiveStatus(config.getId(), false);
                    out.println("🤖 [AUTOBID TERMINATED] User ID [" + config.getBidderId() + "] tự động tắt vì giá vượt đỉnh trần.");
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi nghiêm trọng trong chuỗi xử lý AutoBid chạy ngầm:");
            e.printStackTrace();
        }
    }

    private void createBidNotifications(long auctionId, long bidderId, double amount, boolean isAutoBid) {
        try {
            server.repository.BidDAO bidDAO = new server.repository.BidDAO();
            server.repository.AuctionDAO auctionDAO = new server.repository.AuctionDAO();
            server.repository.NotificationDAO notificationDAO = new server.repository.NotificationDAO();
            server.repository.UserDAO uDAO = new server.repository.UserDAO();
            server.repository.ItemDAO itemDAO = new server.repository.ItemDAO();

            com.auction.common.dto.AuctionDTO auction = auctionDAO.findById(auctionId);
            if (auction == null) return;

            com.auction.common.dto.ItemDTO item = itemDAO.getById(auction.getItemId());
            String title = (item != null) ? item.getName() : "Phòng #" + auctionId;

            com.auction.common.dto.UserDTO bidder = uDAO.findById(bidderId);
            String bidderName = (bidder != null) ? bidder.getUsername() : "Ai đó";

            java.util.List<Long> involvedUsers = bidDAO.getDistinctBiddersByAuction(auctionId);
            if (!involvedUsers.contains(auction.getSellerId())) {
                involvedUsers.add(auction.getSellerId());
            }

            for (Long uid : involvedUsers) {
                if (uid == bidderId && !isAutoBid) continue;

                com.auction.common.dto.NotificationDTO notif = new com.auction.common.dto.NotificationDTO();
                notif.setUserId(uid);
                notif.setRelatedAuctionId(auctionId);
                notif.setRead(false);
                notif.setType("BID_PLACED");

                if (uid == bidderId && isAutoBid) {
                    notif.setTitle("Auto Bid tự động nâng giá");
                    notif.setMessage("Hệ thống vừa tự động đặt " + String.format("%,.0f", amount) + " VND cho phiên đấu giá " + title);
                } else {
                    String typeStr = isAutoBid ? " (Auto Bid)" : "";
                    notif.setTitle("Có người vừa đặt giá mới");
                    notif.setMessage(bidderName + " vừa đặt " + String.format("%,.0f", amount) + " VND cho phiên đấu giá " + title + typeStr);
                }
                notificationDAO.insert(notif);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi tạo thông báo đặt giá: " + e.getMessage());
        }
    }
}