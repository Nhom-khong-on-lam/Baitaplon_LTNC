package model;

import Enum.SystemRole;
import Enum.AccountStatus;

public class User extends BaseEntity {
    private String username;
    private String passwordHash;
    private String email;
    private SystemRole systemRole;
    private AccountStatus accountStatus;

    public User(Long id, String username, String passwordHash, String email, SystemRole systemRole) {
        //super(id);
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.systemRole = systemRole;
        this.accountStatus = accountStatus;
    }

    //GETTER
    public String getUsername() {
        return this.username;
    }

    public String getPasswordHash() {
        return this.passwordHash;
    }

    public String getEmail() {
        return this.email;
    }

    public AccountStatus getAccountStatus() {
        return this.accountStatus;
    }

    public void login() {}

    public void logout() {}

    public boolean isAdmin() {
        return false;
    }
}
