package dev.dagless.service.splitting;

import dev.dagless.model.Variable;
import dev.dagless.model.config.Config;
import dev.dagless.model.splitting.SplitFunction;
import dev.dagless.model.splitting.SplitFunctionVariableIO;
import dev.dagless.model.splitting.StatementRange;
import dev.dagless.model.splitting.VariableOptimizedWorkflow;
import spoon.Launcher;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.logging.Logger;

public abstract class AbstractSplitService {

    protected final Logger logger = Logger.getLogger(AbstractSplitService.class.getName());
    protected final Config config;
    protected final Set<SplitFunction> splitFunctions;
    protected int order = 0;
    protected StatementRange statementRange;
    protected SplitFunctionVariableIO splitFunctionVariableIO;
    protected List<CtStatement> functionStatements;
    protected List<CtStatement> ctStatements;

    protected AbstractSplitService(Config config) {
        this.config = config;
        this.statementRange = new StatementRange();
        this.splitFunctionVariableIO = new SplitFunctionVariableIO();
        this.functionStatements = new ArrayList<>();
        this.splitFunctions = new TreeSet<>();
        this.ctStatements = getMainMethod().getBody().getStatements();
    }

    public abstract VariableOptimizedWorkflow createWorkflow();

    protected CtMethod<?> getMainMethod() {
        Launcher launcher = new Launcher();
        launcher.addInputResource(config.getPathToInputProject().toString());
        launcher.getEnvironment().setAutoImports(false);
        launcher.run();

        return launcher.getFactory().Method().getMainMethods().stream().findFirst().orElseThrow();
    }

    protected void resetSplitting() {
        // reset the statements for the next function
        functionStatements = new ArrayList<>();
        // increment the order
        order++;
        // adjust statement numbers
        statementRange.setStartToEnd();
        // output variables become input variables for the next function
        splitFunctionVariableIO.setOutputAsInput();
        // make all variable none parallelizable
        splitFunctionVariableIO.resetParallelVariables();
    }

    /**
     * TODO write doc here
     */
    protected void performForEachModifications() {
        // since in ConfigBased splitting we do not know how many for each loop there are,
        // we need to iterate over the functionStatements
        functionStatements
                .stream()
                .filter(ctStatement -> ctStatement instanceof CtForEach)
                .forEach(ctStatement -> {
                    CtForEach ctForEach = (CtForEach) ctStatement;
                    addCollectionVariableAsFunctionInput(ctForEach);
                });

        // collect all collection names so the declarations can be removed
        List<String> collectionsNamesToRemove = functionStatements
                .stream()
                .filter(ctStatement -> ctStatement instanceof CtForEach)
                .map(ctStatement -> ((CtForEach) ctStatement).getExpression().toString())
                .toList();

        Predicate<CtStatement> removeCollectionDeclaration = ctStatement -> {
            if (ctStatement instanceof CtLocalVariable<?> ctLocalVariable) {
                return collectionsNamesToRemove.contains(ctLocalVariable.getReference().getSimpleName());
            }
            return false;
        };

        functionStatements.removeIf(removeCollectionDeclaration);
    }

    private void addCollectionVariableAsFunctionInput(CtForEach ctForEach){
        // get the name and type of the collection
        String collectionName = ctForEach.getExpression().toString();
        String collectionType = ctForEach.getExpression().getType().toString();

        // since the collection should be parallelizable the collection needs to be an input to the function
        Variable collection = new Variable(collectionName, collectionType, true);
        splitFunctionVariableIO.addParallelCollection(collection);
    }

    protected boolean hasForEachLoop() {
        return functionStatements.stream().anyMatch(ctStatement -> ctStatement instanceof CtForEach);
    }
}
