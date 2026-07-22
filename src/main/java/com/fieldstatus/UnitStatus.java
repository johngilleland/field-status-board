package com.fieldstatus;

import com.ditto.java.DittoException;
import com.ditto.java.serialization.DittoCborSerializable;

public class UnitStatus {

    private final String callsign;
    private final String status;
    private final long lastTelemetryTick;

    public UnitStatus(String callsign, String status, long lastTelemetryTick) {
        this.callsign = callsign;
        this.status = status;
        this.lastTelemetryTick = lastTelemetryTick;
    }

    public String getCallsign() {
        return callsign;
    }

    public String getStatus() {
        return status;
    }

    public long getLastTelemetryTick() {
        return lastTelemetryTick;
    }

    public static String documentIdFor(String callsign) {
        return callsign.toLowerCase().replace(" ", "-");
    }

    public DittoCborSerializable.Dictionary toDocument() {
        return DittoCborSerializable.buildDictionary()
            .put("_id", documentIdFor(callsign))
            .put("callsign", callsign)
            .put("status", status)
            .put("lastTelemetryTick", lastTelemetryTick)
            .build();
    }

    public static UnitStatus fromDocument(DittoCborSerializable.Dictionary doc) throws DittoException {
        String callsign = doc.get("callsign").asString();
        String status = doc.get("status").asString();
        long lastTelemetryTick = doc.get("lastTelemetryTick").asLong();
        return new UnitStatus(callsign, status, lastTelemetryTick);
    }

    @Override
    public String toString() {
        return "UnitStatus{callsign='" + callsign + "', status='" + status + "', lastTelemetryTick=" + lastTelemetryTick + "}";
    }
}