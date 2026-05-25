package com.auction.common.model;


import com.auction.common.enums.AccountStatus;
import com.auction.common.enums.SystemRole;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class User extends BaseEntity implements Serializable {
    private String username;
    private String passwordHash;
    private String email;
    private SystemRole systemRole;
    private AccountStatus accountStatus;
    private LocalDateTime joinedDate ;
    private int bidCount;
    private String accountNumber;
    private String bankName;
    private String cardholderName;
    private Double balance = 0.0;

    public User(Long id, String username, String passwordHash, String email, SystemRole systemRole) {
        super(id);
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.systemRole = systemRole;
        this.accountStatus = AccountStatus.ACTIVE;
    }

    public User(String testUser) {
    }

    public String getUsername() {
        return this.username;
    }

    public String getPasswordHash() {
        return this.passwordHash;
    }

    public SystemRole getSystemRole(){
        return this.systemRole;
    }

    public String getEmail() {
        return this.email;
    }

    public AccountStatus getAccountStatus() {
        return this.accountStatus;
    }

    public int getBidCount() {return this.bidCount;}

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getJoinedDate() {
        if (joinedDate == null) return "N/A";
        // Định dạng ngày theo kiểu ngày/tháng/năm
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return joinedDate.format(formatter);
    }
    public void setJoinedDate(LocalDateTime joinedDate) {
        this.joinedDate = joinedDate;
    }

    public void login() {}

    public void logout() {}

    public boolean isAdmin() {

        return systemRole == SystemRole.ADMIN;
    }
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setbidCount(int bidCount) { this.bidCount = bidCount; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getCardholderName() { return cardholderName; }
    public void setCardholderName(String cardholderName) { this.cardholderName = cardholderName; }

    public Double getBalance() {
        return this.balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}
