package server.repository;

import server.common.model.PaymentDTO;
import server.database.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {

    // Thêm mới thanh toán (Insert)
    public boolean insert(PaymentDTO p) {
        String sql = "INSERT INTO payment (auction_id, buyer_id, seller_id, amount, status, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            Connection conn = DBConnection.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, p.getAuctionId());
                ps.setLong(2, p.getBuyerId());
                ps.setLong(3, p.getSellerId());
                ps.setDouble(4, p.getAmount());
                ps.setString(5, p.getStatus()); // Ví dụ: 'PENDING', 'COMPLETED'
                ps.setTimestamp(6, Timestamp.valueOf(p.getCreatedAt()));

                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Lỗi insert Payment: " + e.getMessage());
            return false;
        }
    }

    // Lấy tất cả thanh toán (GetAll)
    public List<PaymentDTO> getAll() {
        List<PaymentDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM payment";
        try {
            Connection conn = DBConnection.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PaymentDTO p = new PaymentDTO();
                    p.setId(rs.getLong("id"));
                    p.setAuctionId(rs.getLong("auction_id"));
                    p.setBuyerId(rs.getLong("buyer_id"));
                    p.setSellerId(rs.getLong("seller_id"));
                    p.setAmount(rs.getDouble("amount"));
                    p.setStatus(rs.getString("status"));
                    p.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    list.add(p);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}