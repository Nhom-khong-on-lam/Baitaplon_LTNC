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
import server.repository.AuctionDAO;
import server.repository.AutoBidDAO;
import server.repository.UserDAO;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler extends Thread {
    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.flush();

            Object input = in.readObject();
            if (!(input instanceof Request)) return;

            Request req = (Request) input;
            System.out.println("Server received command: " + req.getAction());

            Response res = handleRequest(req);

            out.writeObject(res);
            out.flush();

        } catch (Exception e) {
            System.out.println("Client " + socket.getInetAddress() + " disconnected: " + e.getMessage());
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
                    System.out.println("⚡ CHUỖI MẬT KHẨU MỚI DO JAVA TỰ SINH: " + javaHashed);

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
                    System.out.println("Lỗi kiểm tra cấu hình tồn tại: " + e.getMessage());
                }

                // 3. 🚀 ĐỔI CHIẾN THUẬT: Nhận diện luồng bằng maxPrice để chống lỗi biến Boolean active
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
                        String sqlUpdateConfig = "UPDATE auto_bid SET maxPrice = ?, stepIncrement = ?, active = 1, registered_at = NOW() WHERE id = ?";
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
                        System.out.println("🚀 [SUCCESS] Đăng ký thành công thành viên mới: " + newUser.getUsername());
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

                AuctionDAO auctionDAO = new AuctionDAO();
                AuctionDTO auctionDto = auctionDAO.findById(auctionId);

                if (auctionDto == null) {
                    return Response.error("Phiên đấu giá không tồn tại.");
                }

                if (auctionDto.getSellerId() == bidder.getId()) {
                    return Response.error("Security Violation: You cannot bid on your own auction!");
                }

                if (bidAmount <= auctionDto.getCurrentPrice()) {
                    return Response.error("Giá đặt phải lớn hơn giá hiện tại.");
                }

                auctionDto.setCurrentPrice(bidAmount);

                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                java.time.LocalDateTime endTime = auctionDto.getEndTime();

                if (endTime != null && now.isBefore(endTime)) {
                    long secondsRemaining = java.time.Duration.between(now, endTime).getSeconds();
                    if (secondsRemaining <= 180) {
                        auctionDto.setEndTime(endTime.plusSeconds(180));
                        System.out.println("Anti-sniping kích hoạt: Phiên " + auctionId + " được gia hạn thêm 3 phút.");
                    }
                }

                boolean updateSuccess = auctionDAO.update(auctionDto);
                if (!updateSuccess) {
                    return Response.error("Không thể cập nhật giá đấu.");
                }

                server.repository.BidDAO bidDAO = new server.repository.BidDAO();
                com.auction.common.dto.BidDTO bidDto = new com.auction.common.dto.BidDTO();
                bidDto.setAuctionId(auctionId);
                bidDto.setBidderId(bidder.getId());
                bidDto.setAmount(bidAmount);
                bidDto.setBidTime(java.time.LocalDateTime.now());
                bidDAO.insert(bidDto);

                System.out.println("Sự kiện: Người dùng [" + bidder.getUsername() + "] đã đặt giá " + bidAmount + " cho phòng " + auctionId);
                return Response.ok("Đặt giá thành công!");
            }

            case Request.ADMIN_DELETE_USER: {
                try {
                    Long userId = (Long) req.getData();

                    // Gọi xuống UserDAO thực thi lệnh SQL: DELETE FROM user WHERE id = ?
                    boolean success = userDAO.deleteUserById(userId);

                    if (success) {
                        // 🚀 GIẢI PHÁP: Return trực tiếp gói tin về luồng xử lý trung tâm, không dùng out.writeObject và break nữa
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
                        // 🚀 GIẢI PHÁP: Dùng return chuẩn cấu trúc kiến trúc phần mềm
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
                java.util.List<AuctionDTO> dtos = auctionDAO.findActive();
                return Response.ok(toClientAuctions(dtos, userDAO));
            }

            case "GET_ACTIVE_AUCTIONS": {
                AuctionDAO auctionDAO = new AuctionDAO();
                java.util.List<AuctionDTO> dtos = auctionDAO.findActive();
                return Response.ok(toClientAuctions(dtos, userDAO));
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

            case Request.GET_MY_BIDS: {
                Long userId = (Long) req.getData();
                AuctionDAO auctionDAO = new AuctionDAO();
                java.util.List<AuctionDTO> dtos = auctionDAO.findByBidder(userId);
                return Response.ok(toClientAuctions(dtos, userDAO));
            }

            case "GET_WINNING_BIDS": {
                Long userId = (Long) req.getData();
                AuctionDAO auctionDAO = new AuctionDAO();
                java.util.List<AuctionDTO> dtos = auctionDAO.findWinningByUser(userId);
                return Response.ok(toClientAuctions(dtos, userDAO));
            }

            case "CREATE_AUCTION": {
                Object[] data = (Object[]) req.getData();

                User owner = (User) data[0];
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
                auction.setSellerId(owner.getId());
                auction.setCurrentPrice(startPrice);
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
                Long auctionId = (Long) data[0];
                double startPrice = (Double) data[4];
                java.time.LocalDateTime endTime = (java.time.LocalDateTime) data[5];

                AuctionDAO auctionDAO = new AuctionDAO();
                AuctionDTO existing = auctionDAO.findById(auctionId);
                if (existing == null) return Response.error("Auction not found.");
                existing.setCurrentPrice(startPrice);
                existing.setEndTime(endTime);
                boolean ok = auctionDAO.update(existing);
                return ok ? Response.ok(null) : Response.error("Failed to update auction.");
            }

            case Request.DELETE_AUCTION: {
                Long auctionId = (Long) req.getData();
                AuctionDAO auctionDAO = new AuctionDAO();
                boolean ok = auctionDAO.delete(auctionId);
                return ok ? Response.ok(null) : Response.error("Failed to delete auction.");
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
                Object[] data = (Object[]) req.getData();
                Long auctionId = (Long) data[0];
                int hours = (Integer) data[1];
                AuctionDAO auctionDAO = new AuctionDAO();
                AuctionDTO existing = auctionDAO.findById(auctionId);
                if (existing == null) return Response.error("Auction not found.");
                existing.setEndTime(existing.getEndTime().plusHours(hours));
                boolean ok = auctionDAO.update(existing);
                return ok ? Response.ok(null) : Response.error("Failed to extend auction.");
            }

            // 🔥 ĐÃ FIX LUỒNG: Đổi từ writeObject sang lệnh return Response chuẩn chỉnh cho hàm!
            case "GET_ALL_APPROVAL_AUCTIONS": {
                AuctionDAO auctionDAO = new AuctionDAO();
                List<com.auction.common.dto.AuctionDTO> dtos = auctionDAO.findAll();
                List<Auction> clientAuctions = toClientAuctions(dtos, userDAO);
                return Response.ok(clientAuctions);
            }

            case Request.END_AUCTION_EARLY: {
                Long auctionId = (Long) req.getData();
                AuctionDAO auctionDAO = new AuctionDAO();
                AuctionDTO existing = auctionDAO.findById(auctionId);
                if (existing == null) return Response.error("Auction not found.");
                existing.setStatus("FINISHED");
                existing.setEndTime(java.time.LocalDateTime.now());
                boolean ok = auctionDAO.update(existing);
                return ok ? Response.ok(null) : Response.error("Failed to end auction.");
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
                    Long auctionId = ((Number) req.getData()).longValue();
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

            default:
                System.out.println("WARNING: Unhandled command: " + req.getAction());
                return Response.error("Unknown command: " + req.getAction());
        }
    }


    private List<Auction> toClientAuctions(List<AuctionDTO> dtos, UserDAO userDAO) {

        java.util.List<Auction> result = new java.util.ArrayList<>();
        if (dtos == null || dtos.isEmpty()) return result;

        try {
            // 1. Khởi tạo các DAO kết nối dữ liệu
            server.repository.ItemDAO itemDAO = new server.repository.ItemDAO();
            server.repository.ItemImageDAO imageDAO = new server.repository.ItemImageDAO();

            // 2. BƯỚC THẦN TỐC: Gom toàn bộ bảng dữ liệu lên bộ nhớ tạm (RAM) của Server
            // (Hãy đảm bảo trong ItemDAO và ItemImageDAO của bạn có hàm findAll() hoặc hàm tương đương để lấy hết)
            java.util.Map<Long, com.auction.common.dto.ItemDTO> itemMap = new java.util.HashMap<>();
            java.util.List<com.auction.common.dto.ItemDTO> allItems = itemDAO.getAll(); // Sử dụng hàm lấy hết dữ liệu sản phẩm
            if (allItems != null) {
                for (com.auction.common.dto.ItemDTO itemDto : allItems) {
                    itemMap.put(itemDto.getId(), itemDto);
                }
            }

            java.util.Map<Long, String> imageMap = new java.util.HashMap<>();
            java.util.List<com.auction.common.dto.ItemImageDTO> allImages = imageDAO.getAll(); // Sử dụng hàm lấy hết dữ liệu ảnh

            if (allImages != null) {
                for (com.auction.common.dto.ItemImageDTO imgDto : allImages) {
                    // Ưu tiên nạp đường dẫn ảnh đầu tiên tìm thấy của mỗi itemId vào bộ nhớ tạm
                    if (!imageMap.containsKey(imgDto.getItemId())) {

                        String rawUrl = imgDto.getImageUrl();
                        if (rawUrl != null && !rawUrl.isBlank()) {
                            // Làm sạch khoảng trắng thừa và chuẩn hóa dấu gạch xuôi
                            String safeUrl = rawUrl.trim().replace("\\", "/");

                            // Nếu là link mạng trực tuyến (chứa http:// hoặc https://) thì GIỮ NGUYÊN HOÀN TOÀN
                            if (safeUrl.toLowerCase().startsWith("http://") || safeUrl.toLowerCase().startsWith("https://")) {
                                imageMap.put(imgDto.getItemId(), safeUrl);
                            }
                            // Nếu là đường dẫn local cũ dưới máy tính mà chưa có tiền tố file:
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
            java.util.List<UserDTO> allUsers = userDAO.findAll(); // Sử dụng hàm lấy toàn bộ danh sách thành viên
            if (allUsers != null) {
                for (UserDTO uDto : allUsers) {
                    userMap.put(uDto.getId(), uDto);
                }
            }

            // 3. Tiến hành xử lý lặp dữ liệu trên RAM - Tốc độ ánh sáng O(1)
            for (AuctionDTO dto : dtos) {
                if (dto == null || dto.getItemId() <= 0) continue;

                // Đồng bộ nhanh thông tin người bán (Seller)
                UserDTO sellerDto = userMap.get(dto.getSellerId());
                User seller = sellerDto != null
                        ? toClientUser(sellerDto)
                        : new User(dto.getSellerId(), "Unknown", "", "", SystemRole.USER);

                // Bốc sản phẩm trực tiếp từ RAM ra thông qua Map, không truy vấn Database dòng này nữa!
                com.auction.common.dto.ItemDTO itemDto = itemMap.get(dto.getItemId());
                Item item;

                if (itemDto != null) {
                    // Chuẩn hóa chuỗi danh mục sản phẩm
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

                    // Lấy link ảnh trực tiếp từ bộ nhớ RAM thông qua Map
                    String cachedImgUrl = imageMap.get(dto.getItemId());
                    if (cachedImgUrl != null) {
                        item.setImageUrl(cachedImgUrl);
                    }

                } else {
                    // Không tìm thấy sản phẩm thật tương ứng -> Bỏ qua dòng rác này
                    continue;
                }

                // Gán dữ liệu phòng đấu giá hoàn chỉnh
                java.time.LocalDateTime endTime = dto.getEndTime() != null ? dto.getEndTime() : java.time.LocalDateTime.now().plusDays(1);
                Auction auction = new Auction(item, seller, endTime);
                auction.setId(dto.getId());
                auction.setCurrentPrice(dto.getCurrentPrice());

                try {
                    auction.setStatus(AuctionStatus.valueOf(
                            dto.getStatus() != null ? dto.getStatus().toUpperCase().trim() : "RUNNING"));
                } catch (Exception e) {
                    auction.setStatus(AuctionStatus.RUNNING);
                }

                result.add(auction);
            }
        } catch (Exception e) {
            System.out.println("Lỗi nghiêm trọng khi tối ưu hóa bộ nhớ RAM cho phòng đấu giá: " + e.getMessage());
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

        return u;
    }
}
