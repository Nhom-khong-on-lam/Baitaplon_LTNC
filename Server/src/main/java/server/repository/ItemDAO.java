package server.repository;

import server.common.model.ItemDTO;
import server.database.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    // 1. Lấy tất cả item từ Database
    public List<ItemDTO> getAll() {
        List<ItemDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM item";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ItemDTO item = new ItemDTO();

                // Set các trường thông tin cơ bản từ ResultSet
                item.setId(rs.getLong("id"));
                item.setName(rs.getString("name"));
                item.setDescription(rs.getString("description"));
                item.setStartingPrice(rs.getDouble("starting_price"));
                item.setCategory(rs.getString("category"));

                // Các trường đặc thù trong sơ đồ ERD của bạn
                item.setBrandMake(rs.getString("brand_make"));
                item.setModel(rs.getString("model"));
                item.setArtist(rs.getString("artist"));

                // production_year có thể null nên dùng getObject
                Integer year = rs.getObject("production_year", Integer.class);
                item.setProductionYear(year);

                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    item.setCreatedAt(createdAt.toLocalDateTime());
                }

                list.add(item);
            }
        } catch (SQLException e) {
            System.err.println("LỖI trong ItemDAO.getAll(): " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    // 2. Thêm mới một item
    public boolean insert(ItemDTO item) {
        // Câu SQL đầy đủ các cột theo sơ đồ ERD
        String sql = "INSERT INTO item (name, description, starting_price, category, brand_make, model, artist, production_year) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setDouble(3, item.getStartingPrice());
            ps.setString(4, item.getCategory());
            ps.setString(5, item.getBrandMake());
            ps.setString(6, item.getModel());
            ps.setString(7, item.getArtist());

            if (item.getProductionYear() != null) {
                ps.setInt(8, item.getProductionYear());
            } else {
                ps.setNull(8, Types.INTEGER);
            }

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("LỖI trong ItemDAO.insert(): " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // 3. Tìm kiếm Item theo ID (Cần thiết cho AuctionDAO)
    public ItemDTO getById(long id) {
        String sql = "SELECT * FROM item WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ItemDTO item = new ItemDTO();
                    item.setId(rs.getLong("id"));
                    item.setName(rs.getString("name"));
                    item.setStartingPrice(rs.getDouble("starting_price"));
                    item.setCategory(rs.getString("category"));
                    // ... set các trường khác tương tự getAll nếu cần
                    return item;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}