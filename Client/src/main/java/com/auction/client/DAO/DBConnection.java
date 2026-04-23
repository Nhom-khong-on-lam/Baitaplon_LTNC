package com.auction.client.DAO;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL =
            "jdbc:mysql://127.0.0.1:3306/auction_system?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";

    private static final String USER = "root";
    private static final String PASSWORD = "20112007";

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.out.println(" Kết nối DB thất bại!");
            e.printStackTrace();
            return null;
        }
    }
}