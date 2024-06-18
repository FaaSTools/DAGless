package dev.dagless.service.generation;

import dev.dagless.model.config.DataTransferMode;
import dev.dagless.model.config.FunctionProvider;
import dev.dagless.model.splitting.SplitFunction;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.factory.Factory;

public class FileIOService {

    String workingDirectory;

    public FileIOService(FunctionProvider provider) {
        resolveWorkingDirectory(provider);
    }

    private void resolveWorkingDirectory(FunctionProvider provider) {
        switch (provider) {
            case AWS -> this.workingDirectory = "/tmp/";
            case GCP -> this.workingDirectory = "/workspace/";
            default -> throw new IllegalStateException("Unexpected value: " + provider);
        }
    }

    /**
     * Generates the download statements for the split function, this method considers:
     * - if directory monitoring is enabled -> download the monitor directory
     * - if syncDirectories is enabled -> download the function directory
     * - if syncDirectories is disabled -> download input files of the function
     *
     * @param mainBody
     * @param splitFunction
     */
    public void generateDownloadStatements(CtBlock<?> mainBody, SplitFunction splitFunction) {
        if (splitFunction.getSplitFunctionFileIO().getDataTransfer().equals(DataTransferMode.NONE))
            return;

        String statement = "dev.dagless.JStorage jStorage = new dev.dagless.JStorage()";
        addStatementToBody(mainBody, statement);
        statement = "jStorage.clearFunctionDirectory()";
        addStatementToBody(mainBody, statement);

        // if traceFileTransfer is enabled -> create a list of file transfers
        if (splitFunction.getSplitFunctionFileIO().isTraceFileTransfer()) {
            statement = "java.util.List<dev.dagless.model.transfer.FileTransfer> fileTransfers = new java.util.ArrayList<>()";
            addStatementToBody(mainBody, statement);
        }

        // download the monitor directory if directory monitoring is enabled
        if (splitFunction.isDirectoryMonitoring()) {
            statement = splitFunction.getSplitFunctionFileIO().generateDownloadStatement("monitor/", workingDirectory);
            addStatementToBody(mainBody, statement);
        }
        // depending on the data transfer mode, download the function directory or the input files
        switch (splitFunction.getSplitFunctionFileIO().getDataTransfer()) {
            case COMPLETE -> {
                statement = splitFunction.getSplitFunctionFileIO().generateDownloadStatement("function/", workingDirectory);
                addStatementToBody(mainBody, statement);
            }
            case HARDCODED -> {
                splitFunction.getSplitFunctionFileIO().getDownloadFilePaths().forEach(file -> {
                    String lambdaStatement = splitFunction.getSplitFunctionFileIO().generateDownloadStatement(removeSurroundingQuotes(file), prependWorkingDir(removeSurroundingQuotes(file)));
                    addStatementToBody(mainBody, lambdaStatement);
                });
            }
            case MANUAL -> {
                statement = splitFunction.getSplitFunctionFileIO().generateManualDownloadStatement();
                addStatementToBody(mainBody, statement);
            }
        }
    }

    /**
     * Generates the upload statements for the split function, this method considers:
     * - if syncDirectories is enabled -> upload the function directory
     * - if syncDirectories is disabled -> upload output files of the function
     *
     * @param mainBody
     * @param splitFunction
     */
    public void generateUploadStatements(CtBlock<?> mainBody, SplitFunction splitFunction) {
        String statement;
        switch (splitFunction.getSplitFunctionFileIO().getDataTransfer()) {
            case COMPLETE -> {
                statement = splitFunction.getSplitFunctionFileIO().generateUploadStatement(prependWorkingDir("function/"), "");
                addStatementToBody(mainBody, statement);
            }
            case HARDCODED -> {
                splitFunction.getSplitFunctionFileIO().getUploadFilePaths().forEach(file -> {
                    String lambdaStatement = splitFunction.getSplitFunctionFileIO().generateUploadStatement(prependWorkingDir(removeSurroundingQuotes(file)), removeSurroundingQuotes(file));
                    addStatementToBody(mainBody, lambdaStatement);
                });
            }
            case MANUAL -> {
                statement = splitFunction.getSplitFunctionFileIO().generateManualUploadStatement();
                addStatementToBody(mainBody, statement);
            }
        }
    }

    private String prependWorkingDir(String path) {
        if (path.startsWith("/")) {
            return workingDirectory + path.substring(1);
        }
        return workingDirectory + path;
    }

    private String removeSurroundingQuotes(String string) {
        if (string.startsWith("\"") && string.endsWith("\"")) {
            return string.substring(1, string.length() - 1);
        }
        return string;
    }

    private void addStatementToBody(CtBlock<?> mainBody, String statement) {
        Factory factory = mainBody.getFactory();
        CtCodeSnippetStatement snippetStatement = factory.Core().createCodeSnippetStatement();
        snippetStatement.setValue(statement);
        mainBody.addStatement(snippetStatement);
    }

}
