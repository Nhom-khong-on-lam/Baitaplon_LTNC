package server.repository;

import com.auction.common.dto.AutoBidDTO;
import server.database.DBConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AutoBidDAO {
    private static final Logger LOGGER = Logger.getLogger(AutoBidDAO.class.getName());

    public long insert(AutoBidDTO config) {
        // ĐÃ ĐỒNG BỘ: Sử dụng maxPrice và stepIncrement theo đúng Database của bạn
        String sql = "INSERT INTO auto_bid (auction_id, bidder_id, maxPrice, stepIncrement, active, registered_at) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, config.getAuctionId());
            ps.setLong(2, config.getBidderId());
            ps.setDouble(3, config.getMaxPrice());
            ps.setDouble(4, config.getStepIncrement());
            ps.setBoolean(5, config.isActive());

            LocalDateTime timeToSave = (config.getRegisteredAt() != null) ? config.getRegisteredAt() : LocalDateTime.now();
            ps.setTimestamp(6, Timestamp.valueOf(timeToSave));

            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi insert AutoBid", e);
        }
        return -1;
    }

    public List<AutoBidDTO> getConfigsByAuctionId(long auctionId) {
        List<AutoBidDTO> configs = new ArrayList<>();
        // ĐÃ ĐỒNG BỘ: SELECT chính xác maxPrice, stepIncrement
        String sql = "SELECT id, auction_id, bidder_id, maxPrice, stepIncrement, active, registered_at FROM auto_bid WHERE auction_id = ? AND active = true";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AutoBidDTO config = new AutoBidDTO();
                    config.setId(rs.getLong("id"));
                    config.setAuctionId(rs.getLong("auction_id"));
                    config.setBidderId(rs.getLong("bidder_id"));

                    // Lấy dữ liệu theo tên cột mới cập nhật trong DB
                    config.setMaxPrice(rs.getDouble("maxPrice"));
                    config.setStepIncrement(rs.getDouble("stepIncrement"));

                    config.setActive(rs.getBoolean("active"));

                    Timestamp ts = rs.getTimestamp("registered_at");
                    if (ts != null) config.setRegisteredAt(ts.toLocalDateTime());
                    configs.add(config);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi lấy danh sách AutoBid", e);
        }
        return configs;
    }

    public AutoBidDTO findById(long id) {
        // ĐÃ ĐỒNG BỘ: SELECT chính xác maxPrice, stepIncrement
        String sql = "SELECT id, auction_id, bidder_id, maxPrice, stepIncrement, active, registered_at FROM auto_bid WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    AutoBidDTO config = new AutoBidDTO();
                    config.setId(rs.getLong("id"));
                    config.setAuctionId(rs.getLong("auction_id"));
                    config.setBidderId(rs.getLong("bidder_id"));

                    // Lấy dữ liệu theo tên cột mới cập nhật trong DB
                    config.setMaxPrice(rs.getDouble("maxPrice"));
                    config.setStepIncrement(rs.getDouble("stepIncrement"));

                    config.setActive(rs.getBoolean("active"));

                    Timestamp ts = rs.getTimestamp("registered_at");
                    if (ts != null) config.setRegisteredAt(ts.toLocalDateTime());
                    return config;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi tìm AutoBid theo ID", e);
        }
        return null;
    }

    public boolean update(AutoBidDTO config) {
        // ĐÃ ĐỒNG BỘ: SET theo tên cột mới
        String sql = "UPDATE auto_bid SET maxPrice = ?, stepIncrement = ?, active = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, config.getMaxPrice());
            ps.setDouble(2, config.getStepIncrement());
            ps.setBoolean(3, config.isActive());
            ps.setLong(4, config.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi update AutoBid", e);
            return false;
        }
    }
}