package dev.dagless.model.splitting;

import dev.dagless.model.Variable;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtStatement;

import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class AbstractWorkflow {

    Logger logger = Logger.getLogger(AbstractWorkflow.class.getName());
    Set<Variable> workflowInputVariables;
    Set<Variable> workflowOutputVariables;
    Set<SplitFunction> functions;

    public AbstractWorkflow(Set<Variable> workflowInputVariables, Set<Variable> workflowOutputVariables, Set<SplitFunction> functions) {
        this.workflowInputVariables = workflowInputVariables;
        this.workflowOutputVariables = workflowOutputVariables;
        this.functions = functions;
        setWorkflowInputVariables();
        setWorkflowOutputVariables();
        enforceNamingConventions();
    }

    public Set<SplitFunction> getSplitFunctions() {
        return functions;
    }

    /**
     * This method adds the workflow inputs to the first Splitfunction. It adds all workflow input variables to the first
     * function. It removes all ctLocalVariables statements with the same identifier as the workflow inputs in first function.
     * This is necessary to avoid duplicate variable declarations in the first function.
     */
    private void setWorkflowInputVariablesOld(){
        // add the input variables to the first function
        functions.stream().findFirst().ifPresent(splitFunction -> splitFunction.getSplitFunctionVariableIO().extendInputVariables(workflowInputVariables));
        // predicate to find ctLocalVariables in functionStatements that are contained in the input variables
        Predicate<CtStatement> isCtLocalVariable = ctStatement -> ctStatement instanceof CtLocalVariable<?> ctLocalVariable && ctLocalVariableIsContainedInInputVariables(ctLocalVariable);
        // remove all ctLocalVariables that are contained in the input variables from the first function
        functions.stream().findFirst().ifPresent(splitFunction -> splitFunction.getFunctionStatements().removeIf(isCtLocalVariable));
    }

    private void setWorkflowInputVariables(){
        // add the input variables to the first function
        functions.forEach(splitFunction -> splitFunction.getSplitFunctionVariableIO().extendInputVariables(workflowInputVariables));
        // predicate to find ctLocalVariables in functionStatements that are contained in the input variables
        Predicate<CtStatement> isCtLocalVariable = ctStatement -> ctStatement instanceof CtLocalVariable<?> ctLocalVariable && ctLocalVariableIsContainedInInputVariables(ctLocalVariable);
        // remove all ctLocalVariables that are contained in the input variables from the first function
        functions.forEach(splitFunction -> splitFunction.getFunctionStatements().removeIf(isCtLocalVariable));
        // add the input variables as output to all functions except the last so that they are passed until used
        functions
                .stream()
                .filter(splitFunction -> splitFunction.getOrder()-1 != functions.size())
                .forEach(splitFunction -> splitFunction.getSplitFunctionVariableIO().extendOutputVariables(workflowInputVariables));
    }

    /**
     * This method adds the workflow outputs to the last Splitfunction.
     */
    private void setWorkflowOutputVariables(){
        SplitFunction lastFunction = functions
                .stream()
                .filter(function -> function.getOrder() == functions.size()-1)
                .findFirst()
                .orElseThrow();
        lastFunction.getSplitFunctionVariableIO().setOutputVariables(workflowOutputVariables);

    }
    private boolean ctLocalVariableIsContainedInInputVariables(CtLocalVariable<?> ctLocalVariable){
        return workflowInputVariables.stream()
                .anyMatch(variable -> variable.getIdentifier().equals(ctLocalVariable.getSimpleName()));
    }

    private void enforceNamingConventions() {
        Set<String> declaredVariables = functions
                .stream()
                .map(function -> function.getFunctionStatements()
                        .stream()
                        .filter(statement -> statement instanceof CtLocalVariable<?>)
                        .map(ctLocalVariable -> ((CtLocalVariable<?>) ctLocalVariable).getSimpleName())
                        .collect(Collectors.toSet()))
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        Set<String> inputVariables = workflowInputVariables.stream().map(Variable::getIdentifier).collect(Collectors.toSet());
        Set<String> outputVariables = workflowOutputVariables.stream().map(Variable::getIdentifier).collect(Collectors.toSet());

        declaredVariables.addAll(inputVariables);
        declaredVariables.addAll(outputVariables);

        Set<String> reservedIdentifiers = Set.of(
                "output", "directoryMonitor", "startTime", "executionTimeNs", "jStorage", "fileTransfers");

        if (declaredVariables.stream().anyMatch(reservedIdentifiers::contains)) {
            throw new IllegalArgumentException("The following identifiers are reserved and cannot be used as variable names: " + reservedIdentifiers);
        }
    }

    @Override
    public String toString() {
        return "AbstractWorkflow{" +
                "workflowInputVariables=" + workflowInputVariables +
                ", workflowOutputVariables=" + workflowOutputVariables +
                ", functions=" + functions +
                '}';
    }
}
