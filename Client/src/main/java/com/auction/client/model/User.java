package com.auction.client.model;

import com.auction.client.Enum.SystemRole;
import com.auction.client.Enum.AccountStatus;

public class User extends BaseEntity {
    private String username;
    private String passwordHash;
    private String email;
    private SystemRole systemRole;
    private AccountStatus accountStatus;

    public User(Long id, String username, String passwordHash, String email, SystemRole systemRole) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.systemRole = systemRole;
        this.accountStatus = accountStatus;
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

    public void login() {}

    public void logout() {}

    public boolean isAdmin() {

        return systemRole == SystemRole.ADMIN;
    }
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
