package com.auction;

import com.auction.client.Enum.AccountStatus;
import com.auction.client.Enum.SystemRole;
import com.auction.client.controller.AdminUsersController;
import com.auction.client.model.User;
import javafx.application.Platform;
import javafx.scene.control.*;
import org.junit.jupiter.api.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import static org.junit.jupiter.api.Assertions.*;

public class AdminUsersControllerTest {
    private AdminUsersController controller;
    private List<User> testUsers;

    @BeforeAll
    static void initJFX() throws InterruptedException {
        // Khởi tạo JavaFX Toolkit một lần duy nhất cho toàn bộ class test
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(latch::countDown);
        latch.await();
    }

    @BeforeEach
    void setUp() {
        controller = new AdminUsersController();

        // Tạo dữ liệu mẫu dựa trên User model
        testUsers = new ArrayList<>();
        testUsers.add(new User(1L, "admin_test", "hash", "admin@test.com", SystemRole.ADMIN));

        User activeUser = new User(2L, "anh_nguyen", "hash", "anh@test.com", SystemRole.USER);
        testUsers.add(activeUser);

        User bannedUser = new User(3L, "bad_guy", "hash", "bad@test.com", SystemRole.USER);
        bannedUser.setAccountStatus(AccountStatus.BANNED); //
        testUsers.add(bannedUser);

        // Inject các thành phần thật để tránh lỗi Mockito
        setPrivateField(controller, "allUsers", testUsers);
        setPrivateField(controller, "usersTable", new TableView<User>());
        setPrivateField(controller, "totalLabel", new Label());
        setPrivateField(controller, "searchField", new TextField());

        // Mock các tab button
        setPrivateField(controller, "tabAll", new Button());
        setPrivateField(controller, "tabActive", new Button());
        setPrivateField(controller, "tabBanned", new Button());
        setPrivateField(controller, "tabAdmin", new Button());
    }

    @Test
    @DisplayName("Chức năng: Lọc tất cả người dùng")
    void testShowAll() {
        controller.showAll();
        TableView<User> table = getTable();
        assertEquals(3, table.getItems().size(), "Phải hiển thị đủ 3 users");
    }

    @Test
    @DisplayName("Chức năng: Lọc người dùng đang hoạt động (Status ACTIVE)")
    void testShowActive() {
        controller.showActive();
        TableView<User> table = getTable();
        // Admin và Active user đều có status ACTIVE
        assertEquals(2, table.getItems().size());
        assertTrue(table.getItems().stream().allMatch(u -> u.getAccountStatus() == AccountStatus.ACTIVE));
    }

    @Test
    @DisplayName("Chức năng: Lọc người dùng bị khóa (Status BANNED)")
    void testShowBanned() {
        controller.showBanned();
        TableView<User> table = getTable();
        assertEquals(1, table.getItems().size());
        assertEquals(AccountStatus.BANNED, table.getItems().get(0).getAccountStatus());
    }

    @Test
    @DisplayName("Chức năng: Lọc danh sách quản trị viên (Role ADMIN)")
    void testShowAdmins() {
        controller.showAdmins();
        TableView<User> table = getTable();
        assertEquals(1, table.getItems().size());
        assertEquals("admin_test", table.getItems().get(0).getUsername());
    }

    @Test
    @DisplayName("Chức năng: Tìm kiếm theo Username")
    void testHandleSearch_Username() {
        TextField search = (TextField) getPrivateField(controller, "searchField");
        search.setText("anh");

        controller.handleSearch();

        TableView<User> table = getTable();
        assertEquals(1, table.getItems().size());
        assertEquals("anh_nguyen", table.getItems().get(0).getUsername());
    }

    @Test
    @DisplayName("Chức năng: Tìm kiếm theo Email")
    void testHandleSearch_Email() {
        TextField search = (TextField) getPrivateField(controller, "searchField");
        search.setText("bad@test.com");

        controller.handleSearch();

        TableView<User> table = getTable();
        assertEquals(1, table.getItems().size());
        assertEquals("bad_guy", table.getItems().get(0).getUsername());
    }

    @Test
    @DisplayName("Chức năng: Nhận keyword search từ controller khác")
    void testApplySearch() {
        // Hàm này nhận String keyword và tự động trigger search
        controller.applySearch("admin");

        TableView<User> table = getTable();
        assertEquals(1, table.getItems().size());
        assertTrue(table.getItems().get(0).isAdmin());
    }

    // --- Helpers ---
    private TableView<User> getTable() {
        return (TableView<User>) getPrivateField(controller, "usersTable");
    }

    private void setPrivateField(Object obj, String fieldName, Object value) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private Object getPrivateField(Object obj, String fieldName) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) { return null; }
    }
}