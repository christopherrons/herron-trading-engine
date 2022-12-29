package com.herron.exchange.tradingengine.server.matchingengine.model;

import com.herron.exchange.common.api.common.enums.OrderTypeEnum;
import com.herron.exchange.tradingengine.server.matchingengine.comparator.FifoOrderBookComparator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.herron.exchange.tradingengine.server.matchingengine.utils.EventCreatorUtils.buildOrderCreate;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ActiveOrdersTest {
    private ActiveOrders activeOrders;

    @BeforeEach
    void init() {
        this.activeOrders = new ActiveOrders(new FifoOrderBookComparator());
    }

    @Test
    void testAddToActiveOrders() {
        activeOrders.addOrder(buildOrderCreate(0, 100, 10, OrderTypeEnum.BUY, "1"));
        activeOrders.addOrder(buildOrderCreate(0, 100, 10, OrderTypeEnum.BUY, "2"));
        activeOrders.addOrder(buildOrderCreate(2, 101, 10, OrderTypeEnum.ASK, "3"));
        activeOrders.addOrder(buildOrderCreate(2, 101, 10, OrderTypeEnum.ASK, "4"));
        activeOrders.addOrder(buildOrderCreate(2, 101, 10, OrderTypeEnum.ASK, "5"));

        assertEquals(5, activeOrders.totalNumberOfActiveOrders());
        assertEquals(2, activeOrders.totalNumberOfBidOrders());
        assertEquals(3, activeOrders.totalNumberOfAskOrders());
    }

    @Test
    void testUpdateActiveOrder() {
        activeOrders.addOrder(buildOrderCreate(0, 100, 10, OrderTypeEnum.BUY, "1"));
        assertEquals(1, activeOrders.totalNumberOfPriceLevels());
        assertEquals(1, activeOrders.totalNumberOfBidPriceLevels());
        assertEquals(0, activeOrders.totalNumberOfAskPriceLevels());
        assertEquals(10, activeOrders.getOrder("1").currentVolume());
        assertEquals(100, activeOrders.getOrder("1").price());

        activeOrders.updateOrder(buildOrderCreate(0, 99, 9, OrderTypeEnum.BUY, "1"));
        assertEquals(1, activeOrders.totalNumberOfPriceLevels());
        assertEquals(1, activeOrders.totalNumberOfBidPriceLevels());
        assertEquals(0, activeOrders.totalNumberOfAskPriceLevels());
        assertEquals(9, activeOrders.getOrder("1").currentVolume());
        assertEquals(99, activeOrders.getOrder("1").price());
    }

    @Test
    void testBestPriceAfterInsertActiveOrders() {
        activeOrders.addOrder(buildOrderCreate(0, 100, 10, OrderTypeEnum.BUY, "1"));
        activeOrders.addOrder(buildOrderCreate(2, 102, 10, OrderTypeEnum.ASK, "2"));
        assertEquals(100, activeOrders.getBestBidPrice());
        assertEquals(102, activeOrders.getBestAskPrice());

        activeOrders.addOrder(buildOrderCreate(0, 101, 10, OrderTypeEnum.BUY, "3"));
        activeOrders.addOrder(buildOrderCreate(2, 101, 10, OrderTypeEnum.ASK, "3"));
        assertEquals(101, activeOrders.getBestBidPrice());
        assertEquals(101, activeOrders.getBestAskPrice());
    }

    @Test
    void testRemoveToActiveOrders() {
        activeOrders.addOrder(buildOrderCreate(0, 100, 10, OrderTypeEnum.BUY, "1"));
        activeOrders.addOrder(buildOrderCreate(0, 100, 10, OrderTypeEnum.BUY, "2"));
        activeOrders.addOrder(buildOrderCreate(2, 101, 10, OrderTypeEnum.ASK, "3"));
        activeOrders.addOrder(buildOrderCreate(2, 102, 10, OrderTypeEnum.ASK, "4"));

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
    void testBestPriceAfterRemoveActiveOrders() {
        activeOrders.addOrder(buildOrderCreate(0, 100, 10, OrderTypeEnum.BUY, "1"));
        activeOrders.addOrder(buildOrderCreate(2, 102, 10, OrderTypeEnum.ASK, "2"));
        activeOrders.addOrder(buildOrderCreate(0, 99, 10, OrderTypeEnum.BUY, "3"));
        activeOrders.addOrder(buildOrderCreate(2, 103, 10, OrderTypeEnum.ASK, "4"));
        assertEquals(100, activeOrders.getBestBidPrice());
        assertEquals(102, activeOrders.getBestAskPrice());

        activeOrders.removeOrder("1");
        activeOrders.removeOrder("2");
        assertEquals(99, activeOrders.getBestBidPrice());
        assertEquals(103, activeOrders.getBestAskPrice());
    }

    @Test
    void testVolume() {
        activeOrders.addOrder(buildOrderCreate(0, 100, 11, OrderTypeEnum.BUY, "1"));
        activeOrders.addOrder(buildOrderCreate(2, 102, 13, OrderTypeEnum.ASK, "2"));
        activeOrders.addOrder(buildOrderCreate(0, 99, 12, OrderTypeEnum.BUY, "3"));
        activeOrders.addOrder(buildOrderCreate(2, 103, 10, OrderTypeEnum.ASK, "4"));
        assertEquals(46, activeOrders.totalOrderVolume());
        assertEquals(23, activeOrders.totalBidVolume());
        assertEquals(23, activeOrders.totalAskVolume());


        activeOrders.removeOrder("1");
        activeOrders.removeOrder("2");
        assertEquals(22, activeOrders.totalOrderVolume());
        assertEquals(12, activeOrders.totalBidVolume());
        assertEquals(10, activeOrders.totalAskVolume());
    }

    @Test
    void testVolumeAtLevel() {
        activeOrders.addOrder(buildOrderCreate(0, 100, 11, OrderTypeEnum.BUY, "1"));
        activeOrders.addOrder(buildOrderCreate(2, 102, 13, OrderTypeEnum.ASK, "2"));
        activeOrders.addOrder(buildOrderCreate(0, 99, 12, OrderTypeEnum.BUY, "3"));
        activeOrders.addOrder(buildOrderCreate(2, 103, 10, OrderTypeEnum.ASK, "4"));
        activeOrders.addOrder(buildOrderCreate(0, 100, 10, OrderTypeEnum.BUY, "5"));
        assertEquals(34, activeOrders.totalVolumeAtPriceLevel(1));
        assertEquals(21, activeOrders.totalBidVolumeAtPriceLevel(1));
        assertEquals(13, activeOrders.totalAskVolumeAtPriceLevel(1));
        assertEquals(22, activeOrders.totalVolumeAtPriceLevel(2));
        assertEquals(12, activeOrders.totalBidVolumeAtPriceLevel(2));
        assertEquals(10, activeOrders.totalAskVolumeAtPriceLevel(2));


        activeOrders.removeOrder("1");
        activeOrders.removeOrder("2");
        assertEquals(20, activeOrders.totalVolumeAtPriceLevel(1));
        assertEquals(10, activeOrders.totalBidVolumeAtPriceLevel(1));
        assertEquals(10, activeOrders.totalAskVolumeAtPriceLevel(1));
        assertEquals(12, activeOrders.totalVolumeAtPriceLevel(2));
        assertEquals(12, activeOrders.totalBidVolumeAtPriceLevel(2));
        assertEquals(0, activeOrders.totalAskVolumeAtPriceLevel(2));
    }
}