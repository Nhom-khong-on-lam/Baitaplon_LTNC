module com.auction.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires jakarta.mail;
    requires java.sql;
    requires jbcrypt;
    requires server;
    opens com.auction.client.controller to javafx.fxml;
    exports com.auction.client.controller;
    exports com.auction.client;
    exports com.auction.client.model;
}