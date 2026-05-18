package com.auction;

import com.auction.client.controller.SceneManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SceneManagerTest {

    // =========================
    // TEST singleton
    // =========================
    @Test
    void testSingleton() {

        SceneManager s1 = SceneManager.get();

        SceneManager s2 = SceneManager.get();

        assertSame(s1, s2);
    }

    // =========================
    // TEST enum LOGIN
    // =========================
    @Test
    void testLoginScreenPath() {

        assertEquals("/com/auction/client/login.fxml", SceneManager.Screen.LOGIN.path);
    }

    // =========================
    // TEST enum REGISTER
    // =========================
    @Test
    void testRegisterScreenPath() {

        assertEquals("/com/auction/client/register.fxml", SceneManager.Screen.REGISTER.path);
    }

    // =========================
    // TEST enum MAIN
    // =========================
    @Test
    void testMainScreenPath() {

        assertEquals("/com/auction/client/main.fxml", SceneManager.Screen.MAIN.path);
    }

    // =========================
    // TEST enum ADMIN_MAIN
    // =========================
    @Test
    void testAdminMainScreenPath() {

        assertEquals("/com/auction/client/admin_main.fxml", SceneManager.Screen.ADMIN_MAIN.path);
    }

    // =========================
    // TEST enum SPLASH
    // =========================
    @Test
    void testSplashScreenPath() {

        assertEquals("/com/auction/client/splash.fxml", SceneManager.Screen.SPLASH.path);
    }

    // =========================
    // TEST enum count
    // =========================
    @Test
    void testScreenEnumCount() {

        assertEquals(5, SceneManager.Screen.values().length);
    }
}