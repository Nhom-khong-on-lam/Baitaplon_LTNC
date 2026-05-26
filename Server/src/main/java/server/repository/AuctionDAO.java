package server.repository;


import com.auction.common.dto.AuctionDTO;
import server.database.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionDAO {

    private static final Logger LOGGER = Logger.getLogger(AuctionDAO.class.getName());

    // ── INSERT ────────────────────────────────────────────────────────────────
    public long insert(AuctionDTO a) {
        String sql = "INSERT INTO auction (item_id, seller_id, current_price, start_time, end_time, status) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, a.getItemId());
            ps.setLong(2, a.getSellerId());
            ps.setDouble(3, a.getCurrentPrice());
            ps.setTimestamp(4, Timestamp.valueOf(a.getStartTime()));
            ps.setTimestamp(5, Timestamp.valueOf(a.getEndTime()));
            ps.setString(6, a.getStatus() != null ? a.getStatus() : "PENDING_APPROVAL");

            // Ép kiểu tường minh bằng cách kiểm tra nếu lấy ra bị lỗi thì gán cứng số, hoặc in ra log ngay tại đây
            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        long id = rs.getLong(1);
                        LOGGER.info("INSERT auction SUCCESS: ID=" + id);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "INSERT auction ERROR", e);
        }
        return -1;
    }

    // ── FIND BY ID (Đã nâng cấp thông minh lấy giá đỉnh thực tế từ bảng BID) ──
    public AuctionDTO findById(long id) {
        String sql = "SELECT a.*, " +
                "COALESCE((SELECT MAX(amount) FROM bid WHERE auction_id = a.id), a.current_price) AS real_current_price " +
                "FROM auction a WHERE a.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    AuctionDTO dto = mapRow(rs);
                    // Ép đè giá trị thực tế cao nhất vào DTO để tránh bị ghi đè giá cũ khi UPDATE
                    dto.setCurrentPrice(rs.getDouble("real_current_price"));
                    return dto;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findById auction ERROR id=" + id, e);
        }
        return null;
    }

    // ── FIND ALL ──────────────────────────────────────────────────────────────
    // SỬA TẠI AuctionDAO.java (SERVER) - Hàm findAll()
    public List<AuctionDTO> findAll() {
        List<AuctionDTO> list = new ArrayList<>();
        // Bổ sung Subquery lấy giá đỉnh thực tế và LEFT JOIN để giữ trạng thái thanh toán
        String sql = "SELECT a.*, p.status AS payment_status, " +
                "COALESCE((SELECT MAX(amount) FROM bid WHERE auction_id = a.id), a.current_price) AS real_current_price " +
                "FROM auction a " +
                "LEFT JOIN payment p ON a.id = p.auction_id " +
                "ORDER BY a.start_time DESC";

        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                AuctionDTO dto = mapRow(rs);
                dto.setCurrentPrice(rs.getDouble("real_current_price")); // Ép giá đỉnh thực tế vào DTO
                list.add(dto);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findAll auction ERROR", e);
        }
        return list;
    }

    // ── FIND ACTIVE (status = RUNNING và chưa hết hạn) ───────────────────────
    public List<AuctionDTO> findActive() {
        List<AuctionDTO> list = new ArrayList<>();
        // Bổ sung Subquery lấy giá cược cao nhất thực tế cho các phiên đang active
        String sql = "SELECT a.*, " +
                "COALESCE((SELECT MAX(amount) FROM bid WHERE auction_id = a.id), a.current_price) AS real_current_price " +
                "FROM auction a " +
                "WHERE a.status = 'RUNNING' AND a.end_time > NOW() ORDER BY a.end_time ASC";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                AuctionDTO dto = mapRow(rs);
                dto.setCurrentPrice(rs.getDouble("real_current_price")); // Ép giá đỉnh thực tế vào DTO
                list.add(dto);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findActive auction ERROR", e);
        }
        return list;
    }

    // ── FIND PUBLIC (status = RUNNING hoặc FINISHED hoặc LIVE) ───────────────────────
    public List<AuctionDTO> findPublic() {
        List<AuctionDTO> list = new ArrayList<>();
        // Thêm trạng thái 'LIVE' để khi gia hạn xong trang chủ vẫn load được phòng, đồng thời nạp giá đỉnh thực tế
        String sql = "SELECT a.*, " +
                "COALESCE((SELECT MAX(amount) FROM bid WHERE auction_id = a.id), a.current_price) AS real_current_price " +
                "FROM auction a " +
                "WHERE a.status IN ('RUNNING', 'FINISHED', 'LIVE') ORDER BY a.end_time DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                AuctionDTO dto = mapRow(rs);
                dto.setCurrentPrice(rs.getDouble("real_current_price")); // Ép giá đỉnh thực tế vào DTO
                list.add(dto);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findPublic auction ERROR", e);
        }
        return list;
    }

    // ── FIND BY SELLER ────────────────────────────────────────────────────────
    // ── FIND BY SELLER (Đã cập nhật LEFT JOIN để lấy trạng thái thanh toán) ────────
    public List<AuctionDTO> findBySeller(long sellerId) {
        List<AuctionDTO> list = new ArrayList<>();
        // Thực hiện LEFT JOIN để lấy cột status từ bảng payment
        String sql = "SELECT a.*, p.status AS payment_status " +
                "FROM auction a " +
                "LEFT JOIN payment p ON a.id = p.auction_id " +
                "WHERE a.seller_id = ? ORDER BY a.start_time DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findBySeller auction ERROR sellerId=" + sellerId, e);
        }
        return list;
    }

    // ── FIND BY BIDDER (auctions the user has bid on) ─────────────────────────
    public List<AuctionDTO> findByBidder(long bidderId) {
        List<AuctionDTO> list = new ArrayList<>();
        String sql = "SELECT DISTINCT a.* FROM auction a " +
                "JOIN bid b ON a.id = b.auction_id " +
                "WHERE b.bidder_id = ? ORDER BY a.end_time DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bidderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findByBidder auction ERROR bidderId=" + bidderId, e);
        }
        return list;
    }

    // ── FIND WINNING BIDS (auctions user is highest bidder) ──────────────────
    public List<AuctionDTO> findWinningByUser(long userId) {
        List<AuctionDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM auction WHERE highest_bidder_id = ? ORDER BY end_time DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findWinningByUser auction ERROR userId=" + userId, e);
        }
        return list;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────
    // ── UPDATE (ĐÃ TỰ ĐỘNG SINH HÓA ĐƠN PENDING KHI KẾT THÚC PHIÊN) ────────────────
    public boolean update(AuctionDTO a) {
        String updateAuctionSql = "UPDATE auction SET current_price=?, highest_bidder_id=?, end_time=?, status=? WHERE id=? AND current_price < ?";
        String checkPaymentSql = "SELECT COUNT(*) FROM payment WHERE auction_id = ?";
        String insertPaymentSql = "INSERT INTO payment (auction_id, user_id, amount, status, created_at) VALUES (?, ?, ?, 'PENDING', NOW())";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // BẬT TRANSACTION: Đảm bảo đồng bộ an toàn cả 2 bảng

            // 1. Thực hiện cập nhật thông tin phiên đấu giá như bình thường
            try (PreparedStatement ps = conn.prepareStatement(updateAuctionSql)) {
                ps.setDouble(1, a.getCurrentPrice());
                if (a.getHighestBidderId() != null && a.getHighestBidderId() > 0) {
                    ps.setLong(2, a.getHighestBidderId());
                } else {
                    ps.setNull(2, Types.BIGINT);
                }
                ps.setTimestamp(3, Timestamp.valueOf(a.getEndTime()));
                ps.setString(4, a.getStatus());
                ps.setLong(5, a.getId());
                ps.setDouble(6, a.getCurrentPrice()); // chỉ update nếu giá trong DB vẫn thấp hơn giá mới

                int rowsAffected = ps.executeUpdate();
                if (rowsAffected == 0) {
                    conn.rollback();
                    return false;
                }
            }

            // 2. TỰ ĐỘNG XỬ LÝ SINH HÓA ĐƠN KHI TRẠNG THÁI CHUYỂN SANG FINISHED
            if ("FINISHED".equalsIgnoreCase(a.getStatus()) && a.getHighestBidderId() != null && a.getHighestBidderId() > 0) {

                // Kiểm tra xem hóa đơn cho phiên đấu giá này đã được tạo trước đó chưa (tránh trùng lặp dữ liệu)
                boolean isPaymentExist = false;
                try (PreparedStatement psCheck = conn.prepareStatement(checkPaymentSql)) {
                    psCheck.setLong(1, a.getId());
                    try (ResultSet rs = psCheck.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            isPaymentExist = true;
                        }
                    }
                }

                // Nếu chưa từng có bản ghi hóa đơn, thực hiện INSERT trạng thái PENDING
                if (!isPaymentExist) {
                    try (PreparedStatement psInsert = conn.prepareStatement(insertPaymentSql)) {
                        psInsert.setLong(1, a.getId());
                        psInsert.setLong(2, a.getHighestBidderId()); // Người thắng cuộc phải trả tiền
                        psInsert.setDouble(3, a.getCurrentPrice());  // Số tiền cuối cùng chốt phiên

                        psInsert.executeUpdate();
                        LOGGER.info("🌟 AUTO GENERATE INVOICE SUCCESS: Auction ID=" + a.getId() + " is now PENDING payment.");
                    }
                }
            }

            conn.commit(); // Hoàn tất giao dịch thành công cho cả 2 bảng
            LOGGER.info("UPDATE auction SUCCESS: ID=" + a.getId());
            return true;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "UPDATE auction TRANSACTION ERROR id=" + a.getId(), e);
            if (conn != null) {
                try {
                    conn.rollback(); // Hủy bỏ toàn bộ thao tác nếu một trong hai câu lệnh SQL bị lỗi
                    LOGGER.info("TRANSACTION ROLLBACK SUCCESS due to error.");
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Rollback failed", ex);
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    // ── 1. LẤY DANH SÁCH AUCTION THEO TRẠNG THÁI CỦA ADMIN ─────────────────────
    public List<AuctionDTO> getByStatus(String status) {
        List<AuctionDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM auction WHERE status = ? ORDER BY id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error in getByStatus with status: " + status, e);
        }
        return list;
    }

    // ── 2. DUYỆT PHÒNG ĐẤU GIÁ (Đổi trạng thái sang RUNNING + Đặt thời gian bắt đầu) ──
    public boolean updateStatusAndStartTime(long auctionId, String status, java.time.LocalDateTime startTime) {
        String sql = "UPDATE auction SET status = ?, start_time = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setTimestamp(2, Timestamp.valueOf(startTime));
            ps.setLong(3, auctionId);

            boolean ok = ps.executeUpdate() > 0;
            if (ok) LOGGER.info("ADMIN APPROVED AUCTION SUCCESS: ID=" + auctionId);
            return ok;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error in updateStatusAndStartTime for ID = " + auctionId, e);
        }
        return false;
    }

    // ── 3. TỪ CHỐI DUYỆT PHÒNG ĐẤU GIÁ (Cập nhật trạng thái thành REJECTED) ──────────
    public boolean updateStatus(long auctionId, String status) {
        String sql = "UPDATE auction SET status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setLong(2, auctionId);

            boolean ok = ps.executeUpdate() > 0;
            if (ok) LOGGER.info("ADMIN REJECTED AUCTION SUCCESS: ID=" + auctionId);
            return ok;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error in updateStatus for ID = " + auctionId, e);
        }
        return false;
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    public boolean delete(long id) {
        String sql = "DELETE FROM auction WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) LOGGER.info("DELETE auction SUCCESS: ID=" + id);
            return ok;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "DELETE auction ERROR id=" + id, e);
        }
        return false;
    }

    // ── MAP ROW TRỰC TIẾP SANG MODEL AUCTION (Cập nhật bên Server) ───────────────
    private com.auction.common.dto.AuctionDTO mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        com.auction.common.dto.AuctionDTO dto = new com.auction.common.dto.AuctionDTO();

        dto.setId(rs.getLong("id"));
        dto.setItemId(rs.getLong("item_id"));
        dto.setSellerId(rs.getLong("seller_id"));
        dto.setHighestBidderId(rs.getLong("highest_bidder_id"));
        dto.setCurrentPrice(rs.getDouble("current_price"));
        dto.setStatus(rs.getString("status"));

        // Đọc các trường mở rộng phục vụ hiển thị trực tiếp từ SQL JOIN
        try {
            dto.setItemName(rs.getString("item_name"));
        } catch (Exception e) {
        }
        try {
            dto.setCategory(rs.getString("category"));
        } catch (Exception e) {
        }
        try {
            dto.setSellerUsername(rs.getString("seller_username"));
        } catch (Exception e) {
        }

        // Ép kiểu an toàn trường dữ liệu thời gian
        try {
            if (rs.getTimestamp("start_time") != null) {
                dto.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
            }
            if (rs.getTimestamp("end_time") != null) {
                dto.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
            }
        } catch (java.sql.SQLException e) {
        }

        // TRẠNG THÁI THANH TOÁN (Từ câu lệnh LEFT JOIN bảng payment)
        try {
            // Kiểm tra xem ResultSet hiện tại có chứa cột "payment_status" hay không
            java.sql.ResultSetMetaData metaData = rs.getMetaData();
            boolean hasPaymentStatusColumn = false;
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                if ("payment_status".equalsIgnoreCase(metaData.getColumnLabel(i))) {
                    hasPaymentStatusColumn = true;
                    break;
                }
            }

            if (hasPaymentStatusColumn) {
                String payStatusStr = rs.getString("payment_status");
                if (payStatusStr != null) {
                    String cleanStatus = payStatusStr.trim().toUpperCase();
                    if ("PENDING".equals(cleanStatus) || "UNPAID".equals(cleanStatus)) {
                        dto.setPaymentStatus(com.auction.common.enums.PaymentStatus.PENDING);
                    } else if ("COMPLETED".equals(cleanStatus) || "SUCCESS".equals(cleanStatus) || "PAID".equals(cleanStatus)) {
                        dto.setPaymentStatus(com.auction.common.enums.PaymentStatus.COMPLETED);
                    } else if ("FAILED".equals(cleanStatus)) {
                        dto.setPaymentStatus(com.auction.common.enums.PaymentStatus.FAILED);
                    } else if ("REFUNDED".equals(cleanStatus)) {
                        dto.setPaymentStatus(com.auction.common.enums.PaymentStatus.REFUNDED);
                    } else {
                        dto.setPaymentStatus(com.auction.common.enums.PaymentStatus.valueOf(cleanStatus));
                    }
                } else {
                    dto.setPaymentStatus(null);
                }
            } else {
                // Nếu hàm gọi không JOIN bảng payment, ta giữ nguyên giá trị paymentStatus hiện tại của DTO (không ép về null)
                // Hoặc tạm thời để null nếu đây là lần khởi tạo đầu tiên
                dto.setPaymentStatus(null);
            }
        } catch (Exception e) {
            dto.setPaymentStatus(null);
        }

        return dto;
    }
    // ── THÊM VÀO ĐỂ GIẢI QUYẾT TRIỆT ĐỂ LỖI KHÔNG XÓA ĐƯỢC ITEM ──────────────────

    /**
     * Lấy mã định danh món hàng (item_id) gắn liền với phiên đấu giá trước khi tiêu hủy phiên
     */
    public long getItemIdByAuctionId(long auctionId) {
        String sql = "SELECT item_id FROM auction WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("item_id");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error querying item_id for auctionId = " + auctionId, e);
        }
        return -1;
    }

    /**
     * Thực hiện xóa vĩnh viễn dữ liệu hàng hóa gốc trong bảng item
     */
    public boolean deleteItem(long itemId) {
        String sql = "DELETE FROM item WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, itemId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRITICAL ERROR: Failed to delete record from item table, ID = " + itemId, e);
            return false;
        }
    }
}