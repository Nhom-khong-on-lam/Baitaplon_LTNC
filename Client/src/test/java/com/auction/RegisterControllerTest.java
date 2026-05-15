package com.auction;

import com.auction.client.controller.RegisterController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RegisterControllerTest {

    private RegisterController controller;

    @BeforeEach
    void setup() {
        controller = new RegisterController();
    }

    // ── Controller instantiation ──────────────────────────────
    @Test
    void testControllerCreated() {
        assertNotNull(controller);
    }

    // ── AuthService is initialized ────────────────────────────
    @Test
    void testAuthServiceNotNull() throws Exception {
        java.lang.reflect.Field field = RegisterController.class.getDeclaredField("authService");
        field.setAccessible(true);
        assertNotNull(field.get(controller));
    }
}