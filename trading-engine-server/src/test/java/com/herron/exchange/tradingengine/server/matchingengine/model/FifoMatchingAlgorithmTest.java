package com.herron.exchange.tradingengine.server.matchingengine.model;

import com.herron.exchange.common.api.common.api.trading.OrderbookEvent;
import com.herron.exchange.common.api.common.api.trading.orders.Order;
import com.herron.exchange.common.api.common.api.trading.trades.Trade;
import com.herron.exchange.common.api.common.api.trading.trades.TradeExecution;
import com.herron.exchange.common.api.common.enums.TimeInForceEnum;
import com.herron.exchange.common.api.common.messages.common.*;
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
import static com.herron.exchange.common.api.common.enums.MatchingAlgorithmEnum.FIFO;
import static com.herron.exchange.common.api.common.enums.OrderOperationCauseEnum.*;
import static com.herron.exchange.common.api.common.enums.OrderSideEnum.ASK;
import static com.herron.exchange.common.api.common.enums.OrderSideEnum.BID;
import static com.herron.exchange.common.api.common.enums.OrderTypeEnum.LIMIT;
import static com.herron.exchange.common.api.common.enums.OrderTypeEnum.MARKET;
import static com.herron.exchange.common.api.common.enums.TimeInForceEnum.FOK;
import static com.herron.exchange.common.api.common.enums.TimeInForceEnum.SESSION;
import static com.herron.exchange.common.api.common.enums.TradingStatesEnum.*;
import static com.herron.exchange.tradingengine.server.matchingengine.utils.MessageCreatorTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

class FifoMatchingAlgorithmTest {
    private Orderbook orderbook;

    @BeforeEach
    void init() {
        var orderbookData = ImmutableDefaultOrderbookData.builder()
                .orderbookId("orderbookId")
                .matchingAlgorithm(FIFO)
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

        orderbook.updateOrderbook(buildOrderDelete(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderDelete(4, 101, 10, ASK, "3"));
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
    void test_matching_algorithm_same_price_Level() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        var order = buildOrderAdd(2, 100, 15, ASK, "2");
        orderbook.updateOrderbook(order);

        List<OrderbookEvent> matchingEvents = orderbook.runMatchingAlgorithm(order).messages();

        Trade trade = matchingEvents.stream().filter(m -> m instanceof Trade).map(t -> (Trade) t).findFirst().get();
        assertEquals(5, orderbook.totalOrderVolume().getValue());
        assertNotEquals("0", trade.tradeId());
        assertFalse(trade.isBidSideAggressor());

        order = buildOrderAdd(3, 100, 6, BID, "3");
        orderbook.updateOrderbook(order);
        matchingEvents = orderbook.runMatchingAlgorithm(order).messages();

        trade = matchingEvents.stream().filter(m -> m instanceof Trade).map(t -> (Trade) t).findFirst().get();
        assertEquals(1, orderbook.totalOrderVolume().getValue());
        assertNotEquals("0", trade.tradeId());
        assertTrue(trade.isBidSideAggressor());
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
    void test_add_non_active_order_to_orderbook() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "3"));

        assertEquals(3, orderbook.totalNumberOfActiveOrders());
        assertEquals(2, orderbook.totalNumberOfBidOrders());
        assertEquals(1, orderbook.totalNumberOfAskOrders());

