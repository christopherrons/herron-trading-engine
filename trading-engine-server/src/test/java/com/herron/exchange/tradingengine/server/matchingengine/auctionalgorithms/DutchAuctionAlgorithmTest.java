package com.herron.exchange.tradingengine.server.matchingengine.auctionalgorithms;

import com.herron.exchange.common.api.common.enums.OrderSideEnum;
import com.herron.exchange.tradingengine.server.matchingengine.comparator.FifoOrderBookComparator;
import com.herron.exchange.tradingengine.server.matchingengine.orderbook.ActiveOrders;
import com.herron.exchange.tradingengine.server.matchingengine.utils.MessageCreatorTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.herron.exchange.tradingengine.server.matchingengine.utils.MessageCreatorTestUtils.buildOrderAdd;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DutchAuctionAlgorithmTest {

    private ActiveOrders activeOrders;
    private DutchAuctionAlgorithm auctionAlgorithm;

    @BeforeEach
    void init() {
        this.activeOrders = new ActiveOrders(new FifoOrderBookComparator());
        this.auctionAlgorithm = new DutchAuctionAlgorithm(activeOrders);
    }

    @Test
    void test_equilibrium_price() {
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(899, 32.00, 2, OrderSideEnum.BID, "1"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(900, 32.00, 1, OrderSideEnum.BID, "2"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(911, 32.00, 8, OrderSideEnum.BID, "3"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(902, 31.90, 6, OrderSideEnum.BID, "4"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(910, 31.90, 3, OrderSideEnum.BID, "5"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(914, 31.90, 2, OrderSideEnum.BID, "6"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(913, 31.80, 2, OrderSideEnum.BID, "7"));

        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(901, 31.90, 2, OrderSideEnum.ASK, "8"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(910, 31.90, 8, OrderSideEnum.ASK, "9"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(905, 32.00, 10, OrderSideEnum.ASK, "10"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(913, 32.00, 4, OrderSideEnum.ASK, "11"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(914, 32.00, 2, OrderSideEnum.ASK, "12"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(912, 32.10, 6, OrderSideEnum.ASK, "13"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(913, 32.10, 2, OrderSideEnum.ASK, "14"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(901, 32.20, 4, OrderSideEnum.ASK, "15"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(908, 32.20, 2, OrderSideEnum.ASK, "16"));
        activeOrders.addOrder(MessageCreatorTestUtils.buildOrderAdd(912, 32.20, 1, OrderSideEnum.ASK, "17"));

        var result = auctionAlgorithm.calculateEquilibriumPrice();
        assertEquals(11, result.optimalPrice().matchedVolume().getValue());
        assertEquals(32.00, result.optimalPrice().equilibriumPrice().getValue());
    }

}