package com.herron.exchange.tradingengine.server.matchingengine.orderbook;

import com.herron.exchange.common.api.common.api.referencedata.orderbook.OrderbookData;
import com.herron.exchange.common.api.common.api.trading.Order;
import com.herron.exchange.common.api.common.api.trading.OrderbookEvent;
import com.herron.exchange.common.api.common.enums.MatchingAlgorithmEnum;
import com.herron.exchange.common.api.common.enums.OrderOperationEnum;
import com.herron.exchange.common.api.common.enums.TradingStatesEnum;
import com.herron.exchange.common.api.common.locks.LockHandler;
import com.herron.exchange.common.api.common.messages.common.Price;
import com.herron.exchange.common.api.common.messages.common.Timestamp;
import com.herron.exchange.common.api.common.messages.common.Volume;
import com.herron.exchange.common.api.common.messages.trading.*;
import com.herron.exchange.tradingengine.server.matchingengine.api.AuctionAlgorithm;
import com.herron.exchange.tradingengine.server.matchingengine.api.MatchingAlgorithm;
import com.herron.exchange.tradingengine.server.matchingengine.api.Orderbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.herron.exchange.common.api.common.enums.EventType.SYSTEM;
import static com.herron.exchange.common.api.common.enums.QuoteTypeEnum.*;
import static com.herron.exchange.common.api.common.enums.TradingStatesEnum.*;


