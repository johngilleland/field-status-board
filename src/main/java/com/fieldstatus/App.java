package com.fieldstatus;

public class App {
    public static void main(String[] args) throws Exception {
        System.out.println("Field Status Board - hello, world");
        
        DittoService dittoService = new DittoService();
        Thread.sleep(3000);

        UnitStatusRepository repository = new UnitStatusRepository(dittoService.getDitto());

        repository.observerActive(units -> {
            System.out.println("--- Board Update (" + units.size() + " active) ---");
            for (UnitStatus unit : units) {
                System.out.println(unit);
            }
        });

        String id = UnitStatus.documentIdFor("ALPHA-1");

        repository.upsert(new UnitStatus("ALPHA-1", "active", System.currentTimeMillis()))
            .toCompletableFuture().get();
        Thread.sleep(1000);     

        repository.setStatus(id, "degraded").toCompletableFuture().get();
        Thread.sleep(1000);
        
        repository.tick(id, System.currentTimeMillis()).toCompletableFuture().get();
        Thread.sleep(1000);

        repository.tombstone(id).toCompletableFuture().get();
        Thread.sleep(1000);
    }
}