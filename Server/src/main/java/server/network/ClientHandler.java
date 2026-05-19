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
             ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream())) {

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
                String[] creds    = (String[]) req.getData();
                String   username = creds[0];
                String   password = creds[1];

                UserDTO user = userDAO.findByUsername(username);
                if (user == null || !user.getPassword().equals(password)) {
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

            case Request.REGISTER: {
                User incoming = (User) req.getData();

                if (userDAO.isExisted("username", incoming.getUsername())) {
                    return Response.error("Username already exists.");
                }
                if (userDAO.isExisted("email", incoming.getEmail())) {
                    return Response.error("Email is already in use.");
                }

                UserDTO newUser = new UserDTO();
                newUser.setUsername(incoming.getUsername());
                newUser.setEmail(incoming.getEmail());
                newUser.setPassword(incoming.getPasswordHash());
                newUser.setSystemRole("USER");
                newUser.setAccountStatus("ACTIVE");

                long id = userDAO.insert(newUser);
                if (id == -1) return Response.error("System error. Please try again.");
                return Response.ok(null);
            }

            case Request.LOGOUT: {
                return Response.ok(null);
            }

            case Request.CHECK_USER_EXISTS: {
                String  username = (String) req.getData();
                boolean exists   = userDAO.isExisted("username", username);
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
                String  email  = (String) req.getData();
                boolean exists = userDAO.isExisted("email", email);
                return new Response(exists, exists ? "Email already registered." : "Email is available.", null);
            }

            case "CHECK_PASSWORD": {
                Object[] data   = (Object[]) req.getData();
                Long     userId = (Long) data[0];
                String   pass   = (String) data[1];
                UserDTO  user   = userDAO.findById(userId);
                if (user == null) return Response.error("User not found.");
                boolean match = user.getPassword().equals(pass);
                return new Response(match, match ? "Password is correct." : "Incorrect password.", null);
            }

            case "UPDATE_PASSWORD": {
                Object[] data    = (Object[]) req.getData();
                String   email   = (String) data[0];
                String   newPass = (String) data[1];
                UserDTO  user    = userDAO.findByField("email", email);
                if (user == null) return Response.error("User not found.");
                user.setPassword(newPass);
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
                java.util.List<UserDTO> dtos  = userDAO.findAll();
                java.util.List<User> users = new java.util.ArrayList<>();
                for (UserDTO dto : dtos) users.add(toClientUser(dto));
                return Response.ok(users);
            }

            case Request.PLACE_BID: { // Hoặc thay bằng Request.PLACE_BID nếu bạn có định nghĩa hằng số
                Object[] data = (Object[]) req.getData();
                Long auctionId = (Long) data[0];
                User bidder = (User) data[1];
                Double bidAmount = (Double) data[2];

                AuctionDAO auctionDAO = new AuctionDAO();
                AuctionDTO auctionDto = auctionDAO.findById(auctionId);

                if (auctionDto == null) {
                    return Response.error("Phiên đấu giá không tồn tại.");
                }

                // 🚀 CHỐT CHẶN BẢO MẬT TUYỆT ĐỐI: Người bán không được phép tự đặt giá sản phẩm của mình
                if (auctionDto.getSellerId() == bidder.getId()) {
                    return Response.error("Security Violation: You cannot bid on your own auction!");
                }

                // Kiểm tra xem giá bid mới có lớn hơn giá hiện tại không
                if (bidAmount <= auctionDto.getCurrentPrice()) {
                    return Response.error("Giá đặt phải lớn hơn giá hiện tại.");
                }

                // ── TIẾN HÀNH LƯU ĐẶT GIÁ VÀO DATABASE ──
                // 1. Cập nhật lại giá hiện tại của phiên đấu giá trong DB
                auctionDto.setCurrentPrice(bidAmount);
                // Nếu DB của bạn có trường lưu ID người giữ giá cao nhất, gán thêm tại đây:
                // auctionDto.setHighestBidderId(bidder.getId());

                //  THÊM LOGIC ANTI-SNIPING Ở ĐÂY ĐỂ LƯU XUỐNG DATABASE 
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                java.time.LocalDateTime endTime = auctionDto.getEndTime();

                if (endTime != null && now.isBefore(endTime)) {
                    // Tính số giây còn lại thực tế trên Server
                    long secondsRemaining = java.time.Duration.between(now, endTime).getSeconds();

                    // Luật: Nếu lượt đặt giá diễn ra trong 3 phút cuối cùng (180 giây)
                    if (secondsRemaining <= 180) {
                        // Cộng thêm 3 phút (180 giây) vào thời gian kết thúc
                        auctionDto.setEndTime(endTime.plusSeconds(180));
                        System.out.println("Anti-sniping kích hoạt: Phiên " + auctionId + " được gia hạn thêm 3 phút.");
                    }
                }

                boolean updateSuccess = auctionDAO.update(auctionDto);
                if (!updateSuccess) {
                    return Response.error("Không thể cập nhật giá đấu.");
                }

                // 2. Lưu thông tin giao dịch đặt giá vào bảng lịch sử đấu giá (Bảng bid / bid_transaction)
                // Hãy đảm bảo bạn đã tạo lớp BidDAO hoặc xử lý lưu lịch sử đấu giá ở đây nếu dự án yêu cầu:
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
                Long    userId = (Long) req.getData();
                boolean ok     = userDAO.delete(userId);
                return ok ? Response.ok(null) : Response.error("Failed to delete user.");
            }

            case Request.ADMIN_UPDATE_USER_STATUS: {
                Object[] data   = (Object[]) req.getData();
                Long     userId = (Long) data[0];
                String   status = data[1].toString();
                UserDTO  user   = userDAO.findById(userId);
                if (user == null) return Response.error("User not found.");
                user.setAccountStatus(status);
                boolean ok = userDAO.update(user);
                return ok ? Response.ok(null) : Response.error("Failed to update account status.");
            }

            // ── AUCTION ──────────────────────────────────────────────────────

            case Request.GET_AUCTIONS: {
                AuctionDAO auctionDAO = new AuctionDAO();
                java.util.List<AuctionDTO> dtos = auctionDAO.findAll();
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

                User owner          = (User) data[0];
                String title        = data.length > 1 ? (String) data[1] : "Sản phẩm không tên";
                String description  = data.length > 2 ? (String) data[2] : "";
                String category     = data.length > 3 ? (String) data[3] : "Electronics";
                String condition    = data.length > 4 ? (String) data[4] : "New";
                double startPrice   = data.length > 5 ? (Double) data[5] : 0.0;

                java.time.LocalDateTime startTime = data.length > 8 ? (java.time.LocalDateTime) data[8] : java.time.LocalDateTime.now();
                java.time.LocalDateTime endTime   = data.length > 9 ? (java.time.LocalDateTime) data[9] : java.time.LocalDateTime.now().plusDays(1);
                String imagePath    = data.length > 10 ? (String) data[10] : null;

                // ── BƯỚC 1: TẠO VÀ LƯU SẢN PHẨM (ITEM) VÀO DATABASE TRƯỚC ──
                server.repository.ItemDAO itemDAO = new server.repository.ItemDAO();
                com.auction.common.dto.ItemDTO newItemDto = new com.auction.common.dto.ItemDTO();
                newItemDto.setName(title);
                newItemDto.setDescription(description);
                newItemDto.setCategory(category);
                newItemDto.setStartingPrice(startPrice);

                // Thực hiện hàm insert sản phẩm của bạn để lấy ID tự sinh từ DB
                long generatedItemId = itemDAO.insert(newItemDto);
                if (generatedItemId == -1) return Response.error("Failed to create product item.");

                // ── BƯỚC 2: TẠO VÀ LƯU ẢNH SẢN PHẨM (NẾU CÓ TRUYỀN LINK ẢNH) ──
                if (imagePath != null && !imagePath.isBlank()) {
                    server.repository.ItemImageDAO imageDAO = new server.repository.ItemImageDAO();
                    com.auction.common.dto.ItemImageDTO imgDto = new com.auction.common.dto.ItemImageDTO();
                    imgDto.setItemId(generatedItemId);
                    imgDto.setImageUrl(imagePath);
                    imageDAO.insert(imgDto); // Lưu đường dẫn ảnh xuống DB
                }

                // ── BƯỚC 3: TẠO PHIÊN ĐẤU GIÁ LIÊN KẾT CHÍNH XÁC VỚI ITEM VỪA TẠO ──
                AuctionDTO auction = new AuctionDTO();
                auction.setItemId(generatedItemId); // Đã đồng bộ ID thật, không còn dùng placeholder số 1 nữa!
                auction.setSellerId(owner.getId());
                auction.setCurrentPrice(startPrice);
                auction.setStartTime(startTime != null ? startTime : java.time.LocalDateTime.now());
                auction.setEndTime(endTime);
                auction.setStatus("RUNNING");

                AuctionDAO auctionDAO = new AuctionDAO();
                long newId = auctionDAO.insert(auction);
                if (newId == -1) return Response.error("Failed to create auction.");

                return Response.ok(newId);
            }

            case Request.UPDATE_AUCTION: {
                Object[] data = (Object[]) req.getData();
                Long auctionId   = (Long)   data[0];
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

                // Khởi tạo lớp xử lý User, đổi tên biến thành userRepo để không trùng
                server.repository.UserDAO userRepo = new server.repository.UserDAO();

                // Khớp chuẩn 100% với các hàm có sẵn trong file AuctionDAO.java của bạn
                java.util.List<com.auction.common.dto.AuctionDTO> allDtos     = auctionDAO.findAll();
                java.util.List<com.auction.common.dto.AuctionDTO> bidDtos     = auctionDAO.findByBidder(userId);
                java.util.List<com.auction.common.dto.AuctionDTO> winningDtos = auctionDAO.findWinningByUser(userId);
                java.util.List<com.auction.common.dto.AuctionDTO> productDtos = auctionDAO.findBySeller(userId);
                java.util.List<com.auction.common.dto.AuctionDTO> liveDtos    = auctionDAO.findActive();

                // Chuyển đổi DTO sang Model List<Auction> thông qua hàm xử lý của bạn
                java.util.List<Auction> all      = toClientAuctions(allDtos, userRepo);
                java.util.List<Auction> bids     = toClientAuctions(bidDtos, userRepo);
                java.util.List<Auction> winning  = toClientAuctions(winningDtos, userRepo);
                java.util.List<Auction> products = toClientAuctions(productDtos, userRepo);
                java.util.List<Auction> live     = toClientAuctions(liveDtos, userRepo);

                // Gom gọn gàng vào 1 Object duy nhất để chuyển về cho Client
                DashboardData dashboardData = new DashboardData(all, bids, winning, products, live);

                return Response.ok(dashboardData);
            }

            case Request.EXTEND_AUCTION: {
                Object[] data  = (Object[]) req.getData();
                Long auctionId = (Long) data[0];
                int hours      = (Integer) data[1];
                AuctionDAO auctionDAO = new AuctionDAO();
                AuctionDTO existing = auctionDAO.findById(auctionId);
                if (existing == null) return Response.error("Auction not found.");
                existing.setEndTime(existing.getEndTime().plusHours(hours));
                boolean ok = auctionDAO.update(existing);
                return ok ? Response.ok(null) : Response.error("Failed to extend auction.");
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

            case Request.GET_BID_HISTORY: { // Hoặc case "GET_BID_HISTORY": nếu bạn dùng chuỗi
                Long targetAuctionId = (Long) req.getData();
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

                        // Đóng gói thành Model gửi về Client (lấy luôn trạng thái isAutoBid từ DB)
                        BidTransaction transaction = new BidTransaction(bidderUser, bDto.getAmount(), bDto.isAutoBid());

                        // Đồng bộ thời gian đặt giá chuẩn từ Database lên giao diện
                        if (bDto.getBidTime() != null) {
                            transaction.setBidTime(bDto.getBidTime()); // Hoặc setBidTime tuỳ theo tên hàm trong class BidTransaction của bạn
                        }

                        transactions.add(transaction);
                    }
                }
                return Response.ok(transactions);
            }

            // ── DEFAULT ──────────────────────────────────────────────────────

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
                        if (rawUrl != null) {
                            // Xử lý đường dẫn Windows thành chuẩn URI của JavaFX ngay tại đây
                            String safeUrl = rawUrl.replace("\\", "/");
                            if (!safeUrl.startsWith("http") && !safeUrl.startsWith("file:")) {
                                safeUrl = "file:///" + safeUrl;
                            }

                            // Lưu đường dẫn đã xử lý an toàn vào Map
                            imageMap.put(imgDto.getItemId(), safeUrl);
                        }
                    }
                }
            }

            // 3. Tiến hành xử lý lặp dữ liệu trên RAM - Tốc độ ánh sáng O(1)
            for (AuctionDTO dto : dtos) {
                if (dto == null || dto.getItemId() <= 0) continue;

                // Đồng bộ nhanh thông tin người bán (Seller)
                UserDTO sellerDto = userDAO.findById(dto.getSellerId());
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
