package server.database;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {
    private static final Properties props = new Properties();

    static {
        try (InputStream is = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (is == null) {
                System.err.println("LỖI: Không tìm thấy file 'db.properties' trong thư mục resources!");
            } else {
                props.load(is);
                System.out.println("Đã tải cấu hình Database từ file thành công.");
            }
        } catch (Exception e) {
            System.err.println("LỖI: Không thể đọc file db.properties!");
            e.printStackTrace();
        }
    }

    public static Connection getConnection() {
        Connection conn = null;
        try {
            String url = props.getProperty("db.url");
            String user = props.getProperty("db.user");
            String pass = props.getProperty("db.password");
            String driver = props.getProperty("db.driver"); // Lấy driver từ file properties

            // Nạp Driver
            if (driver != null) {
                Class.forName(driver);
            }

            conn = DriverManager.getConnection(url, user, pass);
        } catch (ClassNotFoundException e) {
            System.err.println("LỖI: Không tìm thấy Driver MySQL trong hệ thống! Hãy kiểm tra Maven.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("LỖI: Kết nối thất bại! Kiểm tra lại URL/User/Pass trong db.properties.");
            System.err.println("Chi tiết lỗi: " + e.getMessage());
        }
        return conn;
    }

    public static void main(String[] args) {
        System.out.println("Đang thử kết nối đến Cloud Database...");
        Connection testConn = getConnection();

        if (testConn != null) {
            System.out.println("------------------------------------------");
            System.out.println("CHÚC MỪNG ÔNG! KẾT NỐI THÀNH CÔNG RỒI.");
            System.out.println("------------------------------------------");
            try { testConn.close(); } catch (SQLException e) {}
        } else {
            System.out.println("------------------------------------------");
            System.out.println("VẪN THẤT BẠI. Hãy kiểm tra lại file pom.xml và db.properties.");
            System.out.println("------------------------------------------");
        }
    }
}