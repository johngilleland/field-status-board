package com.fieldstatus;

import com.ditto.java.Ditto;
import com.ditto.java.DittoException;
import com.ditto.java.DittoQueryResult;
import com.ditto.java.DittoStore;
import com.ditto.java.serialization.DittoCborSerializable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class UnitStatusRepository {

    private static final String UPSERT =
        "INSERT INTO units DOCUMENTS (:doc) ON ID CONFLICT DO UPDATE";
    private static final String FIND_ACTIVE = 
        "SELECT * FROM units";
    private static final String FIND_BY_ID = 
        "SELECT * FROM units WHERE _id = :id";

    private final DittoStore store;

    public UnitStatusRepository(Ditto ditto) {
        this.store = ditto.getStore();
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