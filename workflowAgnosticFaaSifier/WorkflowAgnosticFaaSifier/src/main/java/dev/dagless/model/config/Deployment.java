package dev.dagless.model.config;

public class Deployment {

    private String provider = "";
    private String region = "";
    private int memory = 0;

    public Deployment(String provider, String region, int memory) {
        this.provider = provider;
        this.region = region;
        this.memory = memory;
    }

    public Deployment() {
    }

    public String getProvider() {
        return provider;
    }

    public String getRegion() {
        return region;
    }

    public int getMemory() {
        return memory;
    }
}
