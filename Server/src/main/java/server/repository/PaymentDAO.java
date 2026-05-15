package server.repository;


import com.auction.common.dto.PaymentDTO;
import server.database.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {

    public boolean insert(PaymentDTO p) {
        String sql = "INSERT INTO payment (auction_id, buyer_id, seller_id, amount, status, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, p.getAuctionId());
            ps.setLong(2, p.getBuyerId());
            ps.setLong(3, p.getSellerId());
            ps.setDouble(4, p.getAmount());
            ps.setString(5, p.getStatus());
            ps.setTimestamp(6, Timestamp.valueOf(p.getCreatedAt()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi insert Payment: " + e.getMessage());
            return false;
        }
    }

    public List<PaymentDTO> getAll() {
        List<PaymentDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM payment";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToDTO(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public PaymentDTO getByAuctionId(Long auctionId) {
        String sql = "SELECT * FROM payment WHERE auction_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDTO(rs);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    private PaymentDTO mapResultSetToDTO(ResultSet rs) throws SQLException {
        PaymentDTO p = new PaymentDTO();
        p.setId(rs.getLong("id"));
        p.setAuctionId(rs.getLong("auction_id"));
        p.setBuyerId(rs.getLong("buyer_id"));
        p.setSellerId(rs.getLong("seller_id"));
        p.setAmount(rs.getDouble("amount"));
        p.setStatus(rs.getString("status"));
        p.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return p;
    }
}