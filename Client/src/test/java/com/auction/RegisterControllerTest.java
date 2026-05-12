package com.auction;

import com.auction.client.controller.RegisterController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class RegisterControllerTest {

    private RegisterController controller;

    @BeforeEach
    void setup() {

        controller = new RegisterController();
    }

    // =========================
    // TEST generatedOtp
    // =========================
    @Test
    void testGeneratedOtp() throws Exception {

        setField("generatedOtp", "123456");

        String otp = (String) getField("generatedOtp");

        assertEquals("123456", otp);
    }

    // =========================
    // TEST otpExpiry
    // =========================
    @Test
    void testOtpExpiry() throws Exception {

        long time = 99999L;

        setField("otpExpiry", time);

        long result = (long) getField("otpExpiry");

        assertEquals(time, result);
    }

    // =========================
    // TEST lastOtpSent
    // =========================
    @Test
    void testLastOtpSent() throws Exception {

        setField("lastOtpSent", 5000L);

        long result = (long) getField("lastOtpSent");

        assertEquals(5000L, result);
    }

    // =========================
    // TEST controller created
    // =========================
    @Test
    void testControllerCreated() {

        assertNotNull(controller);
    }

    // =========================
    // HELPER
    // =========================
    private void setField(String name, Object value) throws Exception {

        Field field = RegisterController.class.getDeclaredField(name);

        field.setAccessible(true);

        field.set(controller, value);
    }

    private Object getField(String name) throws Exception {

        Field field = RegisterController.class.getDeclaredField(name);

        field.setAccessible(true);

        return field.get(controller);
    }
}