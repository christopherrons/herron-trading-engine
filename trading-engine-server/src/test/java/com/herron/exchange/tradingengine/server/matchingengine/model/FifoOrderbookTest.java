package com.herron.exchange.tradingengine.server.matchingengine.model;

import com.herron.exchange.common.api.common.api.*;
import com.herron.exchange.common.api.common.enums.*;
import com.herron.exchange.common.api.common.messages.herron.HerronOrderbookData;
import com.herron.exchange.common.api.common.model.Member;
import com.herron.exchange.common.api.common.model.Participant;
import com.herron.exchange.common.api.common.model.User;
import com.herron.exchange.tradingengine.server.matchingengine.orderbook.FifoOrderbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.herron.exchange.tradingengine.server.matchingengine.utils.EventCreatorTestUtils.buildOrder;
import static com.herron.exchange.tradingengine.server.matchingengine.utils.EventCreatorTestUtils.buildOrderCreate;
import static org.junit.jupiter.api.Assertions.*;

class FifoOrderbookTest {
    private FifoOrderbook fifoOrderBook;

    @BeforeEach
    void init() {
        this.fifoOrderBook = new FifoOrderbook(new HerronOrderbookData("orderbookId", "instrumentId", MatchingAlgorithmEnum.FIFO, 0));
    }

