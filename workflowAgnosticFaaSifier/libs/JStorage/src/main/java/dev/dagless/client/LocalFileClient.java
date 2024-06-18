package dev.dagless.client;

import dev.dagless.model.path.FilePath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LocalFileClient {

    protected static java.io.File createLocalFile(FilePath bucketPath, FilePath localPath) {
        java.io.File localFile = new java.io.File(localPath.getFilePath());
        java.io.File bucketFile = new java.io.File(bucketPath.getFilePath());

        // if the local path is a directory, create a new file with the same name as the bucket file
        if (localPath.isDirectory()){
            localFile = new java.io.File(localPath.getFilePath() + bucketFile.getName());
        }

        // if file exists, delete it - this is done to ensure the download is performed
        if (localFile.exists()) {
            if(!localFile.delete()){
                throw new RuntimeException("Could not delete local file");
            };
        }

        // mkdir for new local files
        if (!localFile.getParentFile().exists()){
            if (!localFile.getParentFile().mkdirs()){
                throw new RuntimeException("Could not create local file directory");
            }
        }
        return localFile;
    }

    protected static java.io.File createLocalFileForFolder(String bucketKey, FilePath localPath){
        // the localPath is a path to a directory where the files are to be downloaded
        java.io.File localFile = new java.io.File(localPath.getFilePath() + bucketKey);


        // if file exists, delete it - this is done to ensure the download is performed
        if (localFile.exists()) {
            if(!localFile.delete()){
                throw new RuntimeException("Could not delete local file");
            }
        }

        // mkdir for new local files
        if (!localFile.getParentFile().exists()){
            if (!localFile.getParentFile().mkdirs()){
                throw new RuntimeException("Could not create local file directory");
            }
        }
        return localFile;
    }

    protected static float getFileSizeFromLocalFile(java.io.File file){
        try {
            return (float) Files.size(file.toPath()) / 1000000;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected static List<FilePath> getFilePathsInDir(FilePath sourcePath){
        List<FilePath> files = new ArrayList<>();
        try {
            Files.walk(Path.of(sourcePath.getFilePath())).forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    files.add(new FilePath(filePath.toFile().getPath()));
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return files;
    }

    protected static boolean isDirectory(String filePath) {
        return filePath.endsWith("/");
    }
}
