package dev.dagless;

import dev.dagless.client.EnvironmentClient;
import dev.dagless.client.GCPStorageClient;
import dev.dagless.client.S3StorageClient;
import dev.dagless.client.StorageClient;
import dev.dagless.model.path.FilePath;
import dev.dagless.model.path.Location;
import dev.dagless.model.transfer.FileTransfer;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

public class JStorage {

    private final StorageClient s3StorageClient;
    private final StorageClient gcpStorageClient;

    private static final Logger logger = Logger.getLogger(JStorage.class.getName());

    public JStorage(){
        logger.info("Initializing JStorage");
        s3StorageClient = new S3StorageClient();
        gcpStorageClient = new GCPStorageClient();
        logger.info("JStorage initialized with provider " + EnvironmentClient.getProvider());

    }

    public void copy(String source, String destination){
        copyTraced(source, destination);
    }

    public FileTransfer copyTraced(String source, String destination){
        FilePath sourcePath = new FilePath(source);
        FilePath destinationPath = new FilePath(destination);

        if (sourcePath.getLocation() != Location.LOCAL && destinationPath.getLocation() != Location.LOCAL){
            throw new IllegalStateException("Cross Provider copying is not implemented!");
        } else if (sourcePath.getLocation() == Location.LOCAL && destinationPath.getLocation() == Location.LOCAL) {
            throw new IllegalStateException("Local to local coping is not implemented!");
        }

        FileTransfer fileTransfer;
        if (sourcePath.getLocation() == Location.LOCAL) {
            switch (destinationPath.getLocation()){
                case S3 -> fileTransfer = s3StorageClient.upload(sourcePath, destinationPath);
                case GS -> fileTransfer = gcpStorageClient.upload(sourcePath, destinationPath);
                default -> throw new RuntimeException("Invalid upload location");
            }
        } else {
            switch (sourcePath.getLocation()){
                case S3 -> fileTransfer = s3StorageClient.download(sourcePath, destinationPath);
                case GS -> fileTransfer = gcpStorageClient.download(sourcePath, destinationPath);
                default -> throw new RuntimeException("Invalid download location");
            }
        }

        return fileTransfer;
    }

    public void clearFunctionDirectory(){
        switch (EnvironmentClient.getProvider()){
            case AWS -> {
                File functionDir = new File("/tmp/function/");
                clearFunctionDir(functionDir);
            }
            case GCP -> {
                File functionDir = new File("/workspace/function/");
                clearFunctionDir(functionDir);
            }
            case LOCAL -> {
                logger.info("Local environment, no need to clear working directory");
            }
        }
    }

    public Set<String> listFiles(String uri){
        FilePath filePath = new FilePath(uri);
        return switch (filePath.getLocation()){
                case S3 -> s3StorageClient.listFilesInDirectory(filePath);
                case GS -> gcpStorageClient.listFilesInDirectory(filePath);
                default -> throw new RuntimeException("Invalid location");
            };
    }

    public List<FileTransfer> downloadUrisParallelTraced(List<String> uris){
        ForkJoinPool pool = new ForkJoinPool(uris.size());
        return pool.submit(() -> uris.parallelStream()
                .map(uri -> copyTraced(uri, getLocalFilePathForDownload(uri)))
                .toList()
        ).join();
    }

    public void downloadUrisParallel(List<String> uris){
        downloadUrisParallelTraced(uris);
    }

    public List<FileTransfer> uploadUrisParallelTraced(List<String> uris){
        ForkJoinPool pool = new ForkJoinPool(uris.size());
        return pool.submit(() -> uris.parallelStream()
                .map(uri -> copyTraced(getLocalSourcePathForUpload(uri), uri))
                .toList()
        ).join();
    }

    public void uploadUrisParallel(List<String> uris){
        uploadUrisParallelTraced(uris);
    }

    public static String getLocalFilePathForDownload(String uri){
        FilePath filePath = new FilePath(uri);
        if (filePath.isDirectory()){
            return addProviderPathPrefix(getParentDirWithTrailingSlash(filePath.getFilePath())); // since we work with absolute paths we can just return the root for downloads
        }
        return addProviderPathPrefix(filePath.getFilePath());
    }

    public static String getLocalSourcePathForUpload(String uri){
        FilePath filePath = new FilePath(uri);
        return addProviderPathPrefix(filePath.getFilePath());
    }

    public static String getLocalDestinationPathForUpload(String uri){
        FilePath filePath = new FilePath(uri);
        if (filePath.isDirectory()){
            return filePath.getProviderPrefix() + filePath.getBucketName() + getParentDirWithTrailingSlash(filePath.getFilePath());
        }
        return uri;
    }
    private static String addProviderPathPrefix(String path){
        return switch (EnvironmentClient.getProvider()) {
            case AWS -> "/tmp" + path;
            case GCP -> "/workspace" + path;
            case LOCAL -> path;
        };
    }

    private static String getParentDirWithTrailingSlash(String path){
        Path parent = Path.of(path).getParent();
        if (parent == null){
            return "/";
        }
        return parent.endsWith("/") ? parent.toString() : parent + "/";
    }

    /**
     * Recursively deletes all files and folders in the function directory
     * @param functionDir
     */
    private static void clearFunctionDir(File functionDir){
        logger.info("Clearing function directory");
        if (functionDir.exists()){
            for (File file : Objects.requireNonNull(functionDir.listFiles())){
                if (file.isDirectory()){
                    clearFunctionDir(file);
                }
                if (!file.delete()){
                    throw new RuntimeException("Could not delete file " + file);
                }
            }
        }

    }

}
