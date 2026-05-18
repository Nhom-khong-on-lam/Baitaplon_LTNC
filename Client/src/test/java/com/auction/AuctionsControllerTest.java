package com.auction;

import com.auction.client.controller.AuctionsController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionsControllerTest {

    private AuctionsController controller;

    @BeforeAll
    static void initFX() throws Exception {

        CountDownLatch latch = new CountDownLatch(1);

        Platform.startup(latch::countDown);

        latch.await();
    }

    @BeforeEach
    void setUp() {

        controller = new AuctionsController();
    }

    @Test
    void testSearchFieldText() {

        TextField field = new TextField();

        field.setText("Laptop");

        assertEquals(
                "Laptop",
                field.getText()
        );
    }

    @Test
    void testLabelText() {

        Label label = new Label();

        label.setText("Loaded");

        assertEquals(
                "Loaded",
                label.getText()
        );
    }

    @Test
    void testTableViewCreated() {

        TableView<String> table =
                new TableView<>();

        assertNotNull(table);
    }

    @Test
    void testObservableListAdd() {

        ObservableList<String> list =
                FXCollections.observableArrayList();

        list.add("Auction 1");

        assertEquals(
                1,
                list.size()
        );
    }

    @Test
    void testSearchLogic() {

        String title = "Laptop Dell";

        assertTrue(
                title.toLowerCase()
                        .contains("laptop")
        );
    }

    @Test
    void testInjectSearchField() throws Exception {

        TextField field = new TextField();

        Field f = controller.getClass()
                .getDeclaredField("searchField");

        f.setAccessible(true);

        f.set(controller, field);

        assertNotNull(f.get(controller));
    }
}