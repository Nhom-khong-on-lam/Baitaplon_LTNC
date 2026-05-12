package server.repository;

import server.common.model.AuctionDTO;
import server.common.model.UserDTO;
import server.common.enums.AuctionStatus;
import server.database.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {

    // 1. Lấy tất cả danh sách đấu giá
    public List<AuctionDTO> getAll() {
        List<AuctionDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM auction";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                AuctionDTO a = new AuctionDTO();
                a.setId(rs.getLong("id"));
                a.setItemId(rs.getLong("item_id"));
                a.setCurrentPrice(rs.getDouble("current_price"));

                // Xử lý thời gian
                Timestamp start = rs.getTimestamp("start_time");
                Timestamp end = rs.getTimestamp("end_time");
                if (start != null) a.setStartTime(start.toLocalDateTime());
                if (end != null) a.setEndTime(end.toLocalDateTime());

                // Khớp Enum: 'OPEN', 'RUNNING', 'FINISHED', 'PAID', 'CANCELED'
                String statusStr = rs.getString("status");
                if (statusStr != null) {
                    a.setStatus(AuctionStatus.valueOf(statusStr.toUpperCase()));
                }

                // Mapping Seller (Khóa ngoại seller_id)
                long sellerId = rs.getLong("seller_id");
                if (!rs.wasNull()) {
                    UserDTO seller = new UserDTO();
                    seller.setId(sellerId);
                    a.setSeller(seller);
                }

                // Mapping Highest Bidder (Khóa ngoại highest_bidder_id)
                long bidderId = rs.getLong("highest_bidder_id");
                if (!rs.wasNull()) {
                    UserDTO bidder = new UserDTO();
                    bidder.setId(bidderId);
                    a.setHighestBidder(bidder);
                }

                list.add(a);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 2. Thêm mới một phiên đấu giá (Khớp với các trường bắt buộc trong ERD)
    public boolean insert(AuctionDTO a) {
        String sql = "INSERT INTO auction (item_id, seller_id, current_price, start_time, end_time, status) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, a.getItemId());
            ps.setLong(2, a.getSeller().getId());
            ps.setDouble(3, a.getCurrentPrice());
            ps.setTimestamp(4, Timestamp.valueOf(a.getStartTime()));
            ps.setTimestamp(5, Timestamp.valueOf(a.getEndTime()));
            // Mặc định trạng thái khi tạo thường là 'OPEN'
            ps.setString(6, a.getStatus() != null ? a.getStatus().name() : "OPEN");

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 3. Cập nhật khi có người trả giá mới (Update current_price và highest_bidder_id)
    public boolean updateBid(long auctionId, double price, long bidderId) {
        String sql = "UPDATE auction SET current_price = ?, highest_bidder_id = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, price);
            ps.setLong(2, bidderId);
            ps.setLong(3, auctionId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 4. Cập nhật trạng thái (Dùng cho FINISHED, PAID, CANCELED theo ERD)
    public boolean updateStatus(long id, AuctionStatus status) {
        String sql = "UPDATE auction SET status = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setLong(2, id);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}