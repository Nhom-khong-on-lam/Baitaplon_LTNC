module server {
    exports server.repository;
    exports server.database;
    exports server.common.enums;
    exports server.common.model;

    requires java.sql;
}