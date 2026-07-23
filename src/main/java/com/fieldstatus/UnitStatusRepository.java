package com.fieldstatus;

import com.ditto.java.Ditto;
import com.ditto.java.DittoException;
import com.ditto.java.DittoQueryResult;
import com.ditto.java.DittoStore;
import com.ditto.java.serialization.DittoCborSerializable;
import com.ditto.java.DittoStoreObserver;
import com.ditto.java.DittoSync;
import com.ditto.java.DittoSyncSubscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public class UnitStatusRepository {

    private static final String UPSERT =
        "INSERT INTO units DOCUMENTS (:doc) ON ID CONFLICT DO UPDATE";
    private static final String FIND_ACTIVE = 
        "SELECT * FROM units WHERE NOT deleted";
    private static final String FIND_BY_ID = 
        "SELECT * FROM units WHERE _id = :id";
    private static final String SET_STATUS = 
        "UPDATE units SET status = :status WHERE _id = :id";
    private static final String TICK = 
        "UPDATE units SET lastTelemetryTick = :tick WHERE _id = :id";
    private static final String TOMBSTONE = 
        "UPDATE units SET deleted = true WHERE _id = :id";
    private static final String EVICT = 
        "EVICT FROM units WHERE _id = :id";

    private final DittoStore store;
    private final DittoSync sync;

    public UnitStatusRepository(Ditto ditto) {
        this.store = ditto.getStore();
        this.sync = ditto.getSync();
    }

    public DittoSyncSubscription subscribeActive() throws DittoException {
        return sync.registerSubscription(FIND_ACTIVE);
    }

    public CompletionStage<Void> upsert(UnitStatus unit) {
        DittoCborSerializable.Dictionary args = DittoCborSerializable.buildDictionary()
            .put("doc", unit.toDocument())
            .build();
        return store.execute(UPSERT, args);
    }

    public CompletionStage<List<UnitStatus>> findActive() {
        return store.executeRaw(FIND_ACTIVE)
            .thenApply(this::mapResults);
    }

    public CompletionStage<Optional<UnitStatus>> findById(String id) {
        DittoCborSerializable.Dictionary args = DittoCborSerializable.buildDictionary()
            .put("id", id)
            .build();
        return store.executeRaw(FIND_BY_ID, args)
            .thenApply(result -> mapResults(result).stream().findFirst());
    }

    public CompletionStage<Void> setStatus(String id, String status) {
        DittoCborSerializable.Dictionary args = DittoCborSerializable.buildDictionary()
            .put("status", status)
            .put("id", id)
            .build();
        return store.execute(SET_STATUS, args);
    }

    public CompletionStage<Void> tick(String id, long timestamp) {
        DittoCborSerializable.Dictionary args = DittoCborSerializable.buildDictionary()
            .put("tick", timestamp)
            .put("id", id)
            .build();
        return store.execute(TICK, args);
    }

    public CompletionStage<Void> tombstone(String id) {
        DittoCborSerializable.Dictionary args = DittoCborSerializable.buildDictionary()
            .put("id", id)
            .build();
        return store.execute(TOMBSTONE, args);
    }

    public CompletionStage<Void> evict(String id) {
        DittoCborSerializable.Dictionary args = DittoCborSerializable.buildDictionary()
            .put("id", id)
            .build();
        return store.execute(EVICT, args);
    }

    public DittoStoreObserver observerActive(Consumer<List<UnitStatus>> onChange) throws DittoException {
        return store.registerObserver(FIND_ACTIVE, result -> onChange.accept(mapResults(result)));
    }

    private List<UnitStatus> mapResults(DittoQueryResult result) {
        List<UnitStatus> units = new ArrayList<>();
        for (var item : result.getItems()) {
            try {
                units.add(UnitStatus.fromDocument(item.getValue()));
            } catch (DittoException e) {
                throw new RuntimeException("Failed to map document", e);
            }
        }
        return units;
    }
}