package dev.dagless.service.generation;

import dev.dagless.model.config.DataTransferMode;
import dev.dagless.model.config.FunctionProvider;
import dev.dagless.model.splitting.SplitFunction;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.factory.Factory;

public class VariableIOService {

    FunctionProvider provider;

    public VariableIOService(FunctionProvider provider) {
        this.provider = provider;
    }

    public void generateInputStatements(CtBlock<?> mainBody, SplitFunction splitFunction) {
        String statement = "com.google.gson.Gson gson = new com.google.gson.Gson()";
        addStatementToBody(mainBody, statement);

        // GCP needs to parse the input from the request
        if (provider.equals(FunctionProvider.GCP)) {
            statement = "java.util.Map<java.lang.String, java.lang.String> input = gson.fromJson(request.getReader(), java.util.Map.class)";
            addStatementToBody(mainBody, statement);
        }

        // depending on the data transfer mode, different input variables need to be parsed
        // TODO handle this correctly
        boolean isManual = (splitFunction.getSplitFunctionFileIO().getDataTransfer()) == DataTransferMode.MANUAL;
        boolean isNone = (splitFunction.getSplitFunctionFileIO().getDataTransfer()) == DataTransferMode.NONE;
        if (!isManual && !isNone) {
            statement = "java.lang.String downloadBucketUri = gson.fromJson(input.get(\"downloadBucketUri\"), new com.google.gson.reflect.TypeToken<java.lang.String>(){}.getType())";
            addStatementToBody(mainBody, statement);
            statement = "java.lang.String uploadBucketUri = gson.fromJson(input.get(\"uploadBucketUri\"), new com.google.gson.reflect.TypeToken<java.lang.String>(){}.getType())";
            addStatementToBody(mainBody, statement);
        } else if (isManual) {
            statement = "java.util.List<String> downloadUris = gson.fromJson(input.get(\"downloadUris\"), new com.google.gson.reflect.TypeToken<java.util.List<java.lang.String>>(){}.getType())";
            addStatementToBody(mainBody, statement);
            statement = "java.util.List<String> uploadUris = gson.fromJson(input.get(\"uploadUris\"), new com.google.gson.reflect.TypeToken<java.util.List<java.lang.String>>(){}.getType())";
            addStatementToBody(mainBody, statement);
        }

        if (splitFunction.isDirectoryMonitoring()) {
            statement = "java.lang.Boolean enableDirectoryMonitoring = gson.fromJson(input.get(\"enableDirectoryMonitoring\"), new com.google.gson.reflect.TypeToken<java.lang.Boolean>(){}.getType())";
            addStatementToBody(mainBody, statement);
            statement = "java.lang.String monitorUtilsBucketUri = gson.fromJson(input.get(\"monitorUtilsBucketUri\"), new com.google.gson.reflect.TypeToken<java.lang.String>(){}.getType())";
            addStatementToBody(mainBody, statement);
        }

        // add all the input variables to the body
        splitFunction.getSplitFunctionVariableIO().getInputVariables().forEach(variable -> {
            String lambdaStatement = variable.getClassType() + " " + variable.getIdentifier() + " = " + "gson.fromJson(input.get(\"" + variable.getIdentifier() + "\"), new com.google.gson.reflect.TypeToken<" + variable.getClassType() + ">(){}.getType())";
            addStatementToBody(mainBody, lambdaStatement);
        });

    }

    public void generateOutputStatements(CtBlock<?> mainBody, SplitFunction splitFunction) {
        String statement = "java.util.HashMap<java.lang.String, java.lang.Object> output = new java.util.HashMap<>()";
        addStatementToBody(mainBody, statement);

        splitFunction.getSplitFunctionVariableIO().getOutputVariables().forEach(variable -> {
            String lambdaStatement = "output.put(\"" + variable.getIdentifier() + "\", " + variable.getIdentifier() + ")";
            addStatementToBody(mainBody, lambdaStatement);
        });

        if (splitFunction.getSplitFunctionFileIO().isTraceFileTransfer()) {
            statement = "output.put(\"fileTransfers\", fileTransfers)";
            addStatementToBody(mainBody, statement);
        }

        if (splitFunction.isDirectoryMonitoring()) {
            statement = "output.put(\"monitorResult\", directoryMonitor.getMonitorResults())";
            addStatementToBody(mainBody, statement);
        }

        if (splitFunction.isMeasureExecutionTime()) {
            statement = "output.put(\"codeExecutionTimeNs\", codeExecutionTimeNs)";
            addStatementToBody(mainBody, statement);
        }

        if (splitFunction.isMeasureExecutionTime()) {
            statement = "output.put(\"functionExecutionTimeNs\", System.nanoTime() - functionStartTimeNs)";
            addStatementToBody(mainBody, statement);
        }

        switch (provider) {
            case AWS -> {
                statement = "return output";
                addStatementToBody(mainBody, statement);
            }
            case GCP -> {
                statement = "response.getWriter().write(gson.toJson(output))";
                addStatementToBody(mainBody, statement);
            }
        }
    }

    private void addStatementToBody(CtBlock<?> mainBody, String statement) {
        Factory factory = mainBody.getFactory();
        CtCodeSnippetStatement snippetStatement = factory.Core().createCodeSnippetStatement();
        snippetStatement.setValue(statement);
        mainBody.addStatement(snippetStatement);
    }
}
