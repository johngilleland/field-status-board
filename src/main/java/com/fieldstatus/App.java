package com.fieldstatus;

import com.ditto.java.*;

public class App {
    public static void main(String[] args) throws Exception {
        System.out.println("Field Status Board - hello, world");
        
        DittoService dittoService = new DittoService();
        Thread.sleep(3000);

        var store = dittoService.getDitto().getStore();

        String insertQuery = """
            INSERT INTO units DOCUMENTS ({"_id": "alpha-1", "callsign": "ALPHA-1", "status": "active" })
            """;

        store.execute(insertQuery).toCompletableFuture().get();

        DittoQueryResult result = store.executeRaw("SELECT * FROM units").toCompletableFuture().get();

        for (DittoQueryResultItem item : result.getItems()) {
            System.out.println(item.getJsonString());
        }
    }
}