        orderbook.updateOrderbook(buildOrderAdd(3, 100, 10, BID, "4", FOK, LIMIT));
        orderbook.updateOrderbook(buildOrderAdd(4, 100, 10, BID, "5", TimeInForceEnum.FAK, LIMIT));
        orderbook.updateOrderbook(buildOrderAdd(5, 100, 10, BID, "6", SESSION, MARKET));
        orderbook.updateOrderbook(buildOrderAdd(6, 100, 10, BID, "7", FOK, MARKET));
        orderbook.updateOrderbook(buildOrderAdd(7, 100, 10, BID, "8", TimeInForceEnum.FAK, MARKET));

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
        assertEquals(KILLED, ((Order) result.get(0)).orderOperationCause());
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
        assertEquals(FILLED, ((Order) result.get(0)).orderOperationCause());
        assertEquals(PARTIAL_FILL, ((Order) result.get(1)).orderOperationCause());
        assertEquals(10, ((Trade) result.get(2)).volume().getValue());
        assertEquals(FILLED, ((Order) result.get(3)).orderOperationCause());
        assertEquals(FILLED, ((Order) result.get(4)).orderOperationCause());
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
        assertEquals(FILLED, ((Order) result.get(0)).orderOperationCause());
    }

    @Test
    void test_matching_algorithm_limit_fill_and_kill_killed() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, ASK, "4"));

        var order = buildOrderAdd(3, 100.5, 20, BID, "5", TimeInForceEnum.FAK, LIMIT);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(1, result.size());
        assertEquals(KILLED, ((Order) result.get(0)).orderOperationCause());
    }

    @Test
    void test_matching_algorithm_limit_fill_and_kill_partial_filled() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, ASK, "4"));

        var order = buildOrderAdd(3, 102, 20, BID, "5", TimeInForceEnum.FAK, LIMIT);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(6, result.size());
        assertEquals(FILLED, ((Order) result.get(0)).orderOperationCause());
        assertEquals(PARTIAL_FILL, ((Order) result.get(1)).orderOperationCause());
        assertEquals(10, ((Trade) result.get(2)).volume().getValue());
        assertEquals(FILLED, ((Order) result.get(3)).orderOperationCause());
        assertEquals(FILLED, ((Order) result.get(4)).orderOperationCause());
        assertEquals(10, ((Trade) result.get(5)).volume().getValue());
    }

    @Test
    void test_matching_algorithm_limit_fill_and_kill_filled() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, ASK, "4"));

        var order = buildOrderAdd(3, 101, 10, BID, "5", TimeInForceEnum.FAK, LIMIT);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(3, result.size());
        assertEquals(FILLED, ((Order) result.get(0)).orderOperationCause());
    }

    @Test
    void test_matching_algorithm_market_fill_killed() {
        var order = buildOrderAdd(3, 100.5, 10, BID, "5", SESSION, MARKET);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(1, result.size());
        assertEquals(KILLED, ((Order) result.get(0)).orderOperationCause());
    }

    @Test
    void test_matching_algorithm_market_fill_partial_filled() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, ASK, "4"));

        var order = buildOrderAdd(3, Integer.MAX_VALUE, 100, BID, "5", SESSION, MARKET);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(7, result.size());
        assertEquals(FILLED, ((Order) result.get(0)).orderOperationCause());
        assertEquals(PARTIAL_FILL, ((Order) result.get(1)).orderOperationCause());
        assertEquals(10, ((Trade) result.get(2)).volume().getValue());
        assertEquals(FILLED, ((Order) result.get(3)).orderOperationCause());
        assertEquals(PARTIAL_FILL, ((Order) result.get(4)).orderOperationCause());
        assertEquals(10, ((Trade) result.get(5)).volume().getValue());
        assertEquals(KILLED, ((Order) result.get(6)).orderOperationCause());
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

        var order = buildOrderAdd(3, Integer.MAX_VALUE, 10, BID, "5", SESSION, MARKET);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(3, result.size());
        assertEquals(FILLED, ((Order) result.get(0)).orderOperationCause());
    }

    @Test
    void test_matching_algorithm_limit_fill_or_kill_killed() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, ASK, "4"));

        var order = buildOrderAdd(3, Integer.MAX_VALUE, 100, BID, "5", FOK, LIMIT);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(1, result.size());
        assertEquals(KILLED, ((Order) result.get(0)).orderOperationCause());
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
        assertEquals(FILLED, ((Order) result.get(0)).orderOperationCause());
        assertEquals(PARTIAL_FILL, ((Order) result.get(1)).orderOperationCause());
        assertEquals(10, ((Trade) result.get(5)).volume().getValue());
        assertEquals(FILLED, ((Order) result.get(0)).orderOperationCause());
        assertEquals(PARTIAL_FILL, ((Order) result.get(1)).orderOperationCause());
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
        assertEquals(FILLED, ((Order) result.get(0)).orderOperationCause());
    }

    @Test
    void test_matching_algorithm_market_fill_and_kill_killed() {
        var order = buildOrderAdd(3, Integer.MAX_VALUE, 100, BID, "5", TimeInForceEnum.FAK, MARKET);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(1, result.size());
        assertEquals(KILLED, ((Order) result.get(0)).orderOperationCause());
    }

    @Test
    void test_matching_algorithm_market_fill_and_kill_partial_fill() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, ASK, "4"));

        var order = buildOrderAdd(3, Integer.MAX_VALUE, 20, BID, "5", TimeInForceEnum.FAK, MARKET);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(6, result.size());
        assertEquals(FILLED, ((Order) result.get(0)).orderOperationCause());
        assertEquals(PARTIAL_FILL, ((Order) result.get(1)).orderOperationCause());
        assertEquals(10, ((Trade) result.get(2)).volume().getValue());
        assertEquals(FILLED, ((Order) result.get(3)).orderOperationCause());
        assertEquals(FILLED, ((Order) result.get(4)).orderOperationCause());
        assertEquals(10, ((Trade) result.get(5)).volume().getValue());
    }

    @Test
    void test_matching_algorithm_market_fill_and_kill_filled() {
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(0, 100, 10, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(2, 101, 10, ASK, "3"));
        orderbook.updateOrderbook(buildOrderAdd(2, 102, 10, ASK, "4"));

        var order = buildOrderAdd(3, Integer.MAX_VALUE, 10, BID, "5", TimeInForceEnum.FAK, MARKET);
        var result = orderbook.runMatchingAlgorithm(order).messages();
        assertEquals(3, result.size());
        assertEquals(FILLED, ((Order) result.get(0)).orderOperationCause());
        assertEquals(FILLED, ((Order) result.get(1)).orderOperationCause());
        assertEquals(10, ((Trade) result.get(2)).volume().getValue());
    }

    @Test
    void test_auction_matching() {
        orderbook.updateOrderbook(buildOrderAdd(899, 32.00, 2, BID, "1"));
        orderbook.updateOrderbook(buildOrderAdd(900, 32.00, 1, BID, "2"));
        orderbook.updateOrderbook(buildOrderAdd(911, 32.00, 8, BID, "3"));
        orderbook.updateOrderbook(buildOrderAdd(902, 31.90, 6, BID, "4"));
        orderbook.updateOrderbook(buildOrderAdd(910, 31.90, 3, BID, "5"));
        orderbook.updateOrderbook(buildOrderAdd(914, 31.90, 2, BID, "6"));
        orderbook.updateOrderbook(buildOrderAdd(913, 31.80, 2, BID, "7"));

        orderbook.updateOrderbook(buildOrderAdd(901, 31.90, 2, ASK, "8"));
        orderbook.updateOrderbook(buildOrderAdd(910, 31.90, 8, ASK, "9"));
        orderbook.updateOrderbook(buildOrderAdd(905, 32.00, 10, ASK, "10"));
        orderbook.updateOrderbook(buildOrderAdd(913, 32.00, 4, ASK, "11"));
        orderbook.updateOrderbook(buildOrderAdd(914, 32.00, 2, ASK, "12"));
        orderbook.updateOrderbook(buildOrderAdd(912, 32.10, 6, ASK, "13"));
        orderbook.updateOrderbook(buildOrderAdd(913, 32.10, 2, ASK, "14"));
        orderbook.updateOrderbook(buildOrderAdd(901, 32.20, 4, ASK, "15"));
        orderbook.updateOrderbook(buildOrderAdd(908, 32.20, 2, ASK, "16"));
        orderbook.updateOrderbook(buildOrderAdd(912, 32.20, 1, ASK, "17"));

        orderbook.updateState(CLOSING_AUCTION_TRADING);
        orderbook.updateState(CLOSING_AUCTION_RUN);
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
