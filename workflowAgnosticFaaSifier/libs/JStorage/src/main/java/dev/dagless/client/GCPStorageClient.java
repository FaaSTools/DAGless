package dev.dagless.client;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.*;
import dev.dagless.model.path.FilePath;
import dev.dagless.model.transfer.File;
import dev.dagless.model.transfer.Provider;
import dev.dagless.model.transfer.FileTransfer;
import dev.dagless.model.transfer.TransferType;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GCPStorageClient implements StorageClient{

    private final Storage storage;
    private final Logger logger = Logger.getLogger(GCPStorageClient.class.getName());

    public GCPStorageClient() {
        this.storage = createStorage();
    }

    @Override
    public FileTransfer download(FilePath sourcePath, FilePath destinationPath) {
        FileTransfer fileTransfer = new FileTransfer(Provider.GCP, TransferType.DOWNLOAD);
        List<File> files = new ArrayList<>();
        if (sourcePath.isDirectory() && destinationPath.isDirectory()){
            // adjust signature to source and destination
            files.addAll(downloadFolderFromGCP(sourcePath.getBucketName(), sourcePath, destinationPath));
        } else if (!sourcePath.isDirectory()) {
            // adjust signature to source and destination
            files.add(downloadFileFromGCP(sourcePath.getBucketName(), sourcePath, destinationPath));
        } else {
            logger.severe("Source cannot be a directory while destination is a file!");
        }
        fileTransfer.setFiles(files);
        return fileTransfer;
    }

    @Override
    public FileTransfer upload(FilePath sourcePath, FilePath destinationPath) {
        FileTransfer fileTransfer = new FileTransfer(Provider.GCP, TransferType.UPLOAD);
        List<File> files = new ArrayList<>();
        if (sourcePath.isDirectory() && destinationPath.isDirectory()){
            // adjust signature to source and destination
            files.addAll(uploadFolderToGCP(sourcePath, destinationPath));
        } else if (!sourcePath.isDirectory()) {
            // adjust signature to source and destination
            File file = uploadFileToGCP(sourcePath, destinationPath);
            if (file != null){
                files.add(file);
            }
        } else {
            logger.severe("Source cannot be a directory while destination is a file!");
        }
        fileTransfer.setFiles(files);
        return fileTransfer;
    }

    @Override
    public Set<String> listFilesInDirectory(FilePath storageUri) {
        // List objects in the bucket with the specified prefix
        String folderPrefix = "function/";
        Iterable<Blob> blobs = storage.list(storageUri.getBucketName(), Storage.BlobListOption.prefix(folderPrefix)).iterateAll();

        // Store object names in a set
        Set<String> objectNames = new HashSet<>();
        for (Blob blob : blobs) {
            objectNames.add(blob.getName());
        }
        return objectNames.stream()
                .map(s -> s.substring(folderPrefix.length()))
                .filter(s -> !s.isEmpty() && !s.endsWith("/"))
                .collect(Collectors.toSet());
    }

    private List<File> uploadFolderToGCP(FilePath sourcePath, FilePath destinationPath){
        List<FilePath> files = LocalFileClient.getFilePathsInDir(sourcePath);
        logger.info("Uploading folder with " + files.size() + " files to GCP");

        List<File> transferredFiles = new ArrayList<>();
        for (FilePath filePath : files) {
            String bucketKey = getBucketKeyForFolderUpload(sourcePath, filePath, destinationPath);

            java.io.File sourceFile = new java.io.File(filePath.getFilePath());

            File file = putFileGCP(bucketKey, sourceFile, destinationPath);
            if (file != null){
                transferredFiles.add(file);
            }
        }
        return transferredFiles;
    }

    private String getBucketKeyForFolderUpload(FilePath sourcePath, FilePath currentSourceFile, FilePath destinationPath){
        Path sourceDirPath = Path.of(sourcePath.getFilePath()).getFileName();
        String pathWithoutSourcePath = currentSourceFile.getFilePath().replace(sourcePath.getFilePath(), "");
        return Path.of(destinationPath.getFilePathWithoutLeadingSlash(), sourceDirPath.toString(),  pathWithoutSourcePath).toString();
    }

    @Nullable
    private File uploadFileToGCP(FilePath sourcePath, FilePath destinationPath){
        java.io.File sourceFile = new java.io.File(sourcePath.getFilePath());
        String bucketKey;
        if (destinationPath.isDirectory()){
            // if destination is a dir we want to upload the file to the dir
            bucketKey = destinationPath.getFilePathWithoutLeadingSlash() + new java.io.File(sourcePath.getFilePath()).getName();
        } else {
            // if destination is a file we want to upload the file to the file
            bucketKey = destinationPath.getFilePathWithoutLeadingSlash();
        }

        return putFileGCP(bucketKey, sourceFile, destinationPath);
    }

    @Nullable
    private File putFileGCP(String bucketKey, java.io.File sourceFile, FilePath destinationPath){

        long startTime;
        long endTime;
        logger.info("Uploading: " + sourceFile.getPath() + " to " + Path.of(destinationPath.getBucketName(), bucketKey));
        try {
            BlobId blobId = BlobId.of(destinationPath.getBucketName(), bucketKey);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
            startTime = System.nanoTime();
            storage.create(blobInfo, Files.readAllBytes(Path.of(sourceFile.getPath())));
            endTime = System.nanoTime();
        } catch (IOException e) {
            logger.severe("Failed to upload file " + sourceFile.getPath() + " to GCP");
            return null;
        }

        return new File(sourceFile.getPath(), LocalFileClient.getFileSizeFromLocalFile(sourceFile), endTime - startTime);
    }

    private File downloadFileFromGCP(String bucketName, FilePath sourcePath, FilePath destinationPath){

        java.io.File localFile = LocalFileClient.createLocalFile(sourcePath, destinationPath);

        logger.info("Downloading: " + sourcePath.getFilePath() + " to " + localFile.getPath());

        long startTime = System.nanoTime();
        storage.get(bucketName, sourcePath.getFilePathWithoutLeadingSlash()).downloadTo(localFile.toPath());
        long endTime = System.nanoTime();

        return new File(localFile.getPath(), LocalFileClient.getFileSizeFromLocalFile(localFile), endTime - startTime);
    }

    private List<File> downloadFolderFromGCP(String bucketName, FilePath sourcePath, FilePath destinationPath){

        List<File> files = new ArrayList<>();
        Page<Blob> blobs = storage.list(bucketName, Storage.BlobListOption.prefix(sourcePath.getFilePathWithoutLeadingSlash()));
        logger.info("Downloading folder with " + blobs.streamAll().count() + " files from GCP");
        for (Blob blob : blobs.iterateAll()) {
            if (blob.getName().endsWith("/")) {
                continue;
            }
            java.io.File localFile = LocalFileClient.createLocalFileForFolder(blob.getName(), destinationPath);

            long startTime = System.nanoTime();
            blob.downloadTo(localFile.toPath());
            long endTime = System.nanoTime();

            files.add(new File(localFile.getPath(), LocalFileClient.getFileSizeFromLocalFile(localFile), endTime - startTime));
        }

        return files;
    }


    private Storage createStorage(){
        Provider provider = EnvironmentClient.getProvider();
        if (provider.equals(Provider.GCP) || provider.equals(Provider.LOCAL)){
            try {
                return StorageOptions.newBuilder().build().getService();
            } catch (Exception e) {
                logger.severe("Failed to create GCP Storage Client, calls to GCP will fail");
                return null;
            }
        } else {
            // TO ENABLE CLOUD FEDERATION ADD CREDENTIALS HERE
            return StorageOptions.newBuilder().build().getService();
        }
    }
}
