package dev.dagless.model.splitting;

import dev.dagless.model.config.Config;
import dev.dagless.model.config.DataTransferMode;
import dev.dagless.model.config.IterationDependency;

import java.util.ArrayList;
import java.util.List;

public class SplitFunctionFileIO {

    private final List<String> downloadFilePaths;
    private final List<String> uploadFilePaths;
    private final DataTransferMode dataTransferMode;
    private final List<IterationDependency> iterationDependencies;
    private final boolean traceFileTransfer;

    public SplitFunctionFileIO(Config config) {
        this.downloadFilePaths = new ArrayList<>();
        this.uploadFilePaths = new ArrayList<>();
        this.dataTransferMode = config.getDataTransferMode();
        this.traceFileTransfer = config.getTraceFileTransfer();
        this.iterationDependencies = new ArrayList<>();
    }

    public SplitFunctionFileIO(List<String> downloadFilePaths, List<String> uploadFilePaths, List<IterationDependency> iterationDependencies, Config config) {
        this.downloadFilePaths = downloadFilePaths;
        this.uploadFilePaths = uploadFilePaths;
        this.dataTransferMode = config.getDataTransferMode();
        this.traceFileTransfer = config.getTraceFileTransfer();
        this.iterationDependencies = iterationDependencies;
    }

    public List<String> getDownloadFilePaths() {
        return downloadFilePaths;
    }

    public List<String> getUploadFilePaths() {
        return uploadFilePaths;
    }

    public DataTransferMode getDataTransfer() {
        return dataTransferMode;
    }

    public boolean isTraceFileTransfer() {
        return traceFileTransfer;
    }

    // moved from FileIOService here to keep the code duplication low
    public String generateDownloadStatement(String source, String destination) {
        return "if (enableDirectoryMonitoring)\n\tjStorage.copy( monitorUtilsBucketUri + \"" + source + "\", \"" + destination + "\")";
    }

    public String generateUploadStatement(String source, String destination) {
        if (traceFileTransfer) {
            return "fileTransfers.add(jStorage.copyTraced(\"" + source + "\", uploadBucketUri + \"" + destination + "\"))";
        }
        return "jStorage.copy(\"" + source + "\",  uploadBucketUri + \"" + destination + "\")";
    }

    public String generateManualDownloadStatement() {
        if (traceFileTransfer) {
            return "downloadUris.forEach(uri -> fileTransfers.add(jStorage.copyTraced(uri, dev.dagless.JStorage.getLocalFilePathForDownload(uri))))";
        }
        return "downloadUris.forEach(uri -> jStorage.copy(uri, dev.dagless.JStorage.getLocalFilePathForDownload(uri)))";
    }

    public String generateManualUploadStatement() {
        if (traceFileTransfer) {
            return "uploadUris.forEach(uri -> fileTransfers.add(jStorage.copyTraced(dev.dagless.JStorage.getLocalSourcePathForUpload(uri), dev.dagless.JStorage.getLocalDestinationPathForUpload(uri))))";
        }
        return "uploadUris.forEach(uri -> jStorage.copy(jStorage.copyTraced(dev.dagless.JStorage.getLocalSourcePathForUpload(uri), dev.dagless.JStorage.getLocalDestinationPathForUpload(uri)))";
    }

    public String getIterationDependenciesAsJson() {
        return "[" +
                String.join(",", iterationDependencies.stream().map(IterationDependency::getIterationDependencyAsJSON).toArray(String[]::new)) +
                "]";
    }

    public String getFilesAsJson() {
        return "{\n" +
                "  \"inputFiles\": " + downloadFilePaths + ",\n" +
                "  \"outputFiles\": " + uploadFilePaths + "\n" +
                "}";
    }

}
