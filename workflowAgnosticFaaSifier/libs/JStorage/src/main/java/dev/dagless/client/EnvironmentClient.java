package dev.dagless.client;

import dev.dagless.model.transfer.Provider;

public class EnvironmentClient {

    public static Provider getProvider(){
        String provider = System.getenv("PROVIDER");
        if (provider == null){
            return Provider.LOCAL;
        }
        return switch (provider.toLowerCase()) {
            case "aws" -> Provider.AWS;
            case "gcp" -> Provider.GCP;
            default -> Provider.LOCAL;
        };
    }
}
