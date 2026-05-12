package com.auction;

import com.auction.client.Enum.AccountStatus;
import com.auction.client.Enum.SystemRole;
import com.auction.client.controller.ProfileController;
import com.auction.client.model.Auction;
import com.auction.client.model.Item;
import com.auction.client.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProfileControllerTest {

    private ProfileController controller;

    @BeforeEach
    void setup() {

        controller = new ProfileController();
    }

    // =========================================
    // TEST currentUser
    // =========================================
    @Test
    void testSetCurrentUser() throws Exception {

        User user = createUser();

        setField("currentUser", user);

        User result = (User) getField("currentUser");

        assertEquals("chau", result.getUsername());
    }

    // =========================================
    // TEST username update
    // =========================================
    @Test
    void testUpdateUsername() {

        User user = createUser();

        user.setUsername("newname");

        assertEquals("newname", user.getUsername());
    }

    // =========================================
    // TEST email update
    // =========================================
    @Test
    void testUpdateEmail() {

        User user = createUser();

        user.setEmail("new@gmail.com");

        assertEquals("new@gmail.com", user.getEmail());
    }

    // =========================================
    // TEST admin role
    // =========================================
    @Test
    void testAdminRole() {

        User admin = new User(
                1L,
                "admin",
                "123456",
                "admin@gmail.com",
                SystemRole.ADMIN
        );

        assertTrue(admin.isAdmin());
    }

    // =========================================
    // TEST user role
    // =========================================
    @Test
    void testUserRole() {

        User user = createUser();

        assertFalse(user.isAdmin());
    }

    // =========================================
    // TEST live auction
    // =========================================
    @Test
    void testLiveAuction() {

        Auction auction = createAuction(true);

        assertTrue(auction.isLive());
    }

    // =========================================
    // TEST finished auction
    // =========================================
    @Test
    void testFinishedAuction() {

        Auction auction = createAuction(false);

        assertFalse(auction.isLive());
    }

    // =========================================
    // TEST current price
    // =========================================
    @Test
    void testCurrentPrice() {

        Auction auction = createAuction(true);

        auction.setCurrentPrice(3000);

        assertEquals(3000, auction.getCurrentPrice());
    }

    // =========================================
    // TEST auction title
    // =========================================
    @Test
    void testAuctionTitle() {

        Auction auction = createAuction(true);

        assertEquals("Laptop", auction.getTitle());
    }

    // =========================================
    // TEST private method exists
    // =========================================
    @Test
    void testPrivateMethodExists() throws Exception {

        Method method = ProfileController.class.getDeclaredMethod("buildWonList", List.class);

        assertNotNull(method);
    }

    // =========================================
    // CREATE USER
    // =========================================
    private User createUser() {

        User user = new User(1L, "chau", "123456", "chau@gmail.com", SystemRole.USER);

        user.setAccountStatus(AccountStatus.ACTIVE);

        return user;
    }

    // =========================================
    // CREATE AUCTION
    // =========================================
    private Auction createAuction(boolean live) {

        User seller = createUser();

        Item item = new Item() {

            @Override
            public String getCategory() {
                return "Electronics";
            }
        };

        item.setName("Laptop");

        LocalDateTime endTime;

        if (live) {
            endTime = LocalDateTime.now().plusDays(1);
        } else {
            endTime = LocalDateTime.now().minusDays(1);
        }

        Auction auction =
                new Auction(item, seller, endTime);

        auction.setCurrentPrice(1500);

        return auction;
    }

    // =========================================
    // REFLECTION HELPERS
    // =========================================
    private void setField(String fieldName, Object value)
            throws Exception {

        Field field = ProfileController.class.getDeclaredField(fieldName);

        field.setAccessible(true);

        field.set(controller, value);
    }

    private Object getField(String fieldName) throws Exception {

        Field field = ProfileController.class.getDeclaredField(fieldName);

        field.setAccessible(true);

        return field.get(controller);
    }
}