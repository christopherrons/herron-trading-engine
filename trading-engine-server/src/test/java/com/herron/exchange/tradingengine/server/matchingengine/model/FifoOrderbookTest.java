package com.herron.exchange.tradingengine.server.matchingengine.model;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.api.Order;
import com.herron.exchange.common.api.common.api.Trade;
import com.herron.exchange.common.api.common.enums.MatchingAlgorithmEnum;
import com.herron.exchange.common.api.common.enums.OrderSideEnum;
import com.herron.exchange.common.api.common.messages.herron.HerronOrderbookData;
import com.herron.exchange.common.api.common.model.Member;
import com.herron.exchange.common.api.common.model.Participant;
import com.herron.exchange.common.api.common.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.herron.exchange.tradingengine.server.matchingengine.utils.EventCreatorUtils.buildOrderCreate;
import static org.junit.jupiter.api.Assertions.*;

class FifoOrderbookTest {
    private FifoOrderbook fifoOrderBook;

    @BeforeEach
    void init() {
        this.fifoOrderBook = new FifoOrderbook(new HerronOrderbookData("orderbookId", "instrumentId", MatchingAlgorithmEnum.FIFO, 0));
    }

    @Test
    void testAddToOrderBook() {
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "1"));
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "2"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 101, 10, OrderSideEnum.ASK, "3"));

        assertEquals(3, fifoOrderBook.totalNumberOfActiveOrders());
        assertEquals(2, fifoOrderBook.totalNumberOfBidOrders());
        assertEquals(1, fifoOrderBook.totalNumberOfAskOrders());
    }

    @Test
    void testUpdateOrderBook() {
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "1"));
        assertEquals(1, fifoOrderBook.totalNumberOfPriceLevels());
        assertEquals(1, fifoOrderBook.totalNumberOfBidPriceLevels());
        assertEquals(0, fifoOrderBook.totalNumberOfAskPriceLevels());
        assertEquals(10, fifoOrderBook.getOrder("1").currentVolume());
        assertEquals(100, fifoOrderBook.getOrder("1").price());

        fifoOrderBook.updateOrder(buildOrderCreate(0, 99, 9, OrderSideEnum.BID, "1"));
        assertEquals(1, fifoOrderBook.totalNumberOfPriceLevels());
        assertEquals(1, fifoOrderBook.totalNumberOfBidPriceLevels());
        assertEquals(0, fifoOrderBook.totalNumberOfAskPriceLevels());
        assertEquals(9, fifoOrderBook.getOrder("1").currentVolume());
        assertEquals(99, fifoOrderBook.getOrder("1").price());
    }

    @Test
    void testBestPriceAfterInsertOrders() {
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "1"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 102, 10, OrderSideEnum.ASK, "2"));
        assertEquals(100, fifoOrderBook.getBestBidPrice());
        assertEquals(102, fifoOrderBook.getBestAskPrice());

        fifoOrderBook.addOrder(buildOrderCreate(0, 101, 10, OrderSideEnum.BID, "3"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 101, 10, OrderSideEnum.ASK, "3"));
        assertEquals(101, fifoOrderBook.getBestBidPrice());
        assertEquals(101, fifoOrderBook.getBestAskPrice());
    }

    @Test
    void testRemoveFromOrderBook() {
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "1"));
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "2"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 101, 10, OrderSideEnum.ASK, "3"));

        assertEquals(3, fifoOrderBook.totalNumberOfActiveOrders());
        assertEquals(2, fifoOrderBook.totalNumberOfBidOrders());
        assertEquals(1, fifoOrderBook.totalNumberOfAskOrders());

        fifoOrderBook.removeOrder("2");
        fifoOrderBook.removeOrder("3");
        assertEquals(1, fifoOrderBook.totalNumberOfActiveOrders());
        assertEquals(1, fifoOrderBook.totalNumberOfBidOrders());
        assertEquals(0, fifoOrderBook.totalNumberOfAskOrders());
    }

    @Test
    void testBestPriceAfterRemoveOrder() {
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "1"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 102, 10, OrderSideEnum.ASK, "2"));
        fifoOrderBook.addOrder(buildOrderCreate(0, 99, 10, OrderSideEnum.BID, "3"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 103, 10, OrderSideEnum.ASK, "4"));
        assertEquals(100, fifoOrderBook.getBestBidPrice());
        assertEquals(102, fifoOrderBook.getBestAskPrice());

        fifoOrderBook.removeOrder("1");
        fifoOrderBook.removeOrder("2");
        assertEquals(99, fifoOrderBook.getBestBidPrice());
        assertEquals(103, fifoOrderBook.getBestAskPrice());
    }

    @Test
    void testVolume() {
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 11, OrderSideEnum.BID, "1"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 102, 13, OrderSideEnum.ASK, "2"));
        fifoOrderBook.addOrder(buildOrderCreate(0, 99, 12, OrderSideEnum.BID, "3"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 103, 10, OrderSideEnum.ASK, "4"));
        assertEquals(46, fifoOrderBook.totalOrderVolume());
        assertEquals(23, fifoOrderBook.totalBidVolume());
        assertEquals(23, fifoOrderBook.totalAskVolume());


        fifoOrderBook.removeOrder("1");
        fifoOrderBook.removeOrder("2");
        assertEquals(22, fifoOrderBook.totalOrderVolume());
        assertEquals(12, fifoOrderBook.totalBidVolume());
        assertEquals(10, fifoOrderBook.totalAskVolume());
    }

    @Test
    void testVolumeAtLevel() {
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 11, OrderSideEnum.BID, "1"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 102, 13, OrderSideEnum.ASK, "2"));
        fifoOrderBook.addOrder(buildOrderCreate(0, 99, 12, OrderSideEnum.BID, "3"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 103, 10, OrderSideEnum.ASK, "4"));
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "5"));
        assertEquals(34, fifoOrderBook.totalVolumeAtPriceLevel(1));
        assertEquals(21, fifoOrderBook.totalBidVolumeAtPriceLevel(1));
        assertEquals(13, fifoOrderBook.totalAskVolumeAtPriceLevel(1));
        assertEquals(22, fifoOrderBook.totalVolumeAtPriceLevel(2));
        assertEquals(12, fifoOrderBook.totalBidVolumeAtPriceLevel(2));
        assertEquals(10, fifoOrderBook.totalAskVolumeAtPriceLevel(2));


        fifoOrderBook.removeOrder("1");
        fifoOrderBook.removeOrder("2");
        assertEquals(20, fifoOrderBook.totalVolumeAtPriceLevel(1));
        assertEquals(10, fifoOrderBook.totalBidVolumeAtPriceLevel(1));
        assertEquals(10, fifoOrderBook.totalAskVolumeAtPriceLevel(1));
        assertEquals(12, fifoOrderBook.totalVolumeAtPriceLevel(2));
        assertEquals(12, fifoOrderBook.totalBidVolumeAtPriceLevel(2));
        assertEquals(0, fifoOrderBook.totalAskVolumeAtPriceLevel(2));
    }

    @Test
    void testMatchingAlgorithmSamePriceLevel() {
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "1"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 100, 15, OrderSideEnum.ASK, "2"));

        List<Message> matchingMessages = fifoOrderBook.runMatchingAlgorithmNonActiveOrder();
        addMessages(matchingMessages);
        Trade trade = matchingMessages.stream().filter(m -> m instanceof Trade).map(t -> (Trade) t).findFirst().get();
        assertEquals(5, fifoOrderBook.totalOrderVolume());
        assertNotEquals(0, trade.tradeId());
        assertFalse(trade.isBidSideAggressor());

        fifoOrderBook.addOrder(buildOrderCreate(3, 100, 6, OrderSideEnum.BID, "3"));

        matchingMessages = fifoOrderBook.runMatchingAlgorithmNonActiveOrder();
        addMessages(matchingMessages);
        trade = matchingMessages.stream().filter(m -> m instanceof Trade).map(t -> (Trade) t).findFirst().get();
        assertEquals(1, fifoOrderBook.totalOrderVolume());
        assertNotEquals(0, trade.tradeId());
        assertTrue(trade.isBidSideAggressor());
    }

    @Test
    void testMatchingAlgorithmSelfMatch() {
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "1", new Participant(new Member("member"), new User("user"))));
        fifoOrderBook.addOrder(buildOrderCreate(2, 100, 15, OrderSideEnum.ASK, "2", new Participant(new Member("member"), new User("user"))));

        List<Message> matchingMessages = fifoOrderBook.runMatchingAlgorithmNonActiveOrder();
        addMessages(matchingMessages);
        Optional<Trade> trade = matchingMessages.stream().filter(m -> m instanceof Trade).map(t -> (Trade) t).findFirst();
        assertFalse(trade.isPresent());
    }

    private void addMessages(List<Message> messages) {
        for (var message : messages) {
            if (message instanceof Order order) {
                switch (order.orderOperation()) {
                    case CREATE -> fifoOrderBook.addOrder(order);
                    case UPDATE -> fifoOrderBook.updateOrder(order);
                    case DELETE -> fifoOrderBook.removeOrder(order);
                    default -> {
                    }
                }
            }
        }

    }
}