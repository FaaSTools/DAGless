package dev.dagless.client;

import dev.dagless.model.path.FilePath;
import dev.dagless.model.transfer.Provider;
import dev.dagless.model.transfer.File;
import dev.dagless.model.transfer.FileTransfer;
import dev.dagless.model.transfer.TransferType;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class S3StorageClient implements StorageClient{

    private S3Client s3Client;
    private final Logger logger = Logger.getLogger(S3StorageClient.class.getName());

    public S3StorageClient(){
        s3Client = createS3Client();
    }

    @Override
    public FileTransfer download(FilePath sourcePath, FilePath destinationPath) {
        FileTransfer fileTransfer = new FileTransfer(Provider.AWS, TransferType.DOWNLOAD);

        // set region to the region of the sourcePath
        s3Client.close();
        s3Client = createS3Client(getRegionFromURI(sourcePath.getBucketName()));

        List<File> files = new ArrayList<>();
        if (sourcePath.isDirectory() && destinationPath.isDirectory()){
            // adjust signature to source and destination
            files.addAll(downloadFolderFromS3(sourcePath.getBucketName(), sourcePath, destinationPath));
        } else if (!sourcePath.isDirectory()) {
            // adjust signature to source and destination
            files.add(downloadFileFromS3(sourcePath.getBucketName(), sourcePath, destinationPath));
        } else {
           logger.severe("Source cannot be a directory while destination is a file!");
        }
        fileTransfer.setFiles(files);
        return fileTransfer;
    }

    @Override
    public FileTransfer upload(FilePath sourcePath, FilePath destinationPath){
        FileTransfer fileTransfer = new FileTransfer(Provider.AWS, TransferType.UPLOAD);

        // set region to the region of the destinationPath
        s3Client.close();
        s3Client = createS3Client(getRegionFromURI(destinationPath.getBucketName()));

        List<File> files = new ArrayList<>();
        if (sourcePath.isDirectory() && destinationPath.isDirectory()){
            // adjust signature to source and destination
            files.addAll(uploadFolderToS3(sourcePath, destinationPath));
        } else if (!sourcePath.isDirectory()) {
            // adjust signature to source and destination
            File file = uploadFileToS3(sourcePath, destinationPath);
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
        String storagePrefix = "function/";
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(storageUri.getBucketName())
                .prefix(storagePrefix)
                .build();

        ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
        List<S3Object> objects = listResponse.contents();

        Set<String> files = new HashSet<>();
        for (S3Object object : objects) {
            files.add(object.key());
        }
        return files.stream()
                .map(s -> s.substring(storagePrefix.length()))
                .filter(s -> !s.isEmpty() && !s.endsWith("/"))
                .collect(Collectors.toSet());
    }

    private List<File> downloadFolderFromS3(String bucketName, FilePath sourcePath, FilePath destinationPath){
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(sourcePath.getFilePathWithoutLeadingSlash())
                .build();

        List<S3Object> objects = s3Client.listObjectsV2(listRequest).contents();

        logger.info("Downloading folder with " + objects.size() + " files from S3");

        List<File> transferredFiles = new ArrayList<>();
        for (S3Object object : objects) {
            String key = object.key();

            // skip directories so they do not get created at the destination
            if (LocalFileClient.isDirectory(key)) {
                continue;
            }

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();


            java.io.File localFile = LocalFileClient.createLocalFileForFolder(key, destinationPath);

            long startTime = System.nanoTime();
            s3Client.getObject(getObjectRequest, localFile.toPath());
            long endTime = System.nanoTime();

            transferredFiles.add(new File(localFile.getPath(), LocalFileClient.getFileSizeFromLocalFile(localFile), endTime - startTime));
        }
        return transferredFiles;
    }

    private File downloadFileFromS3(String bucketName, FilePath bucketPath, FilePath localPath) {

        java.io.File localFile = LocalFileClient.createLocalFile(bucketPath, localPath);

        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .key(bucketPath.getFilePathWithoutLeadingSlash())
                .bucket(bucketName)
                .build();

        long startTime = System.nanoTime();
        GetObjectResponse object = s3Client.getObject(objectRequest, localFile.toPath());
        long endTime = System.nanoTime();

        if (object == null) {
            logger.severe("Failed to download file " + bucketPath.getFilePathWithoutLeadingSlash() + " from S3");
        }

        return new File(localFile.getPath(), LocalFileClient.getFileSizeFromLocalFile(localFile), endTime - startTime);
    }

    private List<File> uploadFolderToS3(FilePath sourcePath, FilePath destinationPath){
        List<FilePath> files = LocalFileClient.getFilePathsInDir(sourcePath);
        logger.info("Uploading folder with " + files.size() + " files to S3");

        List<File> transferredFiles = new ArrayList<>();
        for (FilePath filePath : files) {
            String pathWithoutSourcePath = filePath.getFilePath().replace(sourcePath.getFilePath(), "");
            String bucketKey = getBucketKeyForFolderUpload(sourcePath, filePath, destinationPath);

            java.io.File sourceFile = new java.io.File(filePath.getFilePath());

            transferredFiles.add(putFileS3(bucketKey, sourceFile, destinationPath));

        }
        return transferredFiles;
    }

    private String getBucketKeyForFolderUpload(FilePath sourcePath, FilePath currentSourceFile, FilePath destinationPath){
        Path sourceDirPath = Path.of(sourcePath.getFilePath()).getFileName();
        String pathWithoutSourcePath = currentSourceFile.getFilePath().replace(sourcePath.getFilePath(), "");
        return Path.of(destinationPath.getFilePathWithoutLeadingSlash(), sourceDirPath.toString(),  pathWithoutSourcePath).toString();
    }

    private File putFileS3(String bucketKey, java.io.File sourceFile, FilePath destinationPath){
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(destinationPath.getBucketName())
                .key(bucketKey)
                .build();

        long startTime = System.nanoTime();
        s3Client.putObject(putRequest, RequestBody.fromFile(sourceFile));
        long endTime = System.nanoTime();

        return new File(sourceFile.getPath(), LocalFileClient.getFileSizeFromLocalFile(sourceFile), endTime - startTime);
    }

    @Nullable
    private File uploadFileToS3(FilePath sourcePath, FilePath destinationPath){
        try {
            java.io.File sourceFile = new java.io.File(sourcePath.getFilePath());
            String bucketKey;
            if (destinationPath.isDirectory()){
                // if destination is a dir we want to upload the file to the dir
                bucketKey = destinationPath.getFilePathWithoutLeadingSlash() + new java.io.File(sourcePath.getFilePath()).getName();
            } else {
                // if destination is a file we want to upload the file to the file
                bucketKey = destinationPath.getFilePathWithoutLeadingSlash();
            }

            return putFileS3(bucketKey, sourceFile, destinationPath);

        } catch (S3Exception e) {
            logger.severe("Failed to upload file " + sourcePath.getFilePath() + " to S3");
            return null;
        }
    }

    private Region getRegionFromURI(String uri){
        logger.info("Getting region from URI: " + uri);
        Pattern pattern = Pattern.compile("(us(-gov)?|ap|ca|cn|eu|sa)-(central|(north|south)?(east|west)?)-\\d");
        Matcher matcher = pattern.matcher(uri);
        if (matcher.find()){
            logger.info("Found region: " + matcher.group());
            return Region.of(matcher.group());
        }
        logger.info("No region found, using default region");
        return Region.EU_CENTRAL_1;
    }

    private S3Client createS3Client(){
        Provider provider = EnvironmentClient.getProvider();
        if (provider.equals(Provider.AWS) || provider.equals(Provider.LOCAL)){
            try {
                return S3Client.builder().build();
            } catch (Exception e) {
                logger.severe("Failed to create S3Client, calls to S3 will fail!");
                return null;
            }
        } else {
            // TODO: Add CredentialsManager to add AWS Credenetials to this object build
            return null;
        }
    }

    private S3Client createS3Client(Region region){
        Provider provider = EnvironmentClient.getProvider();
        if (provider.equals(Provider.AWS) || provider.equals(Provider.LOCAL)){
            try {
                logger.info("Creating S3Client with region " + region);
                return S3Client.builder().region(region).build();
            } catch (Exception e) {
                logger.severe("Failed to create S3Client, calls to S3 will fail!");
                return null;
            }
        } else {
            // TO ENABLE CLOUD FEDERATION ADD THE CREDENTIALS HERE
            return null;
        }
    }
}

