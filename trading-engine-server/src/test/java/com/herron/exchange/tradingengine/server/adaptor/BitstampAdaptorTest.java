package com.herron.exchange.tradingengine.server.adaptor;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.enums.MessageTypesEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BitstampAdaptorTest {

    @Test
    void test_order_mapping() {
        String messageJson = "{\"orderOperation\":\"CREATE\",\"participant\":\"Bitstamp;Izzy BackyetHauck\",\"orderId\":\"1570769173954560\",\"orderType\":0,\"initialVolume\":3171.97089843,\"currentVolume\":3171.97089843,\"price\":0.34566,\"timeStampInMs\":1672323542518,\"instrumentId\":\"stock_live_orders_xrpusd\",\"orderbookId\":\"bitstamp_stock_live_orders_xrpusd\",\"id\":\"bitstamp_stock_live_orders_xrpusd\",\"eventTypeEnum\":\"ORDER\",\"messageType\":\"BSOR\",\"timeStampMs\":1672323542518}";
        Message message = MessageTypesEnum.decodeMessage(MessageTypesEnum.BITSTAMP_ADD_ORDER.getMessageTypeId(), messageJson);
        Assertions.assertNotNull(message);
    }
}