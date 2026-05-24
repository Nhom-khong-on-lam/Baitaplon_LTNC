package server.repository;

import com.auction.common.dto.PaymentDTO;
import server.database.DBConnection;
import java.sql.*;

public class PaymentDAO {

    // Lấy thông tin hóa đơn theo auctionId
    public PaymentDTO getByAuctionId(long auctionId) {
        String sql = "SELECT * FROM payment WHERE auction_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PaymentDTO p = new PaymentDTO();
                    p.setId(rs.getLong("id"));
                    p.setAuctionId(rs.getLong("auction_id"));
                    p.setBuyerId(rs.getLong("buyer_id"));
                    p.setSellerId(rs.getLong("seller_id"));
                    p.setAmount(rs.getDouble("amount"));
                    p.setStatus(rs.getString("status"));
                    return p;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Insert thông tin hóa đơn thông thường
    public boolean insert(PaymentDTO p) {
        String sql = "INSERT INTO payment (auction_id, buyer_id, seller_id, amount, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, p.getAuctionId());
            ps.setLong(2, p.getBuyerId());
            ps.setLong(3, p.getSellerId());
            ps.setDouble(4, p.getAmount());
            ps.setString(5, p.getStatus());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Cập nhật trạng thái dùng chung Connection phục vụ Transaction dòng tiền
    public boolean updateStatusWithConn(Connection conn, long paymentId, String status) throws SQLException {
        String sql = "UPDATE payment SET status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, paymentId);
            return ps.executeUpdate() > 0;
        }
    }

    // Tạo hóa đơn mới dùng chung Connection phục vụ Transaction dòng tiền
    public boolean insertWithConn(Connection conn, PaymentDTO p) throws SQLException {
        String sql = "INSERT INTO payment (auction_id, buyer_id, seller_id, amount, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, p.getAuctionId());
            ps.setLong(2, p.getBuyerId());
            ps.setLong(3, p.getSellerId());
            ps.setDouble(4, p.getAmount());
            ps.setString(5, p.getStatus());
            return ps.executeUpdate() > 0;
        }
    }
}