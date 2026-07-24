package com.fieldstatus;

import com.ditto.java.*;
import io.github.cdimascio.dotenv.Dotenv;

public class DittoService {

    private final Ditto ditto;

    public DittoService(String instanceName) {
        Dotenv dotenv = Dotenv.load();

        String endpoint = dotenv.get("DITTO_ENDPOINT_URL");
        String databaseId = dotenv.get("DITTO_DATABASE_ID");
        String authToken = dotenv.get("DITTO_AUTH_TOKEN");

        DittoConfig.Builder configBuilder = new DittoConfig.Builder(databaseId)
            .connect(new DittoConfig.Connect.Server(endpoint));
        
        if (instanceName != null) {
            configBuilder = configBuilder.persistenceDirectory("./ditto/" + instanceName);
        }

        DittoConfig config = configBuilder.build();

        try {
            this.ditto = DittoFactory.create(config);

            ditto.updateTransportConfig(transportConfig -> {
                transportConfig.peerToPeer(p2p -> {
                    p2p.lan(lan -> lan.isEnabled(false));
                });
            });

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

    public void stopSync() {
        ditto.getSync().stop();
    }

    public void startSync() throws DittoException {
        ditto.getSync().start();
    }
}
