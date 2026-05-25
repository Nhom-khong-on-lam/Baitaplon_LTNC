import com.auction.common.enums.AuctionStatus;
import com.auction.common.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionTest {

    // ─── Helpers ────────────────────────────────────────────────────────────

    private User makeUser(long id, String username) {
        return new User(id, username, "hash", username + "@test.com",
                com.auction.common.enums.SystemRole.USER);
    }

    private Art makeArt(String name, double startPrice) {
        return new Art(name, "desc", startPrice, "Artist", 2020);
    }

    // ════════════════════════════════════════════════════════════════════════
    // AutoBidConfig
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("AutoBidConfig")
    class AutoBidConfigTest {

        User bidder;
        AutoBidConfig config;

        @BeforeEach
        void setUp() {
            bidder = makeUser(1L, "alice");
            config = new AutoBidConfig(bidder, 500.0, 20.0);
        }

        @Test
        @DisplayName("Mới tạo thì active = true")
        void newConfigIsActive() {
            assertTrue(config.isActive());
        }

        @Test
        @DisplayName("deactivate() tắt config")
        void deactivateSetsActiveFalse() {
            config.deactivate();
            assertFalse(config.isActive());
        }

        @Test
        @DisplayName("getNextBid trả về currentPrice + step khi còn trong giới hạn")
        void getNextBid_withinLimit() {
            // currentPrice = 400, step = 20 → next = 420 ≤ maxPrice 500
            assertEquals(420.0, config.getNextBid(400.0));
        }

        @Test
        @DisplayName("getNextBid trả về -1 khi vượt maxPrice")
        void getNextBid_exceedsMax() {
            // currentPrice = 490, step = 20 → next = 510 > 500
            assertEquals(-1, config.getNextBid(490.0));
        }

        @Test
        @DisplayName("getNextBid trả về -1 khi config đã bị deactivate")
        void getNextBid_whenInactive() {
            config.deactivate();
            assertEquals(-1, config.getNextBid(100.0));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // BidTransaction
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("BidTransaction")
    class BidTransactionTest {

        User bidder;

        @BeforeEach
        void setUp() {
            bidder = makeUser(2L, "bob");
        }

        @Test
        @DisplayName("isValid đúng khi amount >= currentPrice + step")
        void isValid_true() {
            BidTransaction bid = new BidTransaction(bidder, 130.0, false);
            assertTrue(bid.isValid(100.0, 20.0)); // 130 >= 120
        }

        @Test
        @DisplayName("isValid sai khi amount < currentPrice + step")
        void isValid_false() {
            BidTransaction bid = new BidTransaction(bidder, 110.0, false);
            assertFalse(bid.isValid(100.0, 20.0)); // 110 < 120
        }

        @Test
        @DisplayName("getFormattedTime trả về HH:mm:ss")
        void getFormattedTime_format() {
            BidTransaction bid = new BidTransaction(bidder, 100.0, false);
            // Kiểm tra pattern HH:mm:ss (e.g. "14:05:09")
            assertTrue(bid.getFormattedTime().matches("\\d{2}:\\d{2}:\\d{2}"));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Auction — addBid
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Auction.addBid")
    class AddBidTest {

        Auction auction;
        User seller;
        User bidder;

        @BeforeEach
        void setUp() {
            seller = makeUser(1L, "seller");
            bidder = makeUser(2L, "bidder");
            Art art = makeArt("Mona Lisa", 100.0);
            auction = new Auction(art, seller, LocalDateTime.now().plusHours(2));
        }

        @Test
        @DisplayName("Bid hợp lệ thì trả về true và cập nhật giá")
        void validBid_accepted() {
            BidTransaction bid = new BidTransaction(bidder, 120.0, false); // minIncrement = 10
            assertTrue(auction.addBid(bid));
            assertEquals(120.0, auction.getCurrentPrice());
            assertEquals(bidder, auction.getHighestBidder());
        }

        @Test
        @DisplayName("Bid thấp hơn currentPrice + minIncrement thì bị từ chối")
        void lowBid_rejected() {
            BidTransaction bid = new BidTransaction(bidder, 105.0, false); // < 100 + 10
            assertFalse(auction.addBid(bid));
            assertEquals(100.0, auction.getCurrentPrice()); // giá không đổi
        }

        @Test
        @DisplayName("Bid khi auction đã FINISHED thì bị từ chối")
        void bidOnFinishedAuction_rejected() {
            auction.closeAuction();
            BidTransaction bid = new BidTransaction(bidder, 200.0, false);
            assertFalse(auction.addBid(bid));
        }

        @Test
        @DisplayName("Bid hợp lệ thêm vào bidHistory")
        void validBid_addedToHistory() {
            BidTransaction bid = new BidTransaction(bidder, 120.0, false);
            auction.addBid(bid);
            assertEquals(1, auction.getBidHistory().size());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Auction — getMinIncrement
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Auction.getMinIncrement")
    class MinIncrementTest {

        Auction auction;

        @BeforeEach
        void setUp() {
            User seller = makeUser(1L, "seller");
            Art art = makeArt("Test", 100.0);
            auction = new Auction(art, seller, LocalDateTime.now().plusHours(1));
        }

        @Test
        @DisplayName("Giá <= 500 → increment = 10")
        void increment_low() {
            auction.setCurrentPrice(300.0);
            assertEquals(10.0, auction.getMinIncrement());
        }

        @Test
        @DisplayName("Giá 501–1000 → increment = 20")
        void increment_mid() {
            auction.setCurrentPrice(750.0);
            assertEquals(20.0, auction.getMinIncrement());
        }

        @Test
        @DisplayName("Giá > 1000 → increment = 50")
        void increment_high() {
            auction.setCurrentPrice(1500.0);
            assertEquals(50.0, auction.getMinIncrement());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Auction — trạng thái và thời gian
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Auction — status & time helpers")
    class StatusTimeTest {

        Auction auction;

        @BeforeEach
        void setUp() {
            User seller = makeUser(1L, "seller");
            Art art = makeArt("Test", 100.0);
            auction = new Auction(art, seller, LocalDateTime.now().plusHours(2));
        }

        @Test
        @DisplayName("isLive() = true khi RUNNING và chưa hết giờ")
        void isLive_true() {
            assertTrue(auction.isLive());
        }

        @Test
        @DisplayName("isLive() = false sau khi closeAuction()")
        void isLive_afterClose() {
            auction.closeAuction();
            assertFalse(auction.isLive());
        }

        @Test
        @DisplayName("isEndingSoon() = true khi còn < 60 phút")
        void isEndingSoon_true() {
            auction.setEndTime(LocalDateTime.now().plusMinutes(30));
            assertTrue(auction.isEndingSoon());
        }

        @Test
        @DisplayName("isEndingSoon() = false khi còn > 60 phút")
        void isEndingSoon_false() {
            // endTime đã set là +2h trong setUp
            assertFalse(auction.isEndingSoon());
        }

        @Test
        @DisplayName("getRemainingSeconds() > 0 khi chưa hết giờ")
        void remainingSeconds_positive() {
            assertTrue(auction.getRemainingSeconds() > 0);
        }

        @Test
        @DisplayName("getRemainingSeconds() = 0 khi đã hết giờ")
        void remainingSeconds_zero() {
            auction.setEndTime(LocalDateTime.now().minusMinutes(1));
            assertEquals(0, auction.getRemainingSeconds());
        }

        @Test
        @DisplayName("getTimeRemaining() trả về format HH:mm:ss")
        void getTimeRemaining_format() {
            assertTrue(auction.getTimeRemaining().matches("\\d{2}:\\d{2}:\\d{2}"));
        }

        @Test
        @DisplayName("extendEndTime() cộng thêm giây vào endTime")
        void extendEndTime() {
            LocalDateTime before = auction.getEndTime();
            auction.extendEndTime(180);
            assertEquals(before.plusSeconds(180), auction.getEndTime());
        }

        @Test
        @DisplayName("closeAuction() chuyển status sang FINISHED")
        void closeAuction_setsFinished() {
            auction.closeAuction();
            assertEquals(AuctionStatus.FINISHED, auction.getStatus());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Auction — anti-sniping (tự động gia hạn khi bid vào 3 phút cuối)
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Auction — anti-sniping")
    class AntiSnipingTest {

        @Test
        @DisplayName("Bid trong 3 phút cuối → endTime tự động +3 phút")
        void bidInLastMinutes_extendsTime() {
            User seller = makeUser(1L, "seller");
            User bidder = makeUser(2L, "bidder");
            Art art = makeArt("Test", 100.0);

            // Đặt endTime còn 2 phút (< 180 giây) để trigger anti-sniping
            Auction auction = new Auction(art, seller, LocalDateTime.now().plusSeconds(120));
            LocalDateTime endBefore = auction.getEndTime();

            BidTransaction bid = new BidTransaction(bidder, 115.0, false);
            auction.addBid(bid);

            // endTime phải được cộng thêm 180 giây
            assertTrue(auction.getEndTime().isAfter(endBefore));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Auction — isUserWinning / isUserWon / getUserBidAmount
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Auction — user bid helpers")
    class UserBidHelpersTest {

        Auction auction;
        User seller;
        User alice;
        User bob;

        @BeforeEach
        void setUp() {
            seller = makeUser(1L, "seller");
            alice  = makeUser(2L, "alice");
            bob    = makeUser(3L, "bob");
            Art art = makeArt("Test", 100.0);
            auction = new Auction(art, seller, LocalDateTime.now().plusHours(1));
        }

        @Test
        @DisplayName("isUserWinning() đúng cho highest bidder")
        void isUserWinning_true() {
            auction.addBid(new BidTransaction(alice, 120.0, false));
            assertTrue(auction.isUserWinning(2L));
            assertFalse(auction.isUserWinning(3L));
        }

        @Test
        @DisplayName("isUserWon() chỉ true khi FINISHED và là highest bidder")
        void isUserWon() {
            auction.addBid(new BidTransaction(alice, 120.0, false));
            assertFalse(auction.isUserWon(2L)); // chưa finish
            auction.closeAuction();
            assertTrue(auction.isUserWon(2L));
            assertFalse(auction.isUserWon(3L));
        }

        @Test
        @DisplayName("getUserBidAmount() trả về bid cao nhất của user đó")
        void getUserBidAmount() {
            auction.addBid(new BidTransaction(alice, 120.0, false));
            auction.addBid(new BidTransaction(bob,   150.0, false));
            auction.addBid(new BidTransaction(alice, 180.0, false));
            assertEquals(180.0, auction.getUserBidAmount(2L));
        }

        @Test
        @DisplayName("getUserBidAmount() = 0 nếu user chưa bid")
        void getUserBidAmount_noBid() {
            assertEquals(0.0, auction.getUserBidAmount(99L));
        }
    }
}