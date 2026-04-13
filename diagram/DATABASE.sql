Create database auction_system;
Use auction_system;
create table user(
id bigint primary key auto_increment,
username varchar(250) NOT NULL unique,
password varchar(500) NOT NULL,
email varchar(200) ,
systemRole Enum('USER','ADMIN') default 'USER',
accountStatus ENUM('ACTIVE', 'SUSPENDED', 'BANNED') DEFAULT 'ACTIVE',
created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
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
    FOREIGN KEY (seller_id) REFERENCES user(id),
    FOREIGN KEY (highest_bidder_id) REFERENCES user(id)
);
CREATE TABLE bid (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    auction_id BIGINT NOT NULL,
    bidder_id BIGINT NOT NULL,
    amount DOUBLE NOT NULL,
    bid_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    auto_bid BOOLEAN DEFAULT FALSE,

    FOREIGN KEY (auction_id) REFERENCES auction(id),
    FOREIGN KEY (bidder_id) REFERENCES user(id)
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
    FOREIGN KEY (bidder_id) REFERENCES user(id)
);

