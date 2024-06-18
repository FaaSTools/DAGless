package dev.dagless.model.transfer;

import dev.dagless.client.EnvironmentClient;
public record File(String filePath, float fileSizeInMB, long transferTimeInNanoSeconds) {
    public File(String filePath, float fileSizeInMB, long transferTimeInNanoSeconds) {
        this.filePath = filePath.replace(getProviderPathPrefix(), "");
        this.fileSizeInMB = fileSizeInMB;
        this.transferTimeInNanoSeconds = transferTimeInNanoSeconds;
    }

    @Override
    public String toString() {
        return "File{" +
                "filePath='" + filePath + '\'' +
                ", fileSizeInMB=" + fileSizeInMB +
                ", transferTimeInNanoSeconds=" + transferTimeInNanoSeconds +
                '}';
    }

    private String getProviderPathPrefix() {
        return switch (EnvironmentClient.getProvider()) {
            case AWS -> "/tmp";
            case GCP -> "/workspace";
            case LOCAL -> "";
        };
    }

}
