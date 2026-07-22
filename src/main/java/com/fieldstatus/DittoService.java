package com.fieldstatus;

import com.ditto.java.*;
import io.github.cdimascio.dotenv.Dotenv;

public class DittoService {

    private final Ditto ditto;

    public DittoService() {
        Dotenv dotenv = Dotenv.load();

        String endpoint = dotenv.get("DITTO_ENDPOINT_URL");
        String databaseId = dotenv.get("DITTO_DATABASE_ID");
        String authToken = dotenv.get("DITTO_AUTH_TOKEN");

        DittoConfig config = new DittoConfig.Builder(databaseId)
            .connect(new DittoConfig.Connect.Server(endpoint))
            .build();

        try {
            this.ditto = DittoFactory.create(config);

            ditto.getAuth().setExpirationHandler((expiringDitto, timeUntilExpiration) ->
                expiringDitto.getAuth().login(authToken, DittoAuthenticationProvider.development())
                    .thenAccept(result -> System.out.println("Authentication successful"))
                    .exceptionally(error -> {
                        System.out.println("Authentication failed: " + error);
                        return null;
                    })
            );

            ditto.getSync().start();
        } catch (DittoException e) {
            throw new RuntimeException("Ditto initialization failed: " + e.getMessage(), e);
        }
    }

    public Ditto getDitto() {
        return ditto;
    }
}
