package com.herron.exchange.tradingengine.server.matchingengine.model;


import com.herron.exchange.common.api.common.api.trading.OrderbookEvent;
import com.herron.exchange.common.api.common.api.trading.orders.CancelOrder;
import com.herron.exchange.common.api.common.api.trading.orders.UpdateOrder;
import com.herron.exchange.common.api.common.api.trading.trades.Trade;
import com.herron.exchange.common.api.common.messages.common.DefaultBusinessCalendar;
import com.herron.exchange.common.api.common.messages.common.Member;
import com.herron.exchange.common.api.common.messages.common.Participant;
import com.herron.exchange.common.api.common.messages.common.User;
import com.herron.exchange.common.api.common.messages.refdata.ImmutableDefaultEquityInstrument;
import com.herron.exchange.common.api.common.messages.refdata.ImmutableDefaultMarket;
import com.herron.exchange.common.api.common.messages.refdata.ImmutableDefaultOrderbookData;
import com.herron.exchange.common.api.common.messages.refdata.ImmutableDefaultProduct;
import com.herron.exchange.common.api.common.messages.trading.DefaultTradingCalendar;
import com.herron.exchange.tradingengine.server.matchingengine.api.Orderbook;
import com.herron.exchange.tradingengine.server.matchingengine.factory.OrderbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.herron.exchange.common.api.common.enums.AuctionAlgorithmEnum.DUTCH;
import static com.herron.exchange.common.api.common.enums.MatchingAlgorithmEnum.PRO_RATA;
import static com.herron.exchange.common.api.common.enums.OrderCancelOperationTypeEnum.FILLED;
import static com.herron.exchange.common.api.common.enums.OrderCancelOperationTypeEnum.KILLED;
import static com.herron.exchange.common.api.common.enums.OrderSideEnum.ASK;
import static com.herron.exchange.common.api.common.enums.OrderSideEnum.BID;
import static com.herron.exchange.common.api.common.enums.OrderTypeEnum.LIMIT;
import static com.herron.exchange.common.api.common.enums.OrderTypeEnum.MARKET;
import static com.herron.exchange.common.api.common.enums.OrderUpdatedOperationTypeEnum.PARTIAL_FILL;
import static com.herron.exchange.common.api.common.enums.StateChangeTypeEnum.CONTINUOUS_TRADING;
import static com.herron.exchange.common.api.common.enums.StateChangeTypeEnum.PRE_TRADE;
import static com.herron.exchange.common.api.common.enums.TimeInForceEnum.*;
import static com.herron.exchange.tradingengine.server.matchingengine.utils.MessageCreatorTestUtils.buildOrderAdd;
import static com.herron.exchange.tradingengine.server.matchingengine.utils.MessageCreatorTestUtils.buildOrderUpdate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProRataOrderbookTest {
    private Orderbook orderbook;

    @BeforeEach
    void init() {
        var orderbookData = ImmutableDefaultOrderbookData.builder()
                .orderbookId("orderbookId")
                .matchingAlgorithm(PRO_RATA)
                .tradingCurrency("eur")
                .minTradeVolume(0)
                .auctionAlgorithm(DUTCH)
                .tradingCalendar(DefaultTradingCalendar.twentyFourSevenTradingCalendar())
                .instrument(ImmutableDefaultEquityInstrument.builder()
                        .instrumentId("instrumendId")
                        .firstTradingDate(LocalDate.MIN)
                        .lastTradingDate(LocalDate.MAX)
                        .product(ImmutableDefaultProduct.builder().currency("eur").productId("product").market(ImmutableDefaultMarket.builder().marketId("market").businessCalendar(DefaultBusinessCalendar.defaultWeekendCalendar()).build()).build())
                        .build())
                .build();
        this.orderbook = OrderbookFactory.createOrderbook(orderbookData);
        orderbook.updateState(PRE_TRADE);
        orderbook.updateState(CONTINUOUS_TRADING);
    }

    @Test
    void test_add_to_orderbook() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "3"));

        assertEquals(3, orderbook.totalNumberOfActiveOrders());
        assertEquals(2, orderbook.totalNumberOfBidOrders());
        assertEquals(1, orderbook.totalNumberOfAskOrders());
    }

    @Test
    void test_update_orderbook() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        assertEquals(1, orderbook.totalNumberOfPriceLevels());
        assertEquals(1, orderbook.totalNumberOfBidPriceLevels());
        assertEquals(0, orderbook.totalNumberOfAskPriceLevels());
        assertEquals(10, orderbook.getOrder("1").currentVolume().getValue());
        assertEquals(100, orderbook.getOrder("1").price().getValue());

        orderbook.updateOrderbook(buildOrderUpdate(0, 99, 9, BID, "1"));
        assertEquals(1, orderbook.totalNumberOfPriceLevels());
        assertEquals(1, orderbook.totalNumberOfBidPriceLevels());
        assertEquals(0, orderbook.totalNumberOfAskPriceLevels());
        assertEquals(9, orderbook.getOrder("1").currentVolume().getValue());
        assertEquals(99, orderbook.getOrder("1").price().getValue());
    }

    @Test
    void test_best_price_after_insert_orders() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, ASK, "2"));
        assertEquals(100, orderbook.getBestBidPrice().getValue());
        assertEquals(102, orderbook.getBestAskPrice().getValue());

        orderbook.updateOrderbook(buildOrderAdd(0, 101, 10, BID, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "3"));
        assertEquals(101, orderbook.getBestBidPrice().getValue());
        assertEquals(101, orderbook.getBestAskPrice().getValue());
    }

    @Test
    void test_remove_from_orderbook() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "3"));

        assertEquals(3, orderbook.totalNumberOfActiveOrders());
        assertEquals(2, orderbook.totalNumberOfBidOrders());
        assertEquals(1, orderbook.totalNumberOfAskOrders());

        orderbook.removeOrder("2");
        orderbook.removeOrder("3");
        assertEquals(1, orderbook.totalNumberOfActiveOrders());
        assertEquals(1, orderbook.totalNumberOfBidOrders());
        assertEquals(0, orderbook.totalNumberOfAskOrders());
    }

    @Test
    void test_best_price_after_remove_order() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, ASK, "2"));
        orderbook.updateOrderbook(buildOrderAdd(0, 99, 10, BID, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 103, 10, ASK, "4"));
        assertEquals(100, orderbook.getBestBidPrice().getValue());
        assertEquals(102, orderbook.getBestAskPrice().getValue());

        orderbook.removeOrder("1");
        orderbook.removeOrder("2");
        assertEquals(99, orderbook.getBestBidPrice().getValue());
        assertEquals(103, orderbook.getBestAskPrice().getValue());
    }

    @Test
    void test_volume() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 11, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 13, ASK, "2"));
        orderbook.updateOrderbook(buildOrderAdd(0, 99, 12, BID, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 103, 10, ASK, "4"));
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
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 11, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 13, ASK, "2"));
        orderbook.updateOrderbook(buildOrderAdd(0, 99, 12, BID, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 103, 10, ASK, "4"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "5"));
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
    void test_matching_algorithm_self_match() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1", new Participant(new Member("member"), new User("user"))));
        var order = buildOrderAdd(2, 100, 15, ASK, "2", new Participant(new Member("member"), new User("user")));
        orderbook.updateOrderbook(order);
        List<OrderbookEvent> matchingEvents = orderbook.runMatchingAlgorithm(order).messages();
        Optional<Trade> trade = matchingEvents.stream().filter(m -> m instanceof Trade).map(t -> (Trade) t).findFirst();
        assertFalse(trade.isPresent());
    }

    @Test
    void test_matching_algorithm_limit_same_proportion_fill_fill() {
        orderbook.updateOrderbook(buildOrderAdd(0, 99, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "4"));

        var order = buildOrderAdd(3, 101, 10, BID, "5", FILL, LIMIT);
        var result = orderbook.runMatchingAlgorithm(order).messages();

        assertEquals(6, result.size());
        assertEquals(5, ((Trade) result.get(2)).volume().getValue());
        assertEquals(5, ((Trade) result.get(5)).volume().getValue());
    }

    @Test
    void test_matching_algorithm_limit_uneven_proportion_fill_fill() {
        orderbook.updateOrderbook(buildOrderAdd(0, 99, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 50, ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 20, ASK, "4"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 30, ASK, "5"));

        var order = buildOrderAdd(3, 101, 10, BID, "6", FILL, LIMIT);
        var result = orderbook.runMatchingAlgorithm(order).messages();

        assertEquals(9, result.size());
        assertEquals(5, ((Trade) result.get(2)).volume().getValue());
        assertEquals(3, ((Trade) result.get(5)).volume().getValue());
        assertEquals(2, ((Trade) result.get(8)).volume().getValue());
    }

    @Test
    void test_matching_algorithm_limit_uneven_proportion_multiple_level_fill_fill() {
        orderbook.updateOrderbook(buildOrderAdd(0, 99, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 50, ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 20, ASK, "4"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 30, ASK, "5"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 45, ASK, "6"));

        var order = buildOrderAdd(3, 102, 200, BID, "7", FILL, LIMIT);
        var result = orderbook.runMatchingAlgorithm(order).messages();

        assertEquals(12, result.size());
        assertEquals(50, ((Trade) result.get(2)).volume().getValue());
        assertEquals(30, ((Trade) result.get(5)).volume().getValue());
        assertEquals(20, ((Trade) result.get(8)).volume().getValue());
        assertEquals(45, ((Trade) result.get(11)).volume().getValue());
    }

    @Test
    void test_add_non_active_order_to_orderbook() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "3"));

        assertEquals(3, orderbook.totalNumberOfActiveOrders());
        assertEquals(2, orderbook.totalNumberOfBidOrders());
        assertEquals(1, orderbook.totalNumberOfAskOrders());

        orderbook.updateOrderbook(buildOrderAdd(3, 100, 10, BID, "4", FOK, LIMIT));
        orderbook.updateOrderbook(buildOrderAdd(4, 100, 10, BID, "5", FAK, LIMIT));
        orderbook.updateOrderbook(buildOrderAdd(5, 100, 10, BID, "6", FILL, MARKET));
        orderbook.updateOrderbook(buildOrderAdd(6, 100, 10, BID, "7", FOK, MARKET));
        orderbook.updateOrderbook(buildOrderAdd(7, 100, 10, BID, "8", FAK, MARKET));

        assertEquals(3, orderbook.totalNumberOfActiveOrders());
        assertEquals(2, orderbook.totalNumberOfBidOrders());
        assertEquals(1, orderbook.totalNumberOfAskOrders());
    }

    @Test
    void test_matching_algorithm_limit_fill_or_kill_kill() {
        orderbook.updateOrderbook(buildOrderAdd(0, 99, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "3"));

        var order = buildOrderAdd(3, 101, 100, BID, "4", FOK, LIMIT);
        var result = orderbook.runMatchingAlgorithm(order).messages();

        assertEquals(1, result.size());
        assertEquals(KILLED, ((CancelOrder) result.get(0)).cancelOperationType());
    }

    @Test
    void test_matching_algorithm_limit_fill_or_kill_partial_fill() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, ASK, "4"));

        var order = buildOrderAdd(3, 102, 20, BID, "5", FOK, LIMIT);
        var result = orderbook.runMatchingAlgorithm(order).messages();

        assertEquals(6, result.size());
        assertEquals(PARTIAL_FILL, ((UpdateOrder) result.get(1)).updateOperationType());
        assertEquals(10, ((Trade) result.get(2)).volume().getValue());
        assertEquals(FILLED, ((CancelOrder) result.get(3)).cancelOperationType());
        assertEquals(10, ((Trade) result.get(5)).volume().getValue());
    }

    @Test
    void test_matching_algorithm_limit_fill_or_kill_filled() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, ASK, "4"));

        var order = buildOrderAdd(3, 102, 10, BID, "5", FOK, LIMIT);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(3, result.size());
        assertEquals(FILLED, ((CancelOrder) result.get(0)).cancelOperationType());
        assertEquals(10, ((Trade) result.get(2)).volume().getValue());
    }

    @Test
    void test_matching_algorithm_limit_fill_and_kill_killed() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, ASK, "4"));

        var order = buildOrderAdd(3, 100.5, 20, BID, "5", FAK, LIMIT);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(1, result.size());
        assertEquals(KILLED, ((CancelOrder) result.get(0)).cancelOperationType());
    }

    @Test
    void test_matching_algorithm_limit_fill_and_kill_partial_filled() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, ASK, "4"));

        var order = buildOrderAdd(3, 102, 20, BID, "5", FAK, LIMIT);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(6, result.size());
        assertEquals(PARTIAL_FILL, ((UpdateOrder) result.get(1)).updateOperationType());
        assertEquals(10, ((Trade) result.get(2)).volume().getValue());
        assertEquals(FILLED, ((CancelOrder) result.get(3)).cancelOperationType());
        assertEquals(10, ((Trade) result.get(5)).volume().getValue());
    }

    @Test
    void test_matching_algorithm_limit_fill_and_kill_filled() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, ASK, "4"));

        var order = buildOrderAdd(3, 101, 10, BID, "5", FAK, LIMIT);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(3, result.size());
        assertEquals(FILLED, ((CancelOrder) result.get(1)).cancelOperationType());
        assertEquals(10, ((Trade) result.get(2)).volume().getValue());
    }

    @Test
    void test_matching_algorithm_market_fill_killed() {
        var order = buildOrderAdd(3, 100.5, 10, BID, "5", FILL, MARKET);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(1, result.size());
        assertEquals(KILLED, ((CancelOrder) result.get(0)).cancelOperationType());
    }

    @Test
    void test_matching_algorithm_market_fill_partial_filled() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, ASK, "4"));

        var order = buildOrderAdd(3, Integer.MAX_VALUE, 100, BID, "5", FILL, MARKET);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(7, result.size());
        assertEquals(PARTIAL_FILL, ((UpdateOrder) result.get(1)).updateOperationType());
        assertEquals(10, ((Trade) result.get(2)).volume().getValue());
        assertEquals(FILLED, ((CancelOrder) result.get(3)).cancelOperationType());
        assertEquals(10, ((Trade) result.get(5)).volume().getValue());
        assertEquals(KILLED, ((CancelOrder) result.get(6)).cancelOperationType());

    }

    @Test
    void test_matching_algorithm_market_fill_filled() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, ASK, "4"));

        assertEquals(4, orderbook.totalNumberOfActiveOrders());
        assertEquals(2, orderbook.totalNumberOfBidOrders());
        assertEquals(2, orderbook.totalNumberOfAskOrders());

        var order = buildOrderAdd(3, Integer.MAX_VALUE, 10, BID, "5", FILL, MARKET);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(3, result.size());
        assertEquals(FILLED, ((CancelOrder) result.get(0)).cancelOperationType());
        assertEquals(10, ((Trade) result.get(2)).volume().getValue());
    }

    @Test
    void test_matching_algorithm_market_fill_or_kill_killed() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, ASK, "4"));

        var order = buildOrderAdd(3, Integer.MAX_VALUE, 100, BID, "5", FOK, MARKET);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(1, result.size());
        assertEquals(KILLED, ((CancelOrder) result.get(0)).cancelOperationType());
    }

    @Test
    void test_matching_algorithm_market_fill_or_kill_partial_fill() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, ASK, "4"));

        var order = buildOrderAdd(3, Integer.MAX_VALUE, 20, BID, "5", FOK, MARKET);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(6, result.size());
        assertEquals(PARTIAL_FILL, ((UpdateOrder) result.get(1)).updateOperationType());
        assertEquals(10, ((Trade) result.get(2)).volume().getValue());
        assertEquals(FILLED, ((CancelOrder) result.get(3)).cancelOperationType());
        assertEquals(10, ((Trade) result.get(5)).volume().getValue());
    }

    @Test
    void test_matching_algorithm_market_fill_or_kill_filled() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, ASK, "4"));

        var order = buildOrderAdd(3, Integer.MAX_VALUE, 10, BID, "5", FOK, MARKET);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(3, result.size());
        assertEquals(FILLED, ((CancelOrder) result.get(0)).cancelOperationType());
        assertEquals(10, ((Trade) result.get(2)).volume().getValue());
    }

    @Test
    void test_matching_algorithm_market_fill_and_kill_killed() {
        var order = buildOrderAdd(3, Integer.MAX_VALUE, 100, BID, "5", FAK, MARKET);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(1, result.size());
        assertEquals(KILLED, ((CancelOrder) result.get(0)).cancelOperationType());
    }

    @Test
    void test_matching_algorithm_market_fill_and_kill_partial_fill() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, ASK, "4"));

        var order = buildOrderAdd(3, Integer.MAX_VALUE, 20, BID, "5", FAK, MARKET);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(6, result.size());
        assertEquals(PARTIAL_FILL, ((UpdateOrder) result.get(1)).updateOperationType());
        assertEquals(10, ((Trade) result.get(2)).volume().getValue());
        assertEquals(FILLED, ((CancelOrder) result.get(3)).cancelOperationType());
        assertEquals(10, ((Trade) result.get(5)).volume().getValue());
    }

    @Test
    void test_matching_algorithm_market_fill_and_kill_filled() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, ASK, "4"));

        var order = buildOrderAdd(3, Integer.MAX_VALUE, 10, BID, "5", FAK, MARKET);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(3, result.size());
        assertEquals(FILLED, ((CancelOrder) result.get(1)).cancelOperationType());
        assertEquals(10, ((Trade) result.get(2)).volume().getValue());
    }
}