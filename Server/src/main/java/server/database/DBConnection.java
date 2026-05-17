package server.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {
    private static HikariDataSource dataSource;

    static {
        try (InputStream is = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            Properties props = new Properties();
            if (is == null) {
                System.err.println("LỖI: Không tìm thấy file 'db.properties'!");
            } else {
                props.load(is);

                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(props.getProperty("db.url"));
                config.setUsername(props.getProperty("db.user"));
                config.setPassword(props.getProperty("db.password"));
                config.setDriverClassName(props.getProperty("db.driver"));

                // Cấu hình tối ưu tốc độ và chống nghẽn
                config.setMaximumPoolSize(20); // Tối đa 20 kết nối chạy song song
                config.setMinimumIdle(5);
                config.setIdleTimeout(30000);
                config.setConnectionTimeout(20000); // Không lo bị đứng luồng quá lâu

                dataSource = new HikariDataSource(config);
                System.out.println("Khởi tạo HikariCP Connection Pool thành công.");
            }
        } catch (Exception e) {
            System.err.println("LỖI: Không thể khởi tạo Connection Pool!");
            e.printStackTrace();
        }
    }

    // Mỗi lần DAO gọi getConnection(), HikariCP sẽ cấp 1 kết nối có sẵn từ Pool
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource chưa được khởi tạo!");
        }
        return dataSource.getConnection();
    }

    public static void closeConnection() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("Đã đóng Connection Pool.");
        }
    }
}