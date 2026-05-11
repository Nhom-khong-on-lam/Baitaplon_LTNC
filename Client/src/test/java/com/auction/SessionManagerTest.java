package com.auction;

import com.auction.client.Enum.SystemRole;
import com.auction.client.controller.SessionManager;
import com.auction.client.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SessionManagerTest {

    // =========================
    // TEST singleton
    // =========================
    @Test
    void testSingleton() {

        SessionManager s1 = SessionManager.get();

        SessionManager s2 = SessionManager.get();

        assertSame(s1, s2);
    }

    // =========================
    // TEST login()
    // =========================
    @Test
    void testLogin() {

        SessionManager session = SessionManager.get();

        User user = new User(
                1L,
                "chau",
                "123456",
                "chau@gmail.com",
                SystemRole.USER
        );

        session.login(user);

        assertEquals(user, session.getUser());
    }

    // =========================
    // TEST logout()
    // =========================
    @Test
    void testLogout() {

        SessionManager session = SessionManager.get();

        User user = new User(1L, "chau", "123456", "chau@gmail.com", SystemRole.USER);

        session.login(user);

        session.logout();

        assertNull(session.getUser());
    }

    // =========================
    // TEST isLoggedIn() = true
    // =========================
    @Test
    void testIsLoggedInTrue() {

        SessionManager session = SessionManager.get();

        User user = new User(1L, "chau", "123456", "chau@gmail.com", SystemRole.USER);

        session.login(user);

        assertTrue(session.isLoggedIn());
    }

    // =========================
    // TEST isLoggedIn() = false
    // =========================
    @Test
    void testIsLoggedInFalse() {

        SessionManager session = SessionManager.get();

        session.logout();

        assertFalse(session.isLoggedIn());
    }

    // =========================
    // TEST isAdmin() = true
    // =========================
    @Test
    void testIsAdminTrue() {

        SessionManager session = SessionManager.get();

        User admin = new User(1L, "admin", "123456", "admin@gmail.com", SystemRole.ADMIN);

        session.login(admin);

        assertTrue(session.isAdmin());
    }

    // =========================
    // TEST isAdmin() = false
    // =========================
    @Test
    void testIsAdminFalse() {

        SessionManager session = SessionManager.get();

        User user = new User(1L, "chau", "123456", "chau@gmail.com", SystemRole.USER);

        session.login(user);

        assertFalse(session.isAdmin());
    }

    // =========================
    // TEST isAdmin() khi chưa login
    // =========================
    @Test
    void testIsAdminWhenNoUser() {

        SessionManager session = SessionManager.get();

        session.logout();

        assertFalse(session.isAdmin());
    }
}