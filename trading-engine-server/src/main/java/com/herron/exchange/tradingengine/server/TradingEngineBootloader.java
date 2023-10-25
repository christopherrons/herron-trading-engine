package com.herron.exchange.tradingengine.server;

import com.herron.exchange.common.api.common.bootloader.Bootloader;
import com.herron.exchange.tradingengine.server.consumers.ReferenceDataConsumer;
import com.herron.exchange.tradingengine.server.consumers.UserOrderDataConsumer;
import com.herron.exchange.tradingengine.server.matchingengine.StateChangeOrchestrator;

public class TradingEngineBootloader extends Bootloader {

    private final ReferenceDataConsumer referenceDataConsumer;
    private final StateChangeOrchestrator stateChangeOrchestrator;
    private final UserOrderDataConsumer userOrderDataConsumer;

    public TradingEngineBootloader(ReferenceDataConsumer referenceDataConsumer,
                                   StateChangeOrchestrator stateChangeOrchestrator,
                                   UserOrderDataConsumer userOrderDataConsumer) {
        super("Trading-Engine");
        this.referenceDataConsumer = referenceDataConsumer;
        this.stateChangeOrchestrator = stateChangeOrchestrator;
        this.userOrderDataConsumer = userOrderDataConsumer;
    }

    @Override
    protected void bootloaderInit() {
        referenceDataConsumer.consumerInit();
        referenceDataConsumer.await();
        stateChangeOrchestrator.scheduleStateChanges();
        userOrderDataConsumer.init();
        bootloaderComplete();
    }
}
