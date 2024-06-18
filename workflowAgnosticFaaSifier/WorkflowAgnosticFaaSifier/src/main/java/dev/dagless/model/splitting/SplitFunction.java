package dev.dagless.model.splitting;

import dev.dagless.model.config.Config;
import dev.dagless.model.config.ConfigSplitFunction;
import dev.dagless.model.config.Deployment;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtStatement;

import java.util.List;

public class SplitFunction implements Comparable<SplitFunction> {

    private final int order;
    private final StatementRange statementRange;
    private final List<CtStatement> functionStatements;
    private final SplitFunctionVariableIO splitFunctionVariableIO;
    private final SplitFunctionFileIO splitFunctionFileIO;
    private final Deployment deployment;
    private final boolean directoryMonitoring; // TODO if true
    private final boolean measureExecutionTime;
    private final boolean forEachLoop; // TODO if true ==>

    /**
     * Constructor for the SplitFunction class as used in the RuleBasedSplitService.
     * The remaining fields are not set.
     * directoryMonitoring is set to true by default.
     *
     * @param order                   - the order of the function in the workflow
     * @param statementRange          - the range of statements that belong to the function
     * @param splitFunctionVariableIO - the input and output variables of the function
     * @param functionStatements      - the CTStatements that belong to the function
     * @param config                  - the config object
     * @param forEachLoop             - whether the function contains a for each loop
     */
    public SplitFunction(int order, StatementRange statementRange, SplitFunctionVariableIO splitFunctionVariableIO, List<CtStatement> functionStatements, Config config, boolean forEachLoop) {
        // This validation is necessary however, ideally "ForEachSplitfunction" would be subclassed from SplitFunction
        if (forEachLoop){
            if (functionStatements.size() != 1 || !(functionStatements.get(0) instanceof CtForEach)){
                throw new RuntimeException("For each Split function must contain only one statement of type CtForEach!");
            }
        }

        this.order = order;
        this.statementRange = statementRange;
        this.splitFunctionVariableIO = splitFunctionVariableIO;
        this.functionStatements = functionStatements;

        this.splitFunctionFileIO = new SplitFunctionFileIO(config);
        this.directoryMonitoring = config.getDirectoryMonitoring();
        this.measureExecutionTime = config.getMeasureExecutionTime();
        this.forEachLoop = forEachLoop;
        this.deployment =new Deployment();

    }

    /**
     * Constructor for the SplitFunction class as used in the ConfigBasedSplitService.
     *
     * @param order                   - the order of the function in the workflow
     * @param functionStatements      - the CTStatements that belong to the function
     * @param splitFunctionVariableIO - the input and output variables of the function
     * @param splitFunctionFileIO     - the input and output files of the function
     * @param forEachLoop             - whether the function should parallelize for each loops
     */
    public SplitFunction(int order, ConfigSplitFunction configSplitFunction, SplitFunctionVariableIO splitFunctionVariableIO, List<CtStatement> functionStatements, SplitFunctionFileIO splitFunctionFileIO, boolean forEachLoop) {
        this.order = order;
        this.statementRange = configSplitFunction.getStatementRange();
        this.functionStatements = functionStatements;
        this.splitFunctionVariableIO = splitFunctionVariableIO;
        this.splitFunctionFileIO = splitFunctionFileIO;
        this.directoryMonitoring = configSplitFunction.isDirectoryMonitoring();
        this.measureExecutionTime = configSplitFunction.isMeasureExecutionTime();
        this.forEachLoop = forEachLoop;
        this.deployment = new Deployment(configSplitFunction.getProvider(), configSplitFunction.getRegion(), configSplitFunction.getMemory());
    }

    public int getOrder() {
        return order;
    }

    public List<CtStatement> getFunctionStatements() {
        return functionStatements;
    }

    public SplitFunctionVariableIO getSplitFunctionVariableIO() {
        return splitFunctionVariableIO;
    }

    public SplitFunctionFileIO getSplitFunctionFileIO() {
        return splitFunctionFileIO;
    }

    public boolean isDirectoryMonitoring() {
        return directoryMonitoring;
    }

    public String getOrderAsString() {
        return String.valueOf(order);
    }

    public boolean isMeasureExecutionTime() {
        return measureExecutionTime;
    }

    public StatementRange getStatementRange() {
        return statementRange;
    }

    public boolean isForEachLoop() {
        return forEachLoop;
    }

    public String getSplitFunctionAsJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"order\": ").append(order).append(",\n");
        sb.append("  \"statementRange\": ").append(statementRange.getStatementRangeAsJson()).append(",\n");
        // The following fields are only set if present
        if (deployment.getMemory() != 0) {
            sb.append("  \"memory\": ").append(deployment.getMemory()).append(",\n");
        }
        if (!deployment.getProvider().isEmpty()) {
            sb.append("  \"provider\": \"").append(deployment.getProvider()).append("\",\n");
        }
        if (!deployment.getRegion().isEmpty()) {
            sb.append("  \"region\": \"").append(deployment.getRegion()).append("\",\n");
        }
        sb.append("  \"downloadFilePaths\": ").append(splitFunctionFileIO.getDownloadFilePaths()).append(",\n");
        sb.append("  \"uploadFilePaths\": ").append(splitFunctionFileIO.getUploadFilePaths()).append(",\n");
        sb.append("  \"forEachIterations\": ").append(splitFunctionFileIO.getIterationDependenciesAsJson()).append(",\n");
        sb.append("  \"inputVariables\": ").append(splitFunctionVariableIO.getInputVariablesAsJson()).append(",\n");
        sb.append("  \"outputVariables\": ").append(splitFunctionVariableIO.getOutputVariablesAsJson()).append(",\n");
        sb.append("  \"parallel\": ").append(forEachLoop).append("\n");

        sb.append("}");
        return sb.toString();
    }

    @Override
    public String toString() {
        return "SplitFunction{" +
                "order=" + order +
                ", statementRange=" + statementRange +
                ", statements=" + functionStatements +
                ", splitFunctionVariableIO=" + splitFunctionVariableIO +
                ", splitFunctionFileIO=" + splitFunctionFileIO +
                ", directoryMonitoring=" + directoryMonitoring +
                '}';
    }

    @Override
    public int compareTo(SplitFunction o) {
        return Integer.compare(this.order, o.order);
    }
}
