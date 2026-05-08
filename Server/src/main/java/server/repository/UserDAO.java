package server.repository;

import server.database.DBConnection;

import com.auction.client.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    // Helper: Map data từ SQL sang Object User
    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setEmail(rs.getString("email"));
        user.setSystemRole(rs.getString("systemRole"));
        user.setAccountStatus(rs.getString("accountStatus"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        return user;
    }
    // --- CREATE ---
    public long insert(User user) {
        String sql = "INSERT INTO user (username, password, email, systemRole, accountStatus, created_at) VALUES (?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getSystemRole() != null ? user.getSystemRole() : "USER");
            ps.setString(5, user.getAccountStatus() != null ? user.getAccountStatus() : "ACTIVE");

            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getLong(1);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }
    // --- READ (Dùng chung field name để tối ưu) ---
    public User findByField(String fieldName, Object value) {
        String sql = "SELECT * FROM user WHERE " + fieldName + " = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }
    public User findById(long id) { return findByField("id", id); }
    public User findByUsername(String username) { return findByField("username", username); }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM user";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) users.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return users;
    }
    // --- UPDATE ---
    public boolean update(User user) {
        String sql = "UPDATE user SET email = ?, systemRole = ?, accountStatus = ?, password = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getSystemRole());
            ps.setString(3, user.getAccountStatus());
            ps.setString(4, user.getPassword());
            ps.setLong(5, user.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }
    // --- DELETE ---
    public boolean delete(long id) {
        String sql = "DELETE FROM user WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    // --- UTILITY ---
    public boolean isExisted(String fieldName, String value) {
        return findByField(fieldName, value) != null;
    }
}