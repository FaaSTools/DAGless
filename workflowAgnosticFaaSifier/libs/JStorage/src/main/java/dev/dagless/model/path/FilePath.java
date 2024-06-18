package dev.dagless.model.path;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilePath {

    private final String providerPrefix;
    private final Location location;
    private final String bucketName;
    private final String filePath;
    private final boolean isDirectory;

    public FilePath(String filePath) {
        // to avoid painful stack traces when having a blank space in a file path
        filePath = filePath.trim();
        this.providerPrefix = parseProviderPrefix(filePath);
        this.location = parseLocation(filePath);
        this.bucketName = parseBucketName(filePath);
        this.filePath = parseFilePath(filePath);
        this.isDirectory = isDirectory(filePath);
    }

    private String parseProviderPrefix(String filePath) {
       return switch (filePath.substring(0, 2)) {
            case "s3" -> "s3://";
            case "gs" -> "gs://";
            default -> "";
        };
    }

    private Location parseLocation(String filePath) {
        return switch (filePath.substring(0, 2)) {
            case "s3" -> Location.S3;
            case "gs" -> Location.GS;
            default -> Location.LOCAL;
        };
    }

    private String parseBucketName(String filePath) {
        Pattern pattern = Pattern.compile("^(s3://|gs://)[\\w-\\d]+");
        Matcher matcher = pattern.matcher(filePath);
        String match = matcher.find() ? matcher.group() : null;
        if (match == null) {
            return null;
        }
        return match.substring(5);
    }

    private String parseFilePath(String filePath){
        if (location == Location.LOCAL || bucketName == null) {
            return filePath.trim();
        }
        return filePath.substring(5 + bucketName.length());
    }

    private boolean isDirectory(String filePath) {
        return filePath.endsWith("/");
    }

    public String getUriWithFunctionPostfix() {
        return providerPrefix + bucketName + "/function/";
    }
    public Location getLocation() {
        return location;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFilePathWithoutLeadingSlash() {
        if (filePath.startsWith("/")) {
            return filePath.substring(1);
        }
        return filePath;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public String getProviderPrefix() {
        return providerPrefix;
    }

    @Override
    public String toString() {
        return "FilePath{" +
                "location=" + location +
                ", bucketName='" + bucketName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", isDirectory=" + isDirectory +
                '}';
    }
}
