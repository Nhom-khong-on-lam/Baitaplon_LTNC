module server {
    exports server.repository;
    exports server.database;

    requires java.sql;
}