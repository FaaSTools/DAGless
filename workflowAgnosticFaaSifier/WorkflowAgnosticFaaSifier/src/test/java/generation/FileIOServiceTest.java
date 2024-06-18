package generation;

import dev.dagless.model.config.Config;
import dev.dagless.model.config.FunctionProvider;
import dev.dagless.model.splitting.SplitFunction;
import dev.dagless.model.splitting.SplitFunctionFileIO;
import dev.dagless.model.splitting.SplitFunctionVariableIO;
import dev.dagless.model.splitting.StatementRange;
import dev.dagless.service.generation.FileIOService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtStatement;
import spoon.reflect.factory.CoreFactory;
import spoon.reflect.factory.Factory;
import spoon.reflect.factory.FactoryImpl;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileIOServiceTest {
    // TODO implement tests for FileIOService when functionality is finalized
    /*

    CtBlock<?> mainBody;
    Config config;
    List<String> inputFiles;
    List<String> outputFiles;
    StatementRange statementRange;
    SplitFunctionVariableIO splitFunctionVariableIO;
    List<CtStatement> statements;
    int order;

    @BeforeEach
    public void setupBody() {
        // Create a Spoon Launcher
        Launcher launcher = new Launcher();

        CoreFactory coreFactory = launcher.getFactory().Core();

        Factory factory = new FactoryImpl(coreFactory, launcher.getEnvironment());

        // Create an empty CtBlock
        mainBody = factory.createBlock();
    }

    @BeforeEach
    public void setupSplitFunction(){
        order = 0;
        statementRange = new StatementRange();
        config = new Config();
        config.setSyncDirectories(true);
        config.setDirectoryMonitoring(true);
        config.setTraceFileTransfer(true);
        config.setWorkflowInputs(new ArrayList<>());
        config.setWorkflowOutputs(new ArrayList<>());
        splitFunctionVariableIO = new SplitFunctionVariableIO(config);

        inputFiles = new ArrayList<>();
        inputFiles.add("inputFile1.txt");
        inputFiles.add("inputFile2.txt");

        outputFiles = new ArrayList<>();
        outputFiles.add("outputFile1.txt");
        outputFiles.add("outputFile2.txt");

        statements = new ArrayList<>();
    }

    @Test
    public void testDownloadInit() {
        FileIOService fileIOService = new FileIOService(FunctionProvider.AWS);
        SplitFunctionFileIO splitFunctionFileIO = new SplitFunctionFileIO(config);
        SplitFunction splitFunction = new SplitFunction(order, statementRange, splitFunctionVariableIO, statements, splitFunctionFileIO, false, false);

        fileIOService.generateDownloadStatements(mainBody, splitFunction);

        // init jStorage
        String expected = "dev.dagless.JStorage jStorage = new dev.dagless.JStorage()";
        String actual = mainBody.getStatement(0).toString();

        assertEquals(expected, actual);
    }

    @Test
    public void testDownloadInitTransferList() {
        FileIOService fileIOService = new FileIOService(FunctionProvider.AWS);
        SplitFunctionFileIO splitFunctionFileIO = new SplitFunctionFileIO(config);
        SplitFunction splitFunction = new SplitFunction(order, statementRange, splitFunctionVariableIO, statements, splitFunctionFileIO, false, false);

        fileIOService.generateDownloadStatements(mainBody, splitFunction);

        // init jStorage
        String expected = "java.util.List<dev.dagless.model.transfer.FileTransfer> fileTransfers = new java.util.ArrayList<>()";
        String actual = mainBody.getStatement(1).toString();

        assertEquals(expected, actual);
    }

    @Test
    public void testDownloadDirectoryMonitoringAWS() {
        FileIOService fileIOService = new FileIOService(FunctionProvider.AWS);
        SplitFunctionFileIO splitFunctionFileIO = new SplitFunctionFileIO(config);
        SplitFunction splitFunction = new SplitFunction(order, statementRange, splitFunctionVariableIO, statements, splitFunctionFileIO, true, false);

        fileIOService.generateDownloadStatements(mainBody, splitFunction);

        // init jStorage
        String expected = "fileTransfers.add(jStorage.copyTraced( bucketUri + \"monitor/\", \"/tmp/\"))";
        String actual = mainBody.getStatement(2).toString();

        assertEquals(expected, actual);
    }

    @Test
    public void testDownloadDirectoryMonitoringGCP() {
        FileIOService fileIOService = new FileIOService(FunctionProvider.GCP);
        SplitFunctionFileIO splitFunctionFileIO = new SplitFunctionFileIO(config);
        SplitFunction splitFunction = new SplitFunction(order, statementRange, splitFunctionVariableIO, statements, splitFunctionFileIO, true, false);

        fileIOService.generateDownloadStatements(mainBody, splitFunction);

        String expected = "fileTransfers.add(jStorage.copyTraced( bucketUri + \"monitor/\", \"/workspace/\"))";
        String actual = mainBody.getStatement(2).toString();

        assertEquals(expected, actual);
    }

    @Test
    public void testDownloadFunctionDirectoryAWS() {
        FileIOService fileIOService = new FileIOService(FunctionProvider.AWS);
        SplitFunctionFileIO splitFunctionFileIO = new SplitFunctionFileIO(config);
        SplitFunction splitFunction = new SplitFunction(order, statementRange, splitFunctionVariableIO, statements, splitFunctionFileIO, true, false);

        fileIOService.generateDownloadStatements(mainBody, splitFunction);

        String expected = "fileTransfers.add(jStorage.copyTraced( bucketUri + \"function/\", \"/tmp/\"))";
        String actual = mainBody.getStatement(3).toString();

        assertEquals(expected, actual);
    }

    @Test
    public void testDownloadFunctionDirectoryGCP() {
        FileIOService fileIOService = new FileIOService(FunctionProvider.GCP);
        SplitFunctionFileIO splitFunctionFileIO = new SplitFunctionFileIO(config);
        SplitFunction splitFunction = new SplitFunction(order, statementRange, splitFunctionVariableIO, statements, splitFunctionFileIO, true, false);

        fileIOService.generateDownloadStatements(mainBody, splitFunction);

        String expected = "fileTransfers.add(jStorage.copyTraced( bucketUri + \"function/\", \"/workspace/\"))";
        String actual = mainBody.getStatement(3).toString();

        assertEquals(expected, actual);
    }

    @Test
    public void testDownloadFileDownloadsAWS() {
        FileIOService fileIOService = new FileIOService(FunctionProvider.AWS);
        config.setSyncDirectories(false);
        SplitFunctionFileIO splitFunctionFileIO = new SplitFunctionFileIO(inputFiles, outputFiles, config);
        SplitFunction splitFunction = new SplitFunction(order, statementRange, splitFunctionVariableIO, statements, splitFunctionFileIO, true, false);

        fileIOService.generateDownloadStatements(mainBody, splitFunction);

        String expected = "fileTransfers.add(jStorage.copyTraced( bucketUri + \"inputFile1.txt\", \"/tmp/inputFile1.txt\"))";
        String actual = mainBody.getStatement(3).toString();

        assertEquals(expected, actual);

        expected = "fileTransfers.add(jStorage.copyTraced( bucketUri + \"inputFile2.txt\", \"/tmp/inputFile2.txt\"))";
        actual = mainBody.getStatement(4).toString();

        assertEquals(expected, actual);
    }

    @Test
    public void testDownloadFileDownloadsGCP() {
        FileIOService fileIOService = new FileIOService(FunctionProvider.GCP);
        config.setSyncDirectories(false);
        SplitFunctionFileIO splitFunctionFileIO = new SplitFunctionFileIO(inputFiles, outputFiles, config);
        SplitFunction splitFunction = new SplitFunction(order, statementRange, splitFunctionVariableIO, statements, splitFunctionFileIO, true, false);

        fileIOService.generateDownloadStatements(mainBody, splitFunction);

        String expected = "fileTransfers.add(jStorage.copyTraced( bucketUri + \"inputFile1.txt\", \"/workspace/inputFile1.txt\"))";
        String actual = mainBody.getStatement(3).toString();

        assertEquals(expected, actual);

        expected = "fileTransfers.add(jStorage.copyTraced( bucketUri + \"inputFile2.txt\", \"/workspace/inputFile2.txt\"))";
        actual = mainBody.getStatement(4).toString();

        assertEquals(expected, actual);
    }

    @Test
    public void testUploadFunctionDirectoryAWS(){
        FileIOService fileIOService = new FileIOService(FunctionProvider.AWS);
        SplitFunctionFileIO splitFunctionFileIO = new SplitFunctionFileIO(inputFiles, outputFiles, config);
        SplitFunction splitFunction = new SplitFunction(order, statementRange, splitFunctionVariableIO, statements, splitFunctionFileIO, true, false);

        fileIOService.generateUploadStatements(mainBody, splitFunction);

        String expected = "fileTransfers.add(jStorage.copyTraced(\"/tmp/function/\", bucketUri + \"\"))";
        String actual = mainBody.getStatement(0).toString();

        assertEquals(expected, actual);
    }

    @Test
    public void testUploadFunctionDirectoryGCP(){
        FileIOService fileIOService = new FileIOService(FunctionProvider.GCP);
        SplitFunctionFileIO splitFunctionFileIO = new SplitFunctionFileIO(inputFiles, outputFiles, config);
        SplitFunction splitFunction = new SplitFunction(order, statementRange, splitFunctionVariableIO, statements, splitFunctionFileIO, true, false);

        fileIOService.generateUploadStatements(mainBody, splitFunction);

        String expected = "fileTransfers.add(jStorage.copyTraced(\"/workspace/function/\", bucketUri + \"\"))";
        String actual = mainBody.getStatement(0).toString();

        assertEquals(expected, actual);
    }

    @Test
    public void testUploadFileUploadAWS(){
        FileIOService fileIOService = new FileIOService(FunctionProvider.AWS);
        config.setSyncDirectories(false);
        SplitFunctionFileIO splitFunctionFileIO = new SplitFunctionFileIO(inputFiles, outputFiles, config);
        SplitFunction splitFunction = new SplitFunction(order, statementRange, splitFunctionVariableIO, statements, splitFunctionFileIO, true, false);

        fileIOService.generateUploadStatements(mainBody, splitFunction);

        String expected = "fileTransfers.add(jStorage.copyTraced(\"/tmp/outputFile1.txt\", bucketUri + \"outputFile1.txt\"))";
        String actual = mainBody.getStatement(0).toString();

        assertEquals(expected, actual);

        expected = "fileTransfers.add(jStorage.copyTraced(\"/tmp/outputFile2.txt\", bucketUri + \"outputFile2.txt\"))";
        actual = mainBody.getStatement(1).toString();

        assertEquals(expected, actual);
    }

    @Test
    public void testUploadFileUploadGCP(){
        FileIOService fileIOService = new FileIOService(FunctionProvider.GCP);
        config.setSyncDirectories(false);
        SplitFunctionFileIO splitFunctionFileIO = new SplitFunctionFileIO(inputFiles, outputFiles, config);
        SplitFunction splitFunction = new SplitFunction(order, statementRange, splitFunctionVariableIO, statements, splitFunctionFileIO, true, false);

        fileIOService.generateUploadStatements(mainBody, splitFunction);

        String expected = "fileTransfers.add(jStorage.copyTraced(\"/workspace/outputFile1.txt\", bucketUri + \"outputFile1.txt\"))";
        String actual = mainBody.getStatement(0).toString();

        assertEquals(expected, actual);

        expected = "fileTransfers.add(jStorage.copyTraced(\"/workspace/outputFile2.txt\", bucketUri + \"outputFile2.txt\"))";
        actual = mainBody.getStatement(1).toString();

        assertEquals(expected, actual);
    }

     */

}
