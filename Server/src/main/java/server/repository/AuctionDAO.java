package server.repository;

import com.auction.common.dto.AuctionDTO;
import server.database.DBConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionDAO {

    private static final Logger LOGGER = Logger.getLogger(AuctionDAO.class.getName());

    public long insert(AuctionDTO a) {
        String sql = "INSERT INTO auction (item_id, seller_id, current_price, start_time, end_time, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, a.getItemId());
            ps.setLong(2, a.getSellerId());
            ps.setDouble(3, a.getCurrentPrice());
            ps.setTimestamp(4, Timestamp.valueOf(a.getStartTime()));
            ps.setTimestamp(5, Timestamp.valueOf(a.getEndTime()));
            ps.setString(6, a.getStatus() != null ? a.getStatus().toUpperCase() : "RUNNING"); // Ép viết hoa đồng bộ trạng thái

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
            LOGGER.log(Level.SEVERE, "LỖI INSERT AUCTION: Hãy kiểm tra các trường khóa ngoại item_id hoặc seller_id xem có tồn tại không!", e);
        }
        return -1;
    }

    public AuctionDTO findById(long id) {
        String sql = "SELECT id, item_id, seller_id, highest_bidder_id, current_price, start_time, end_time, status FROM auction WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findById auction ERROR id=" + id, e);
        }
        return null;
    }

    public List<AuctionDTO> findAll() {
        List<AuctionDTO> list = new ArrayList<>();
        String sql = "SELECT id, item_id, seller_id, highest_bidder_id, current_price, start_time, end_time, status FROM auction ORDER BY start_time DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findAll auction ERROR", e);
        }
        return list;
    }

    // TỐI ƯU MỚI: Truyền thời gian hiện tại từ Java để sửa triệt để lỗi lệch múi giờ NOW() của MySQL
    public List<AuctionDTO> findActive() {
        List<AuctionDTO> list = new ArrayList<>();
        String sql = "SELECT id, item_id, seller_id, highest_bidder_id, current_price, start_time, end_time, status " +
                "FROM auction WHERE status = 'RUNNING' AND end_time > ? ORDER BY end_time ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Lấy thời gian chuẩn của máy chạy ứng dụng
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findActive auction ERROR", e);
        }
        return list;
    }

    public List<AuctionDTO> findBySeller(long sellerId) {
        List<AuctionDTO> list = new ArrayList<>();
        // Màn hình MyProduct: Cho hiển thị tất cả các trạng thái để dễ quản lý, tránh filter chặt quá bị trống màn hình
        String sql = "SELECT id, item_id, seller_id, highest_bidder_id, current_price, start_time, end_time, status FROM auction WHERE seller_id = ? ORDER BY start_time DESC";
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

    public List<AuctionDTO> findByBidder(long bidderId) {
        List<AuctionDTO> list = new ArrayList<>();
        String sql = "SELECT DISTINCT a.id, a.item_id, a.seller_id, a.highest_bidder_id, a.current_price, a.start_time, a.end_time, a.status " +
                "FROM auction a JOIN bid b ON a.id = b.auction_id WHERE b.bidder_id = ? ORDER BY a.end_time DESC";
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

    public List<AuctionDTO> findWinningByUser(long userId) {
        List<AuctionDTO> list = new ArrayList<>();
        String sql = "SELECT id, item_id, seller_id, highest_bidder_id, current_price, start_time, end_time, status FROM auction WHERE highest_bidder_id = ? ORDER BY end_time DESC";
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

    public boolean update(AuctionDTO a) {
        String sql = "UPDATE auction SET current_price=?, highest_bidder_id=?, end_time=?, status=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, a.getCurrentPrice());
            if (a.getHighestBidderId() != null) ps.setLong(2, a.getHighestBidderId());
            else ps.setNull(2, Types.BIGINT);
            ps.setTimestamp(3, Timestamp.valueOf(a.getEndTime()));
            ps.setString(4, a.getStatus());
            ps.setLong(5, a.getId());
            boolean ok = ps.executeUpdate() > 0;
            if (ok) LOGGER.info("UPDATE auction SUCCESS: ID=" + a.getId());
            return ok;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "UPDATE auction ERROR id=" + a.getId(), e);
        }
        return false;
    }

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

    private AuctionDTO mapRow(ResultSet rs) throws SQLException {
        AuctionDTO a = new AuctionDTO();
        a.setId(rs.getLong("id"));
        a.setItemId(rs.getLong("item_id"));
        a.setSellerId(rs.getLong("seller_id"));

        long bidderId = rs.getLong("highest_bidder_id");
        if (!rs.wasNull()) a.setHighestBidderId(bidderId);

        a.setCurrentPrice(rs.getDouble("current_price"));

        // Cải tiến kiểm tra dữ liệu ngày tháng, tránh crash luồng giao diện khi DB trả về null
        Timestamp startTs = rs.getTimestamp("start_time");
        if (startTs != null) a.setStartTime(startTs.toLocalDateTime());

        Timestamp endTs = rs.getTimestamp("end_time");
        if (endTs != null) a.setEndTime(endTs.toLocalDateTime());

        a.setStatus(rs.getString("status"));
        return a;
    }
}