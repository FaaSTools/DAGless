package splitting;

import dev.dagless.model.Variable;
import dev.dagless.model.config.Config;
import dev.dagless.model.splitting.AbstractWorkflow;
import dev.dagless.model.splitting.SplitFunction;
import dev.dagless.model.splitting.SplitRule;
import dev.dagless.service.splitting.RuleBasedSplitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RuleBasedSplitServiceTest {

    Config mockConfig;

    @BeforeEach
    public void setupConfigMock(){
        mockConfig = mock(Config.class);
        when(mockConfig.getTraceFileTransfer()).thenReturn(false);
        when(mockConfig.getDirectoryMonitoring()).thenReturn(false);
        when(mockConfig.getMeasureExecutionTime()).thenReturn(false);
    }

    @Test
    public void testSingleStatement(){
        when(mockConfig.getPathToInputProject()).thenReturn(Path.of("src/test/java/splitting/sample/SingleStatement.java"));
        when(mockConfig.getPathToSplitFunctions()).thenReturn(Path.of("/tmp/"));
        when(mockConfig.getSplitRules()).thenReturn(Set.of());
        when(mockConfig.getWorkflowInputs()).thenReturn(Set.of());
        when(mockConfig.getWorkflowOutputs()).thenReturn(Set.of());

        RuleBasedSplitService ruleBasedSplitService = new RuleBasedSplitService(mockConfig);

        AbstractWorkflow workflow = ruleBasedSplitService.createWorkflow();

        assertEquals(1, workflow.getSplitFunctions().size());

        Set<SplitFunction> splitFunctions = workflow.getSplitFunctions();

        // checking order
        assertEquals(0, splitFunctions.stream().findFirst().orElseThrow().getOrder());
        // check if the number of statements is correct
        assertEquals(1, splitFunctions.stream().findFirst().orElseThrow().getFunctionStatements().size());
        // check if the statement range is correct
        assertEquals(1, splitFunctions.stream().findFirst().orElseThrow().getStatementRange().getStart());
        assertEquals(1, splitFunctions.stream().findFirst().orElseThrow().getStatementRange().getEnd());
        // check if the split function variable IO is correct
        assertTrue(splitFunctions.stream().findFirst().orElseThrow().getSplitFunctionVariableIO().getInputVariables().isEmpty());
        assertTrue(splitFunctions.stream().findFirst().orElseThrow().getSplitFunctionVariableIO().getOutputVariables().isEmpty());
        // checking boolean flags
        assertFalse(splitFunctions.stream().findFirst().orElseThrow().isDirectoryMonitoring());
        assertFalse(splitFunctions.stream().findFirst().orElseThrow().isForEachLoop());
        assertFalse(splitFunctions.stream().findFirst().orElseThrow().isMeasureExecutionTime());
    }

    @Test
    public void testWorkflowVariableIO(){
        Set<Variable> workflowInputVars = Set.of(
                new Variable("a", "int"),   // should replace declaration of a
                new Variable("c", "int"),   // should be ignored
                new Variable("d", "int")    // should be used due to output
        );
        // TODO: output variables which are never declared in the function should be ignored
        Set<Variable> workflowOutputVars = Set.of(
                new Variable("d", "int")
        );

        when(mockConfig.getPathToInputProject()).thenReturn(Path.of("src/test/java/splitting/sample/WorkflowVariableIO.java"));
        when(mockConfig.getPathToSplitFunctions()).thenReturn(Path.of("/tmp/"));
        when(mockConfig.getSplitRules()).thenReturn(Set.of());
        when(mockConfig.getWorkflowInputs()).thenReturn(workflowInputVars);
        when(mockConfig.getWorkflowOutputs()).thenReturn(workflowOutputVars);


        RuleBasedSplitService ruleBasedSplitService = new RuleBasedSplitService(mockConfig);

        AbstractWorkflow workflow = ruleBasedSplitService.createWorkflow();

        assertEquals(1, workflow.getSplitFunctions().size());

        Set<SplitFunction> splitFunctions = workflow.getSplitFunctions();

        // checking order
        assertEquals(0, splitFunctions.stream().findFirst().orElseThrow().getOrder());
        // 3 since int a = 1 is replaced by the workflow input variable
        assertEquals(3, splitFunctions.stream().findFirst().orElseThrow().getFunctionStatements().size());
        // check if the statement range is correct
        assertEquals(1, splitFunctions.stream().findFirst().orElseThrow().getStatementRange().getStart());
        assertEquals(4, splitFunctions.stream().findFirst().orElseThrow().getStatementRange().getEnd());
        // 2 since int c = 3 is ignored and int d = 4 is used due to output
        assertEquals(2, splitFunctions.stream().findFirst().orElseThrow().getSplitFunctionVariableIO().getInputVariables().size());
        // 1 since int d = 4 is used due to output
        assertEquals(1, splitFunctions.stream().findFirst().orElseThrow().getSplitFunctionVariableIO().getOutputVariables().size());
        // checking boolean flags
        assertFalse(splitFunctions.stream().findFirst().orElseThrow().isDirectoryMonitoring());
        assertFalse(splitFunctions.stream().findFirst().orElseThrow().isForEachLoop());
        assertFalse(splitFunctions.stream().findFirst().orElseThrow().isMeasureExecutionTime());
    }

    @Test
    public void testCustomMethodInvocationsWithOutWorkflowIO(){
        when(mockConfig.getPathToInputProject()).thenReturn(Path.of("src/test/java/splitting/sample/CustomMethodInvocations.java"));
        when(mockConfig.getPathToSplitFunctions()).thenReturn(Path.of("/tmp/"));
        when(mockConfig.getSplitRules()).thenReturn(Set.of(SplitRule.CUSTOM_METHOD_INVOCATION));
        when(mockConfig.getWorkflowInputs()).thenReturn(Set.of());
        when(mockConfig.getWorkflowOutputs()).thenReturn(Set.of());

        RuleBasedSplitService ruleBasedSplitService = new RuleBasedSplitService(mockConfig);

        AbstractWorkflow workflow = ruleBasedSplitService.createWorkflow();

        assertEquals(3, workflow.getSplitFunctions().size());

        Set<SplitFunction> splitFunctions = workflow.getSplitFunctions();

        // checking order
        List<SplitFunction> splitFunctionList = splitFunctions.stream().toList();
        for (int i = 0; i < splitFunctions.size(); i++) {
            assertEquals(i, splitFunctionList.get(i).getOrder());
        }

        // checking first function
        SplitFunction firstFunction = splitFunctionList.get(0);
        // variable IO
        assertTrue(firstFunction.getSplitFunctionVariableIO().getInputVariables().isEmpty());
        // a and c
        assertEquals(2, firstFunction.getSplitFunctionVariableIO().getOutputVariables().size());
        // statement range
        assertEquals(1, firstFunction.getStatementRange().getStart());
        assertEquals(4, firstFunction.getStatementRange().getEnd());

        // checking second function
        SplitFunction secondFunction = splitFunctionList.get(1);
        // variable IO
        // a and c
        assertEquals(2, secondFunction.getSplitFunctionVariableIO().getInputVariables().size());
        // a, b, and d
        assertEquals(3, secondFunction.getSplitFunctionVariableIO().getOutputVariables().size());
        // statement range
        assertEquals(5, secondFunction.getStatementRange().getStart());
        assertEquals(7, secondFunction.getStatementRange().getEnd());

        // checking third function
        SplitFunction thirdFunction = splitFunctionList.get(2);
        // variable IO
        // a, b, and d
        assertEquals(3, thirdFunction.getSplitFunctionVariableIO().getInputVariables().size());
        assertTrue(thirdFunction.getSplitFunctionVariableIO().getOutputVariables().isEmpty());
        // statement range
        assertEquals(8, thirdFunction.getStatementRange().getStart());
        assertEquals(9, thirdFunction.getStatementRange().getEnd());


        // checking boolean flags
        assertFalse(splitFunctions.stream().reduce(true , (a, b) -> a && b.isDirectoryMonitoring(), Boolean::logicalAnd));
        assertFalse(splitFunctions.stream().reduce(true , (a, b) -> a && b.isForEachLoop(), Boolean::logicalAnd));
        assertFalse(splitFunctions.stream().reduce(true , (a, b) -> a && b.isMeasureExecutionTime(), Boolean::logicalAnd));
    }

    @Test
    public void testCustomMethodInvocationsWithWorkflowIO(){
        Set<Variable> workflowInputVars = Set.of(
                new Variable("workflowOutput", "int"),  // should be added due to output
                new Variable("b", "int"),               // should be added since it is declared in second function
                new Variable("notDeclared", "int")      // should not be added since it is not declared
        );
        // TODO: output variables which are never declared in the function should be ignored
        Set<Variable> workflowOutputVars = Set.of(
                new Variable("workflowOutput", "int")
        );
        when(mockConfig.getPathToInputProject()).thenReturn(Path.of("src/test/java/splitting/sample/CustomMethodInvocations.java"));
        when(mockConfig.getPathToSplitFunctions()).thenReturn(Path.of("/tmp/"));
        when(mockConfig.getSplitRules()).thenReturn(Set.of(SplitRule.CUSTOM_METHOD_INVOCATION));
        when(mockConfig.getWorkflowInputs()).thenReturn(workflowInputVars);
        when(mockConfig.getWorkflowOutputs()).thenReturn(workflowOutputVars);

        RuleBasedSplitService ruleBasedSplitService = new RuleBasedSplitService(mockConfig);

        AbstractWorkflow workflow = ruleBasedSplitService.createWorkflow();

        assertEquals(3, workflow.getSplitFunctions().size());

        Set<SplitFunction> splitFunctions = workflow.getSplitFunctions();

        // checking order
        List<SplitFunction> splitFunctionList = splitFunctions.stream().toList();
        for (int i = 0; i < splitFunctions.size(); i++) {
            assertEquals(i, splitFunctionList.get(i).getOrder());
        }

        // checking first function
        SplitFunction firstFunction = splitFunctionList.get(0);
        // variable IO - two since "b" and "workflowOutput" are added to the input variables
        assertEquals(2, firstFunction.getSplitFunctionVariableIO().getInputVariables().size());
        // a, b, c and workflowOutput
        assertEquals(4, firstFunction.getSplitFunctionVariableIO().getOutputVariables().size());
        // statement range
        assertEquals(1, firstFunction.getStatementRange().getStart());
        assertEquals(4, firstFunction.getStatementRange().getEnd());

        // checking second function
        SplitFunction secondFunction = splitFunctionList.get(1);
        // variable IO
        // a, b, c and workflowOutput
        assertEquals(4, secondFunction.getSplitFunctionVariableIO().getInputVariables().size());
        // a, b, d and workflowOutput
        assertEquals(4, secondFunction.getSplitFunctionVariableIO().getOutputVariables().size());
        // statement range
        assertEquals(5, secondFunction.getStatementRange().getStart());
        assertEquals(7, secondFunction.getStatementRange().getEnd());

        // checking third function
        SplitFunction thirdFunction = splitFunctionList.get(2);
        // variable IO
        // a, b, d and workflowOutput
        assertEquals(4, thirdFunction.getSplitFunctionVariableIO().getInputVariables().size());
        // workflowOutput
        assertEquals(1, thirdFunction.getSplitFunctionVariableIO().getOutputVariables().size());
        // statement range
        assertEquals(8, thirdFunction.getStatementRange().getStart());
        assertEquals(9, thirdFunction.getStatementRange().getEnd());


        // checking boolean flags
        assertFalse(splitFunctions.stream().reduce(true , (a, b) -> a && b.isDirectoryMonitoring(), Boolean::logicalAnd));
        assertFalse(splitFunctions.stream().reduce(true , (a, b) -> a && b.isForEachLoop(), Boolean::logicalAnd));
        assertFalse(splitFunctions.stream().reduce(true , (a, b) -> a && b.isMeasureExecutionTime(), Boolean::logicalAnd));
    }

    @Test
    public void testSingleForEachLoop(){
        when(mockConfig.getPathToInputProject()).thenReturn(Path.of("src/test/java/splitting/sample/SingleForEachLoop.java"));
        when(mockConfig.getPathToSplitFunctions()).thenReturn(Path.of("/tmp/"));
        when(mockConfig.getSplitRules()).thenReturn(Set.of(SplitRule.FOR_EACH));
        when(mockConfig.getWorkflowInputs()).thenReturn(Set.of());
        when(mockConfig.getWorkflowOutputs()).thenReturn(Set.of());

        AbstractWorkflow workflow = new RuleBasedSplitService(mockConfig).createWorkflow();

        // BEGIN ASSERTIONS
        assertEquals(2, workflow.getSplitFunctions().size());

        Set<SplitFunction> splitFunctions = workflow.getSplitFunctions();

        // checking order
        for (int i = 0; i < splitFunctions.size(); i++) {
            assertEquals(i, splitFunctions.stream().toList().get(i).getOrder());
        }

        // checking first function
        SplitFunction firstFunction = splitFunctions.stream().findFirst().orElseThrow();
        // variable IO
        assertTrue(firstFunction.getSplitFunctionVariableIO().getInputVariables().isEmpty());
        // numbers
        assertEquals(1, firstFunction.getSplitFunctionVariableIO().getOutputVariables().size());
        // statement range
        assertEquals(1, firstFunction.getStatementRange().getStart());
        assertEquals(1, firstFunction.getStatementRange().getEnd());
        // flag
        assertFalse(firstFunction.isForEachLoop());

        // checking second function
        SplitFunction secondFunction = splitFunctions.stream().toList().get(1);
        // variable IO
        // numbers
        assertEquals(1, secondFunction.getSplitFunctionVariableIO().getInputVariables().size());
        Variable inputVariable = secondFunction.getSplitFunctionVariableIO().getInputVariables().stream().findFirst().orElseThrow();
        assertEquals("numbers", inputVariable.getIdentifier());
        assertEquals("int[]", inputVariable.getType());
        assertTrue(inputVariable.isParallel());
        // none since no workflow output is defined
        assertEquals(0, secondFunction.getSplitFunctionVariableIO().getOutputVariables().size());
        // statement range
        assertEquals(2, secondFunction.getStatementRange().getStart());
        assertEquals(2, secondFunction.getStatementRange().getEnd());
        // flag
        assertTrue(secondFunction.isForEachLoop());

        // checking boolean flags
        assertFalse(splitFunctions.stream().findFirst().orElseThrow().isDirectoryMonitoring());
        assertFalse(splitFunctions.stream().findFirst().orElseThrow().isMeasureExecutionTime());
    }

    @Test
    public void testComments(){
        when(mockConfig.getPathToInputProject()).thenReturn(Path.of("src/test/java/splitting/sample/Comments.java"));
        when(mockConfig.getPathToSplitFunctions()).thenReturn(Path.of("/tmp/"));
        when(mockConfig.getSplitRules()).thenReturn(Set.of(SplitRule.CUSTOM_METHOD_INVOCATION));
        when(mockConfig.getWorkflowInputs()).thenReturn(Set.of());
        when(mockConfig.getWorkflowOutputs()).thenReturn(Set.of());

        AbstractWorkflow workflow = new RuleBasedSplitService(mockConfig).createWorkflow();

        // BEGIN ASSERTIONS

        // checking number of split functions
        assertEquals(2, workflow.getSplitFunctions().size());

        Set<SplitFunction> splitFunctions = workflow.getSplitFunctions();

        // checking order
        List<SplitFunction> splitFunctionList = splitFunctions.stream().toList();
        for (int i = 0; i < splitFunctions.size(); i++) {
            assertEquals(i, splitFunctionList.get(i).getOrder());
        }

        // checking first function
        SplitFunction firstFunction = splitFunctionList.get(0);
        // variable IO
        assertTrue(firstFunction.getSplitFunctionVariableIO().getInputVariables().isEmpty());
        // a and b
        assertEquals(2, firstFunction.getSplitFunctionVariableIO().getOutputVariables().size());
        // statement range
        assertEquals(1, firstFunction.getStatementRange().getStart());
        // 3 since the comment is not a statement
        assertEquals(3, firstFunction.getStatementRange().getEnd());

        // checking second function
        SplitFunction secondFunction = splitFunctionList.get(1);
        // variable IO
        // a and b
        assertEquals(2, secondFunction.getSplitFunctionVariableIO().getInputVariables().size());
        // none since no workflow output is defined
        assertEquals(0, secondFunction.getSplitFunctionVariableIO().getOutputVariables().size());
        // statement range
        assertEquals(4, secondFunction.getStatementRange().getStart());
        assertEquals(4, secondFunction.getStatementRange().getEnd());

        // checking boolean flags
        assertFalse(splitFunctions.stream().reduce(true , (a, b) -> a && b.isDirectoryMonitoring(), Boolean::logicalAnd));
        assertFalse(splitFunctions.stream().reduce(true , (a, b) -> a && b.isForEachLoop(), Boolean::logicalAnd));
        assertFalse(splitFunctions.stream().reduce(true , (a, b) -> a && b.isMeasureExecutionTime(), Boolean::logicalAnd));
    }
}
