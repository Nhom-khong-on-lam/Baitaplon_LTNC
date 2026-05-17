package com.auction.common.model;

import com.auction.common.enums.AuctionStatus;
import com.auction.common.model.BaseEntity;
import com.auction.common.observer.Observer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import java.util.stream.Collectors;


public class Auction extends BaseEntity {
    private Item item ;
    private User seller;
    private User highestBidder;
    private List<BidTransaction> bidHistory = new ArrayList<>();
    private List<Observer> observers = new ArrayList<>();
    private List<AutoBidConfig> autoBidConfigs = new ArrayList<>();
    private double currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;

    // Constructor
    public Auction(Item item, User seller, LocalDateTime endTime) {
        this.item = item;
        this.seller = seller;
        this.startTime = LocalDateTime.now();
        this.endTime = endTime;
        this.currentPrice = item.getStartingPrice();
        this.status = AuctionStatus.RUNNING;
    }

    public boolean addBid(BidTransaction bid) {
        if (status != AuctionStatus.RUNNING) return false;
        if (bid.getAmount() <= currentPrice) return false;
        if (bid.getAmount() < currentPrice + getMinIncrement()) return false;

        this.currentPrice = bid.getAmount();
        this.highestBidder = bid.getBidder();
        bidHistory.add(bid);

        // CHỈ gọi auto bid nếu đây là lượt đặt giá thủ công (không phải từ auto bid khác)
        if (!bid.isAutoBid()) {
            processAutoBids();
        }

        notifyObservers();
        return true;
    }

    public double getMinIncrement() {
        if (currentPrice < 100) return 5;
        else if (currentPrice < 500) return 10;
        else if (currentPrice < 1000) return 20;
        else return 50;
    }

    public void processAutoBids() {
        List<AutoBidConfig> activeConfigs = autoBidConfigs.stream()
                .filter(AutoBidConfig::isActive)
                .filter(c -> !c.getBidder().equals(highestBidder))
                .collect(Collectors.toList());

        boolean changed;
        do {
            changed = false;
            for (AutoBidConfig config : activeConfigs) {
                double nextBid = config.getNextBid(currentPrice);
                if (nextBid > 0 && nextBid > currentPrice) {
                    // Tạo bid tự động
                    BidTransaction autoBid = new BidTransaction(config.getBidder(), nextBid, true);
                    if (addBid(autoBid)) {
                        changed = true;
                        break;
                    }
                }
            }
        } while (changed);
    }

    public void closeAuction() {
        this.status = AuctionStatus.FINISHED;
        notifyObservers();
    }

    public void extendEndTime(int seconds) {
        this.endTime = this.endTime.plusSeconds(seconds);
        notifyObservers();
    }

    public void registerAutoBid(AutoBidConfig config) {
        autoBidConfigs.add(config);
    }

