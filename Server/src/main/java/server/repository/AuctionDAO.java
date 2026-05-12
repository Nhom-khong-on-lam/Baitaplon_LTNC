package server.repository;

import server.common.model.AuctionDTO;
import server.database.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {
    // Sửa lỗi insert: Sử dụng trực tiếp getSellerId() kiểu Long
    public boolean insert(AuctionDTO a) {
        String sql = "INSERT INTO auction (item_id, seller_id, current_price, start_time, end_time, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, a.getItemId());
            ps.setLong(2, a.getSellerId()); // Sửa lỗi gọi getSeller().getId()
            ps.setDouble(3, a.getCurrentPrice());
            ps.setTimestamp(4, Timestamp.valueOf(a.getStartTime()));
            ps.setTimestamp(5, Timestamp.valueOf(a.getEndTime()));
            ps.setString(6, a.getStatus()); // Truyền trực tiếp String thay vì .name()
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private AuctionDTO mapResultSetToDTO(ResultSet rs) throws SQLException {
        AuctionDTO a = new AuctionDTO();
        a.setId(rs.getLong("id"));
        a.setItemId(rs.getLong("item_id"));
        a.setSellerId(rs.getLong("seller_id"));

        long bidderId = rs.getLong("highest_bidder_id");
        if (!rs.wasNull()) {
            a.setHighestBidderId(bidderId);
        }

        a.setCurrentPrice(rs.getDouble("current_price"));
        a.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        a.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
        a.setStatus(rs.getString("status"));
        return a;
    }
}