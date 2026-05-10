package server.database;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {
    private static final Properties props = new Properties();
    private static Connection instance;

    // Load config từ db.properties 1 lần duy nhất
    static {
        try (InputStream is = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (is == null) {
                System.err.println("LỖI: Không tìm thấy file 'db.properties'!");
            } else {
                props.load(is);
                Class.forName(props.getProperty("db.driver"));
                System.out.println("Tải cấu hình Database thành công.");
            }
        } catch (Exception e) {
            System.err.println("LỖI: Không thể đọc file db.properties!");
            e.printStackTrace();
        }
    }

    // Singleton — chỉ tạo 1 connection duy nhất
    public static Connection getConnection() throws SQLException {
        if (instance == null || instance.isClosed()) {
            instance = DriverManager.getConnection(
                    props.getProperty("db.url"),
                    props.getProperty("db.user"),
                    props.getProperty("db.password")
            );
            System.out.println("Kết nối Database thành công.");
        }
        return instance;
    }

    // Đóng connection khi tắt server
    public static void closeConnection() {
        if (instance != null) {
            try {
                instance.close();
                System.out.println("Đã đóng kết nối Database.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}