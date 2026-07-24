package com.fieldstatus;

import java.util.Scanner;

public class App {
    public static void main(String[] args) throws Exception {
        String instanceName = args.length > 0 ? args[0] : null;

        System.out.println("Field Status Board");
        
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

        printHelp();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] tokens = line.split("\\s+");
            String command = tokens[0];

            try {
                switch (command) {
                    case "report":
                        handleReport(repository, tokens);
                        break;
                    case "status":
                        handleStatus(repository, tokens);
                        break;
                    case "tick":
                        handleTick(repository, tokens);
                        break;
                    case "list":
                        handleList(repository);
                        break;
                    case "remove":
                        handleRemove(repository, tokens);
                        break;
                    case "help":
                        printHelp();
                        break;
                    case "quit":
                    case "exit":
                        System.out.println("Goodbye.");
                        return;
                    default:
                        System.out.println("Unknown command: " + command + " (type 'help' for commands)");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void handleReport(UnitStatusRepository repository, String[] tokens) throws Exception {
        if (tokens.length < 3) {
            System.out.println("Usage: report <callsign> <status>");
            return;
        }
        String callsign = tokens[1];
        String status = tokens[2];
        repository.upsert(new UnitStatus(callsign, status, System.currentTimeMillis()))
            .toCompletableFuture().get();
    }

    private static void handleStatus(UnitStatusRepository repository, String[] tokens) throws Exception {
        if (tokens.length < 3) {
            System.out.println("Usage: status <callsign> <status>");
            return;
        }
        String id = UnitStatus.documentIdFor(tokens[1]);
        String status = tokens[2];
        repository.setStatus(id, status).toCompletableFuture().get();
    }

    private static void handleTick(UnitStatusRepository repository, String[] tokens) throws Exception {
        if (tokens.length < 2) {
            System.out.println("Usage: tick <callsign>");
            return;
        }
        String id = UnitStatus.documentIdFor(tokens[1]);
        repository.tick(id, System.currentTimeMillis()).toCompletableFuture().get();
    }

    private static void handleList(UnitStatusRepository repository) throws Exception {
        var units = repository.findActive().toCompletableFuture().get();
        if (units.isEmpty()) {
            System.out.println("No active units.");
            return;
        }
        for (UnitStatus unit : units) {
            System.out.println(unit);
        }
    }

    private static void handleRemove(UnitStatusRepository repository, String[] tokens) throws Exception {
        if (tokens.length < 2) {
            System.out.println("Usage: remove <callsing>");
            return;
        }
        String id = UnitStatus.documentIdFor(tokens[1]);
        repository.tombstone(id).toCompletableFuture().get();
    }

    private static void printHelp() {
        System.out.println("""
            Commands:
                report <callsign> <status> Create or update a unit
                status <callsign> <status> Update a unit's status
                tick <callsign>            Update a unit's telemetry timestamp
                list                       Show active units
                remove <callsign>          Soft-delete a unit
                help                       Show this message
                quit / exit                Exit the program
            """);
    }
}