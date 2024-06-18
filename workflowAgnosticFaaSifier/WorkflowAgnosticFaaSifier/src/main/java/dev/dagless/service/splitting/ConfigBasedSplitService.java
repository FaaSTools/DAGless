package dev.dagless.service.splitting;

import dev.dagless.model.config.Config;
import dev.dagless.model.config.ConfigSplitFunction;
import dev.dagless.model.splitting.SplitFunction;
import dev.dagless.model.splitting.SplitFunctionFileIO;
import dev.dagless.model.splitting.VariableOptimizedWorkflow;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtStatement;

public class ConfigBasedSplitService extends AbstractSplitService {
    public ConfigBasedSplitService(Config config) {
        super(config);
    }

    @Override
    public VariableOptimizedWorkflow createWorkflow() {
        logger.fine("Creating workflow using config based splitting...");
        // iterate over the split functions in the config and get the functionStatements that belong to the function
        for (ConfigSplitFunction configSplitFunction : config.getConfigSplitFunctions()) {
            // iterate over the functionStatements in the main method to get the functionStatements that belong to the function and the variables
            int start = configSplitFunction.getStatementRange().getStart()-1;
            int end = configSplitFunction.getStatementRange().getEnd();
            for(int i = start; i < end; i++) {
                CtStatement statement = ctStatements.get(i);

                // check if the statement is a variable declaration and add it to the symbols
                if (statement instanceof CtLocalVariable<?> ctLocalVariable) {
                    splitFunctionVariableIO.addVariableToOutputs(ctLocalVariable);
                }

                // add the statement to the function functionStatements
                functionStatements.add(statement);

            }
            // after each iteration we need to create the split function
            createSplitFunction(configSplitFunction);
        }

        return new VariableOptimizedWorkflow(config.getWorkflowInputs(), config.getWorkflowOutputs(), splitFunctions);
    }

    private void createSplitFunction(ConfigSplitFunction configSplitFunction) {
        logger.fine("Creating split function with order " + order + "...");
        // if statements is empty, do nothing
        if (functionStatements.isEmpty()) {
            return;
        }

        // only perform the for each loop parallelization if the config says so
        if (configSplitFunction.isParallel()) {
            performForEachModifications();
        }

        // create a new split function
        SplitFunction splitFunction = new SplitFunction(
                order,
                configSplitFunction,
                splitFunctionVariableIO.getCurrentVariables(),
                functionStatements,
                new SplitFunctionFileIO(configSplitFunction.getInputFilesForJson(), configSplitFunction.getOutputFilesForJson(), configSplitFunction.getIterationDependencies(), config),
                hasForEachLoop() && configSplitFunction.isParallel()
        );

        // add it to the functions
        splitFunctions.add(splitFunction);

        // reset the splitting
        resetSplitting();
    }
}