public class OrderbookImpl implements Orderbook {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderbookImpl.class);
    private final OrderbookData orderbookData;
    private final ActiveOrders activeOrders;
    private final MatchingAlgorithm matchingAlgorithm;
    private final AuctionAlgorithm auctionAlgorithm;
    private final AtomicReference<PriceQuote> latestPrice = new AtomicReference<>();
    private final LockHandler lock = new LockHandler();
    private TradingStatesEnum currentState = CLOSED;

    public OrderbookImpl(OrderbookData orderbookData,
                         ActiveOrders activeOrders,
                         MatchingAlgorithm matchingAlgorithm,
                         AuctionAlgorithm auctionAlgorithm) {
        this.orderbookData = orderbookData;
        this.activeOrders = activeOrders;
        this.matchingAlgorithm = matchingAlgorithm;
        this.auctionAlgorithm = auctionAlgorithm;
    }

    @Override
    public synchronized boolean updateOrderbook(Order order) {
        return lock.executeWithWriteLock(() -> {
                    if (!isAccepting()) {
                        LOGGER.error("Is not accepting {}.", currentState);
                        return false;
                    }
                    if (order.isActiveOrder()) {
                        return switch (order.orderOperation()) {
                            case INSERT -> addOrder(order);
                            case UPDATE -> updateOrder(order);
                            case CANCEL -> removeOrder(order);
                        };
                    }
                    return true;
                }
        );
    }

    @Override
    public boolean isAccepting() {
        return lock.executeWithReadLock(() -> {
                    if (currentState == null) {
                        return false;
                    }
                    if (currentState == TRADE_HALT) {
                        return false;
                    }

                    if (currentState == CLOSED) {
                        return false;
                    }
                    return true;
                }
        );
    }

    private boolean updateOrder(Order order) {
        return lock.executeWithWriteLock(() -> activeOrders.updateOrder(order));
    }

    private boolean addOrder(Order order) {
        return lock.executeWithWriteLock(() -> activeOrders.addOrder(order));
    }

    public boolean removeOrder(String orderId) {
        return lock.executeWithWriteLock(() -> activeOrders.removeOrder(orderId));
    }

    private boolean removeOrder(Order order) {
        return lock.executeWithWriteLock(() -> activeOrders.removeOrder(order));
    }

    @Override
    public Optional<Price> getBestBidPrice() {
        return lock.executeWithReadLock(activeOrders::getBestBidPrice);
    }

    @Override
    public Optional<Price> getBestAskPrice() {
        return lock.executeWithReadLock(activeOrders::getBestAskPrice);
    }

    @Override
    public boolean hasBidAndAskOrders() {
        return lock.executeWithReadLock(activeOrders::hasBidAndAskOrders);
    }

    @Override
    public long totalNumberOfBidOrders() {
        return lock.executeWithReadLock(activeOrders::totalNumberOfBidOrders);
    }

    @Override
    public long totalNumberOfAskOrders() {
        return lock.executeWithReadLock(activeOrders::totalNumberOfAskOrders);
    }

    @Override
    public long totalNumberOfActiveOrders() {
        return lock.executeWithReadLock(activeOrders::totalNumberOfActiveOrders);
    }

    @Override
    public Volume totalOrderVolume() {
        return lock.executeWithReadLock(activeOrders::totalOrderVolume);
    }

    @Override
    public Volume totalBidVolume() {
        return lock.executeWithReadLock(activeOrders::totalBidVolume);
    }

    @Override
    public Volume totalAskVolume() {
        return lock.executeWithReadLock(activeOrders::totalAskVolume);
    }

    @Override
    public Volume totalVolumeAtPriceLevel(int priceLevel) {
        return activeOrders.totalVolumeAtPriceLevel(priceLevel);
    }

    @Override
    public Volume totalBidVolumeAtPriceLevel(int priceLevel) {
        return lock.executeWithReadLock(() -> activeOrders.totalBidVolumeAtPriceLevel(priceLevel));
    }

    @Override
    public Volume totalAskVolumeAtPriceLevel(int priceLevel) {
        return lock.executeWithReadLock(() -> activeOrders.totalAskVolumeAtPriceLevel(priceLevel));
    }

    @Override
    public int totalNumberOfPriceLevels() {
        return activeOrders.totalNumberOfPriceLevels();
    }

    @Override
    public int totalNumberOfBidPriceLevels() {
        return lock.executeWithReadLock(activeOrders::totalNumberOfBidPriceLevels);
    }

    @Override
    public int totalNumberOfAskPriceLevels() {
        return lock.executeWithReadLock(activeOrders::totalNumberOfAskPriceLevels);
    }

    @Override
    public Order getOrder(String orderId) {
        return lock.executeWithReadLock(() -> activeOrders.getOrder(orderId));
    }

    @Override
    public MatchingAlgorithmEnum getMatchingAlgorithm() {
        return lock.executeWithReadLock(orderbookData::matchingAlgorithm);
    }

    @Override
    public String getOrderbookId() {
        return lock.executeWithReadLock(orderbookData::orderbookId);
    }

    @Override
    public String getInstrumentId() {
        return lock.executeWithReadLock(() -> orderbookData.instrument().instrumentId());
    }

    @Override
    public Optional<Price> getAskPriceAtPriceLevel(int priceLevel) {
        return lock.executeWithReadLock(() -> activeOrders.getAskPriceAtPriceLevel(priceLevel));
    }

    @Override
    public Optional<Price> getBidPriceAtPriceLevel(int priceLevel) {
        return lock.executeWithReadLock(() -> activeOrders.getBidPriceAtPriceLevel(priceLevel));
    }

    @Override
    public Optional<Order> getBestBidOrder() {
        return lock.executeWithReadLock(activeOrders::getBestBidOrder);
    }

    @Override
    public Optional<Order> getBestAskOrder() {
        return lock.executeWithReadLock(activeOrders::getBestAskOrder);
    }

    @Override
    public TopOfBook getTopOfBook() {
        return lock.executeWithReadLock(() -> {
                    var builder = ImmutableTopOfBook.builder()
                            .orderbookId(getOrderbookId())
                            .timeOfEvent(Timestamp.now())
                            .eventType(SYSTEM);

                    Optional.ofNullable(latestPrice.get()).ifPresent(builder::lastQuote);
                    getBestAskOrder()
                            .map(ao -> ImmutablePriceQuote.builder().orderbookId(getOrderbookId()).price(ao.price()).eventType(ao.eventType()).timeOfEvent(ao.timeOfEvent()).quoteType(ASK_PRICE).build())
                            .ifPresent(builder::askQuote);
                    getBestBidOrder()
                            .map(bo -> ImmutablePriceQuote.builder().orderbookId(getOrderbookId()).price(bo.price()).eventType(bo.eventType()).timeOfEvent(bo.timeOfEvent()).quoteType(BID_PRICE).build())
                            .ifPresent(builder::bidQuote);

                    return builder.build();
                }
        );
    }

    @Override
    public MarketByLevel getMarketByLevel(int nrOfLevels) {
        return lock.executeWithReadLock(() -> {
                    List<MarketByLevel.LevelData> levelData = new ArrayList<>();
                    for (int level = 1; level < nrOfLevels + 1; level++) {
                        if (!activeOrders.doOrdersExistAtLevel(level)) {
                            break;
                        }

                        var builder = ImmutableLevelData.builder().level(level);

                        if (activeOrders.doesBidLevelExist(level)) {
                            activeOrders.getBidPriceAtPriceLevel(level).ifPresent(builder::bidPrice);
                            builder.bidVolume(activeOrders.totalBidVolumeAtPriceLevel(level));
                            builder.nrOfBidOrders(activeOrders.totalNrOfBidOrdersAtPriceLevel(level));
                        }

                        if (activeOrders.doesAskLevelExist(level)) {
                            activeOrders.getAskPriceAtPriceLevel(level).ifPresent(builder::askPrice);
                            builder.askVolume(activeOrders.totalAskVolumeAtPriceLevel(level));
                            builder.nrOfAskOrders(activeOrders.totalNrOfAskOrdersAtPriceLevel(level));
                        }

                        levelData.add(builder.build());

                    }

                    return ImmutableMarketByLevel.builder()
                            .orderbookId(getOrderbookId())
                            .timeOfEvent(Timestamp.now())
                            .levelData(levelData)
                            .eventType(SYSTEM)
                            .build();
                }
        );
    }

    @Override
    public boolean updateState(TradingStatesEnum toState) {
        return lock.executeWithWriteLock(() -> {
                    if (toState == currentState) {
                        return true;
                    }

                    if (currentState == null || currentState.isValidStateChange(toState)) {
                        LOGGER.info("Successfully updated orderbook {} from state {} to state {}.", getOrderbookId(), currentState, toState);
                        currentState = toState;
                        return true;
                    }
                    LOGGER.error("Could not updated orderbook {} from state {} to state {}.", getOrderbookId(), currentState, toState);
                    return false;
                }
        );
    }

    @Override
    public TradingStatesEnum getState() {
        return lock.executeWithReadLock(() -> currentState);
    }

    @Override
    public TradeExecution runMatchingAlgorithm(final Order incomingOrder) {
        return lock.executeWithWriteLock(() -> {
                    if (incomingOrder.currentVolume().leq(0) || currentState != CONTINUOUS_TRADING) {
                        return null;
                    }
                    final List<OrderbookEvent> events = new ArrayList<>();
                    List<OrderbookEvent> matchingEvents;
                    Order updatedMatchingOrder = incomingOrder;
                    do {
                        matchingEvents = matchingAlgorithm.matchOrder(updatedMatchingOrder);
                        for (var message : matchingEvents) {
                            events.add(message);
                            if (message instanceof Order order) {
                                updateOrderbook(order);
                                if (order.orderId().equals(updatedMatchingOrder.orderId())) {
                                    updatedMatchingOrder = order;
                                }
                            } else if (message instanceof Trade trade) {
                                latestPrice.set(ImmutablePriceQuote.builder().orderbookId(getOrderbookId()).price(trade.price()).eventType(SYSTEM).timeOfEvent(trade.timeOfEvent()).quoteType(LAST_PRICE).build());
                            }
                        }
                    } while (!matchingEvents.isEmpty() && updatedMatchingOrder.orderOperation() != OrderOperationEnum.CANCEL);

                    return ImmutableTradeExecution.builder()
                            .timeOfEvent(Timestamp.now())
                            .messages(events)
                            .orderbookId(getOrderbookId())
                            .eventType(SYSTEM)
                            .build();
                }
        );
    }

    @Override
    public TradeExecution runAuctionAlgorithm() {
        return lock.executeWithWriteLock(() -> {
                    if (currentState != OPEN_AUCTION_RUN && currentState != CLOSING_AUCTION_RUN) {
                        LOGGER.error("Attempted auction run triggered in {} event to current state {}. Required state is {}/{} ", getOrderbookId(), currentState, OPEN_AUCTION_RUN, CLOSING_AUCTION_RUN);
                        return null;
                    }

                    var equilibriumPrice = auctionAlgorithm.calculateEquilibriumPrice();
                    if (equilibriumPrice == null) {
                        return null;
                    }

                    latestPrice.set(ImmutablePriceQuote.builder().orderbookId(getOrderbookId()).price(equilibriumPrice.optimalPrice().equilibriumPrice()).eventType(SYSTEM).timeOfEvent(equilibriumPrice.timeOfEvent()).quoteType(LAST_PRICE).build());

                    final List<OrderbookEvent> events = new ArrayList<>();
                    List<OrderbookEvent> matchingEvents;
                    do {
                        matchingEvents = matchingAlgorithm.matchAtPrice(equilibriumPrice.optimalPrice().equilibriumPrice());
                        for (var message : matchingEvents) {
                            events.add(message);
                            if (message instanceof Order order) {
                                updateOrderbook(order);
                            }
                        }
                    } while (!matchingEvents.isEmpty());

                    return ImmutableTradeExecution.builder()
                            .timeOfEvent(Timestamp.now())
                            .messages(events)
                            .orderbookId(getOrderbookId())
                            .eventType(SYSTEM)
                            .build();
                }
        );
    }
}
