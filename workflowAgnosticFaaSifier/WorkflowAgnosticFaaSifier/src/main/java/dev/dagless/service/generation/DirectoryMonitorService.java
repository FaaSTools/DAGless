package dev.dagless.service.generation;

import dev.dagless.model.splitting.SplitFunction;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.code.CtForEach;
import spoon.reflect.factory.Factory;

public class DirectoryMonitorService {

    public static void generateDirectoryMonitorStart(CtBlock<?> mainBody) {
        String statement = "dev.dagless.DirectoryMonitor directoryMonitor = new dev.dagless.DirectoryMonitor(enableDirectoryMonitoring)";
        addStatementToBody(mainBody, statement);
        statement = "directoryMonitor.startMonitoring()";
        addStatementToBody(mainBody, statement);
    }

    public static void generateDirectoryMonitoringEnd(CtBlock<?> mainBody) {
        String statement = "directoryMonitor.stopMonitoring(null)";
        addStatementToBody(mainBody, statement);
    }

    public static void generateDirectoryMonitorInitForEach(CtBlock<?> mainBody) {
        String statement = "dev.dagless.DirectoryMonitor directoryMonitor = new dev.dagless.DirectoryMonitor(enableDirectoryMonitoring)";
        addStatementToBody(mainBody, statement);
    }

    public static void generateDirectoryMonitorStartForEach(CtBlock<?> mainBody) {
        String statement = "directoryMonitor.startMonitoring()";
        insertStatementAtBeginning(mainBody, statement);
    }

    public static void generateDirectoryMonitoringEndForEach(CtBlock<?> mainBody, CtForEach ctForEach) {
        String iterationVariable = ctForEach.getVariable().getSimpleName();
        String statement = "directoryMonitor.stopMonitoring(" + iterationVariable + ")";
        addStatementToBody(mainBody, statement);
    }



    private static void addStatementToBody(CtBlock<?> mainBody, String statement) {
        Factory factory = mainBody.getFactory();
        CtCodeSnippetStatement snippetStatement = factory.Core().createCodeSnippetStatement();
        snippetStatement.setValue(statement);
        mainBody.addStatement(snippetStatement);
    }

    private static void insertStatementAtBeginning(CtBlock<?> mainBody, String statement) {
        Factory factory = mainBody.getFactory();
        CtCodeSnippetStatement snippetStatement = factory.Core().createCodeSnippetStatement();
        snippetStatement.setValue(statement);
        mainBody.insertBegin(snippetStatement);
    }
}
