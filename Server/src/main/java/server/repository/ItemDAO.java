package server.repository;

import com.auction.common.dto.ItemDTO;
import server.database.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    public List<ItemDTO> getAll() {
        List<ItemDTO> list = new ArrayList<>();
        // Tối ưu: Liệt kê rõ các cột, tránh dùng SELECT * để an toàn khi DB thay đổi
        String sql = "SELECT id, name, description, starting_price, category, brand_make, model, artist, production_year, created_at FROM item ORDER BY created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToDTO(rs));
            }
        } catch (SQLException e) {
            System.err.println("LỖI trong ItemDAO.getAll(): " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public long insert(ItemDTO item) {
        String sql = "INSERT INTO item (name, description, starting_price, category, brand_make, model, artist, production_year) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

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

            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("LỖI nghiêm trọng trong ItemDAO.insert(): " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    public ItemDTO getById(long id) {
        String sql = "SELECT id, name, description, starting_price, category, brand_make, model, artist, production_year, created_at FROM item WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDTO(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("LỖI trong ItemDAO.getById(): " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

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

        // Xử lý an toàn tránh lỗi cột null hoặc sai kiểu dữ liệu thời gian
        try {
            Timestamp ts = rs.getTimestamp("created_at");
            if (ts != null) item.setCreatedAt(ts.toLocalDateTime());
        } catch (Exception e) {
            System.err.println("Cảnh báo: Không thể parse trường created_at trong item");
        }

        return item;
    }
}