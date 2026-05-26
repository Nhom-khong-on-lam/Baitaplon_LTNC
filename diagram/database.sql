DROP DATABASE IF EXISTS auction_db;
CREATE DATABASE auction_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE auction_db;

CREATE TABLE user(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(250) NOT NULL UNIQUE,
    password VARCHAR(500) NOT NULL,
    email VARCHAR(200),
    systemRole ENUM('USER','ADMIN') DEFAULT 'USER',
    accountStatus ENUM('ACTIVE', 'SUSPENDED', 'BANNED') DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    account_number VARCHAR(50) DEFAULT NULL,
    bank_name VARCHAR(100) DEFAULT NULL,
    cardholder_name VARCHAR(200) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    starting_price DOUBLE NOT NULL,
    category ENUM('ELECTRONICS', 'ART', 'VEHICLE') NOT NULL,
    brand_make VARCHAR(100), 
    model VARCHAR(100),    
    artist VARCHAR(100), 
    production_year INT,   
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE auction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    highest_bidder_id BIGINT,
    current_price DOUBLE,
    start_time DATETIME,
    end_time DATETIME,
    status ENUM('OPEN', 'RUNNING', 'FINISHED', 'PAID', 'CANCELED'),
    FOREIGN KEY (item_id) REFERENCES item(id),
    FOREIGN KEY (seller_id) REFERENCES `user`(id),
    FOREIGN KEY (highest_bidder_id) REFERENCES `user`(id)
);

CREATE TABLE bid (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    auction_id BIGINT NOT NULL,
    bidder_id BIGINT NOT NULL,
    amount DOUBLE NOT NULL,
    bid_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    auto_bid BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (auction_id) REFERENCES auction(id),
    FOREIGN KEY (bidder_id) REFERENCES `user`(id)
);

CREATE TABLE auto_bid (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    auction_id BIGINT NOT NULL,
    bidder_id BIGINT NOT NULL,
    max_amount DOUBLE,
    increment DOUBLE,
    active BOOLEAN DEFAULT TRUE,
    registered_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auction(id),
    FOREIGN KEY (bidder_id) REFERENCES `user`(id)
);

CREATE TABLE user_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(512) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
);

CREATE TABLE item_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    FOREIGN KEY (item_id) REFERENCES item(id) ON DELETE CASCADE
);

CREATE TABLE payment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    auction_id BIGINT NOT NULL UNIQUE,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    amount DOUBLE NOT NULL,
    status ENUM('PENDING','COMPLETED','FAILED','REFUNDED') DEFAULT 'PENDING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auction(id),
    FOREIGN KEY (buyer_id) REFERENCES `user`(id),
    FOREIGN KEY (seller_id) REFERENCES `user`(id)
);

CREATE TABLE auction_watch (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    auction_id BIGINT NOT NULL,
    watched_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, auction_id),
    FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE,
    FOREIGN KEY (auction_id) REFERENCES auction(id) ON DELETE CASCADE
);

CREATE TABLE auction_extension_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    auction_id BIGINT NOT NULL,
    original_end_time DATETIME NOT NULL,
    new_end_time DATETIME NOT NULL,
    FOREIGN KEY (auction_id) REFERENCES auction(id)
);
CREATE TABLE notification (
                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                              user_id BIGINT NOT NULL,
                              title VARCHAR(255) NOT NULL,
                              message TEXT NOT NULL,
                              type ENUM(
                                  'BID_PLACED',
                                  'OUTBID',
                                  'AUCTION_WON',
                                  'AUCTION_LOST',
                                  'AUCTION_ENDED',
                                  'AUCTION_STARTING',
                                  'PAYMENT_DUE',
                                  'PAYMENT_RECEIVED',
                                  'AUCTION_CANCELED',
                                  'SYSTEM'
                                  ) NOT NULL,
                              is_read BOOLEAN DEFAULT FALSE,
                              related_auction_id BIGINT DEFAULT NULL,
                              related_bid_id BIGINT DEFAULT NULL,
                              expires_at DATETIME DEFAULT NULL,
                              created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                              FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE,
                              FOREIGN KEY (related_auction_id) REFERENCES auction(id) ON DELETE SET NULL,
                              FOREIGN KEY (related_bid_id) REFERENCES bid(id) ON DELETE SET NULL,
                              INDEX idx_user_read (user_id, is_read),
                              INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE auto_bid
    RENAME COLUMN max_amount TO max_price;

ALTER TABLE auto_bid
    RENAME COLUMN increment TO step_increment;

ALTER TABLE auto_bid
    RENAME COLUMN max_price TO maxPrice;

ALTER TABLE auto_bid
    RENAME COLUMN step_increment TO stepIncrement;

ALTER TABLE auto_bid
    CHANGE max_price maxPrice DOUBLE;

ALTER TABLE auto_bid
    CHANGE step_increment stepIncrement DOUBLE;

-- Add banking columns to existing user table (run if upgrading existing DB)
