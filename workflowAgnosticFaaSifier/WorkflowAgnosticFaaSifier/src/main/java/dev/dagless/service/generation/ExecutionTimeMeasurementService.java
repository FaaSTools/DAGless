package dev.dagless.service.generation;

import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.factory.Factory;

public class ExecutionTimeMeasurementService {

    public static void generateCodeExecutionTimeMeasurementStart(CtBlock<?> mainBody) {
        String statement = "long codeStartTimeNs = System.nanoTime()";
        addStatementToBody(mainBody, statement);
    }

    public static void generateCodeExecutionTimeMeasurementEnd(CtBlock<?> mainBody) {
        String statement = "long codeExecutionTimeNs = System.nanoTime() - codeStartTimeNs";
        addStatementToBody(mainBody, statement);
    }

    public static void generateFunctionExecutionTimeMeasurementStart(CtBlock<?> mainBody) {
        String statement = "long functionStartTimeNs = System.nanoTime()";
        addStatementToBody(mainBody, statement);
    }

    public static void generateFunctionExecutionTimeMeasurementEnd(CtBlock<?> mainBody) {
        //String statement = "long functionExecutionTimeNs = System.nanoTime() - functionStartTimeNs";
        //addStatementToBody(mainBody, statement);
    }

    private static void addStatementToBody(CtBlock<?> mainBody, String statement) {
        Factory factory = mainBody.getFactory();
        CtCodeSnippetStatement snippetStatement = factory.Core().createCodeSnippetStatement();
        snippetStatement.setValue(statement);
        mainBody.addStatement(snippetStatement);
    }
}
