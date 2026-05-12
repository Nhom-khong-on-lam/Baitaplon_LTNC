package com.auction.client.model;

import com.auction.client.Enum.SystemRole;
import com.auction.client.Enum.AccountStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class User extends BaseEntity {
    private String username;
    private String passwordHash;
    private String email;
    private SystemRole systemRole;
    private AccountStatus accountStatus;
    private LocalDateTime joinedDate = LocalDateTime.now();

    public User(Long id, String username, String passwordHash, String email, SystemRole systemRole) {
        super(id);
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.systemRole = systemRole;
        this.accountStatus = AccountStatus.ACTIVE;
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

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getJoinedDate() {
        if (joinedDate == null) return "N/A";
        // Định dạng ngày theo kiểu ngày/tháng/năm
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return joinedDate.format(formatter);
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
}
