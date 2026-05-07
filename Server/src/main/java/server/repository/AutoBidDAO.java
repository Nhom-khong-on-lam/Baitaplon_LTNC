package server.repository;

import com.auction.client.DAO.DBConnection;
import com.auction.client.model.AutoBidConfig;
import com.auction.client.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AutoBidDAO {

    /**
     * Lưu cấu hình AutoBid mới vào Database.
     */
    public void save(AutoBidConfig config, Long auctionId) {
        // Cập nhật chuẩn bảng auto_bid và các cột theo ERD
        String sql = "INSERT INTO AutoBidConfigs (auction_id, bidder_id, max_amount, increment, active, registered_at) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, auctionId);
            stmt.setLong(2, config.getBidder().getId());

            stmt.setDouble(3, config.getMaxPrice());        
            stmt.setDouble(4, config.getStepIncrement());   

            stmt.setBoolean(5, config.isActive());
            stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));

            stmt.executeUpdate();

            // Gán lại ID cho Config nếu cần thao tác về sau
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                     config.setId(generatedKeys.getLong(1)); // Mở comment nếu AutoBidConfig có biến id
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lưu AutoBidConfig: " + e.getMessage());
        }
    }

    /**
     * Cập nhật trạng thái bật/tắt (active) của một cấu hình AutoBid.
     * (Thường dùng khi hệ thống tự động tắt AutoBid vì giá đã vượt maxAmount)
     */
    public void updateActiveStatus(Long configId, boolean isActive) {
        String sql = "UPDATE auto_bid SET active = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (conn == null) return;
            stmt.setBoolean(1, isActive);
            stmt.setLong(2, configId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Lỗi cập nhật trạng thái AutoBid: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách các cấu hình AutoBid đang KÍCH HOẠT của một phiên đấu giá.
     * Hàm này rất quan trọng để hệ thống chạy logic tự động nâng giá.
     */
    public List<AutoBidConfig> getActiveConfigsForAuction(Long auctionId) {
        List<AutoBidConfig> configs = new ArrayList<>();
        //Tìm các auto_bid đang active = 1 (true)
        // Sắp xếp theo thời gian đăng ký (Ai đăng ký Auto-bid trước sẽ được ưu tiên chạy trước nếu trùng giá)
        String sql = "SELECT * FROM auto_bid WHERE auction_id = ? AND active = 1 ORDER BY registered_at ASC";

        // Dùng vòng lặp while(rs.next()) để lấy dữ liệu, khởi tạo đối tượng AutoBidConfig và add vào list.
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (conn == null) return configs;
            stmt.setLong(1, auctionId);

            try (ResultSet rs = stmt.executeQuery()) {
                UserDAO userDAO = new UserDAO(conn);

                while (rs.next()) {
                    // Gọi UserDAO lấy thông tin người dùng
                    long bidderId = rs.getLong("bidder_id");
                    User realBidder = userDAO.findById(bidderId);

                    if (realBidder == null) continue; // Bỏ qua nếu user không tồn tại

                    // Map từ Database lên Java
                    double maxPrice = rs.getDouble("max_amount");
                    double stepIncrement = rs.getDouble("increment");

                    // Đóng gói vào đối tượng
                    AutoBidConfig config = new AutoBidConfig(realBidder, maxPrice, stepIncrement);
                    config.setId(rs.getLong("id"));
                    config.setActive(rs.getBoolean("active"));
                    config.setRegisteredAt(rs.getTimestamp("registered_at").toLocalDateTime());

                    configs.add(config);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy danh sách AutoBid: " + e.getMessage());
        }
        return configs;
    }
} 
