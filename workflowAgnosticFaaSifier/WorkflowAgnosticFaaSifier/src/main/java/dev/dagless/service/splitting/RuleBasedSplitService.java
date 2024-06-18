package dev.dagless.service.splitting;

import dev.dagless.model.config.Config;
import dev.dagless.model.splitting.SplitFunction;
import dev.dagless.model.splitting.VariableOptimizedWorkflow;
import spoon.reflect.code.*;


public class RuleBasedSplitService extends AbstractSplitService {

    private final SplitRuleService splitRuleService;

    public RuleBasedSplitService(Config config) {
        super(config);
        this.splitRuleService = new SplitRuleService(config);
    }

    @Override
    public VariableOptimizedWorkflow createWorkflow() {
        logger.fine("Creating workflow using rule based splitting...");

        for(CtStatement statement : ctStatements) {
            if (statement instanceof CtForEach){
                // this cause the subsequent for each to be isolated in its own function
                if (!functionStatements.isEmpty()) {
                    createSplitFunction();
                }
            }
            // increment the statement number end
            statementRange.incrementEnd();

            // add the statement to the function statements
            functionStatements.add(statement);

            // check if the statement is a variable declaration and add it to the symbols
            if (statement instanceof CtLocalVariable<?> ctLocalVariable) {
                splitFunctionVariableIO.addVariableToOutputs(ctLocalVariable);
            }

            // based on the rules in the config, check if the statement splits the function
            if (splitRuleService.isSplit(statement)) {
                logger.fine("Splitting function at statement: " + statement);
                createSplitFunction();
            }

        }

        // at the final iteration we need to create the last split function
        if (!functionStatements.isEmpty()) {
            createSplitFunction();
        }
        // createSplitFunction(true, false);
        logger.fine("Creation of workflow using rule based splitting successful!");

        return new VariableOptimizedWorkflow(config.getWorkflowInputs(), config.getWorkflowOutputs(), splitFunctions);
    }

    private void createSplitFunction() {
        // handle the for each loop
        performForEachModifications();
        // create a new split function
        SplitFunction splitFunction = new SplitFunction(
                order,
                statementRange.getCurrentRange(),
                splitFunctionVariableIO.getCurrentVariables(),
                functionStatements,
                config,
                hasForEachLoop());
        // add it to the functions
        splitFunctions.add(splitFunction);
        // reset the splitting
        resetSplitting();
    }


}
