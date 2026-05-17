package server.network;

import com.auction.common.dto.AuctionDTO;
import com.auction.common.dto.BidDTO;
import com.auction.common.dto.ItemDTO;
import com.auction.common.dto.UserDTO;
import com.auction.common.enums.AccountStatus;
import com.auction.common.enums.AuctionStatus;
import com.auction.common.enums.SystemRole;
import com.auction.common.model.*;
import com.auction.common.network.Request;
import com.auction.common.network.Response;
import server.repository.AuctionDAO;
import server.repository.BidDAO;
import server.repository.ItemDAO;
import server.repository.UserDAO;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;



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

            case Request.PLACE_BID: {
                Object[] data = (Object[]) req.getData();
                Long auctionId = (Long) data[0];
                User bidder = (User) data[1];
                Long bidderId  = bidder.getId();
                double amount  = (Double) data[2];

                AuctionDAO auctionDAO = new AuctionDAO();
                AuctionDTO existing = auctionDAO.findById(auctionId);
                if (existing == null) return Response.error("Auction not found.");
                if (existing.getSellerId().equals(bidderId)) {
                    return Response.error("You cannot bid on your own listed product!");
                }
                // Kiểm tra luật: số tiền cược mới phải lớn hơn giá hiện tại
                if (amount <= existing.getCurrentPrice()) {
                    return Response.error("Bid amount must be higher than current price.");
                }

                // Cập nhật giá mới và người ra giá cao nhất vào Object DTO
                existing.setCurrentPrice(amount);
                existing.setHighestBidderId(bidderId);

                // Lưu lại vào Database thông qua DAO
                boolean ok = auctionDAO.update(existing);
                if (!ok) return Response.error("Failed to update bid into Database.");
                try {
                    com.auction.common.dto.BidDTO newBid = new com.auction.common.dto.BidDTO();
                    newBid.setAuctionId(auctionId);
                    newBid.setBidderId(bidderId);
                    newBid.setAmount(amount);
                    newBid.setBidTime(java.time.LocalDateTime.now());
                    newBid.setAutoBid(false); // Mặc định đặt tay thông thường là false

                    // Thực hiện insert dòng lịch sử cược mới này xuống DB bảng bid
                    new server.repository.BidDAO().insert(newBid);
                } catch (Exception e) {
                    System.out.println("WARNING: Failed to save bid transaction history: " + e.getMessage());
                }
                // Đóng gói Auction mới sau khi cập nhật thành công để gửi ngược về cho Client
                List<AuctionDTO> single = new ArrayList<>();
                single.add(existing);
                List<Auction> result = toClientAuctions(single, userDAO);

                // Trả Response OK kèm dữ liệu mới về cho Client
                return Response.ok(result.isEmpty() ? null : result.get(0));
            }

            case Request.LOGOUT: {
                return Response.ok(null);
            }

            case Request.CHECK_USER_EXISTS: {
                String  username = (String) req.getData();
                boolean exists   = userDAO.isExisted("username", username);
                return new Response(exists, exists ? "Username already taken." : "Username is available.", null);
            }

            case Request.CHECK_EMAIL_EXISTS: {
                String  email  = (String) req.getData();
                boolean exists = userDAO.isExisted("email", email);
                return new Response(exists, exists ? "Email already registered." : "Email is available.", null);
            }

            case Request.CHECK_PASSWORD: {
                Object[] data   = (Object[]) req.getData();
                Long     userId = (Long) data[0];
                String   pass   = (String) data[1];
                UserDTO  user   = userDAO.findById(userId);
                if (user == null) return Response.error("User not found.");
                boolean match = user.getPassword().equals(pass);
                return new Response(match, match ? "Password is correct." : "Incorrect password.", null);
            }

            case Request.UPDATE_PASSWORD: {
                Object[] data    = (Object[]) req.getData();
                String   email   = (String) data[0];
                String   newPass = (String) data[1];
                UserDTO  user    = userDAO.findByField("email", email);
                if (user == null) return Response.error("User not found.");
                user.setPassword(newPass);
                boolean ok = userDAO.update(user);
                return ok ? Response.ok(null) : Response.error("Failed to update password.");
            }

            case Request.UPDATE_USER: {
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

            case Request.GET_ACTIVE_AUCTIONS: {
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

            case Request.GET_AUCTIONS_BY_SELLER : {
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

            case Request.GET_WINNING_BIDS: {
                Long userId = (Long) req.getData();
                AuctionDAO auctionDAO = new AuctionDAO();
                java.util.List<AuctionDTO> dtos = auctionDAO.findWinningByUser(userId);
                return Response.ok(toClientAuctions(dtos, userDAO));
            }

            case Request.CREATE_AUCTION: {
                Object[] data = (Object[]) req.getData();
                User owner          = (User)   data[0];
                String title        = (String) data[1];
                String description  = (String) data[2];
                String category     = (String) data[3];
                String condition    = (String) data[4];
                double startPrice   = (Double) data[5];
                LocalDateTime startTime = java.time.LocalDateTime.now();
                LocalDateTime endTime   = java.time.LocalDateTime.now().plusDays(1);
                try {
                    if (data[6] != null && !data[6].toString().isBlank()) {
                        startTime = java.time.LocalDateTime.parse(data[6].toString());
                    }
                    if (data[7] != null && !data[7].toString().isBlank()) {
                        endTime = java.time.LocalDateTime.parse(data[7].toString());
                    }
                } catch (Exception e) {
                    System.out.println("WARNING: Lỗi parse định dạng ngày giờ: " + e.getMessage());
                }
                ItemDAO itemDAO = new ItemDAO();
                ItemDTO itemDTO = new ItemDTO();
                itemDTO.setName(title);
                itemDTO.setDescription(description);
                itemDTO.setCategory(category != null ? category.toUpperCase() : "ELECTRONICS");
                itemDTO.setStartingPrice(startPrice);

                // Set các field riêng theo category
                switch (category != null ? category.toLowerCase() : "") {
                    case "art" -> {
                        itemDTO.setArtist("");        // có thể thêm field sau
                        itemDTO.setProductionYear(0);
                    }
                    case "vehicle" -> {
                        itemDTO.setBrandMake("");
                        itemDTO.setModel("");
                        itemDTO.setProductionYear(0);
                    }
                    default -> {                      // electronics
                        itemDTO.setBrandMake("");
                        itemDTO.setModel("");
                    }
                }

                long itemId = itemDAO.insert(itemDTO);
                if (itemId == -1) return Response.error("Failed to create item.");

                // ✅ Bước 2: Insert Auction với itemId thật
                AuctionDAO auctionDAO = new AuctionDAO();
                AuctionDTO auction = new AuctionDTO();
                auction.setItemId(itemId);            // ← itemId thật, không phải 1L
                auction.setSellerId(owner.getId());
                auction.setCurrentPrice(startPrice);
                auction.setStartTime(startTime != null ? startTime : java.time.LocalDateTime.now());
                auction.setEndTime(endTime);
                auction.setStatus("RUNNING");

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

            // ── DEFAULT ──────────────────────────────────────────────────────

            default:
                System.out.println("WARNING: Unhandled command: " + req.getAction());
                return Response.error("Unknown command: " + req.getAction());
        }
    }

    /**
     * Convert list of AuctionDTO (server) to list of Auction (client model).
     * Creates lightweight Auction objects with Item and User placeholders.
     */
    private List<Auction> toClientAuctions(
            List<AuctionDTO> dtos, UserDAO userDAO) {

        List<Auction> result = new ArrayList<>();
        for (AuctionDTO dto : dtos) {
            try {
                // Build seller User
                UserDTO sellerDto = new server.repository.UserDAO().findById(dto.getSellerId());
                User seller = sellerDto != null
                        ? toClientUser(sellerDto)
                        : new User(dto.getSellerId(), "Unknown", "", "",
                        SystemRole.USER);

                ItemDAO itemDAO = new ItemDAO();
                ItemDTO itemDto = itemDAO.getById(dto.getItemId());

                Item item;
                if (itemDto != null) {
                    item = switch (itemDto.getCategory() != null
                            ? itemDto.getCategory().toLowerCase() : "") {
                        case "art" -> new com.auction.common.model.Art(
                                itemDto.getName(),
                                itemDto.getDescription(),
                                itemDto.getStartingPrice(),
                                itemDto.getArtist() != null ? itemDto.getArtist() : "",
                                itemDto.getProductionYear() != null ? itemDto.getProductionYear() : 0
                        );
                        case "vehicle" -> new com.auction.common.model.Vehicle(
                                itemDto.getName(),
                                itemDto.getDescription(),
                                itemDto.getStartingPrice(),
                                itemDto.getBrandMake() != null ? itemDto.getBrandMake() : "",
                                itemDto.getModel() != null ? itemDto.getModel() : "",
                                itemDto.getProductionYear() != null ? itemDto.getProductionYear() : 0
                        );
                        default -> new com.auction.common.model.Electronics(
                                itemDto.getName(),
                                itemDto.getDescription(),
                                itemDto.getStartingPrice(),
                                itemDto.getBrandMake() != null ? itemDto.getBrandMake() : "",
                                itemDto.getModel() != null ? itemDto.getModel() : ""
                        );
                    };
                } else {
                    // fallback nếu không tìm thấy item trong DB
                    item = new com.auction.common.model.Electronics(
                            "Item #" + dto.getItemId(), "", dto.getCurrentPrice(), "", "");
                }
                item.setId(dto.getItemId());

                // Build Auction
                Auction auction =
                        new Auction(item, seller, dto.getEndTime());
                auction.setId(dto.getId());
                auction.setCurrentPrice(dto.getCurrentPrice());
                auction.setStatus(AuctionStatus.valueOf(dto.getStatus() != null ? dto.getStatus().toUpperCase() : "RUNNING"));
                try {
                    BidDAO bidDAO = new BidDAO();
                    // Lấy toàn bộ danh sách BidDTO thuộc về phiên đấu giá này từ DB lên
                    List<BidDTO> bidDTOs = bidDAO.getBidsByAuctionId(dto.getId());

                    if (bidDTOs != null) {
                        for (BidDTO bDto : bidDTOs) {
                            // Tìm thông tin tài khoản người đặt cược
                            UserDTO bidderDto = userDAO.findById(bDto.getBidderId());
                            User bidder = bidderDto != null ? toClientUser(bidderDto) : null;

                            // Tạo đối tượng BidTransaction khớp với Constructor 3 tham số của bạn
                            // Constructor của bạn: public BidTransaction(User bidder, double amount, boolean autoBid)
                            BidTransaction trans = new BidTransaction(bidder, bDto.getAmount(), bDto.isAutoBid());

                            // Nếu trong Object BidTransaction không có biến chứa ID, dòng dưới đây có thể bỏ qua nếu báo lỗi
                            // trans.setId(bDto.getId());

                            // Nạp vào list lịch sử để hàm .size() bên ngoài tự động đếm
                            auction.getBidHistory().add(trans);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("WARNING: Lỗi nạp lịch sử cược cho phiên " + dto.getId() + ": " + e.getMessage());
                }
                // Set highest bidder if exists
                if (dto.getHighestBidderId() != null) {
                    UserDTO bidderDto = userDAO.findById(dto.getHighestBidderId());
                    if (bidderDto != null) {
                        User highestBidder = toClientUser(bidderDto); // keep seller
                        auction.setHighestBidder(highestBidder);
                    }
                }

                result.add(auction);
            } catch (Exception e) {
                System.out.println("WARNING: Failed to convert AuctionDTO id=" + dto.getId() + ": " + e.getMessage());
            }
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