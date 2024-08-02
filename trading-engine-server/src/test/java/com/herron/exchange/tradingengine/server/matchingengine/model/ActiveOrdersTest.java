package com.herron.exchange.tradingengine.server.matchingengine.model;

import com.herron.exchange.tradingengine.server.matchingengine.comparator.ProRataOrderBookComparator;
import com.herron.exchange.tradingengine.server.matchingengine.orderbook.ActiveOrders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.herron.exchange.common.api.common.enums.OrderSideEnum.ASK;
import static com.herron.exchange.common.api.common.enums.OrderSideEnum.BID;
import static com.herron.exchange.tradingengine.server.matchingengine.utils.MessageCreatorTestUtils.buildOrderAdd;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ActiveOrdersTest {
    private ActiveOrders activeOrders;

    @BeforeEach
    void init() {
        this.activeOrders = new ActiveOrders(new ProRataOrderBookComparator());
    }

    @Test
    void test_add_to_active_orders() {
        activeOrders.addOrder(buildOrderAdd(0, 100, 10, BID, "1"));
        activeOrders.addOrder(buildOrderAdd(0, 100, 10, BID, "2"));
        activeOrders.addOrder(buildOrderAdd(2, 101, 10, ASK, "3"));
        activeOrders.addOrder(buildOrderAdd(2, 101, 10, ASK, "4"));
        activeOrders.addOrder(buildOrderAdd(2, 101, 10, ASK, "5"));

        assertEquals(5, activeOrders.totalNumberOfActiveOrders());
        assertEquals(2, activeOrders.totalNumberOfBidOrders());
        assertEquals(3, activeOrders.totalNumberOfAskOrders());
    }

    @Test
    void test_update_active_order() {
        activeOrders.addOrder(buildOrderAdd(0, 100, 10, BID, "1"));
        assertEquals(1, activeOrders.totalNumberOfPriceLevels());
        assertEquals(1, activeOrders.totalNumberOfBidPriceLevels());
        assertEquals(0, activeOrders.totalNumberOfAskPriceLevels());
        assertEquals(10, activeOrders.getOrder("1").currentVolume().getRealValue());
        assertEquals(100, activeOrders.getOrder("1").price().getRealValue());

        activeOrders.updateOrder(buildOrderAdd(0, 99, 9, BID, "1"));
        assertEquals(1, activeOrders.totalNumberOfPriceLevels());
        assertEquals(1, activeOrders.totalNumberOfBidPriceLevels());
        assertEquals(0, activeOrders.totalNumberOfAskPriceLevels());
        assertEquals(9, activeOrders.getOrder("1").currentVolume().getRealValue());
        assertEquals(99, activeOrders.getOrder("1").price().getRealValue());
    }

    @Test
    void test_best_price_after_insert_active_orders() {
        activeOrders.addOrder(buildOrderAdd(0, 100, 10, BID, "1"));
        activeOrders.addOrder(buildOrderAdd(2, 102, 10, ASK, "2"));
        assertEquals(100, activeOrders.getBestBidPrice().get().getRealValue());
        assertEquals(102, activeOrders.getBestAskPrice().get().getRealValue());

        activeOrders.addOrder(buildOrderAdd(0, 101, 10, BID, "3"));
        activeOrders.addOrder(buildOrderAdd(2, 101, 10, ASK, "3"));
        assertEquals(101, activeOrders.getBestBidPrice().get().getRealValue());
        assertEquals(101, activeOrders.getBestAskPrice().get().getRealValue());
    }

    @Test
    void test_remove_to_active_orders() {
        activeOrders.addOrder(buildOrderAdd(0, 100, 10, BID, "1"));
        activeOrders.addOrder(buildOrderAdd(0, 100, 10, BID, "2"));
        activeOrders.addOrder(buildOrderAdd(2, 101, 10, ASK, "3"));
        activeOrders.addOrder(buildOrderAdd(2, 102, 10, ASK, "4"));

        assertEquals(4, activeOrders.totalNumberOfActiveOrders());
        assertEquals(2, activeOrders.totalNumberOfBidOrders());
        assertEquals(2, activeOrders.totalNumberOfAskOrders());

        activeOrders.removeOrder("2");
        activeOrders.removeOrder("3");
        activeOrders.removeOrder("4");
        assertEquals(1, activeOrders.totalNumberOfActiveOrders());
        assertEquals(1, activeOrders.totalNumberOfBidOrders());
        assertEquals(0, activeOrders.totalNumberOfAskOrders());
    }

    @Test
    void test_best_price_after_remove_active_orders() {
        activeOrders.addOrder(buildOrderAdd(0, 100, 10, BID, "1"));
        activeOrders.addOrder(buildOrderAdd(2, 102, 10, ASK, "2"));
        activeOrders.addOrder(buildOrderAdd(0, 99, 10, BID, "3"));
        activeOrders.addOrder(buildOrderAdd(2, 103, 10, ASK, "4"));
        assertEquals(100, activeOrders.getBestBidPrice().get().getRealValue());
        assertEquals(102, activeOrders.getBestAskPrice().get().getRealValue());

        activeOrders.removeOrder("1");
        activeOrders.removeOrder("2");
        assertEquals(99, activeOrders.getBestBidPrice().get().getRealValue());
        assertEquals(103, activeOrders.getBestAskPrice().get().getRealValue());
    }

    @Test
    void test_best_price_after_remove_active_orders_same_time_stamp() {
        activeOrders.addOrder(buildOrderAdd(0, 100, 10, BID, "1"));
        activeOrders.addOrder(buildOrderAdd(0, 100, 10, BID, "2"));
        activeOrders.addOrder(buildOrderAdd(2, 100, 10, BID, "3"));

        activeOrders.removeOrder("1");
        assertEquals(20, activeOrders.totalBidVolume().getRealValue());
    }

    @Test
    void test_volume() {
        activeOrders.addOrder(buildOrderAdd(0, 100, 11, BID, "1"));
        activeOrders.addOrder(buildOrderAdd(2, 102, 13, ASK, "2"));
        activeOrders.addOrder(buildOrderAdd(0, 99, 12, BID, "3"));
        activeOrders.addOrder(buildOrderAdd(2, 103, 10, ASK, "4"));
        assertEquals(46, activeOrders.totalOrderVolume().getRealValue());
        assertEquals(23, activeOrders.totalBidVolume().getRealValue());
        assertEquals(23, activeOrders.totalAskVolume().getRealValue());

        activeOrders.removeOrder("1");
        activeOrders.removeOrder("2");
        assertEquals(22, activeOrders.totalOrderVolume().getRealValue());
        assertEquals(12, activeOrders.totalBidVolume().getRealValue());
        assertEquals(10, activeOrders.totalAskVolume().getRealValue());
    }

    @Test
    void test_volume_at_level() {
        activeOrders.addOrder(buildOrderAdd(0, 100, 11, BID, "1"));
        activeOrders.addOrder(buildOrderAdd(2, 102, 13, ASK, "2"));
        activeOrders.addOrder(buildOrderAdd(0, 99, 12, BID, "3"));
        activeOrders.addOrder(buildOrderAdd(2, 103, 10, ASK, "4"));
        activeOrders.addOrder(buildOrderAdd(0, 100, 10, BID, "5"));
        assertEquals(34, activeOrders.totalVolumeAtPriceLevel(1).getRealValue());
        assertEquals(21, activeOrders.totalBidVolumeAtPriceLevel(1).getRealValue());
        assertEquals(13, activeOrders.totalAskVolumeAtPriceLevel(1).getRealValue());
        assertEquals(22, activeOrders.totalVolumeAtPriceLevel(2).getRealValue());
        assertEquals(12, activeOrders.totalBidVolumeAtPriceLevel(2).getRealValue());
        assertEquals(10, activeOrders.totalAskVolumeAtPriceLevel(2).getRealValue());

        activeOrders.removeOrder("1");
        activeOrders.removeOrder("2");
        assertEquals(20, activeOrders.totalVolumeAtPriceLevel(1).getRealValue());
        assertEquals(10, activeOrders.totalBidVolumeAtPriceLevel(1).getRealValue());
        assertEquals(10, activeOrders.totalAskVolumeAtPriceLevel(1).getRealValue());
        assertEquals(12, activeOrders.totalVolumeAtPriceLevel(2).getRealValue());
        assertEquals(12, activeOrders.totalBidVolumeAtPriceLevel(2).getRealValue());
        assertEquals(0, activeOrders.totalAskVolumeAtPriceLevel(2).getRealValue());
    }
}