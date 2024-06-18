package dev.dagless.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.dagless.model.Variable;
import dev.dagless.model.splitting.SplitRule;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true) // ignore unknown properties in config file
public class Config {
    private Path pathToInputProject;
    private Path pathToSplitFunctions;
    private Set<FunctionProvider> functionProviders;
    private DataTransferMode dataTransferMode;
    private Boolean traceFileTransfer;
    private Boolean directoryMonitoring;
    private Boolean measureExecutionTime;
    private Set<Variable> workflowInputs;
    private Set<Variable> workflowOutputs;
    private Set<SplitRule> splitRules;
    private List<ConfigSplitFunction> configSplitFunctions;

    public Config() {
    }

    public Path getPathToInputProject() {
        return pathToInputProject;
    }

    public void setPathToInputProject(Path pathToInputProject) {
        this.pathToInputProject = pathToInputProject;
    }

    public Path getPathToSplitFunctions() {
        return pathToSplitFunctions;
    }

    public void setPathToSplitFunctions(Path pathToSplitFunctions) {
        this.pathToSplitFunctions = pathToSplitFunctions;
    }

    public Set<FunctionProvider> getFunctionProviders() {
        return functionProviders;
    }

    public void setFunctionProviders(Set<FunctionProvider> functionProviders) {
        this.functionProviders = functionProviders;
    }

    public DataTransferMode getDataTransferMode() {
        return dataTransferMode;
    }

    public void setDataTransferMode(DataTransferMode dataTransferMode) {
        this.dataTransferMode = dataTransferMode;
    }

    public Boolean getTraceFileTransfer() {
        return traceFileTransfer;
    }

    public void setTraceFileTransfer(Boolean traceFileTransfer) {
        this.traceFileTransfer = traceFileTransfer;
    }

    public Boolean getDirectoryMonitoring() {
        return directoryMonitoring;
    }

    public void setDirectoryMonitoring(Boolean directoryMonitoring) {
        this.directoryMonitoring = directoryMonitoring;
    }

    public Boolean getMeasureExecutionTime() {
        return measureExecutionTime;
    }

    public void setMeasureExecutionTime(Boolean measureExecutionTime) {
        this.measureExecutionTime = measureExecutionTime;
    }

    public Set<Variable> getWorkflowInputs() {
        return workflowInputs;
    }

    public void setWorkflowInputs(Set<Variable> workflowInputs) {
        this.workflowInputs = workflowInputs;
    }

    public Set<Variable> getWorkflowOutputs() {
        return workflowOutputs;
    }

    public void setWorkflowOutputs(Set<Variable> workflowOutputs) {
        this.workflowOutputs = workflowOutputs;
    }

    public Set<SplitRule> getSplitRules() {
        return splitRules;
    }

    public void setSplitRules(Set<SplitRule> splitRules) {
        this.splitRules = splitRules;
    }

    public List<ConfigSplitFunction> getConfigSplitFunctions() {
        return configSplitFunctions;
    }

    public void setConfigSplitFunctions(List<ConfigSplitFunction> configSplitFunctions) {
        this.configSplitFunctions = configSplitFunctions;
    }

    @Override
    public String toString() {
        return "Config{" +
                "pathToInputProject=" + pathToInputProject +
                ", pathToSplitFunctions=" + pathToSplitFunctions +
                ", functionProviders=" + functionProviders +
                ", dataTransferMode=" + dataTransferMode +
                ", traceFileTransfer=" + traceFileTransfer +
                ", directoryMonitoring=" + directoryMonitoring +
                ", measureExecutionTime=" + measureExecutionTime +
                ", workflowInputs=" + workflowInputs +
                ", workflowOutputs=" + workflowOutputs +
                ", splitRules=" + splitRules +
                ", configSplitFunctions=" + configSplitFunctions +
                '}';
    }
}