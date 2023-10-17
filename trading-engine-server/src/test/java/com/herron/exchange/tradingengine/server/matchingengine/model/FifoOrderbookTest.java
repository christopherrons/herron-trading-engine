package com.herron.exchange.tradingengine.server.matchingengine.model;

import com.herron.exchange.common.api.common.api.trading.OrderbookEvent;
import com.herron.exchange.common.api.common.api.trading.orders.CancelOrder;
import com.herron.exchange.common.api.common.api.trading.orders.UpdateOrder;
import com.herron.exchange.common.api.common.api.trading.trades.Trade;
import com.herron.exchange.common.api.common.api.trading.trades.TradeExecution;
import com.herron.exchange.common.api.common.enums.*;
import com.herron.exchange.common.api.common.messages.common.*;
import com.herron.exchange.common.api.common.messages.refdata.ImmutableDefaultEquityInstrument;
import com.herron.exchange.common.api.common.messages.refdata.ImmutableDefaultMarket;
import com.herron.exchange.common.api.common.messages.refdata.ImmutableDefaultOrderbookData;
import com.herron.exchange.common.api.common.messages.refdata.ImmutableDefaultProduct;
import com.herron.exchange.common.api.common.messages.trading.DefaultTradingCalendar;
import com.herron.exchange.tradingengine.server.matchingengine.api.Orderbook;
import com.herron.exchange.tradingengine.server.matchingengine.factory.OrderbookFactory;
import com.herron.exchange.tradingengine.server.matchingengine.utils.MessageCreatorTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.herron.exchange.tradingengine.server.matchingengine.utils.MessageCreatorTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

class FifoOrderbookTest {
    private Orderbook orderbook;

    @BeforeEach
    void init() {
        var orderbookData = ImmutableDefaultOrderbookData.builder()
                .orderbookId("orderbookId")
                .matchingAlgorithm(MatchingAlgorithmEnum.FIFO)
                .tradingCurrency("eur")
                .minTradeVolume(0)
                .auctionAlgorithm(AuctionAlgorithmEnum.DUTCH)
                .tradingCalendar(DefaultTradingCalendar.twentyFourSevenTradingCalendar())
                .instrument(ImmutableDefaultEquityInstrument.builder()
                        .instrumentId("instrumendId")
                        .firstTradingDate(LocalDate.MIN)
                        .lastTradingDate(LocalDate.MAX)
                        .product(ImmutableDefaultProduct.builder().currency("eur").productId("product").market(ImmutableDefaultMarket.builder().marketId("market").businessCalendar(DefaultBusinessCalendar.defaultWeekendCalendar()).build()).build())
                        .build())
                .build();
        this.orderbook = OrderbookFactory.createOrderbook(orderbookData);
        orderbook.updateState(StateChangeTypeEnum.PRE_TRADE);
        orderbook.updateState(StateChangeTypeEnum.CONTINUOUS_TRADING);
    }

