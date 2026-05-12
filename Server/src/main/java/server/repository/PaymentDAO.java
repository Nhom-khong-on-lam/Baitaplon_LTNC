package server.repository;

import server.common.model.PaymentDTO;
import server.database.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {

    // 1. Thêm bản ghi thanh toán mới
    public boolean insert(PaymentDTO p) {
        String sql = "INSERT INTO payment (auction_id, user_id, amount, payment_method, status, payment_date) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, p.getAuctionId());
            ps.setLong(2, p.getUserId());
            ps.setDouble(3, p.getAmount());
            ps.setString(4, p.getPaymentMethod());
            ps.setString(5, p.getStatus() != null ? p.getStatus() : "PENDING");

            // Nếu paymentDate null thì lấy thời gian hiện tại
            LocalDateTime now = LocalDateTime.now();
            ps.setTimestamp(6, p.getPaymentDate() != null ?
                    Timestamp.valueOf(p.getPaymentDate()) : Timestamp.valueOf(now));

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi insert Payment: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    // 2. Cập nhật trạng thái thanh toán
    public boolean updateStatus(long paymentId, String status) {
        String sql = "UPDATE payment SET status = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setLong(2, paymentId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi updateStatus Payment: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    // 3. Lấy lịch sử thanh toán của một người dùng (Trả về danh sách DTO để dễ xử lý ở Client)
    public List<PaymentDTO> getPaymentHistory(long userId) {
        List<PaymentDTO> history = new ArrayList<>();
        String sql = "SELECT * FROM payment WHERE user_id = ? ORDER BY payment_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PaymentDTO p = new PaymentDTO();
                    p.setId(rs.getLong("id"));
                    p.setAuctionId(rs.getLong("auction_id"));
                    p.setUserId(rs.getLong("user_id"));
                    p.setAmount(rs.getDouble("amount"));
                    p.setPaymentMethod(rs.getString("payment_method"));
                    p.setStatus(rs.getString("status"));

                    Timestamp ts = rs.getTimestamp("payment_date");
                    if (ts != null) p.setPaymentDate(ts.toLocalDateTime());

                    history.add(p);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi getPaymentHistory: " + e.getMessage());
            e.printStackTrace();
        }
        return history;
    }
}