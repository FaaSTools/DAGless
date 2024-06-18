package dev.dagless;

public class DaglessPathUtil {

    /**
     * This method is used to get the working directory of the function.
     * This is necessary because the working directory is different for AWS and GCP.
     * The provider is set via the environment variable "PROVIDER".
     * If the environment variable is not set, the path is returned without modification.
     * If the environment variable is set, the provider specific path is returned.
     * @param path The path to the function
     * @return The working directory of the function depending on the provider
     */
    public static String getWorkingDirByEnvironmentVariable(String path){
        String envProvider = System.getenv("PROVIDER");
        if (envProvider == null){
            return path;
        }
        return switch (envProvider.toLowerCase()) {
            case "aws" -> "/tmp/function/";
            case "gcp" -> "/workspace/function/";
            default -> path;
        };
    }
}
