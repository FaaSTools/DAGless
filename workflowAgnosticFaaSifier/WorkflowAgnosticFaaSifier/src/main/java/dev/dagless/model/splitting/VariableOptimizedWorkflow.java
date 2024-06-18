package dev.dagless.model.splitting;

import dev.dagless.model.Variable;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.CtVariableWrite;
import spoon.reflect.visitor.CtScanner;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VariableOptimizedWorkflow extends AbstractWorkflow{

    public VariableOptimizedWorkflow(Set<Variable> workflowInputVariables, Set<Variable> workflowOutputVariables, Set<SplitFunction> splitFunctions) {
        super(workflowInputVariables, workflowOutputVariables, splitFunctions);
        optimizeVariablesIndexed(workflowOutputVariables, splitFunctions);
    }

    private void optimizeVariablesIndexed(Set<Variable> outputVariables, Set<SplitFunction> functions) {
        // we start from the workflow outputs since those variables should always be passed to the next function
        Set<String> usedVariables = outputVariables.stream().map(Variable::getIdentifier).collect(Collectors.toSet());

        // reverse iterate over the functions
        List<SplitFunction> functionList = new ArrayList<>(functions);
        for (int i = functionList.size()-1; i >= 0; i--) {
            // get current split function
            SplitFunction currentSplitFunction = functionList.get(i);

            // add all variables that are used in the current split function
            usedVariables.addAll(getVariablesUsedInFunction(currentSplitFunction));
            //System.out.println("Index: " + i + " used variables: " + usedVariables + " in function: " + currentSplitFunction);

            // remove all variables passed to this function that are not used in the function
            currentSplitFunction.getSplitFunctionVariableIO().removeUnusedInputVariables(usedVariables);

            // remove all variables passed from the previous function that are not used in the current function
            if (i >= 1) {
                SplitFunction previousSplitFunction = functionList.get(i - 1);
                previousSplitFunction.getSplitFunctionVariableIO().removeUnusedOutputVariables(usedVariables);
            }
        }

        // set the optimized functions
        this.functions = new TreeSet<>(functionList);
    }

    private Set<String> getVariablesUsedInFunction(SplitFunction function) {
        Set<String> variablesRead = function.getFunctionStatements().stream().map(this::getAllVariablesReadFrom).toList().stream().flatMap(List::stream).collect(Collectors.toSet());
        Set<String> variablesWritten = function.getFunctionStatements().stream().map(this::getAllVariablesWrittenTo).toList().stream().flatMap(List::stream).collect(Collectors.toSet());
        return Stream.concat(variablesRead.stream(), variablesWritten.stream()).collect(Collectors.toSet());

    }

    private List<String> getAllVariablesReadFrom(CtStatement statement){
        List<String> readVariables = new ArrayList<>();
        CtScanner scanner = new CtScanner() {
            @Override
            public <T> void visitCtVariableRead(CtVariableRead<T> localVariable) {
                super.visitCtVariableRead(localVariable);
                logger.fine("Found local variable read access: " + localVariable.getVariable());
                readVariables.add(localVariable.getVariable().getSimpleName());
            }
        };
        scanner.scan(statement);
        return readVariables;
    }

    private List<String> getAllVariablesWrittenTo(CtStatement statement){
        List<String> writeVariables = new ArrayList<>();
        CtScanner scanner = new CtScanner() {
            @Override
            public <T> void visitCtVariableWrite(CtVariableWrite<T> localVariable) {
                super.visitCtVariableWrite(localVariable);
                logger.fine("Found local variable write access: " + localVariable.getVariable());
                writeVariables.add(localVariable.getVariable().getSimpleName());
            }
        };
        scanner.scan(statement);
        return writeVariables;
    }
}
