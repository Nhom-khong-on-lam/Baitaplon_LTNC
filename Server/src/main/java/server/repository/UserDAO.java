package server.repository;

import server.database.DBConnection;
import com.auction.common.dto.UserDTO;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserDAO {
    private static final Logger LOGGER = Logger.getLogger(UserDAO.class.getName());

    // Helper: Map data từ SQL sang Object UserDTO
    private UserDTO mapRow(ResultSet rs) throws SQLException {
        UserDTO user = new UserDTO();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setEmail(rs.getString("email"));
        user.setSystemRole(rs.getString("systemRole"));
        user.setAccountStatus(rs.getString("accountStatus"));

        Timestamp ts = rs.getTimestamp("created_at");
        if(ts != null) {
            user.setCreatedAt(ts.toLocalDateTime());
        }

        try {
            user.setBidCount(rs.getInt("bid_count"));
        } catch (SQLException ignore) {}

        return user;
    }

    // --- CREATE ---
    // --- CREATE (ĐÃ CHUẨN HÓA CHO TIDB SEQUENCE) ---
    public long insert(UserDTO user) {
        // 1. Lấy ID tiếp theo từ Sequence của TiDB trước
        long nextId = -1;
        String sqlSeq = "SELECT NEXT VALUE FOR seq_user"; // Thay 'user_seq' bằng đúng tên SEQUENCE bạn đã tạo trong DB

        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sqlSeq)) {
            if (rs.next()) {
                nextId = rs.getLong(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Không thể lấy ID từ Sequence, thử dùng cơ chế dự phòng...", e);
        }

        // 2. Tiến hành INSERT kèm theo ID cụ thể vừa lấy được
        String sql = "INSERT INTO user (id, username, password, email, systemRole, accountStatus, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Nếu lấy sequence thành công thì nạp id, nếu thất bại (=-1) để DB tự xử lý dự phòng
            if (nextId != -1) {
                ps.setLong(1, nextId);
            } else {
                ps.setNull(1, Types.BIGINT);
            }

            ps.setString(2, user.getUsername());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getSystemRole() != null ? user.getSystemRole() : "USER");
            ps.setString(6, user.getAccountStatus() != null ? user.getAccountStatus() : "ACTIVE");

            LocalDateTime createdAt = (user.getCreatedAt() != null) ? user.getCreatedAt() : LocalDateTime.now();
            ps.setTimestamp(7, Timestamp.valueOf(createdAt));

            if (ps.executeUpdate() > 0) {
                // Nếu ta chủ động nạp nextId từ đầu, trả về chính nó luôn mà không cần thông qua getGeneratedKeys()
                if (nextId != -1) {
                    LOGGER.info("INSERT SUCCESS: Đã tạo User mới ID=" + nextId + " [" + user.getUsername() + "]");
                    return nextId;
                }

                // Cơ chế dự phòng trong trường hợp không dùng sequence từ đầu
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        long generatedId = rs.getLong(1);
                        LOGGER.info("INSERT SUCCESS (Generated): Đã tạo User mới ID=" + generatedId);
                        return generatedId;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "INSERT ERROR: Không thể thêm User " + user.getUsername(), e);
        }
        return -1;
    }

    // --- CÓ ĐỦ CẢ HÀM FIND_BY_FIELD CHO ĐOẠN "UPDATE_PASSWORD" CỦA BẠN ---
    public UserDTO findByField(String fieldName, Object value) {
        String sql = "SELECT u.*, (SELECT COUNT(DISTINCT auction_id) FROM bid b WHERE b.bidder_id = u.id) AS bid_count " +
                "FROM user u WHERE u." + fieldName + " = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "READ ERROR: Lỗi tìm user theo field " + fieldName, e);
        }
        return null;
    }

    // --- GIỮ NGUYÊN TẤT CẢ CÁC HÀM GỐC KHÁC KHÔNG THAY ĐỔI TÊN ---
    public UserDTO findById(long id) {
        return findByField("id", id);
    }

    public UserDTO findByUsername(String username) {
        return findByField("username", username);
    }

    public UserDTO findByEmail(String email) {
        return findByField("email", email);
    }

    public List<UserDTO> findAll() {
        List<UserDTO> users = new ArrayList<>();
        String sql = "SELECT u.*, (SELECT COUNT(DISTINCT auction_id) FROM bid b WHERE b.bidder_id = u.id) AS bid_count FROM user u";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) users.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "READ ALL ERROR: Lỗi lấy danh sách user", e);
        }
        return users;
    }

    // --- HÀM KIỂM TRA TỒN TẠI (isExisted) GIỮ NGUYÊN ---
    public boolean isExisted(String fieldName, String value) {
        if ("username".equalsIgnoreCase(fieldName)) {
            return findByUsername(value) != null;
        } else if ("email".equalsIgnoreCase(fieldName)) {
            return findByEmail(value) != null;
        }
        return false;
    }

    // --- UPDATE ---
    public boolean update(UserDTO user) {
        String sql = "UPDATE user SET email = ?, systemRole = ?, accountStatus = ?, password = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getSystemRole());
            ps.setString(3, user.getAccountStatus());
            ps.setString(4, user.getPassword());
            ps.setLong(5, user.getId());
            boolean updated = ps.executeUpdate() > 0;
            if (updated) LOGGER.info("UPDATE SUCCESS: Đã cập nhật User ID=" + user.getId());
            return updated;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "UPDATE ERROR: Lỗi cập nhật User ID=" + user.getId(), e);
        }
        return false;
    }

    // --- DELETE ---
    public boolean delete(long id) {
        String sql = "DELETE FROM user WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            boolean deleted = ps.executeUpdate() > 0;
            if (deleted) LOGGER.info("DELETE SUCCESS: Đã xóa User ID=" + id);
            return deleted;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "DELETE ERROR: Không thể xóa User ID=" + id, e);
        }
        return false;
    }
    // Thêm vào UserDAO.java — dùng cho toClientAuctions, không cần bid_count
    public UserDTO findByIdLight(long id) {
        String sql = "SELECT id, username, password, email, systemRole, accountStatus, created_at FROM user WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UserDTO user = new UserDTO();
                    user.setId(rs.getLong("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    user.setEmail(rs.getString("email"));
                    user.setSystemRole(rs.getString("systemRole"));
                    user.setAccountStatus(rs.getString("accountStatus"));

                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                        user.setCreatedAt(ts.toLocalDateTime());
                    }
                    user.setBidCount(0); // Mặc định là 0 để tránh mapRow lỗi cột
                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("LỖI trong UserDAO.findByIdLight(): " + e.getMessage());
        }
        return null;
    }
}