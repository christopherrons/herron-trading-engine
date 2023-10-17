package com.herron.exchange.tradingengine.server.matchingengine.model;

import com.herron.exchange.common.api.common.enums.OrderSideEnum;
import com.herron.exchange.tradingengine.server.matchingengine.comparator.FifoOrderBookComparator;
import com.herron.exchange.tradingengine.server.matchingengine.orderbook.ActiveOrders;
import com.herron.exchange.tradingengine.server.matchingengine.utils.MessageCreatorTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.herron.exchange.tradingengine.server.matchingengine.utils.MessageCreatorTestUtils.buildOrderAdd;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ActiveOrdersTest {
    private ActiveOrders activeOrders;

    @BeforeEach
    void init() {
        this.activeOrders = new ActiveOrders(new FifoOrderBookComparator());
    }

    @Test
    void test_add_to_active_orders() {
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "1"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "2"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(2, 101, 10, OrderSideEnum.ASK, "3"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(2, 101, 10, OrderSideEnum.ASK, "4"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(2, 101, 10, OrderSideEnum.ASK, "5"));

        assertEquals(5, activeOrders.totalNumberOfActiveOrders());
        assertEquals(2, activeOrders.totalNumberOfBidOrders());
        assertEquals(3, activeOrders.totalNumberOfAskOrders());
    }

    @Test
    void test_update_active_order() {
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "1"));
        assertEquals(1, activeOrders.totalNumberOfPriceLevels());
        assertEquals(1, activeOrders.totalNumberOfBidPriceLevels());
        assertEquals(0, activeOrders.totalNumberOfAskPriceLevels());
        assertEquals(10, activeOrders.getOrder("1").currentVolume().getValue());
        assertEquals(100, activeOrders.getOrder("1").price().getValue());

        activeOrders.updateOrder(MessageCreatorTestUtils.buildOrderAdd(0, 99, 9, OrderSideEnum.BID, "1"));
        assertEquals(1, activeOrders.totalNumberOfPriceLevels());
        assertEquals(1, activeOrders.totalNumberOfBidPriceLevels());
        assertEquals(0, activeOrders.totalNumberOfAskPriceLevels());
        assertEquals(9, activeOrders.getOrder("1").currentVolume().getValue());
        assertEquals(99, activeOrders.getOrder("1").price().getValue());
    }

    @Test
    void test_best_price_after_insert_active_orders() {
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "1"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(2, 102, 10, OrderSideEnum.ASK, "2"));
        assertEquals(100, activeOrders.getBestBidPrice().getValue());
        assertEquals(102, activeOrders.getBestAskPrice().getValue());

        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(0, 101, 10, OrderSideEnum.BID, "3"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(2, 101, 10, OrderSideEnum.ASK, "3"));
        assertEquals(101, activeOrders.getBestBidPrice().getValue());
        assertEquals(101, activeOrders.getBestAskPrice().getValue());
    }

    @Test
    void test_remove_to_active_orders() {
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "1"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "2"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(2, 101, 10, OrderSideEnum.ASK, "3"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(2, 102, 10, OrderSideEnum.ASK, "4"));

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
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "1"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(2, 102, 10, OrderSideEnum.ASK, "2"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(0, 99, 10, OrderSideEnum.BID, "3"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(2, 103, 10, OrderSideEnum.ASK, "4"));
        assertEquals(100, activeOrders.getBestBidPrice().getValue());
        assertEquals(102, activeOrders.getBestAskPrice().getValue());

        activeOrders.removeOrder("1");
        activeOrders.removeOrder("2");
        assertEquals(99, activeOrders.getBestBidPrice().getValue());
        assertEquals(103, activeOrders.getBestAskPrice().getValue());
    }

    @Test
    void test_volume() {
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(0, 100, 11, OrderSideEnum.BID, "1"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(2, 102, 13, OrderSideEnum.ASK, "2"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(0, 99, 12, OrderSideEnum.BID, "3"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(2, 103, 10, OrderSideEnum.ASK, "4"));
        assertEquals(46, activeOrders.totalOrderVolume().getValue());
        assertEquals(23, activeOrders.totalBidVolume().getValue());
        assertEquals(23, activeOrders.totalAskVolume().getValue());

        activeOrders.removeOrder("1");
        activeOrders.removeOrder("2");
        assertEquals(22, activeOrders.totalOrderVolume().getValue());
        assertEquals(12, activeOrders.totalBidVolume().getValue());
        assertEquals(10, activeOrders.totalAskVolume().getValue());
    }

    @Test
    void test_volume_at_level() {
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(0, 100, 11, OrderSideEnum.BID, "1"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(2, 102, 13, OrderSideEnum.ASK, "2"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(0, 99, 12, OrderSideEnum.BID, "3"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(2, 103, 10, OrderSideEnum.ASK, "4"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "5"));
        assertEquals(34, activeOrders.totalVolumeAtPriceLevel(1).getValue());
        assertEquals(21, activeOrders.totalBidVolumeAtPriceLevel(1).getValue());
        assertEquals(13, activeOrders.totalAskVolumeAtPriceLevel(1).getValue());
        assertEquals(22, activeOrders.totalVolumeAtPriceLevel(2).getValue());
        assertEquals(12, activeOrders.totalBidVolumeAtPriceLevel(2).getValue());
        assertEquals(10, activeOrders.totalAskVolumeAtPriceLevel(2).getValue());

        activeOrders.removeOrder("1");
        activeOrders.removeOrder("2");
        assertEquals(20, activeOrders.totalVolumeAtPriceLevel(1).getValue());
        assertEquals(10, activeOrders.totalBidVolumeAtPriceLevel(1).getValue());
        assertEquals(10, activeOrders.totalAskVolumeAtPriceLevel(1).getValue());
        assertEquals(12, activeOrders.totalVolumeAtPriceLevel(2).getValue());
        assertEquals(12, activeOrders.totalBidVolumeAtPriceLevel(2).getValue());
        assertEquals(0, activeOrders.totalAskVolumeAtPriceLevel(2).getValue());
    }
}