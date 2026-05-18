package server.repository;


import com.auction.common.dto.ItemImageDTO;
import server.database.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemImageDAO {

    // 1. Lấy danh sách ảnh của một Item
    public List<ItemImageDTO> getImagesByItemId(long itemId) {
        List<ItemImageDTO> images = new ArrayList<>();
        String sql = "SELECT * FROM item_image WHERE item_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ItemImageDTO img = new ItemImageDTO();
                    img.setId(rs.getLong("id"));
                    img.setItemId(rs.getLong("item_id"));
                    img.setImageUrl(rs.getString("image_url"));
                    images.add(img);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi getImagesByItemId: " + e.getMessage());
            e.printStackTrace();
        }
        return images;
    }
    // 2. Thêm một ảnh mới
    public boolean insert(ItemImageDTO itemImage) {
        String sql = "INSERT INTO item_image (item_id, image_url) VALUES (?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, itemImage.getItemId());
            ps.setString(2, itemImage.getImageUrl());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi insert ItemImage: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    // 3. Xóa tất cả ảnh của một Item
    public boolean deleteByItemId(long itemId) {
        String sql = "DELETE FROM item_image WHERE item_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, itemId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi delete ItemImage: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    public List<ItemImageDTO> getAll() {
        List<ItemImageDTO> images = new ArrayList<>();
        String sql = "SELECT * FROM item_image";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ItemImageDTO img = new ItemImageDTO();
                img.setId(rs.getLong("id"));
                img.setItemId(rs.getLong("item_id"));
                img.setImageUrl(rs.getString("image_url"));
                images.add(img);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi getAll ItemImage: " + e.getMessage());
            e.printStackTrace();
        }
        return images;
    }
}