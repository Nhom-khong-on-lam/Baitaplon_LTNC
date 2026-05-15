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
            ps.setString(6, a.getStatus() != null ? a.getStatus() : "RUNNING");

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

    // ── FIND BY ID ────────────────────────────────────────────────────────────
    public AuctionDTO findById(long id) {
        String sql = "SELECT * FROM auction WHERE id = ?";
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

    // ── FIND ALL ──────────────────────────────────────────────────────────────
    public List<AuctionDTO> findAll() {
        List<AuctionDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM auction ORDER BY start_time DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findAll auction ERROR", e);
        }
        return list;
    }

    // ── FIND ACTIVE (status = RUNNING và chưa hết hạn) ───────────────────────
    public List<AuctionDTO> findActive() {
        List<AuctionDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM auction WHERE status = 'RUNNING' AND end_time > NOW() ORDER BY end_time ASC";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findActive auction ERROR", e);
        }
        return list;
    }

    // ── FIND BY SELLER ────────────────────────────────────────────────────────
    public List<AuctionDTO> findBySeller(long sellerId) {
        List<AuctionDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM auction WHERE seller_id = ? ORDER BY start_time DESC";
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

    // ── MAP ROW ───────────────────────────────────────────────────────────────
    private AuctionDTO mapRow(ResultSet rs) throws SQLException {
        AuctionDTO a = new AuctionDTO();
        a.setId(rs.getLong("id"));
        a.setItemId(rs.getLong("item_id"));
        a.setSellerId(rs.getLong("seller_id"));

        long bidderId = rs.getLong("highest_bidder_id");
        if (!rs.wasNull()) a.setHighestBidderId(bidderId);

        a.setCurrentPrice(rs.getDouble("current_price"));
        a.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        a.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
        a.setStatus(rs.getString("status"));
        return a;
    }
}