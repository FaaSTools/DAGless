package dev.dagless.process.generation;

import dev.dagless.model.config.FunctionProvider;
import dev.dagless.model.splitting.SplitFunction;
import dev.dagless.service.generation.DirectoryMonitorService;
import dev.dagless.service.generation.ExecutionTimeMeasurementService;
import dev.dagless.service.generation.FileIOService;
import dev.dagless.service.generation.VariableIOService;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtForEach;
import spoon.reflect.declaration.CtMethod;

import static dev.dagless.process.ProcessUtils.clearMethod;
import static dev.dagless.process.ProcessUtils.isMain;

public class ForEachCodeGenerationProcess extends AbstractProcessor<CtMethod> {

    SplitFunction splitFunction;
    FileIOService fileIOService;
    VariableIOService variableIOService;
    CtForEach ctForEach;

    public ForEachCodeGenerationProcess(SplitFunction splitFunction, FunctionProvider provider) {
        this.splitFunction = splitFunction;
        this.fileIOService = new FileIOService(provider);
        this.variableIOService = new VariableIOService(provider);
        this.ctForEach = (CtForEach) splitFunction.getFunctionStatements().get(0).clone();
    }

    @Override
    public void process(CtMethod ctMethod) {
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
                DirectoryMonitorService.generateDirectoryMonitorInitForEach(mainBody);

            // add execution time measurement if necessary
            if (splitFunction.isMeasureExecutionTime())
                ExecutionTimeMeasurementService.generateCodeExecutionTimeMeasurementStart(mainBody);

            // add statements from the split function
            if (splitFunction.isDirectoryMonitoring()){
                CtBlock<?> forEachBody = (CtBlock<?>) ctForEach.getBody();

                DirectoryMonitorService.generateDirectoryMonitorStartForEach(forEachBody);
                DirectoryMonitorService.generateDirectoryMonitoringEndForEach(forEachBody, ctForEach);
            }

            mainBody.addStatement(ctForEach);


            // add execution time measurement if necessary
            if (splitFunction.isMeasureExecutionTime())
                ExecutionTimeMeasurementService.generateCodeExecutionTimeMeasurementEnd(mainBody);

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
