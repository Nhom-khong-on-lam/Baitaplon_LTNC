package com.auction.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
/**
 * UserDTO nằm trong thư mục server/common/model.
 * Dùng để vận chuyển dữ liệu giữa Server và Client.
 */
public class UserDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private String username;
    private String password;
    private String email;
    private String systemRole;
    private String accountStatus;
    private LocalDateTime createdAt;
    private int bidCount;
    private String accountNumber;
    private String bankName;
    private String cardholderName;
    private double balance;

    // 1. Constructor mặc định
    public UserDTO() {
    }

    // 2. Constructor dùng cho Đăng ký (Register)
    // Khớp với logic: new UserDTO(username, hashedParams, email)
    public UserDTO(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    // 3. Constructor đầy đủ cho DAO
    public UserDTO(long id, String username, String password, String email,
                   String systemRole, String accountStatus, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.systemRole = systemRole;
        this.accountStatus = accountStatus;
        this.createdAt = createdAt;
    }

    // --- Getter và Setter ---
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSystemRole() { return systemRole; }
    public void setSystemRole(String systemRole) { this.systemRole = systemRole; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getBidCount() { return bidCount; }
    public void setBidCount(int bidCount) { this.bidCount = bidCount; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getCardholderName() { return cardholderName; }
    public void setCardholderName(String cardholderName) { this.cardholderName = cardholderName; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    @Override
    public String toString() {
        return "UserDTO{" + "username='" + username + "', role='" + systemRole + "'}";
    }
}