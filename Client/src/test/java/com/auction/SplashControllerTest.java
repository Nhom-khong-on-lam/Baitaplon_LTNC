package com.auction;

import com.auction.client.controller.SplashController;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class SplashControllerTest {

    // =========================
    // TEST STATUS_MSGS size
    // =========================
    @Test
    void testStatusMessagesSize() throws Exception {

        SplashController controller = new SplashController();

        Field field = SplashController.class.getDeclaredField("STATUS_MSGS");

        field.setAccessible(true);

        String[] msgs = (String[]) field.get(controller);

        assertEquals(5, msgs.length);
    }

    // =========================
    // TEST first status message
    // =========================
    @Test
    void testFirstStatusMessage() throws Exception {

        SplashController controller = new SplashController();

        Field field = SplashController.class.getDeclaredField("STATUS_MSGS");

        field.setAccessible(true);

        String[] msgs = (String[]) field.get(controller);

        assertEquals("Initializing core...", msgs[0]);
    }

    // =========================
    // TEST last status message
    // =========================
    @Test
    void testLastStatusMessage() throws Exception {

        SplashController controller = new SplashController();

        Field field = SplashController.class.getDeclaredField("STATUS_MSGS");

        field.setAccessible(true);

        String[] msgs = (String[]) field.get(controller);

        assertEquals("Ready!", msgs[4]);
    }

    // =========================
    // TEST all messages not null
    // =========================
    @Test
    void testAllMessagesNotNull() throws Exception {

        SplashController controller = new SplashController();

        Field field = SplashController.class.getDeclaredField("STATUS_MSGS");

        field.setAccessible(true);

        String[] msgs = (String[]) field.get(controller);

        for (String msg : msgs) {

            assertNotNull(msg);

            assertFalse(msg.isEmpty());
        }
    }

    // =========================
    // TEST startProgress exists
    // =========================
    @Test
    void testStartProgressMethodExists() throws Exception {

        Method method = SplashController.class.getDeclaredMethod("startProgress");

        assertNotNull(method);
    }

    @Test
    void testInitializeMethodExists() throws Exception {

        Method method = SplashController.class.getDeclaredMethod("initialize");

        assertNotNull(method);
    }
}