package com.fieldstatus;

public class App {
    public static void main(String[] args) throws Exception {
        String instanceName = args.length > 0 ? args[0] : "a";

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

        String id = UnitStatus.documentIdFor("ALPHA-1");

        System.out.println("Disconnecting (simulated partition)...");
        dittoService.stopSync();
        Thread.sleep(2000);

        if (instanceName.equals("a")) {
            System.out.println("Instance A editing status while offline...");
            repository.setStatus(id, "partitioned-a").toCompletableFuture().get();
        } else {
            System.out.println("Instance B editing telemetry tick while offline...");
            repository.tick(id, System.currentTimeMillis()).toCompletableFuture().get();
        }

        Thread.sleep(2000);

        System.out.println("Reconnecting...");
        dittoService.startSync();
        Thread.sleep(10000);

        System.out.println("Final merged state:");
        System.out.println(repository.findById(id).toCompletableFuture().get());
    }
}