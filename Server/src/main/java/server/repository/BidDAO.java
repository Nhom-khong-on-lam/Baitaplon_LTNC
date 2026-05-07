package server.repository;

import com.auction.client.model.BidTransaction;
import com.auction.client.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {

    // 1. Hàm lưu dữ liệu
    public void save(BidTransaction bid, Long auctionId) {
        String sql = "INSERT INTO bid (auction_id, bidder_id, amount, bid_time, auto_bid) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (conn == null) return;

            stmt.setLong(1, auctionId);
            stmt.setLong(2, bid.getBidder().getId());
            stmt.setDouble(3, bid.getAmount());
            stmt.setTimestamp(4, Timestamp.valueOf(bid.getBidTime()));
            stmt.setBoolean(5, bid.isAutoBid());

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    bid.setId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lưu BidTransaction: " + e.getMessage());
        }
    }

    // 2. Hàm lấy dữ liệu (ĐÃ NÂNG CẤP: Sử dụng UserDAO thay cho Dummy User)
    public List<BidTransaction> getBidsByAuctionId(Long auctionId) {
        List<BidTransaction> bidHistory = new ArrayList<>();
        String sql = "SELECT * FROM bid WHERE auction_id = ? ORDER BY bid_time DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (conn == null) return bidHistory;

            stmt.setLong(1, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {

                // Khởi tạo UserDAO một lần bên ngoài vòng lặp để tối ưu hiệu suất
                // Truyền chung 'conn' đang dùng của BidDAO sang cho UserDAO dùng ké
                UserDAO userDAO = new UserDAO(conn);

                while (rs.next()) {
                    // Lấy ID của người dùng từ bảng 'bid'
                    long bidderId = rs.getLong("bidder_id");

                    // Dùng UserDAO để lấy TOÀN BỘ thông tin User thật từ bảng 'user'
                    User realBidder = userDAO.findById(bidderId);

                    // Bắt lỗi nếu Database bị lỗi mất dữ liệu User
                    if (realBidder == null) {
                        System.err.println("Cảnh báo: Không tìm thấy User ID " + bidderId + " trong DB!");
                        continue; // Bỏ qua lượt đặt giá bị lỗi này
                    }

                    // Đọc các thông tin còn lại của giao dịch
                    double amount = rs.getDouble("amount");
                    boolean isAuto = rs.getBoolean("auto_bid");

                    // Lắp ráp đối tượng BidTransaction hoàn chỉnh với User thật
                    BidTransaction bid = new BidTransaction(realBidder, amount, isAuto);
                    bid.setId(rs.getLong("id"));

                    // Ghi đè thời gian từ Database (ép kiểu từ Timestamp sang LocalDateTime)
                    bid.setBidTime(rs.getTimestamp("bid_time").toLocalDateTime());

                    bidHistory.add(bid);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy lịch sử đấu giá: " + e.getMessage());
        }
        return bidHistory;
    }
}
