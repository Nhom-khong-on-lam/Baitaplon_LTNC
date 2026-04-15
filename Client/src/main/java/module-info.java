module com.auction.client {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.auction.client.Controller to javafx.fxml;
    exports com.auction.client.Controller;
    exports com.auction.client;
}