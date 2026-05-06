package server.database;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {
    private static final Properties props = new Properties();

    // Khối static này sẽ tự động chạy ngay khi class DBConnection được nạp
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
            // Lấy thông tin từ file properties thông qua các key đã đặt
            String url = props.getProperty("db.url");
            String user = props.getProperty("db.user");
            String pass = props.getProperty("db.password");


            Class.forName("com.mysql.cj.jdbc.Driver");

            // Tạo kết nối
            conn = DriverManager.getConnection(url, user, pass);
        } catch (ClassNotFoundException e) {
            System.err.println("LỖI: Thiếu Driver MySQL (JAR file)!");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("LỖI: Không thể kết nối đến MySQL. Kiểm tra URL/User/Pass trong file properties!");
            e.printStackTrace();
        }
        return conn;
    }

    // Hàm main để bạn chuột phải vào và chọn "Run" để kiểm tra ngay
    public static void main(String[] args) {
        Connection testConn = getConnection();

        if (testConn != null) {
            System.out.println("------------------------------------------");
            System.out.println("KẾT NỐI THÀNH CÔNG (Dùng file properties)!");
            System.out.println("------------------------------------------");
            try { testConn.close(); } catch (SQLException e) {}
        }
    }
}