package com.herron.exchange.tradingengine.server.matchingengine;

import com.herron.exchange.common.api.common.api.CancelOrder;
import com.herron.exchange.common.api.common.api.Trade;
import com.herron.exchange.common.api.common.api.UpdateOrder;
import com.herron.exchange.common.api.common.enums.*;
import com.herron.exchange.common.api.common.messages.herron.HerronOrderbookData;
import com.herron.exchange.common.api.common.messages.herron.HerronStateChange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.herron.exchange.tradingengine.server.matchingengine.utils.EventCreatorTestUtils.buildOrder;
import static com.herron.exchange.tradingengine.server.matchingengine.utils.EventCreatorTestUtils.buildOrderCreate;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchingEngineTest {

    private final MatchingEngine matchingEngine = new MatchingEngine();

    @BeforeEach
    void init() {
        var orderbookDate = new HerronOrderbookData("orderbookId", "instrumentId", MatchingAlgorithmEnum.FIFO, 0);
        var stateChange = new HerronStateChange("orderbookId", StateChangeTypeEnum.CONTINUOUS_TRADING, 0);
        matchingEngine.add(orderbookDate);
        matchingEngine.add(stateChange);
    }

    @Test
    void test_matching_algorithm_limit_fill() {
        matchingEngine.add(buildOrderCreate(1, 99, 10, OrderSideEnum.BID, "1"));
        matchingEngine.add(buildOrderCreate(2, 100, 10, OrderSideEnum.BID, "2"));
        matchingEngine.add(buildOrderCreate(3, 100, 10, OrderSideEnum.BID, "3"));
        matchingEngine.add(buildOrderCreate(4, 101, 10, OrderSideEnum.ASK, "4"));
        matchingEngine.add(buildOrderCreate(5, 102, 10, OrderSideEnum.ASK, "5"));

        var order = buildOrder(6, 102, 20, OrderSideEnum.BID, "6", OrderExecutionTypeEnum.FILL, OrderTypeEnum.LIMIT);
        matchingEngine.add(order);
        var result = matchingEngine.runMatchingAlgorithm(order);
        assertEquals(6, result.size());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(0)).cancelOperationType());
        assertEquals(OrderUpdatedOperationTypeEnum.PARTIAL_FILL, ((UpdateOrder) result.get(1)).updateOperationType());
        assertEquals("6", ((Trade) result.get(2)).buyOrderId());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(3)).cancelOperationType());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(4)).cancelOperationType());
        assertEquals("6", ((Trade) result.get(5)).buyOrderId());
    }

    @Test
    void test_matching_algorithm_limit_fok() {
        matchingEngine.add(buildOrderCreate(1, 99, 10, OrderSideEnum.BID, "1"));
        matchingEngine.add(buildOrderCreate(2, 100, 10, OrderSideEnum.BID, "2"));
        matchingEngine.add(buildOrderCreate(3, 100, 10, OrderSideEnum.BID, "3"));
        matchingEngine.add(buildOrderCreate(4, 101, 10, OrderSideEnum.ASK, "4"));
        matchingEngine.add(buildOrderCreate(5, 102, 10, OrderSideEnum.ASK, "5"));

        var order = buildOrder(6, 102, 15, OrderSideEnum.BID, "6", OrderExecutionTypeEnum.FOK, OrderTypeEnum.LIMIT);
        matchingEngine.add(order);
        var result = matchingEngine.runMatchingAlgorithm(order);
        assertEquals(6, result.size());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(0)).cancelOperationType());
        assertEquals(OrderUpdatedOperationTypeEnum.PARTIAL_FILL, ((UpdateOrder) result.get(1)).updateOperationType());
        assertEquals(10, ((Trade) result.get(2)).volume());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(3)).cancelOperationType());
        assertEquals(OrderUpdatedOperationTypeEnum.PARTIAL_FILL, ((UpdateOrder) result.get(4)).updateOperationType());
        assertEquals(5, ((Trade) result.get(5)).volume());
    }

    @Test
    void test_matching_algorithm_limit_fak() {
        matchingEngine.add(buildOrderCreate(1, 99, 10, OrderSideEnum.BID, "1"));
        matchingEngine.add(buildOrderCreate(2, 100, 10, OrderSideEnum.BID, "2"));
        matchingEngine.add(buildOrderCreate(3, 100, 10, OrderSideEnum.BID, "3"));
        matchingEngine.add(buildOrderCreate(4, 101, 10, OrderSideEnum.ASK, "4"));
        matchingEngine.add(buildOrderCreate(5, 102, 10, OrderSideEnum.ASK, "5"));

        var order = buildOrder(6, 102, 25, OrderSideEnum.BID, "6", OrderExecutionTypeEnum.FAK, OrderTypeEnum.LIMIT);
        matchingEngine.add(order);
        var result = matchingEngine.runMatchingAlgorithm(order);
        assertEquals(7, result.size());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(0)).cancelOperationType());
        assertEquals(OrderUpdatedOperationTypeEnum.PARTIAL_FILL, ((UpdateOrder) result.get(1)).updateOperationType());
        assertEquals(10, ((Trade) result.get(2)).volume());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(3)).cancelOperationType());
        assertEquals(OrderUpdatedOperationTypeEnum.PARTIAL_FILL, ((UpdateOrder) result.get(4)).updateOperationType());
        assertEquals(10, ((Trade) result.get(5)).volume());
        assertEquals(OrderCancelOperationTypeEnum.KILLED, ((CancelOrder) result.get(6)).cancelOperationType());
    }

    @Test
    void test_matching_algorithm_market_fill() {
        matchingEngine.add(buildOrderCreate(1, 99, 10, OrderSideEnum.BID, "1"));
        matchingEngine.add(buildOrderCreate(2, 100, 10, OrderSideEnum.BID, "2"));
        matchingEngine.add(buildOrderCreate(3, 100, 10, OrderSideEnum.BID, "3"));
        matchingEngine.add(buildOrderCreate(4, 101, 10, OrderSideEnum.ASK, "4"));
        matchingEngine.add(buildOrderCreate(5, 102, 10, OrderSideEnum.ASK, "5"));

        var order = buildOrder(6, Integer.MAX_VALUE, 15, OrderSideEnum.BID, "6", OrderExecutionTypeEnum.FILL, OrderTypeEnum.MARKET);
        matchingEngine.add(order);
        var result = matchingEngine.runMatchingAlgorithm(order);
        assertEquals(6, result.size());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(0)).cancelOperationType());
        assertEquals(OrderUpdatedOperationTypeEnum.PARTIAL_FILL, ((UpdateOrder) result.get(1)).updateOperationType());
        assertEquals(10, ((Trade) result.get(2)).volume());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(3)).cancelOperationType());
        assertEquals(OrderUpdatedOperationTypeEnum.PARTIAL_FILL, ((UpdateOrder) result.get(4)).updateOperationType());
        assertEquals(5, ((Trade) result.get(5)).volume());
    }

    @Test
    void test_matching_algorithm_market_fok() {
        matchingEngine.add(buildOrderCreate(1, 99, 10, OrderSideEnum.BID, "1"));
        matchingEngine.add(buildOrderCreate(2, 100, 10, OrderSideEnum.BID, "2"));
        matchingEngine.add(buildOrderCreate(3, 100, 10, OrderSideEnum.BID, "3"));
        matchingEngine.add(buildOrderCreate(4, 101, 10, OrderSideEnum.ASK, "4"));
        matchingEngine.add(buildOrderCreate(5, 102, 10, OrderSideEnum.ASK, "5"));

        var order = buildOrder(6, Integer.MAX_VALUE, 15, OrderSideEnum.BID, "6", OrderExecutionTypeEnum.FOK, OrderTypeEnum.MARKET);
        matchingEngine.add(order);
        var result = matchingEngine.runMatchingAlgorithm(order);
        assertEquals(6, result.size());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(0)).cancelOperationType());
        assertEquals(OrderUpdatedOperationTypeEnum.PARTIAL_FILL, ((UpdateOrder) result.get(1)).updateOperationType());
        assertEquals(10, ((Trade) result.get(2)).volume());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(3)).cancelOperationType());
        assertEquals(OrderUpdatedOperationTypeEnum.PARTIAL_FILL, ((UpdateOrder) result.get(4)).updateOperationType());
        assertEquals(5, ((Trade) result.get(5)).volume());
    }

    @Test
    void test_matching_algorithm_market_fak() {
        matchingEngine.add(buildOrderCreate(1, 99, 10, OrderSideEnum.BID, "1"));
        matchingEngine.add(buildOrderCreate(2, 100, 10, OrderSideEnum.BID, "2"));
        matchingEngine.add(buildOrderCreate(3, 100, 10, OrderSideEnum.BID, "3"));
        matchingEngine.add(buildOrderCreate(4, 101, 10, OrderSideEnum.ASK, "4"));
        matchingEngine.add(buildOrderCreate(5, 102, 10, OrderSideEnum.ASK, "5"));

        var order = buildOrder(6, Integer.MAX_VALUE, 15, OrderSideEnum.BID, "6", OrderExecutionTypeEnum.FAK, OrderTypeEnum.MARKET);
        matchingEngine.add(order);
        var result = matchingEngine.runMatchingAlgorithm(order);
        assertEquals(6, result.size());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(0)).cancelOperationType());
        assertEquals(OrderUpdatedOperationTypeEnum.PARTIAL_FILL, ((UpdateOrder) result.get(1)).updateOperationType());
        assertEquals(10, ((Trade) result.get(2)).volume());
        assertEquals(OrderCancelOperationTypeEnum.FILLED, ((CancelOrder) result.get(3)).cancelOperationType());
        assertEquals(OrderUpdatedOperationTypeEnum.PARTIAL_FILL, ((UpdateOrder) result.get(4)).updateOperationType());
        assertEquals(5, ((Trade) result.get(5)).volume());

    }


}