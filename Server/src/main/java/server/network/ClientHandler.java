package server.network;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.UUID;

import com.auction.client.service.Request;
import com.auction.client.service.Response;
import server.common.model.AuctionDTO;
import server.repository.UserDAO;
import server.repository.AuctionDAO;
import server.common.model.UserDTO;

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
                com.auction.client.model.User clientUser = toClientUser(user);
                Response res = Response.ok(clientUser);
                res.setToken(token);
                return res;
            }

            case Request.REGISTER: {
                com.auction.client.model.User incoming = (com.auction.client.model.User) req.getData();

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
                com.auction.client.model.User incoming = (com.auction.client.model.User) req.getData();
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
                java.util.List<com.auction.client.model.User> users = new java.util.ArrayList<>();
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
                java.util.List<com.auction.client.model.Auction> result = toClientAuctions(single, userDAO);
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
                // data: owner, title, desc, category, condition, startPrice, reservePrice, increment, startTime, endTime, imagePath
                com.auction.client.model.User owner = (com.auction.client.model.User) data[0];
                String title       = (String) data[1];
                String description = (String) data[2];
                String category    = (String) data[3];
                String condition   = (String) data[4];
                double startPrice  = (Double) data[5];
                java.time.LocalDateTime startTime = (java.time.LocalDateTime) data[8];
                java.time.LocalDateTime endTime   = (java.time.LocalDateTime) data[9];

                // Tạo Item trước (nếu có bảng item riêng, insert vào đó)
                // Hiện tại dùng itemId = 0 làm placeholder — cần có ItemDAO thật
                // TODO: thay bằng ItemDAO.insert() khi sẵn sàng
                AuctionDTO auction = new AuctionDTO();
                auction.setItemId(1L); // placeholder
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
    private java.util.List<com.auction.client.model.Auction> toClientAuctions(
            java.util.List<AuctionDTO> dtos, UserDAO userDAO) {

        java.util.List<com.auction.client.model.Auction> result = new java.util.ArrayList<>();
        for (AuctionDTO dto : dtos) {
            try {
                // Build seller User
                UserDTO sellerDto = userDAO.findById(dto.getSellerId());
                com.auction.client.model.User seller = sellerDto != null
                        ? toClientUser(sellerDto)
                        : new com.auction.client.model.User(dto.getSellerId(), "Unknown", "", "",
                        com.auction.client.Enum.SystemRole.USER);

                // Build a minimal Item — use Electronics as concrete subclass (placeholder)
                // Category/type info is not stored in auction table, so default to Electronics
                com.auction.client.model.Electronics item = new com.auction.client.model.Electronics(
                        "Item #" + dto.getItemId(), // name placeholder
                        "",                          // description
                        dto.getCurrentPrice(),        // startingPrice
                        "",                          // brand
                        ""                           // model
                );
                item.setId(dto.getItemId());

                // Build Auction
                com.auction.client.model.Auction auction =
                        new com.auction.client.model.Auction(item, seller, dto.getEndTime());
                auction.setId(dto.getId());
                auction.setCurrentPrice(dto.getCurrentPrice());
                auction.setStatus(com.auction.client.Enum.AuctionStatus.valueOf(
                        dto.getStatus() != null ? dto.getStatus().toUpperCase() : "RUNNING"));

                // Set highest bidder if exists
                if (dto.getHighestBidderId() != null) {
                    UserDTO bidderDto = userDAO.findById(dto.getHighestBidderId());
                    if (bidderDto != null) {
                        auction.setSeller(seller); // keep seller
                        // highestBidder is set via addBid in real flow;
                        // for display purposes we store it on the object
                    }
                }

                result.add(auction);
            } catch (Exception e) {
                System.out.println("WARNING: Failed to convert AuctionDTO id=" + dto.getId() + ": " + e.getMessage());
            }
        }
        return result;
    }

    private com.auction.client.model.User toClientUser(UserDTO dto) {
        com.auction.client.Enum.SystemRole role;
        try {
            role = com.auction.client.Enum.SystemRole.valueOf(
                    dto.getSystemRole() != null ? dto.getSystemRole().toUpperCase() : "USER");
        } catch (IllegalArgumentException e) {
            role = com.auction.client.Enum.SystemRole.USER;
        }

        com.auction.client.model.User u = new com.auction.client.model.User(
                dto.getId(),
                dto.getUsername(),
                dto.getPassword(),
                dto.getEmail(),
                role
        );

        if (dto.getAccountStatus() != null
                && !dto.getAccountStatus().equalsIgnoreCase("ACTIVE")) {
            try {
                com.auction.client.Enum.AccountStatus status =
                        com.auction.client.Enum.AccountStatus.valueOf(dto.getAccountStatus().toUpperCase());
                u.setAccountStatus(status);
            } catch (IllegalArgumentException ignored) {}
        }

        return u;
    }
}