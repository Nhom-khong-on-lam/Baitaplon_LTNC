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
        String sql = "INSERT INTO auto_bid (auction_id, bidder_id, maxPrice, stepIncrement, active, registered_at) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, config.getAuctionId());
            ps.setLong(2, config.getBidderId());

            // Map với Getter của Java
            ps.setDouble(3, config.getMaxPrice());
            ps.setDouble(4, config.getStepIncrement());

            ps.setBoolean(5, config.isActive());

            LocalDateTime timeToSave = (config.getRegisteredAt() != null) ? config.getRegisteredAt() : LocalDateTime.now();
            ps.setTimestamp(6, Timestamp.valueOf(timeToSave));

            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        long id = rs.getLong(1);
                        LOGGER.info("INSERT SUCCESS: Đã lưu AutoBid mới ID=" + id + " cho Auction=" + config.getAuctionId());
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "INSERT ERROR: Lỗi khi lưu AutoBidDTO", e);
        }
        return -1;
    }

    public boolean updateActiveStatus(Long configId, boolean isActive) {
        String sql = "UPDATE auto_bid SET active = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, isActive);
            ps.setLong(2, configId);

            boolean updated = ps.executeUpdate() > 0;
            if (updated) {
                LOGGER.info("UPDATE SUCCESS: Đã đổi trạng thái AutoBid ID=" + configId + " thành " + isActive);
            } else {
                LOGGER.warning("UPDATE WARNING: Không tìm thấy AutoBid ID=" + configId + " để cập nhật trạng thái.");
            }
            return updated;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "UPDATE ERROR: Lỗi cập nhật trạng thái AutoBid ID=" + configId, e);
        }
        return false;
    }

    public List<AutoBidDTO> getActiveConfigsForAuction(Long auctionId) {
        List<AutoBidDTO> configs = new ArrayList<>();
        String sql = "SELECT * FROM auto_bid WHERE auction_id = ? AND active = 1 ORDER BY registered_at ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AutoBidDTO config = new AutoBidDTO();
                    config.setId(rs.getLong("id"));
                    config.setAuctionId(rs.getLong("auction_id"));
                    config.setBidderId(rs.getLong("bidder_id"));

                    config.setMaxPrice(rs.getDouble("maxPrice"));
                    config.setStepIncrement(rs.getDouble("stepIncrement"));

                    config.setActive(rs.getBoolean("active"));

                    Timestamp ts = rs.getTimestamp("registered_at");
                    if (ts != null) config.setRegisteredAt(ts.toLocalDateTime());

                    configs.add(config);
                }
                LOGGER.info("READ SUCCESS: Lấy thành công " + configs.size() + " cấu hình AutoBid active cho Auction ID=" + auctionId);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "READ ERROR: Lỗi lấy danh sách AutoBid của auction_id=" + auctionId, e);
        }
        return configs;
    }

    public AutoBidDTO findById(long id) {
        String sql = "SELECT * FROM auto_bid WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    AutoBidDTO config = new AutoBidDTO();
                    config.setId(rs.getLong("id"));
                    config.setAuctionId(rs.getLong("auction_id"));
                    config.setBidderId(rs.getLong("bidder_id"));

                    config.setMaxPrice(rs.getDouble("maxPrice"));
                    config.setStepIncrement(rs.getDouble("stepIncrement"));
                    config.setActive(rs.getBoolean("active"));

                    Timestamp ts = rs.getTimestamp("registered_at");
                    if (ts != null) config.setRegisteredAt(ts.toLocalDateTime());
                    LOGGER.info("READ SUCCESS: Đã tìm thấy AutoBid ID=" + id);
                    return config;
                } else {
                    LOGGER.warning("READ WARNING: Không tồn tại AutoBid với ID=" + id);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "READ ERROR: Lỗi tìm AutoBid theo ID=" + id, e);
        }
        return null;
    }
}
