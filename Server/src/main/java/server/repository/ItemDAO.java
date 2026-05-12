package server.repository;

import server.common.model.ItemDTO;
import server.database.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    // 1. Lấy tất cả sản phẩm
    public List<ItemDTO> getAll() {
        List<ItemDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM item ORDER BY created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToDTO(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 2. Thêm sản phẩm mới (Theo đúng các trường trong ERD)
    public boolean insert(ItemDTO item) {
        String sql = "INSERT INTO item (name, description, starting_price, category, brand_make, model, artist, production_year) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setDouble(3, item.getStartingPrice());
            ps.setString(4, item.getCategory()); // ELECTRONICS, ART, VEHICLE
            ps.setString(5, item.getBrandMake());
            ps.setString(6, item.getModel());
            ps.setString(7, item.getArtist());
            ps.setInt(8, item.getProductionYear());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi insert Item: " + e.getMessage());
            return false;
        }
    }

    // 3. Tìm kiếm sản phẩm theo ID
    public ItemDTO getById(long id) {
        String sql = "SELECT * FROM item WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDTO(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Helper map dữ liệu
    private ItemDTO mapResultSetToDTO(ResultSet rs) throws SQLException {
        ItemDTO item = new ItemDTO();
        item.setId(rs.getLong("id"));
        item.setName(rs.getString("name"));
        item.setDescription(rs.getString("description"));
        item.setStartingPrice(rs.getDouble("starting_price"));
        item.setCategory(rs.getString("category"));
        item.setBrandMake(rs.getString("brand_make"));
        item.setModel(rs.getString("model"));
        item.setArtist(rs.getString("artist"));
        item.setProductionYear(rs.getInt("production_year"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) item.setCreatedAt(ts.toLocalDateTime());

        return item;
    }
}