    @Test
    void test_add_to_orderbook() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, OrderSideEnum.ASK, "3"));

        assertEquals(3, orderbook.totalNumberOfActiveOrders());
        assertEquals(2, orderbook.totalNumberOfBidOrders());
        assertEquals(1, orderbook.totalNumberOfAskOrders());
    }

    @Test
    void test_update_orderbook() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "1"));
        assertEquals(1, orderbook.totalNumberOfPriceLevels());
        assertEquals(1, orderbook.totalNumberOfBidPriceLevels());
        assertEquals(0, orderbook.totalNumberOfAskPriceLevels());
        assertEquals(10, orderbook.getOrder("1").currentVolume().getValue());
        assertEquals(100, orderbook.getOrder("1").price().getValue());

        orderbook.updateOrderbook(buildOrderUpdate(0, 99, 9, OrderSideEnum.BID, "1"));
        assertEquals(1, orderbook.totalNumberOfPriceLevels());
        assertEquals(1, orderbook.totalNumberOfBidPriceLevels());
        assertEquals(0, orderbook.totalNumberOfAskPriceLevels());
        assertEquals(9, orderbook.getOrder("1").currentVolume().getValue());
        assertEquals(99, orderbook.getOrder("1").price().getValue());
    }

    @Test
    void test_best_price_after_insert_orders() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, OrderSideEnum.ASK, "2"));
        assertEquals(100, orderbook.getBestBidPrice().getValue());
        assertEquals(102, orderbook.getBestAskPrice().getValue());

        orderbook.updateOrderbook(buildOrderAdd(0, 101, 10, OrderSideEnum.BID, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, OrderSideEnum.ASK, "3"));
        assertEquals(101, orderbook.getBestBidPrice().getValue());
        assertEquals(101, orderbook.getBestAskPrice().getValue());
    }

    @Test
    void test_remove_from_orderbook() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, OrderSideEnum.ASK, "3"));

        assertEquals(3, orderbook.totalNumberOfActiveOrders());
        assertEquals(2, orderbook.totalNumberOfBidOrders());
        assertEquals(1, orderbook.totalNumberOfAskOrders());

        orderbook.updateOrderbook(buildOrderDelete(0, 100, 10, OrderSideEnum.BID, "2"));
        orderbook.updateOrderbook(buildOrderDelete(4, 101, 10, OrderSideEnum.ASK, "3"));
        assertEquals(1, orderbook.totalNumberOfActiveOrders());
        assertEquals(1, orderbook.totalNumberOfBidOrders());
        assertEquals(0, orderbook.totalNumberOfAskOrders());
    }

    @Test
    void test_best_price_after_remove_order() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, OrderSideEnum.ASK, "2"));
        orderbook.updateOrderbook(buildOrderAdd(0, 99, 10, OrderSideEnum.BID, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 103, 10, OrderSideEnum.ASK, "4"));
        assertEquals(100, orderbook.getBestBidPrice().getValue());
        assertEquals(102, orderbook.getBestAskPrice().getValue());

        orderbook.removeOrder("1");
        orderbook.removeOrder("2");
        assertEquals(99, orderbook.getBestBidPrice().getValue());
        assertEquals(103, orderbook.getBestAskPrice().getValue());
    }

    @Test
    void test_volume() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 11, OrderSideEnum.BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 13, OrderSideEnum.ASK, "2"));
        orderbook.updateOrderbook(buildOrderAdd(0, 99, 12, OrderSideEnum.BID, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 103, 10, OrderSideEnum.ASK, "4"));
        assertEquals(46, orderbook.totalOrderVolume().getValue());
        assertEquals(23, orderbook.totalBidVolume().getValue());
        assertEquals(23, orderbook.totalAskVolume().getValue());


        orderbook.removeOrder("1");
        orderbook.removeOrder("2");
        assertEquals(22, orderbook.totalOrderVolume().getValue());
        assertEquals(12, orderbook.totalBidVolume().getValue());
        assertEquals(10, orderbook.totalAskVolume().getValue());
    }

    @Test
    void test_volume_at_level() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 11, OrderSideEnum.BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 13, OrderSideEnum.ASK, "2"));
        orderbook.updateOrderbook(buildOrderAdd(0, 99, 12, OrderSideEnum.BID, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 103, 10, OrderSideEnum.ASK, "4"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "5"));
        assertEquals(34, orderbook.totalVolumeAtPriceLevel(1).getValue());
        assertEquals(21, orderbook.totalBidVolumeAtPriceLevel(1).getValue());
        assertEquals(13, orderbook.totalAskVolumeAtPriceLevel(1).getValue());
        assertEquals(22, orderbook.totalVolumeAtPriceLevel(2).getValue());
        assertEquals(12, orderbook.totalBidVolumeAtPriceLevel(2).getValue());
        assertEquals(10, orderbook.totalAskVolumeAtPriceLevel(2).getValue());

        orderbook.removeOrder("1");
        orderbook.removeOrder("2");
        assertEquals(20, orderbook.totalVolumeAtPriceLevel(1).getValue());
        assertEquals(10, orderbook.totalBidVolumeAtPriceLevel(1).getValue());
        assertEquals(10, orderbook.totalAskVolumeAtPriceLevel(1).getValue());
        assertEquals(12, orderbook.totalVolumeAtPriceLevel(2).getValue());
        assertEquals(12, orderbook.totalBidVolumeAtPriceLevel(2).getValue());
        assertEquals(0, orderbook.totalAskVolumeAtPriceLevel(2).getValue());
    }

    @Test
    void test_matching_algorithm_same_price_Level() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "1"));
        var order = buildOrderAdd(2, 100, 15, OrderSideEnum.ASK, "2");
        orderbook.updateOrderbook(order);

        List<OrderbookEvent> matchingEvents = orderbook.runMatchingAlgorithm(order).messages();

        Trade trade = matchingEvents.stream().filter(m -> m instanceof Trade).map(t -> (Trade) t).findFirst().get();
        assertEquals(5, orderbook.totalOrderVolume().getValue());
        assertNotEquals("0", trade.tradeId());
        assertFalse(trade.isBidSideAggressor());

        order = buildOrderAdd(3, 100, 6, OrderSideEnum.BID, "3");
        orderbook.updateOrderbook(order);
        matchingEvents = orderbook.runMatchingAlgorithm(order).messages();

        trade = matchingEvents.stream().filter(m -> m instanceof Trade).map(t -> (Trade) t).findFirst().get();
        assertEquals(1, orderbook.totalOrderVolume().getValue());
        assertNotEquals("0", trade.tradeId());
        assertTrue(trade.isBidSideAggressor());
    }

    @Test
    void test_matching_algorithm_self_match() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "1", new Participant(new Member("member"), new User("user"))));
        var order = buildOrderAdd(2, 100, 15, OrderSideEnum.ASK, "2", new Participant(new Member("member"), new User("user")));
        orderbook.updateOrderbook(order);
        List<OrderbookEvent> matchingEvents = orderbook.runMatchingAlgorithm(order).messages();

        Optional<Trade> trade = matchingEvents.stream().filter(m -> m instanceof Trade).map(t -> (Trade) t).findFirst();
        assertFalse(trade.isPresent());
    }

    @Test
    void test_add_non_active_order_to_orderbook() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, OrderSideEnum.ASK, "3"));

        assertEquals(3, orderbook.totalNumberOfActiveOrders());
        assertEquals(2, orderbook.totalNumberOfBidOrders());
        assertEquals(1, orderbook.totalNumberOfAskOrders());

        orderbook.updateOrderbook(MessageCreatorTestUtils.buildOrderAdd(3, 100, 10, OrderSideEnum.BID, "4", OrderExecutionTypeEnum.FOK, OrderTypeEnum.LIMIT));
        orderbook.updateOrderbook(MessageCreatorTestUtils.buildOrderAdd(4, 100, 10, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FAK, OrderTypeEnum.LIMIT));
        orderbook.updateOrderbook(MessageCreatorTestUtils.buildOrderAdd(5, 100, 10, OrderSideEnum.BID, "6", OrderExecutionTypeEnum.FILL, OrderTypeEnum.MARKET));
        orderbook.updateOrderbook(MessageCreatorTestUtils.buildOrderAdd(6, 100, 10, OrderSideEnum.BID, "7", OrderExecutionTypeEnum.FOK, OrderTypeEnum.MARKET));
        orderbook.updateOrderbook(MessageCreatorTestUtils.buildOrderAdd(7, 100, 10, OrderSideEnum.BID, "8", OrderExecutionTypeEnum.FAK, OrderTypeEnum.MARKET));

        assertEquals(3, orderbook.totalNumberOfActiveOrders());
        assertEquals(2, orderbook.totalNumberOfBidOrders());
        assertEquals(1, orderbook.totalNumberOfAskOrders());
    }

    @Test
    void test_matching_algorithm_limit_fill_or_kill_kill() {
        orderbook.updateOrderbook(buildOrderAdd(0, 99, 10, OrderSideEnum.BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, OrderSideEnum.ASK, "3"));

        var order = MessageCreatorTestUtils.buildOrderAdd(3, 101, 100, OrderSideEnum.BID, "4", OrderExecutionTypeEnum.FOK, OrderTypeEnum.LIMIT);
        var result = orderbook.runMatchingAlgorithm(order).messages();

        assertEquals(1, result.size());
        assertEquals(OrderCancelOperationTypeEnum.KILLED, ((CancelOrder) result.get(0)).cancelOperationType());
    }

    @Test
    void test_matching_algorithm_limit_fill_or_kill_partial_fill() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, OrderSideEnum.ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, OrderSideEnum.ASK, "4"));

        var order = MessageCreatorTestUtils.buildOrderAdd(3, 102, 20, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FOK, OrderTypeEnum.LIMIT);
        var result = orderbook.runMatchingAlgorithm(order).messages();

        assertEquals(6, result.size());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(0)).cancelOperationType());
        assertEquals(OrderUpdatedOperationTypeEnum.PARTIAL_FILL, ((UpdateOrder) result.get(1)).updateOperationType());
        assertEquals(10, ((Trade) result.get(2)).volume().getValue());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(3)).cancelOperationType());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(4)).cancelOperationType());
        assertEquals(10, ((Trade) result.get(5)).volume().getValue());
    }

    @Test
    void test_matching_algorithm_limit_fill_or_kill_filled() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, OrderSideEnum.ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, OrderSideEnum.ASK, "4"));

        var order = buildOrderAdd(3, 102, 10, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FOK, OrderTypeEnum.LIMIT);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(3, result.size());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(0)).cancelOperationType());
    }

    @Test
    void test_matching_algorithm_limit_fill_and_kill_killed() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, OrderSideEnum.ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, OrderSideEnum.ASK, "4"));

        var order = MessageCreatorTestUtils.buildOrderAdd(3, 100.5, 20, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FAK, OrderTypeEnum.LIMIT);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(1, result.size());
        assertEquals(OrderCancelOperationTypeEnum.KILLED, ((CancelOrder) result.get(0)).cancelOperationType());
    }

    @Test
    void test_matching_algorithm_limit_fill_and_kill_partial_filled() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, OrderSideEnum.ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, OrderSideEnum.ASK, "4"));

        var order = MessageCreatorTestUtils.buildOrderAdd(3, 102, 20, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FAK, OrderTypeEnum.LIMIT);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(6, result.size());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(0)).cancelOperationType());
        assertEquals(OrderUpdatedOperationTypeEnum.PARTIAL_FILL, ((UpdateOrder) result.get(1)).updateOperationType());
        assertEquals(10, ((Trade) result.get(2)).volume().getValue());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(3)).cancelOperationType());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(4)).cancelOperationType());
        assertEquals(10, ((Trade) result.get(5)).volume().getValue());
    }

    @Test
    void test_matching_algorithm_limit_fill_and_kill_filled() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, OrderSideEnum.ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, OrderSideEnum.ASK, "4"));

        var order = MessageCreatorTestUtils.buildOrderAdd(3, 101, 10, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FAK, OrderTypeEnum.LIMIT);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(3, result.size());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(0)).cancelOperationType());
    }

    @Test
    void test_matching_algorithm_market_fill_killed() {
        var order = MessageCreatorTestUtils.buildOrderAdd(3, 100.5, 10, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FILL, OrderTypeEnum.MARKET);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(1, result.size());
        assertEquals(OrderCancelOperationTypeEnum.KILLED, ((CancelOrder) result.get(0)).cancelOperationType());
    }

    @Test
    void test_matching_algorithm_market_fill_partial_filled() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, OrderSideEnum.ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, OrderSideEnum.ASK, "4"));

        var order = MessageCreatorTestUtils.buildOrderAdd(3, Integer.MAX_VALUE, 100, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FILL, OrderTypeEnum.MARKET);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(7, result.size());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(0)).cancelOperationType());
        assertEquals(OrderUpdatedOperationTypeEnum.PARTIAL_FILL, ((UpdateOrder) result.get(1)).updateOperationType());
        assertEquals(10, ((Trade) result.get(2)).volume().getValue());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(3)).cancelOperationType());
        assertEquals(OrderUpdatedOperationTypeEnum.PARTIAL_FILL, ((UpdateOrder) result.get(4)).updateOperationType());
        assertEquals(10, ((Trade) result.get(5)).volume().getValue());
        assertEquals(OrderCancelOperationTypeEnum.KILLED, ((CancelOrder) result.get(6)).cancelOperationType());
    }

    @Test
    void test_matching_algorithm_market_fill_filled() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, OrderSideEnum.ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, OrderSideEnum.ASK, "4"));

        assertEquals(4, orderbook.totalNumberOfActiveOrders());
        assertEquals(2, orderbook.totalNumberOfBidOrders());
        assertEquals(2, orderbook.totalNumberOfAskOrders());

        var order = MessageCreatorTestUtils.buildOrderAdd(3, Integer.MAX_VALUE, 10, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FILL, OrderTypeEnum.MARKET);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(3, result.size());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(0)).cancelOperationType());
    }

    @Test
    void test_matching_algorithm_market_fill_or_kill_killed() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, OrderSideEnum.ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, OrderSideEnum.ASK, "4"));

        var order = MessageCreatorTestUtils.buildOrderAdd(3, Integer.MAX_VALUE, 100, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FOK, OrderTypeEnum.MARKET);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(1, result.size());
        assertEquals(OrderCancelOperationTypeEnum.KILLED, ((CancelOrder) result.get(0)).cancelOperationType());
    }

    @Test
    void test_matching_algorithm_market_fill_or_kill_partial_fill() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, OrderSideEnum.ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, OrderSideEnum.ASK, "4"));

        var order = MessageCreatorTestUtils.buildOrderAdd(3, Integer.MAX_VALUE, 20, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FOK, OrderTypeEnum.MARKET);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(6, result.size());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(0)).cancelOperationType());
        assertEquals(OrderUpdatedOperationTypeEnum.PARTIAL_FILL, ((UpdateOrder) result.get(1)).updateOperationType());
        assertEquals(10, ((Trade) result.get(5)).volume().getValue());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(0)).cancelOperationType());
        assertEquals(OrderUpdatedOperationTypeEnum.PARTIAL_FILL, ((UpdateOrder) result.get(1)).updateOperationType());
        assertEquals(10, ((Trade) result.get(5)).volume().getValue());
    }

    @Test
    void test_matching_algorithm_market_fill_or_kill_filled() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, OrderSideEnum.ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, OrderSideEnum.ASK, "4"));

        var order = MessageCreatorTestUtils.buildOrderAdd(3, Integer.MAX_VALUE, 10, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FOK, OrderTypeEnum.MARKET);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(3, result.size());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(0)).cancelOperationType());
    }

    @Test
    void test_matching_algorithm_market_fill_and_kill_killed() {
        var order = MessageCreatorTestUtils.buildOrderAdd(3, Integer.MAX_VALUE, 100, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FAK, OrderTypeEnum.MARKET);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(1, result.size());
        assertEquals(OrderCancelOperationTypeEnum.KILLED, ((CancelOrder) result.get(0)).cancelOperationType());
    }

    @Test
    void test_matching_algorithm_market_fill_and_kill_partial_fill() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, OrderSideEnum.ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, OrderSideEnum.ASK, "4"));

        var order = MessageCreatorTestUtils.buildOrderAdd(3, Integer.MAX_VALUE, 20, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FAK, OrderTypeEnum.MARKET);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(6, result.size());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(0)).cancelOperationType());
        assertEquals(OrderUpdatedOperationTypeEnum.PARTIAL_FILL, ((UpdateOrder) result.get(1)).updateOperationType());
        assertEquals(10, ((Trade) result.get(2)).volume().getValue());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(3)).cancelOperationType());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(4)).cancelOperationType());
        assertEquals(10, ((Trade) result.get(5)).volume().getValue());
    }

    @Test
    void test_matching_algorithm_market_fill_and_kill_filled() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, OrderSideEnum.BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, OrderSideEnum.ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, OrderSideEnum.ASK, "4"));

        var order = MessageCreatorTestUtils.buildOrderAdd(3, Integer.MAX_VALUE, 10, OrderSideEnum.BID, "5", OrderExecutionTypeEnum.FAK, OrderTypeEnum.MARKET);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(3, result.size());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(0)).cancelOperationType());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(1)).cancelOperationType());
        assertEquals(10, ((Trade) result.get(2)).volume().getValue());
    }

    @Test
    void test_auction_matching() {
        orderbook.updateOrderbook(buildOrderAdd(899, 32.00, 2, OrderSideEnum.BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(900, 32.00, 1, OrderSideEnum.BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(911, 32.00, 8, OrderSideEnum.BID, "3"));
        orderbook.updateOrderbook(buildOrderAdd(902, 31.90, 6, OrderSideEnum.BID, "4"));
        orderbook.updateOrderbook(buildOrderAdd(910, 31.90, 3, OrderSideEnum.BID, "5"));
        orderbook.updateOrderbook(buildOrderAdd(914, 31.90, 2, OrderSideEnum.BID, "6"));
        orderbook.updateOrderbook(buildOrderAdd(913, 31.80, 2, OrderSideEnum.BID, "7"));

        orderbook.updateOrderbook(buildOrderAdd(901, 31.90, 2, OrderSideEnum.ASK, "8"));
        orderbook.updateOrderbook(buildOrderAdd(910, 31.90, 8, OrderSideEnum.ASK, "9"));
        orderbook.updateOrderbook(buildOrderAdd(905, 32.00, 10, OrderSideEnum.ASK, "10"));
        orderbook.updateOrderbook(buildOrderAdd(913, 32.00, 4, OrderSideEnum.ASK, "11"));
        orderbook.updateOrderbook(buildOrderAdd(914, 32.00, 2, OrderSideEnum.ASK, "12"));
        orderbook.updateOrderbook(buildOrderAdd(912, 32.10, 6, OrderSideEnum.ASK, "13"));
        orderbook.updateOrderbook(buildOrderAdd(913, 32.10, 2, OrderSideEnum.ASK, "14"));
        orderbook.updateOrderbook(buildOrderAdd(901, 32.20, 4, OrderSideEnum.ASK, "15"));
        orderbook.updateOrderbook(buildOrderAdd(908, 32.20, 2, OrderSideEnum.ASK, "16"));
        orderbook.updateOrderbook(buildOrderAdd(912, 32.20, 1, OrderSideEnum.ASK, "17"));

        orderbook.updateState(StateChangeTypeEnum.AUCTION_TRADING);
        orderbook.updateState(StateChangeTypeEnum.AUCTION_RUN);
        TradeExecution tradeExecution = orderbook.runAuctionAlgorithm();
        List<Trade> trades = tradeExecution.messages().stream().filter(Trade.class::isInstance).map(Trade.class::cast).toList();
        assertEquals(4, trades.size());
        assertEquals(11, trades.stream().map(Trade::volume).reduce(Volume.ZERO, Volume::add).getValue());
        assertEquals(11, orderbook.totalBidVolumeAtPriceLevel(1).getValue());
        assertEquals(2, orderbook.totalBidVolumeAtPriceLevel(2).getValue());
        assertEquals(15, orderbook.totalAskVolumeAtPriceLevel(1).getValue());
        assertEquals(8, orderbook.totalAskVolumeAtPriceLevel(2).getValue());
        assertEquals(7, orderbook.totalAskVolumeAtPriceLevel(3).getValue());
    }
}