    @Test
    void test_add_to_orderbook() {
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "1"));
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "2"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 101, 10, OrderSideEnum.ASK, "3"));

        assertEquals(3, fifoOrderBook.totalNumberOfActiveOrders());
        assertEquals(2, fifoOrderBook.totalNumberOfBidOrders());
        assertEquals(1, fifoOrderBook.totalNumberOfAskOrders());
    }

    @Test
    void test_update_orderbook() {
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
    void test_best_price_after_insert_orders() {
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
    void test_remove_from_orderbook() {
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
    void test_best_price_after_remove_order() {
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
    void test_volume() {
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
    void test_volume_at_level() {
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
    void test_matching_algorithm_same_price_Level() {
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "1"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 100, 15, OrderSideEnum.ASK, "2"));

        List<Message> matchingMessages = fifoOrderBook.runMatchingAlgorithm();
        addMessages(matchingMessages);
        Trade trade = matchingMessages.stream().filter(m -> m instanceof Trade).map(t -> (Trade) t).findFirst().get();
        assertEquals(5, fifoOrderBook.totalOrderVolume());
        assertNotEquals(0, trade.tradeId());
        assertFalse(trade.isBidSideAggressor());

        fifoOrderBook.addOrder(buildOrderCreate(3, 100, 6, OrderSideEnum.BID, "3"));

        matchingMessages = fifoOrderBook.runMatchingAlgorithm();
        addMessages(matchingMessages);
        trade = matchingMessages.stream().filter(m -> m instanceof Trade).map(t -> (Trade) t).findFirst().get();
        assertEquals(1, fifoOrderBook.totalOrderVolume());
        assertNotEquals("0", trade.tradeId());
        assertTrue(trade.isBidSideAggressor());
    }

    @Test
    void test_matching_algorithm_self_match() {
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "1", new Participant(new Member("member"), new User("user"))));
        fifoOrderBook.addOrder(buildOrderCreate(2, 100, 15, OrderSideEnum.ASK, "2", new Participant(new Member("member"), new User("user"))));

        List<Message> matchingMessages = fifoOrderBook.runMatchingAlgorithm();
        addMessages(matchingMessages);
        Optional<Trade> trade = matchingMessages.stream().filter(m -> m instanceof Trade).map(t -> (Trade) t).findFirst();
        assertFalse(trade.isPresent());
    }

    @Test
    void test_add_non_active_order_to_orderbook() {
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "1"));
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "2"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 101, 10, OrderSideEnum.ASK, "3"));

        assertEquals(3, fifoOrderBook.totalNumberOfActiveOrders());
        assertEquals(2, fifoOrderBook.totalNumberOfBidOrders());
        assertEquals(1, fifoOrderBook.totalNumberOfAskOrders());

        fifoOrderBook.addOrder(buildOrder(3, 100, 10, OrderSideEnum.BID, "4", OrderExecutionTypeEnum.FOK, OrderTypeEnum.LIMIT));
        fifoOrderBook.addOrder(buildOrder(4, 100, 10, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FAK, OrderTypeEnum.LIMIT));
        fifoOrderBook.addOrder(buildOrder(5, 100, 10, OrderSideEnum.BID, "6", OrderExecutionTypeEnum.FILL, OrderTypeEnum.MARKET));
        fifoOrderBook.addOrder(buildOrder(6, 100, 10, OrderSideEnum.BID, "7", OrderExecutionTypeEnum.FOK, OrderTypeEnum.MARKET));
        fifoOrderBook.addOrder(buildOrder(7, 100, 10, OrderSideEnum.BID, "8", OrderExecutionTypeEnum.FAK, OrderTypeEnum.MARKET));

        assertEquals(3, fifoOrderBook.totalNumberOfActiveOrders());
        assertEquals(2, fifoOrderBook.totalNumberOfBidOrders());
        assertEquals(1, fifoOrderBook.totalNumberOfAskOrders());
    }

    @Test
    void test_matching_algorithm_limit_fill_or_kill_kill() {
        fifoOrderBook.addOrder(buildOrderCreate(0, 99, 10, OrderSideEnum.BID, "1"));
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "2"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 101, 10, OrderSideEnum.ASK, "3"));

        var order = buildOrder(3, 101, 100, OrderSideEnum.BID, "4", OrderExecutionTypeEnum.FOK, OrderTypeEnum.LIMIT);
        var result = fifoOrderBook.runMatchingAlgorithmNonActiveOrder(order);
        addMessages(result);

        assertEquals(1, result.size());
        assertEquals(OrderCancelOperationTypeEnum.KILLED, ((CancelOrder) result.get(0)).cancelOperationType());
    }

    @Test
    void test_matching_algorithm_limit_fill_or_kill_partial_fill() {
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "1"));
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "2"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 101, 10, OrderSideEnum.ASK, "3"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 102, 10, OrderSideEnum.ASK, "4"));

        var order = buildOrder(3, 102, 20, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FOK, OrderTypeEnum.LIMIT);
        var result = fifoOrderBook.runMatchingAlgorithmNonActiveOrder(order);

        assertEquals(3, result.size());
        assertEquals(OrderUpdatedOperationTypeEnum.PARTIAL_FILL, ((UpdateOrder) result.get(1)).updateOperationType());
    }

    @Test
    void test_matching_algorithm_limit_fill_or_kill_filled() {
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "1"));
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "2"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 101, 10, OrderSideEnum.ASK, "3"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 102, 10, OrderSideEnum.ASK, "4"));

        var order = buildOrder(3, 102, 10, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FOK, OrderTypeEnum.LIMIT);
        var result = fifoOrderBook.runMatchingAlgorithmNonActiveOrder(order);
        assertEquals(3, result.size());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(0)).cancelOperationType());
    }

    @Test
    void test_matching_algorithm_limit_fill_and_kill_killed() {
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "1"));
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "2"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 101, 10, OrderSideEnum.ASK, "3"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 102, 10, OrderSideEnum.ASK, "4"));

        var order = buildOrder(3, 100.5, 20, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FAK, OrderTypeEnum.LIMIT);
        var result = fifoOrderBook.runMatchingAlgorithmNonActiveOrder(order);
        assertEquals(1, result.size());
        assertEquals(OrderCancelOperationTypeEnum.KILLED, ((CancelOrder) result.get(0)).cancelOperationType());
    }

    @Test
    void test_matching_algorithm_limit_fill_and_kill_partial_filled() {
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "1"));
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "2"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 101, 10, OrderSideEnum.ASK, "3"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 102, 10, OrderSideEnum.ASK, "4"));

        var order = buildOrder(3, 102, 20, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FAK, OrderTypeEnum.LIMIT);
        var result = fifoOrderBook.runMatchingAlgorithmNonActiveOrder(order);
        assertEquals(3, result.size());
        assertEquals(OrderUpdatedOperationTypeEnum.PARTIAL_FILL, ((UpdateOrder) result.get(1)).updateOperationType());
    }

    @Test
    void test_matching_algorithm_limit_fill_and_kill_filled() {
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "1"));
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "2"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 101, 10, OrderSideEnum.ASK, "3"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 102, 10, OrderSideEnum.ASK, "4"));

        var order = buildOrder(3, 101, 10, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FAK, OrderTypeEnum.LIMIT);
        var result = fifoOrderBook.runMatchingAlgorithmNonActiveOrder(order);
        assertEquals(3, result.size());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(0)).cancelOperationType());
    }

    @Test
    void test_matching_algorithm_market_fill_killed() {
        var order = buildOrder(3, 100.5, 10, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FILL, OrderTypeEnum.MARKET);
        var result = fifoOrderBook.runMatchingAlgorithmNonActiveOrder(order);
        assertEquals(1, result.size());
        assertEquals(OrderCancelOperationTypeEnum.KILLED, ((CancelOrder) result.get(0)).cancelOperationType());
    }

    @Test
    void test_matching_algorithm_market_fill_partial_filled() {
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "1"));
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "2"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 101, 10, OrderSideEnum.ASK, "3"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 102, 10, OrderSideEnum.ASK, "4"));

        var order = buildOrder(3, Integer.MAX_VALUE, 100, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FILL, OrderTypeEnum.MARKET);
        var result = fifoOrderBook.runMatchingAlgorithmNonActiveOrder(order);
        assertEquals(3, result.size());
        assertEquals(OrderUpdatedOperationTypeEnum.PARTIAL_FILL, ((UpdateOrder) result.get(1)).updateOperationType());
    }

    @Test
    void test_matching_algorithm_market_fill_filled() {
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "1"));
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "2"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 101, 10, OrderSideEnum.ASK, "3"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 102, 10, OrderSideEnum.ASK, "4"));

        assertEquals(4, fifoOrderBook.totalNumberOfActiveOrders());
        assertEquals(2, fifoOrderBook.totalNumberOfBidOrders());
        assertEquals(2, fifoOrderBook.totalNumberOfAskOrders());

        var order = buildOrder(3, Integer.MAX_VALUE, 10, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FILL, OrderTypeEnum.MARKET);
        var result = fifoOrderBook.runMatchingAlgorithmNonActiveOrder(order);
        assertEquals(3, result.size());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(0)).cancelOperationType());
    }

    @Test
    void test_matching_algorithm_market_fill_or_kill_killed() {
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "1"));
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "2"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 101, 10, OrderSideEnum.ASK, "3"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 102, 10, OrderSideEnum.ASK, "4"));

        var order = buildOrder(3, Integer.MAX_VALUE, 100, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FOK, OrderTypeEnum.MARKET);
        var result = fifoOrderBook.runMatchingAlgorithmNonActiveOrder(order);
        assertEquals(1, result.size());
        assertEquals(OrderCancelOperationTypeEnum.KILLED, ((CancelOrder) result.get(0)).cancelOperationType());
    }

    @Test
    void test_matching_algorithm_market_fill_or_kill_partial_fill() {
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "1"));
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "2"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 101, 10, OrderSideEnum.ASK, "3"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 102, 10, OrderSideEnum.ASK, "4"));

        var order = buildOrder(3, Integer.MAX_VALUE, 20, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FOK, OrderTypeEnum.MARKET);
        var result = fifoOrderBook.runMatchingAlgorithmNonActiveOrder(order);
        assertEquals(3, result.size());
        assertEquals(OrderUpdatedOperationTypeEnum.PARTIAL_FILL, ((UpdateOrder) result.get(1)).updateOperationType());
    }

    @Test
    void test_matching_algorithm_market_fill_or_kill_filled() {
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "1"));
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "2"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 101, 10, OrderSideEnum.ASK, "3"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 102, 10, OrderSideEnum.ASK, "4"));

        var order = buildOrder(3, Integer.MAX_VALUE, 10, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FOK, OrderTypeEnum.MARKET);
        var result = fifoOrderBook.runMatchingAlgorithmNonActiveOrder(order);
        assertEquals(3, result.size());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(0)).cancelOperationType());
    }

    @Test
    void test_matching_algorithm_market_fill_and_kill_killed() {
        var order = buildOrder(3, Integer.MAX_VALUE, 100, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FAK, OrderTypeEnum.MARKET);
        var result = fifoOrderBook.runMatchingAlgorithmNonActiveOrder(order);
        assertEquals(1, result.size());
        assertEquals(OrderCancelOperationTypeEnum.KILLED, ((CancelOrder) result.get(0)).cancelOperationType());
    }

    @Test
    void test_matching_algorithm_market_fill_and_kill_partial_fill() {
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "1"));
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "2"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 101, 10, OrderSideEnum.ASK, "3"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 102, 10, OrderSideEnum.ASK, "4"));

        var order = buildOrder(3, Integer.MAX_VALUE, 20, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FAK, OrderTypeEnum.MARKET);
        var result = fifoOrderBook.runMatchingAlgorithmNonActiveOrder(order);
        assertEquals(3, result.size());
        assertEquals(OrderUpdatedOperationTypeEnum.PARTIAL_FILL, ((UpdateOrder) result.get(1)).updateOperationType());
    }

    @Test
    void test_matching_algorithm_market_fill_and_kill_filled() {
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "1"));
        fifoOrderBook.addOrder(buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "2"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 101, 10, OrderSideEnum.ASK, "3"));
        fifoOrderBook.addOrder(buildOrderCreate(2, 102, 10, OrderSideEnum.ASK, "4"));

        var order = buildOrder(3, Integer.MAX_VALUE, 10, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FAK, OrderTypeEnum.MARKET);
        var result = fifoOrderBook.runMatchingAlgorithmNonActiveOrder(order);
        assertEquals(3, result.size());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(1)).cancelOperationType());
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