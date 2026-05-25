

import com.auction.common.model.Auction;
import com.auction.common.model.DashboardData;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class DashboardDataTest {

    @Test
    void testDashboardListsData() {
        List<Auction> all = new ArrayList<>();
        List<Auction> bids = new ArrayList<>();
        List<Auction> winning = new ArrayList<>();
        List<Auction> products = new ArrayList<>();
        List<Auction> live = new ArrayList<>();

        // Test constructor và cơ chế Get/Set dữ liệu danh sách
        DashboardData data = new DashboardData(all, bids, winning, products, live);

        assertNotNull(data.getAllAuctions());
        assertNotNull(data.getMyBids());
        assertNotNull(data.getMyWinning());
        assertNotNull(data.getMyProducts());
        assertNotNull(data.getLiveAuctions());

        // Test setter lẻ
        List<Auction> newLiveList = new ArrayList<>();
        data.setLiveAuctions(newLiveList);
        assertEquals(newLiveList, data.getLiveAuctions());
    }
}