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
                System.err.println("ERROR: 'db.properties' file not found!");
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
                System.out.println("HikariCP connection pool initialized successfully.");
            }
        } catch (Exception e) {
            System.err.println("ERROR: Failed to initialize connection pool!");
            e.printStackTrace();
        }
    }

    // Mỗi lần DAO gọi getConnection(), HikariCP sẽ cấp 1 kết nối có sẵn từ Pool
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource has not been initialized!");
        }
        return dataSource.getConnection();
    }

    public static void closeConnection() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("Connection pool closed.");
        }
    }
}