package com.auction;

import com.auction.common.enums.AuctionStatus;
import com.auction.common.enums.SystemRole;
import com.auction.common.model.Auction;
import com.auction.common.model.BidTransaction;
import com.auction.common.model.Item;
import com.auction.common.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AuctionTest {

    private Auction auction;
    private TestItem item;
    private User seller;
    private User bidder;

    static class TestItem extends Item {
        private String category;

        public TestItem(String name, String description, double startingPrice, String category) {
            super(name, description, startingPrice);
            this.category = category;
        }

        @Override
        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }
    }

    @BeforeEach
    void setUp() {
        item = new TestItem("Test Item", "Description", 100.0, "Electronics");
        seller = new User(1L, "seller", "pass", "seller@test.com", SystemRole.USER);
        bidder = new User(2L, "bidder", "pass", "bidder@test.com", SystemRole.USER);

        LocalDateTime endTime = LocalDateTime.now().plusHours(2);
        auction = new Auction(item, seller, endTime);
    }

    @Test
    @DisplayName("Nên tính toán bước giá tối thiểu chính xác")
    void testGetMinIncrement() {
        // Giá hiện tại là 100 (từ setUp) -> increment = 10
        assertThat(auction.getMinIncrement()).isEqualTo(10.0);

        // Giả lập giá tăng lên cao hơn
        auction.setCurrentPrice(600.0);
        assertThat(auction.getMinIncrement()).isEqualTo(20.0);

        auction.setCurrentPrice(1500.0);
        assertThat(auction.getMinIncrement()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Không nên chấp nhận giá thầu thấp hơn giá hiện tại + bước giá")
    void shouldRejectLowBid() {
        // Given
        double currentPrice = auction.getCurrentPrice(); // 100.0
        double minIncrement = auction.getMinIncrement(); // 10.0

        // Tạo một bid thấp (105.0) trong khi cần ít nhất 110.0
        BidTransaction lowBid = new BidTransaction(bidder, 105.0, false);

        // When
        boolean result = auction.addBid(lowBid);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Nên chấp nhận giá thầu hợp lệ và cập nhật người thắng tạm thời")
    void shouldAcceptValidBid() {
        // Given
        Long bidderId = 2L;

        // Tạo một bid cao hơn giá hiện tại (100) + bước giá (10)
        double validAmount = 200.0;
        BidTransaction validBid = new BidTransaction(bidder, validAmount, false);

        // When
        boolean result = auction.addBid(validBid);

        // Then
        assertThat(result).isTrue();
        assertThat(auction.getCurrentPrice()).isEqualTo(validAmount);
        assertThat(auction.getHighestBidder()).isEqualTo(bidder);
        assertThat(auction.getHighestBidder().getId()).isEqualTo(bidderId);
    }

    @Test
    @DisplayName("Nên kiểm tra trạng thái Live chính xác")
    void testIsLive() {
        // Lúc mới tạo, status là RUNNING và chưa hết hạn
        assertThat(auction.isLive()).isTrue();

        // Sau khi đóng đấu giá
        auction.closeAuction();
        assertThat(auction.isLive()).isFalse();
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.FINISHED);
    }

    @Test
    @DisplayName("Nên định dạng thời gian còn lại đúng kiểu HH:mm:ss")
    void testGetTimeRemaining() {
        // Ép kiểu endTime về 1 giờ 1 phút 1 giây tính từ bây giờ
        auction.setEndTime(LocalDateTime.now().plusHours(1).plusMinutes(1).plusSeconds(1));

        String remaining = auction.getTimeRemaining();

        assertThat(remaining).startsWith("01:01:0"); // Kiểm tra phần đầu vì giây có thể lệch 1s khi chạy
    }

    @Test
    @DisplayName("Nên trả về đúng Icon theo Category")
    void testGetCategoryIcon() {
        item.setCategory("Electronics");
        assertThat(auction.getCategoryIcon()).isEqualTo("CATEGORY_ELECTRONICS");

        // Trường hợp: Art
        item.setCategory("Art");
        assertThat(auction.getCategoryIcon()).isEqualTo("CATEGORY_ART");

        // Trường hợp: Danh mục không tồn tại
        item.setCategory("Furniture");
        assertThat(auction.getCategoryIcon()).isEqualTo("CATEGORY_DEFAULT");

        // Trường hợp: Danh mục bị null
        item.setCategory(null);
        assertThat(auction.getCategoryIcon()).isEqualTo("CATEGORY_DEFAULT");
    }

    @Test
    @DisplayName("Nên xác định được người đang thắng cuộc")
    void testIsUserWinning() {
        Long userId = 2L;

        BidTransaction bid = new BidTransaction(bidder, 200.0, false);
        auction.addBid(bid);

        assertThat(auction.isUserWinning(userId)).isTrue();
        assertThat(auction.isUserWinning(999L)).isFalse();
    }
}