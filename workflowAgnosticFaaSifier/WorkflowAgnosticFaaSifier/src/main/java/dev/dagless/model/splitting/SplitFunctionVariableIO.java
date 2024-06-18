package dev.dagless.model.splitting;

import dev.dagless.model.Variable;
import dev.dagless.model.config.Config;
import spoon.reflect.code.CtLocalVariable;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class SplitFunctionVariableIO {
    private Set<Variable> inputVariables;
    private Set<Variable> outputVariables;

    /**
     * Constructor for SplitFunctionVariableIO, which is used to store the input and output variables of a function.
     * This constructor defines the following input variables:
     * - bucketUri: String
     */
    public SplitFunctionVariableIO() {
        inputVariables = new TreeSet<>();
        outputVariables = new TreeSet<>();
    }

    // only public for more readable tests
    public SplitFunctionVariableIO(Set<Variable> inputVariables, Set<Variable> outputVariables) {
        this.inputVariables = inputVariables;
        this.outputVariables = outputVariables;
    }

    public Set<Variable> getInputVariables() {
        return inputVariables;
    }

    public String getInputVariablesAsJson() {
        return inputVariables.stream().map(Variable::getVariableAsJson).toList().toString();
    }

    public String getOutputVariablesAsJson() {
        return outputVariables.stream().map(Variable::getVariableAsJson).toList().toString();
    }

    public Set<Variable> getOutputVariables() {
        return outputVariables;
    }

    public void addVariableToOutputs(CtLocalVariable<?> ctLocalVariable) {
        outputVariables.add(new Variable(ctLocalVariable.getSimpleName(), ctLocalVariable.getType().toString(), false));
    }

    /**
     * Sets the output of the previous function as the input of the current function.
     */
    public void setOutputAsInput() {
        inputVariables = new TreeSet<>(outputVariables);
    }

    public SplitFunctionVariableIO getCurrentVariables() {
        Set<Variable> copyInputVariables = inputVariables.stream().map(Variable::new).collect(Collectors.toCollection(TreeSet::new));
        Set<Variable> copyOutputVariables = outputVariables.stream().map(Variable::new).collect(Collectors.toCollection(TreeSet::new));
        return new SplitFunctionVariableIO(copyInputVariables, copyOutputVariables);
    }

    public String getSplitFunctionVariableIOAsJson() {
        return "{\"inputVariables\":" + inputVariables.stream().map(Variable::getVariableAsJson).toList() + ", \"outputVariables\":" + outputVariables.stream().map(Variable::getVariableAsJson).toList() + "}";
    }

    public void addParallelCollection(Variable collection) {
        if (inputVariables.contains(collection)) {
            inputVariables.stream().filter(variable -> variable.equals(collection)).forEach(variable -> variable.setParallel(true));
        } else {
            inputVariables.add(collection);
        }


    }

    public void resetParallelVariables() {
        inputVariables.stream().filter(Variable::isParallel).forEach(variable -> variable.setParallel(false));
    }

    public void removeUnusedInputVariables(Set<String> usedIdentifiers){
        inputVariables.removeIf(variable -> !usedIdentifiers.contains(variable.getIdentifier()));
    }

    public void removeUnusedOutputVariables(Set<String> usedIdentifiers){
        outputVariables.removeIf(variable -> !usedIdentifiers.contains(variable.getIdentifier()));
    }

    public void extendInputVariables(Set<Variable> inputVariables) {
        this.inputVariables.addAll(inputVariables);
    }

    public void extendOutputVariables(Set<Variable> outputVariables) {
        this.outputVariables.addAll(outputVariables);
    }
    public void setOutputVariables(Set<Variable> outputVariables) {
        this.outputVariables = outputVariables;
    }

    @Override
    public String toString() {
        return "SplitFunctionVariableIO{" +
                "inputVariables=" + inputVariables +
                ", outputVariables=" + outputVariables +
                '}';
    }
}
