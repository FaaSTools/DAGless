package dev.dagless.process.generation;

import dev.dagless.model.config.FunctionProvider;
import dev.dagless.model.splitting.SplitFunction;
import dev.dagless.service.generation.DirectoryMonitorService;
import dev.dagless.service.generation.ExecutionTimeMeasurementService;
import dev.dagless.service.generation.FileIOService;
import dev.dagless.service.generation.VariableIOService;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtMethod;

import static dev.dagless.process.ProcessUtils.clearMethod;
import static dev.dagless.process.ProcessUtils.isMain;

public class SequentialCodeGenerationProcess extends AbstractProcessor<CtMethod<?>> {

    SplitFunction splitFunction;
    FileIOService fileIOService;
    VariableIOService variableIOService;

    /**
     * This process finds the main method and replaces its body with split function.
     * It adds all statements from the split function to the main method.
     * It also adds input and output statements to the main method.
     * It also adds download and upload statements to the main method.
     * It also adds directory monitoring statements to the main method.
     *
     * @param splitFunction - the split function that will be added to the main method
     * @param provider      - the function provider which the split function belongs to
     */
    public SequentialCodeGenerationProcess(SplitFunction splitFunction, FunctionProvider provider) {
        this.splitFunction = splitFunction;
        this.fileIOService = new FileIOService(provider);
        this.variableIOService = new VariableIOService(provider);

    }

    @Override
    public void process(CtMethod<?> ctMethod) {
        if (isMain(ctMethod)) {
            // clear all statements from the method
            clearMethod(ctMethod);

            // get the main method body
            CtBlock<?> mainBody = ctMethod.getBody();

            // add function execution time measurement if necessary
            if (splitFunction.isMeasureExecutionTime())
                ExecutionTimeMeasurementService.generateFunctionExecutionTimeMeasurementStart(mainBody);

            // add input statements
            variableIOService.generateInputStatements(mainBody, splitFunction);

            // generate download statements
            fileIOService.generateDownloadStatements(mainBody, splitFunction);

            // add directory monitor if necessary
            if (splitFunction.isDirectoryMonitoring())
                DirectoryMonitorService.generateDirectoryMonitorStart(mainBody);

            // add execution time measurement if necessary
            if (splitFunction.isMeasureExecutionTime())
                ExecutionTimeMeasurementService.generateCodeExecutionTimeMeasurementStart(mainBody);

            // add statements from the split function
            for (CtStatement statement : splitFunction.getFunctionStatements()) {
                mainBody.addStatement(statement.clone());
            }

            // add execution time measurement if necessary
            if (splitFunction.isMeasureExecutionTime())
                ExecutionTimeMeasurementService.generateCodeExecutionTimeMeasurementEnd(mainBody);

            // add directory monitor stop if necessary
            if (splitFunction.isDirectoryMonitoring())
                DirectoryMonitorService.generateDirectoryMonitoringEnd(mainBody);

            // generate upload statements
            fileIOService.generateUploadStatements(mainBody, splitFunction);

            // add function execution time measurement if necessary
            if (splitFunction.isMeasureExecutionTime())
                ExecutionTimeMeasurementService.generateFunctionExecutionTimeMeasurementEnd(mainBody);

            // add output statements
            variableIOService.generateOutputStatements(mainBody, splitFunction);
        }
    }
}
