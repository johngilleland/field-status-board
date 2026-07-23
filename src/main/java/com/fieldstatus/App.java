package com.fieldstatus;

import com.ditto.java.*;

public class App {
    public static void main(String[] args) throws Exception {
        System.out.println("Field Status Board - hello, world");
        
        DittoService dittoService = new DittoService();
        Thread.sleep(3000);

        UnitStatusRepository repository = new UnitStatusRepository(dittoService.getDitto());

        UnitStatus alpha = new UnitStatus("ALPHA-1", "active", System.currentTimeMillis());
        repository.upsert(alpha).toCompletableFuture().get();

        for (UnitStatus unit : repository.findActive().toCompletableFuture().get()) {
            System.out.println(unit);
        }
    }
}