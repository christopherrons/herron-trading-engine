package com.herron.exchange.tradingengine.server.matchingengine;

import com.herron.exchange.common.api.common.enums.*;
import com.herron.exchange.common.api.common.messages.HerronOrderbookData;
import com.herron.exchange.common.api.common.messages.HerronStateChange;
import com.herron.exchange.common.api.common.messages.HerronStockInstrument;
import com.herron.exchange.common.api.common.model.PartitionKey;
import com.herron.exchange.common.api.common.request.HerronInstrumentRequest;
import com.herron.exchange.common.api.common.request.HerronOrderRequest;
import com.herron.exchange.common.api.common.request.HerronOrderbookDataRequest;
import com.herron.exchange.common.api.common.request.HerronStateChangeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.herron.exchange.tradingengine.server.matchingengine.utils.MessageCreatorTestUtils.buildOrderCreate;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchingEngineTest {

    private MatchingEngine matchingEngine;

    @BeforeEach
    void init() {
        matchingEngine = new MatchingEngine(new PartitionKey(KafkaTopicEnum.HERRON_AUDIT_TRAIL, 1), null);
    }

    @Test
    void test_handle_orderbook_data_status_request_ok() {
        var orderbookData = new HerronOrderbookData("1", "1", MatchingAlgorithmEnum.PRO_RATA, "eur", 0, 1, AuctionAlgorithmEnum.DUTCH);
        var request = new HerronOrderbookDataRequest(1, orderbookData);
        assertEquals(RequestStatus.OK, matchingEngine.handleMessage(request).requestStatus());
    }

    @Test
    void test_handle_orderbook_data_status_request_error_invalid_matching_algo() {
        var orderbookData = new HerronOrderbookData("1", "1", MatchingAlgorithmEnum.INVALID_MATCHING_ALGORITHM, "eur", 0, 1, AuctionAlgorithmEnum.DUTCH);
        var request = new HerronOrderbookDataRequest(1, orderbookData);
        assertEquals(RequestStatus.ERROR, matchingEngine.handleMessage(request).requestStatus());
    }

    @Test
    void test_handle_instrument_status_request_ok() {
        var instrument = new HerronStockInstrument("1", InstrumentTypeEnum.STOCK, 1);
        var request = new HerronInstrumentRequest(1, instrument);
        assertEquals(RequestStatus.OK, matchingEngine.handleMessage(request).requestStatus());
    }

    @Test
    void test_handle_stateChange_request_ok() {
        var orderbookData = new HerronOrderbookData("1", "1", MatchingAlgorithmEnum.PRO_RATA, "eur", 0, 1, AuctionAlgorithmEnum.DUTCH);
        var orderbookDataRequest = new HerronOrderbookDataRequest(1, orderbookData);
        matchingEngine.handleMessage(orderbookDataRequest);

        var stateChange = new HerronStateChange("1", StateChangeTypeEnum.PRE_TRADE, 1);
        var stateChangeRequest = new HerronStateChangeRequest(1, stateChange);
        assertEquals(RequestStatus.OK, matchingEngine.handleMessage(stateChangeRequest).requestStatus());
    }

    @Test
    void test_handle_stateChange_request_ok_multiple_changes() {
        var orderbookData = new HerronOrderbookData("1", "1", MatchingAlgorithmEnum.PRO_RATA, "eur", 0, 1, AuctionAlgorithmEnum.DUTCH);
        var orderbookDataRequest = new HerronOrderbookDataRequest(1, orderbookData);
        matchingEngine.handleMessage(orderbookDataRequest);

        var stateChange = new HerronStateChange("1", StateChangeTypeEnum.PRE_TRADE, 1);
        var stateChangeRequest = new HerronStateChangeRequest(1, stateChange);
        assertEquals(RequestStatus.OK, matchingEngine.handleMessage(stateChangeRequest).requestStatus());
        waitForEmptyQueue();

        var stateChange2 = new HerronStateChange("1", StateChangeTypeEnum.AUCTION_TRADING, 1);
        var stateChangeRequest2 = new HerronStateChangeRequest(1, stateChange2);
        assertEquals(RequestStatus.OK, matchingEngine.handleMessage(stateChangeRequest2).requestStatus());
        waitForEmptyQueue();

        var stateChange3 = new HerronStateChange("1", StateChangeTypeEnum.TRADE_STOP, 1);
        var stateChangeRequest3 = new HerronStateChangeRequest(1, stateChange3);
        assertEquals(RequestStatus.OK, matchingEngine.handleMessage(stateChangeRequest3).requestStatus());
    }

    @Test
    void test_handle_stateChange_request_error_missing_orderbook() {
        var stateChange = new HerronStateChange("1", StateChangeTypeEnum.AUCTION_RUN, 1);
        var request = new HerronStateChangeRequest(1, stateChange);
        assertEquals(RequestStatus.ERROR, matchingEngine.handleMessage(request).requestStatus());
    }

    @Test
    void test_handle_stateChange_request_error_invalid_changes() {
        var orderbookData = new HerronOrderbookData("1", "1", MatchingAlgorithmEnum.PRO_RATA, "eur", 0, 1, AuctionAlgorithmEnum.DUTCH);
        var orderbookDataRequest = new HerronOrderbookDataRequest(1, orderbookData);
        matchingEngine.handleMessage(orderbookDataRequest);

        var stateChange = new HerronStateChange("1", StateChangeTypeEnum.PRE_TRADE, 1);
        var stateChangeRequest = new HerronStateChangeRequest(1, stateChange);
        assertEquals(RequestStatus.OK, matchingEngine.handleMessage(stateChangeRequest).requestStatus());
        waitForEmptyQueue();

        var stateChange2 = new HerronStateChange("1", StateChangeTypeEnum.AUCTION_TRADING, 1);
        var stateChangeRequest2 = new HerronStateChangeRequest(1, stateChange2);
        assertEquals(RequestStatus.OK, matchingEngine.handleMessage(stateChangeRequest2).requestStatus());
        waitForEmptyQueue();

        var stateChange3 = new HerronStateChange("1", StateChangeTypeEnum.TRADE_STOP, 1);
        var stateChangeRequest3 = new HerronStateChangeRequest(1, stateChange3);
        assertEquals(RequestStatus.OK, matchingEngine.handleMessage(stateChangeRequest3).requestStatus());
        waitForEmptyQueue();

        var stateChange4 = new HerronStateChange("1", StateChangeTypeEnum.CONTINUOUS_TRADING, 1);
        var stateChangeRequest4 = new HerronStateChangeRequest(1, stateChange4);
        assertEquals(RequestStatus.ERROR, matchingEngine.handleMessage(stateChangeRequest4).requestStatus());
    }

    @Test
    void test_handle_order_request_ok() {
        var orderbookData = new HerronOrderbookData("orderbookId", "1", MatchingAlgorithmEnum.PRO_RATA, "eur", 0, 1, AuctionAlgorithmEnum.DUTCH);
        var orderbookDataRequest = new HerronOrderbookDataRequest(1, orderbookData);
        matchingEngine.handleMessage(orderbookDataRequest);

        var stateChange = new HerronStateChange("orderbookId", StateChangeTypeEnum.PRE_TRADE, 1);
        var stateChangeRequest = new HerronStateChangeRequest(1, stateChange);
        matchingEngine.handleMessage(stateChangeRequest);
        waitForEmptyQueue();

        var order = buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "1");
        var orderRequest = new HerronOrderRequest(1, order);

        assertEquals(RequestStatus.OK, matchingEngine.handleMessage(orderRequest).requestStatus());
        assertEquals(0, matchingEngine.getOrderQueueSize());
    }

    @Test
    void test_handle_order_request_error_closed_state() {
        var orderbookData = new HerronOrderbookData("orderbookId", "1", MatchingAlgorithmEnum.PRO_RATA, "eur", 0, 1, AuctionAlgorithmEnum.DUTCH);
        var orderbookDataRequest = new HerronOrderbookDataRequest(1, orderbookData);
        matchingEngine.handleMessage(orderbookDataRequest);

        var order = buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "1");
        var orderRequest = new HerronOrderRequest(1, order);
        assertEquals(RequestStatus.ERROR, matchingEngine.handleMessage(orderRequest).requestStatus());
    }

    @Test
    void test_handle_order_request_error_trade_stop() {
        var orderbookData = new HerronOrderbookData("orderbookId", "1", MatchingAlgorithmEnum.PRO_RATA, "eur", 0, 1, AuctionAlgorithmEnum.DUTCH);
        var orderbookDataRequest = new HerronOrderbookDataRequest(1, orderbookData);
        matchingEngine.handleMessage(orderbookDataRequest);

        var stateChange = new HerronStateChange("orderbookId", StateChangeTypeEnum.PRE_TRADE, 1);
        var stateChangeRequest = new HerronStateChangeRequest(1, stateChange);
        matchingEngine.handleMessage(stateChangeRequest);
        waitForEmptyQueue();

        var order = buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "1");
        var orderRequest = new HerronOrderRequest(1, order);
        matchingEngine.handleMessage(orderRequest);
        waitForEmptyQueue();

        var stateChange2 = new HerronStateChange("orderbookId", StateChangeTypeEnum.TRADE_STOP, 1);
        var stateChangeRequest2 = new HerronStateChangeRequest(1, stateChange2);
        matchingEngine.handleMessage(stateChangeRequest2);
        waitForEmptyQueue();

        var order2 = buildOrderCreate(0, 100, 10, OrderSideEnum.BID, "2");
        var orderRequest2 = new HerronOrderRequest(1, order2);
        assertEquals(RequestStatus.ERROR, matchingEngine.handleMessage(orderRequest2).requestStatus());
        assertEquals(0, matchingEngine.getOrderQueueSize());
    }

    private void waitForEmptyQueue() {
        // State changes are not added in microsecond timeframe during run time, only in unit tests
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignore) {

        }
        while (matchingEngine.getOrderQueueSize() != 0) {

        }
    }
}