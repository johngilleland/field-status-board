package com.fieldstatus;

public class App {
    public static void main(String[] args) throws Exception {
        System.out.println("Field Status Board - hello, world");
        
        DittoService dittoService = new DittoService();
        Thread.sleep(3000);

        UnitStatusRepository repository = new UnitStatusRepository(dittoService.getDitto());

        UnitStatus alpha = new UnitStatus("ALPHA-1", "active", System.currentTimeMillis());
        repository.upsert(alpha).toCompletableFuture().get();

        String id = UnitStatus.documentIdFor("ALPHA-1");
        repository.setStatus(id, "degraded").toCompletableFuture().get();
        repository.tick(id, System.currentTimeMillis()).toCompletableFuture().get();

        System.out.println("Active units after targeted updates:");
        for (UnitStatus unit : repository.findActive().toCompletableFuture().get()) {
            System.out.println(unit);
        }

        repository.tombstone(id).toCompletableFuture().get();

        System.out.println("Active units after tombstone:");
        for (UnitStatus unit : repository.findActive().toCompletableFuture().get()) {
            System.out.println(unit);
        }
    }
}