    public double getCurrentPrice() { return currentPrice; }
    public User getHighestBidder() { return highestBidder; }
    public List<BidTransaction> getBidHistory() { return bidHistory; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    public Item getItem() { return item; }
    public User getSeller() { return seller; }
    public int getBidCount(){ return bidHistory.size();}
    public String getTitle(){return item.getName();}
    public String getDescription(){return item.getDescription();}
    public String getCategory(){return item.getCategory();}
    public void setId(Long id) {
        this.id = id;
    }
    public void setCurrentPrice(double price) {
        this.currentPrice =price;
    }
    public void setHighestBidder(User bidder){
        this.highestBidder = bidder;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public void setSeller(User seller) {
        this.seller = seller;
    }


    public void setBidHistory(List<BidTransaction> bidHistory) {
        this.bidHistory = bidHistory;
    }
    public String getCategoryIcon() {
        String category = item.getCategory();
        if (category == null) return "📦";

        return switch (category.toLowerCase()) {
            case "electronics" -> "💻";
            case "vehicle"     -> "🚗";
            case "art"         -> "🎨";
            default            -> "📦";
        };
    }
    public String getCondition() {
        if (this.endTime == null) return "Stable";

        java.time.Duration remaining = java.time.Duration.between(java.time.LocalDateTime.now(), this.endTime);

        if (remaining.isNegative()) {
            return "Ended"; // Đã kết thúc
        } else if (remaining.toHours() < 1) {
            return "Ending Soon!"; // Sắp hết giờ (Cực nóng)
        } else {
            return "Good Standing"; // Tình trạng ổn định
        }
    }
    public long getBidCountByUser(Long userId) {
        if (userId == null) return 0;
        return bidHistory.stream()
                .filter(bid -> bid.getBidder().getId().equals(userId))
                .count();
    }
    public String getEndTimeFormatted() {
        if (this.endTime == null) return "N/A";

        // Định dạng: Ngày/Tháng Giờ:Phút (Ví dụ: 09/05 22:30)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");
        return this.endTime.format(formatter);
    }
    public String getStatusLabel() {
        if (this.status == null) return "Unknown";

        return switch (this.status) {
            case OPEN -> "Open for Bids";
            case RUNNING -> "Active Now";
            case CANCELLED -> "Cancelled";
            case FINISHED -> "Ended";
            case PAID -> "Sold & Paid";
            default -> "Processing";
        };
    }
    public String getStatusStyleClass() {
        if (this.status == null) return "badge-default";
        return switch (this.status) {
            case RUNNING -> "badge-success";  // Màu xanh lá
            case OPEN -> "badge-warning";     // Màu vàng/cam
            case FINISHED, PAID -> "badge-secondary"; // Màu xám
            case CANCELLED -> "badge-danger"; // Màu đỏ
            default -> "badge-default";
        };
    }
    public boolean isLive() {
        // Điều kiện 1: Trạng thái phải là RUNNING
        // Điều kiện 2: Thời gian hiện tại phải trước (nhỏ hơn) thời gian kết thúc
        return this.status == AuctionStatus.RUNNING
                && LocalDateTime.now().isBefore(this.endTime);
    }

    public boolean isEndingSoon() {
        if (this.endTime == null || this.status != AuctionStatus.RUNNING) {
            return false;
        }
        // Tính số phút còn lại
        long minutesRemaining = java.time.Duration.between(LocalDateTime.now(), this.endTime).toMinutes();

        // Ví dụ: Nếu còn dưới 60 phút thì coi là "sắp kết thúc"
        // Bạn có thể chỉnh con số này tùy ý (5, 10, hoặc 60 phút)
        return minutesRemaining >= 0 && minutesRemaining < 60;
    }

    public long getRemainingSeconds() {
        if (this.endTime == null) return 0;

        // Tính khoảng cách từ "bây giờ" đến "lúc kết thúc"
        java.time.ZonedDateTime nowZoned = java.time.ZonedDateTime.now(java.time.ZoneId.systemDefault());
        java.time.ZonedDateTime endZoned = endTime.atZone(java.time.ZoneId.systemDefault());

        java.time.Duration duration = java.time.Duration.between(nowZoned, endZoned);
        return duration.isNegative() ? 0 : duration.getSeconds();
    }
    public String getTimeRemaining() {
        long seconds = getRemainingSeconds(); // Hàm này bạn đã có rồi

        if (seconds <= 0) {
            return "00:00:00"; // Đã kết thúc
        }

        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;

        // Định dạng thành chuỗi HH:mm:ss (Ví dụ: 02:15:09)
        return String.format("%02d:%02d:%02d", h, m, s);
    }
    public boolean isUserWinning(Long userId) {
        return highestBidder != null && highestBidder.getId().equals(userId);
    }

    // Kiểm tra xem User hiện tại có phải người thắng cuộc khi kết thúc không
    public boolean isUserWon(Long userId) {
        return status == AuctionStatus.FINISHED && isUserWinning(userId);
    }

    // Lấy số tiền mà User hiện tại đã đặt (để hiển thị "Your bid: $...")
    public double getUserBidAmount(Long userId) {
        return bidHistory.stream()
                .filter(bid -> bid.getBidder().getId().equals(userId))
                .mapToDouble(BidTransaction::getAmount)
                .max()
                .orElse(0.0);
    }

    // Observer
    public void registerObserver(Observer observer) { observers.add(observer); }
    public void removeObserver(Observer observer) { observers.remove(observer); }
    public void notifyObservers() {
        for (Observer o : observers) o.update(this);
    }


}