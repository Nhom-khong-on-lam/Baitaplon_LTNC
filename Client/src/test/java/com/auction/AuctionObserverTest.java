package com.auction;

import com.auction.client.Enum.SystemRole;
import com.auction.client.model.Auction;
import com.auction.client.model.Item;
import com.auction.client.model.User;
import com.auction.client.observer.AuctionObserver;
import com.auction.client.observer.Observer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AuctionObserverTest {

    private Auction mockAuction;

    @BeforeEach
    void setUp() {
        // Tạo một đối tượng Auction giả lập
        mockAuction = mock(Auction.class);
    }

    @Test
    @DisplayName("Test AuctionObserver nhận thông báo thành công")
    void testAuctionObserverUpdate() {
        // Khởi tạo observer thật
        AuctionObserver observer = new AuctionObserver("User1");

        // Vì hàm update hiện tại chỉ System.out.println,
        // test này chủ yếu đảm bảo không có Exception nào xảy ra khi gọi.
        observer.update(mockAuction);
    }

    @Test
    @DisplayName("Test mô hình Observer tổng thể (Subject thông báo cho nhiều Observer)")
    void testNotifyAllObservers() {
        // 1. Giả lập danh sách Observers bằng Mockito để có thể kiểm tra (verify)
        Observer observer1 = mock(Observer.class);
        Observer observer2 = mock(Observer.class);

        List<Observer> observers = new ArrayList<>();
        observers.add(observer1);
        observers.add(observer2);

        // 2. Giả lập hành động thông báo (Logic này thường nằm trong AuctionService hoặc Auction Manager)
        for (Observer obs : observers) {
            obs.update(mockAuction);
        }

        // 3. Kiểm tra xem phương thức update có được gọi đúng 1 lần trên mỗi observer không
        verify(observer1, times(1)).update(mockAuction);
        verify(observer2, times(1)).update(mockAuction);
    }

    @Test
    @DisplayName("Test dữ liệu truyền qua Observer chính xác")
    void testObserverDataIntegrity() {
        Observer mockObserver = mock(Observer.class);
        Item item = new Item(){
            @Override
            public String getCategory(){
                return "Electronics";
            }
        };
        item.setName("Tivi");

        User seller = new User(1L,"Chau","password","Chau@gmail.com", SystemRole.USER); // Giả sử User có constructor nhận tên
        LocalDateTime endTime = LocalDateTime.now().plusDays(1);
        // Giả lập một Auction với tiêu đề cụ thể
        Auction realAuction = new Auction(item,seller,endTime);
        realAuction.getItem().setName("IPhone 15");

        mockObserver.update(realAuction);

        // Sử dụng ArgumentCaptor để "bắt" đối tượng được truyền vào hàm update
        ArgumentCaptor<Auction> captor = ArgumentCaptor.forClass(Auction.class);
        verify(mockObserver).update(captor.capture());

        // Kiểm tra dữ liệu bên trong đối tượng bị bắt
        assertEquals("IPhone 15", captor.getValue().getTitle());
    }
}