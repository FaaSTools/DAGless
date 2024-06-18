package dev.dagless.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.dagless.model.splitting.StatementRange;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is a suggestion as to how the spilt function could be represented in the config file.
 */
@JsonIgnoreProperties(ignoreUnknown = true) // ignore unknown properties in config file
public class ConfigSplitFunction {

    int order;
    private StatementRange statementRange;
    private List<String> downloadFilePaths = new ArrayList<>();
    private List<String> uploadFilePaths = new ArrayList<>();
    private List<IterationDependency> iterationDependencies = new ArrayList<>();
    private String provider = ""; // TODO
    private String region = ""; // TODO
    private int memory = 0; // TODO
    private boolean directoryMonitoring;
    private boolean measureExecutionTime;
    private boolean parallel = false;

    public ConfigSplitFunction() {
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public StatementRange getStatementRange() {
        return statementRange;
    }

    public void setStatementRange(StatementRange statementRange) {
        this.statementRange = statementRange;
    }

    public List<String> getDownloadFilePaths() {
        return downloadFilePaths;
    }

    public List<String> getInputFilesForJson() {
        return downloadFilePaths.stream().map(file -> "\"" + file + "\"").toList();
    }

    public void setDownloadFilePaths(List<String> downloadFilePaths) {
        this.downloadFilePaths = downloadFilePaths;
    }

    public List<String> getUploadFilePaths() {
        return uploadFilePaths;
    }

    public List<String> getOutputFilesForJson() {
        return uploadFilePaths.stream().map(file -> "\"" + file + "\"").toList();
    }

    public void setUploadFilePaths(List<String> uploadFilePaths) {
        this.uploadFilePaths = uploadFilePaths;
    }

    public boolean isDirectoryMonitoring() {
        return directoryMonitoring;
    }

    public void setDirectoryMonitoring(boolean directoryMonitoring) {
        this.directoryMonitoring = directoryMonitoring;
    }

    public boolean isMeasureExecutionTime() {
        return measureExecutionTime;
    }

    public void setMeasureExecutionTime(boolean measureExecutionTime) {
        this.measureExecutionTime = measureExecutionTime;
    }

    public boolean isParallel() {
        return parallel;
    }

    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    public List<IterationDependency> getIterationDependencies() {
        return iterationDependencies;
    }

    public void setIterationDependencies(List<IterationDependency> iterationDependencies) {
        this.iterationDependencies = iterationDependencies;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public int getMemory() {
        return memory;
    }

    public void setMemory(int memory) {
        this.memory = memory;
    }

    @Override
    public String toString() {
        return "ConfigSplitFunction{" +
                "order=" + order +
                ", statementRange=" + statementRange +
                ", inputFiles=" + downloadFilePaths +
                ", outputFiles=" + uploadFilePaths +
                ", directoryMonitoring=" + directoryMonitoring +
                '}';
    }
}
