package generation;

import dev.dagless.model.Variable;
import dev.dagless.model.config.Config;
import dev.dagless.model.config.FunctionProvider;
import dev.dagless.model.splitting.SplitFunction;
import dev.dagless.model.splitting.SplitFunctionFileIO;
import dev.dagless.model.splitting.SplitFunctionVariableIO;
import dev.dagless.model.splitting.StatementRange;
import dev.dagless.service.generation.VariableIOService;
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

import static org.junit.jupiter.api.Assertions.*;

public class VariableIOServiceTest {
    // TODO implement tests when functionality is finalized
    /*
    CtBlock<?> mainBody;
    Config config;
    List<Variable> inputVariables;
    List<Variable> outputVariables;
    StatementRange statementRange;
    SplitFunctionVariableIO splitFunctionVariableIO;
    List<CtStatement> statements;
    SplitFunctionFileIO splitFunctionFileIO;
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
        config.setWorkflowOutputs(new ArrayList<>());
        config.setWorkflowInputs(new ArrayList<>());

        statements = new ArrayList<>();
        splitFunctionFileIO = new SplitFunctionFileIO(config);

        inputVariables = new ArrayList<>();
        outputVariables = new ArrayList<>();

    }

    @Test
    public void testVariableIOInitAWS() {
        VariableIOService variableIOService = new VariableIOService(FunctionProvider.AWS);
        splitFunctionVariableIO = new SplitFunctionVariableIO(config);
        SplitFunction splitFunction = new SplitFunction(order, statementRange, splitFunctionVariableIO, statements, config);

        variableIOService.generateInputStatements(mainBody, splitFunction);

        String expected = "com.google.gson.Gson gson = new com.google.gson.Gson()";
        String actual = mainBody.getStatement(0).toString();
        assertEquals(expected, actual);

        expected = "String bucketUri = gson.fromJson(input.get(\"bucketUri\"), String.class)";
        actual = mainBody.getStatement(1).toString();
        assertEquals(expected, actual);

        Exception exception = assertThrows(IndexOutOfBoundsException.class, () -> {
            mainBody.getStatement(2).toString();
        });

        expected = "Index 2 out of bounds for length 2";
        actual = exception.getMessage();

        assertEquals(expected, actual);
    }

    @Test
    public void testVariableIOInitGCP() {
        VariableIOService variableIOService = new VariableIOService(FunctionProvider.GCP);
        splitFunctionVariableIO = new SplitFunctionVariableIO(config);
        SplitFunction splitFunction = new SplitFunction(order, statementRange, splitFunctionVariableIO, statements, config);

        variableIOService.generateInputStatements(mainBody, splitFunction);

        String expected = "com.google.gson.Gson gson = new com.google.gson.Gson()";
        String actual = mainBody.getStatement(0).toString();

        assertEquals(expected, actual);

        expected = "com.google.gson.JsonObject input = gson.fromJson(request.getReader(), com.google.gson.JsonObject.class)";
        actual = mainBody.getStatement(1).toString();

        assertEquals(expected, actual);
    }

    @Test
    public void testVariableIOInputStatemets(){
        VariableIOService variableIOService = new VariableIOService(FunctionProvider.AWS);
        inputVariables.add(new Variable("inputString", "String"));
        inputVariables.add(new Variable("inputInt", "int"));
        inputVariables.add(new Variable("inputDouble", "double"));
        inputVariables.add(new Variable("inputBoolean", "boolean"));
        inputVariables.add(new Variable("inputList", "java.util.List<java.lang.String>"));
        inputVariables.add(new Variable("inputMap", "java.util.Map<java.lang.String, java.lang.String>"));
        inputVariables.add(new Variable("inputVariable", "dev.dagless.model.Variable"));
        config.setWorkflowInputs(new ArrayList<>(inputVariables));
        splitFunctionVariableIO = new SplitFunctionVariableIO(config);
        SplitFunction splitFunction = new SplitFunction(order, statementRange, splitFunctionVariableIO, statements, config);

        variableIOService.generateInputStatements(mainBody, splitFunction);

        String expected = "String bucketUri = gson.fromJson(input.get(\"bucketUri\"), String.class)";
        String actual = mainBody.getStatement(1).toString();
        assertEquals(expected, actual);

        expected = "String inputString = gson.fromJson(input.get(\"inputString\"), String.class)";
        actual = mainBody.getStatement(2).toString();
        assertEquals(expected, actual);

        expected = "Integer inputInt = gson.fromJson(input.get(\"inputInt\"), Integer.class)";
        actual = mainBody.getStatement(3).toString();
        assertEquals(expected, actual);

        expected = "Double inputDouble = gson.fromJson(input.get(\"inputDouble\"), Double.class)";
        actual = mainBody.getStatement(4).toString();
        assertEquals(expected, actual);

        expected = "Boolean inputBoolean = gson.fromJson(input.get(\"inputBoolean\"), Boolean.class)";
        actual = mainBody.getStatement(5).toString();
        assertEquals(expected, actual);

        expected = "java.util.List<java.lang.String> inputList = gson.fromJson(input.get(\"inputList\"), java.util.List.class)";
        actual = mainBody.getStatement(6).toString();
        assertEquals(expected, actual);

        expected = "java.util.Map<java.lang.String, java.lang.String> inputMap = gson.fromJson(input.get(\"inputMap\"), java.util.Map.class)";
        actual = mainBody.getStatement(7).toString();
        assertEquals(expected, actual);

        expected = "dev.dagless.model.Variable inputVariable = gson.fromJson(input.get(\"inputVariable\"), dev.dagless.model.Variable.class)";
        actual = mainBody.getStatement(8).toString();
        assertEquals(expected, actual);
    }

    @Test
    public void testVariableIOOutputAWS() {
        VariableIOService variableIOService = new VariableIOService(FunctionProvider.AWS);
        config.setTraceFileTransfer(false);
        config.setDirectoryMonitoring(false);
        splitFunctionVariableIO = new SplitFunctionVariableIO(config);
        SplitFunction splitFunction = new SplitFunction(order, statementRange, splitFunctionVariableIO, statements, config);

        variableIOService.generateOutputStatements(mainBody, splitFunction);

        String expected = "java.util.HashMap<java.lang.String, java.lang.Object> output = new java.util.HashMap<>()";
        String actual = mainBody.getStatement(0).toString();
        assertEquals(expected, actual);

        expected = "return output";
        actual = mainBody.getStatement(1).toString();
        assertEquals(expected, actual);
    }

    @Test
    public void testVariableIOOutputGCP() {
        VariableIOService variableIOService = new VariableIOService(FunctionProvider.GCP);
        config.setTraceFileTransfer(false);
        config.setDirectoryMonitoring(false);
        splitFunctionVariableIO = new SplitFunctionVariableIO(config);
        SplitFunction splitFunction = new SplitFunction(order, statementRange, splitFunctionVariableIO, statements, config);

        variableIOService.generateOutputStatements(mainBody, splitFunction);

        String expected = "java.util.HashMap<java.lang.String, java.lang.Object> output = new java.util.HashMap<>()";
        String actual = mainBody.getStatement(0).toString();
        assertEquals(expected, actual);

        expected = "response.getWriter().write(gson.toJson(output))";
        actual = mainBody.getStatement(1).toString();
        assertEquals(expected, actual);
    }

    @Test
    public void testVariableIOTracing(){
        VariableIOService variableIOService = new VariableIOService(FunctionProvider.GCP);
        config.setDirectoryMonitoring(false);
        splitFunctionVariableIO = new SplitFunctionVariableIO(config);
        SplitFunction splitFunction = new SplitFunction(order, statementRange, splitFunctionVariableIO, statements, config);

        variableIOService.generateOutputStatements(mainBody, splitFunction);

        String expected = "java.util.HashMap<java.lang.String, java.lang.Object> output = new java.util.HashMap<>()";
        String actual = mainBody.getStatement(0).toString();
        assertEquals(expected, actual);

        expected = "output.put(\"fileTransfers\", fileTransfers)";
        actual = mainBody.getStatement(1).toString();
        assertEquals(expected, actual);
    }

    @Test
    public void testVariableIODirectoryMonitor(){
        VariableIOService variableIOService = new VariableIOService(FunctionProvider.GCP);
        config.setTraceFileTransfer(false);
        splitFunctionVariableIO = new SplitFunctionVariableIO(config);
        SplitFunction splitFunction = new SplitFunction(order, statementRange, splitFunctionVariableIO, statements, config);

        variableIOService.generateOutputStatements(mainBody, splitFunction);

        String expected = "java.util.HashMap<java.lang.String, java.lang.Object> output = new java.util.HashMap<>()";
        String actual = mainBody.getStatement(0).toString();
        assertEquals(expected, actual);

        expected = "output.put(\"monitorResult\", monitorResult)";
        actual = mainBody.getStatement(1).toString();
        assertEquals(expected, actual);
    }

    @Test
    public void testVariableIOStatements(){
        VariableIOService variableIOService = new VariableIOService(FunctionProvider.AWS);
        config.setTraceFileTransfer(false);
        config.setDirectoryMonitoring(false);
        outputVariables.add(new Variable("outputString", "String"));
        outputVariables.add(new Variable("outputInt", "int"));
        outputVariables.add(new Variable("outputDouble", "double"));
        outputVariables.add(new Variable("outputBoolean", "boolean"));
        outputVariables.add(new Variable("outputList", "java.util.List<java.lang.String>"));
        outputVariables.add(new Variable("outputMap", "java.util.Map<java.lang.String, java.lang.String>"));
        outputVariables.add(new Variable("outputVariable", "dev.dagless.model.Variable"));
        splitFunctionVariableIO = new SplitFunctionVariableIO(inputVariables, outputVariables);
        SplitFunction splitFunction = new SplitFunction(order, statementRange, splitFunctionVariableIO, statements, config);

        variableIOService.generateOutputStatements(mainBody, splitFunction);

        String expected = "output.put(\"outputString\", outputString)";
        String actual = mainBody.getStatement(1).toString();
        assertEquals(expected, actual);

        expected = "output.put(\"outputInt\", outputInt)";
        actual = mainBody.getStatement(2).toString();
        assertEquals(expected, actual);

        expected = "output.put(\"outputDouble\", outputDouble)";
        actual = mainBody.getStatement(3).toString();
        assertEquals(expected, actual);

        expected = "output.put(\"outputBoolean\", outputBoolean)";
        actual = mainBody.getStatement(4).toString();
        assertEquals(expected, actual);

        expected = "output.put(\"outputList\", outputList)";
        actual = mainBody.getStatement(5).toString();
        assertEquals(expected, actual);

        expected = "output.put(\"outputMap\", outputMap)";
        actual = mainBody.getStatement(6).toString();
        assertEquals(expected, actual);

        expected = "output.put(\"outputVariable\", outputVariable)";
        actual = mainBody.getStatement(7).toString();
        assertEquals(expected, actual);
    }

     */
}
