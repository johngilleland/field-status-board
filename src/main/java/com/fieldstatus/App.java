package com.fieldstatus;

public class App {
    public static void main(String[] args) throws Exception {
        String instanceName = args.length > 0 ? args[0] : "a";
        boolean isWriter = !instanceName.equals("b");

        System.out.println("Field Status Board - hello, world (instance: " + instanceName + ")");
        
        DittoService dittoService = new DittoService(instanceName);
        Thread.sleep(3000);

        UnitStatusRepository repository = new UnitStatusRepository(dittoService.getDitto());
        repository.subscribeActive();

        repository.observerActive(units -> {
            System.out.println("--- Board Update (" + units.size() + " active) ---");
            for (UnitStatus unit : units) {
                System.out.println(unit);
            }
        });

        if (isWriter) {
             String id = UnitStatus.documentIdFor("ALPHA-1");

            repository.upsert(new UnitStatus("ALPHA-1", "active", System.currentTimeMillis()))
                .toCompletableFuture().get();
            Thread.sleep(2000);     

            repository.setStatus(id, "degraded").toCompletableFuture().get();
            Thread.sleep(15000);
        } else {
            System.out.println("Listening for sync updates...");
            Thread.sleep(15000);    
        } 
    }
}