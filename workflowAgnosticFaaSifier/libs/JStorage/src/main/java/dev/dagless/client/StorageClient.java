package dev.dagless.client;

import dev.dagless.model.path.FilePath;
import dev.dagless.model.transfer.File;
import dev.dagless.model.transfer.FileTransfer;

import java.util.Set;

public interface StorageClient {

    FileTransfer upload(FilePath sourceFilePath, FilePath destinationFilePath);

    FileTransfer download(FilePath sourceFilePath, FilePath destinationFilePath);

    Set<String> listFilesInDirectory(FilePath storageUri);
}
