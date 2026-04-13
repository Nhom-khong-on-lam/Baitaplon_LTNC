package observer;

import model.Auction;

public interface Observer {
    void update(Auction auction);
}
