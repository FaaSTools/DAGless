package dev.dagless.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.dagless.model.config.Config;
import dev.dagless.model.config.DataTransferMode;

import java.io.IOException;
import java.nio.file.Path;

public class ConfigReader {
    public static Config readConfig(Path configFile) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return validateConfig(mapper.readValue(configFile.toFile(), Config.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Config validateConfig(Config config) {
        // PATHS
        if (config.getPathToInputProject() == null) {
            throw new RuntimeException("No input project specified!");
        }
        if (config.getPathToSplitFunctions() == null) {
            throw new RuntimeException("No split functions specified!");
        }
        // PROVIDERS
        if (config.getFunctionProviders() == null) {
            throw new RuntimeException("No function providers specified!");
        }
        // VARIABLE IO
        if (config.getWorkflowInputs() == null) {
            throw new RuntimeException("No workflow inputs specified!");
        }
        if (config.getWorkflowOutputs() == null) {
            throw new RuntimeException("No workflow outputs specified!");
        }
        // SPLITTING
        if (config.getSplitRules() == null && config.getConfigSplitFunctions() == null) {
            throw new RuntimeException("No split rules and split functions specified!");
        }
        if (config.getSplitRules() != null && config.getConfigSplitFunctions() != null) {
            throw new RuntimeException("Split rules and split functions specified!");
        }

        // DATA TRANSFER
        if (config.getDataTransferMode() == null) {
            throw new RuntimeException("No data transfer specified!");
        }
        if (config.getSplitRules() != null && config.getDataTransferMode().equals(DataTransferMode.HARDCODED)) {
            throw new RuntimeException("Hardcoded data transfer and rule based splitting is not supported!");
        }

        // TRACE FILE TRANSFER
        if (config.getSplitRules() != null && config.getTraceFileTransfer() == null) {
            throw new RuntimeException("No trace file transfer specified using rule based splitting!");
        }
        if (config.getConfigSplitFunctions() != null && config.getTraceFileTransfer() == null) {
            throw new RuntimeException("No trace file transfer specified using config based splitting!");
        }
        if (config.getTraceFileTransfer() && config.getDataTransferMode().equals(DataTransferMode.NONE)) {
            throw new RuntimeException("Trace file transfer and no data transfer is not supported!");
        }

        // MONITORING
        if (config.getSplitRules() != null && config.getDirectoryMonitoring() == null) {
            throw new RuntimeException("No directory monitoring specified!");
        }
        if (config.getSplitRules() == null && config.getDirectoryMonitoring() != null) {
            throw new RuntimeException("Config based splitting includes directory monitoring on function level!");
        }

        // MEASURE EXECUTION TIME
        if (config.getSplitRules() != null && config.getMeasureExecutionTime() == null) {
            throw new RuntimeException("No measure execution time specified!");
        }
        if (config.getSplitRules() == null && config.getMeasureExecutionTime() != null) {
            throw new RuntimeException("Config based splitting includes measure execution time on function level!");
        }

        return config;
    }
}
