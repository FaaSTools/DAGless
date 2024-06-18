package dev.dagless.service.generation;

import dev.dagless.model.config.Config;
import dev.dagless.model.config.FunctionProvider;
import dev.dagless.model.splitting.AbstractWorkflow;
import dev.dagless.model.splitting.SplitFunction;
import dev.dagless.process.generation.CodeGeneratorFactory;
import dev.dagless.process.generation.SequentialCodeGenerationProcess;
import dev.dagless.process.faasification.aws.AWSClassFaasificationProcess;
import dev.dagless.process.faasification.aws.AWSMethodFaasificationProcess;
import dev.dagless.process.faasification.gcp.GCPClassFaasificationProcess;
import dev.dagless.process.faasification.gcp.GCPMethodFaasificationProcess;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtImport;
import spoon.reflect.visitor.filter.TypeFilter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class CodeGenerationService {
    private final Logger logger = Logger.getLogger(CodeGenerationService.class.getName());
    private final Config config;
    private final AbstractWorkflow abstractWorkflow;
    private Launcher launcher;

    public CodeGenerationService(AbstractWorkflow abstractWorkflow, Config config) {
        this.abstractWorkflow = abstractWorkflow;
        this.config = config;
    }

    public void exportSplitFunctions() {
        for (SplitFunction splitFunction : abstractWorkflow.getSplitFunctions()) {
            for (FunctionProvider provider : config.getFunctionProviders()) {
                recreateLauncher();
                switch (provider) {
                    case AWS -> {
                        addAWSSplitFaasification(splitFunction);
                        exportCode("aws_" + splitFunction.getOrder());
                        //createConfigFile(Path.of(config.getPathToSplitFunctions().toString(), "aws_" + splitFunction.getOrder()).toString(), splitFunction);
                    }
                    case GCP -> {
                        addGCPSplitFassification(splitFunction);
                        exportCode("gcp_" + splitFunction.getOrder());
                        //createConfigFile(Path.of(config.getPathToSplitFunctions().toString(), "gcp_" + splitFunction.getOrder()).toString(), splitFunction);
                    }
                    default -> throw new RuntimeException("Provider for fassification not supported!");
                }
            }
        }
        createWorkflowConfig();
    }

    private void addAWSSplitFaasification(SplitFunction splitFunction) {
        launcher.addProcessor(CodeGeneratorFactory.createCodeGenerationProcessor(splitFunction, FunctionProvider.AWS));
        launcher.addProcessor(new AWSClassFaasificationProcess());
        launcher.addProcessor(new AWSMethodFaasificationProcess());
    }

    private void addGCPSplitFassification(SplitFunction splitFunction) {
        launcher.addProcessor(CodeGeneratorFactory.createCodeGenerationProcessor(splitFunction, FunctionProvider.GCP));
        launcher.addProcessor(new GCPClassFaasificationProcess());
        launcher.addProcessor(new GCPMethodFaasificationProcess());
    }

    /**
     * Configure the launcher with the source project
     */
    private void recreateLauncher() {
        launcher = new Launcher();
        launcher.addInputResource(config.getPathToInputProject().toString());
        launcher.getEnvironment().setAutoImports(false);
    }

    /**
     * Exports the transformed code to the output directory postfixed with the provider name
     * Also adds the import statements for the used types to the top of each file
     * Note: This method does not support fully qualified names for the used types in the source project
     * e.g. java.util.HashMap instead of HashMap
     */
    private void exportCode(String providerPostfix) {
        launcher.run();
        CtModel model = launcher.getModel();

        // Get all compilation units aka files in the source project
        List<CtCompilationUnit> compilationUnits = new ArrayList<>(launcher.getFactory().CompilationUnit().getMap().values());

        // Iterate over classes in the source project
        for (CtClass<?> ctClass : model.getElements(new TypeFilter<>(CtClass.class))) {
            String className = ctClass.getQualifiedName();

            // Create the file name based on the class name
            Path modifiedPath = Path.of(config.getPathToSplitFunctions().toString(), providerPostfix, "/src/main/java/");
            String fileName = className.replace('.', '/') + ".java";
            File outputFile = new File(modifiedPath.toString(), fileName);

            // Create parent directories if needed
            File parentDir = outputFile.getParentFile();
            if (!parentDir.exists()) {
                if(!parentDir.mkdirs()){
                    throw new RuntimeException("Could not create parent directories for file: " + outputFile);
                }
            }


            // Adds the package statement to the top of the file
            StringBuilder output = new StringBuilder();
            String packageName = ctClass.getPackage().getQualifiedName();
            if (!packageName.isEmpty()) {
                output.append("package ").append(packageName).append(";\n\n");
            }

            // Add the import statements to the top of the file
            for (CtCompilationUnit unit : compilationUnits) {
                if (unit.getMainType().getQualifiedName().equals(ctClass.getQualifiedName())) {
                    for (CtImport ctImport : unit.getImports()) {
                        if (isValidImport(ctImport))
                            output.append(ctImport).append("\n");
                    }
                }
            }

            // Add the class content to the file
            output.append(ctClass);

            // Write the class content to the file
            try (FileWriter writer = new FileWriter(outputFile)) {
                writer.write(output.toString());
            } catch (IOException ignored) {
                throw new RuntimeException("Could not write to file: " + outputFile);
            }
        }
    }

    /**
     * This method checks if the given import is valid. An import is valid if it is not a package import, not a single import
     * This is necessary since spoon creates the imports based on the used types in the class.
     *
     * @param ctImport
     * @return
     */
    private static boolean isValidImport(CtImport ctImport) {
        boolean isPackage = ctImport.toString().equals(ctImport.toString().toLowerCase()) && !ctImport.toString().contains("*");
        boolean isSingleImport = !ctImport.toString().contains(".");
        return !(isPackage || isSingleImport);
    }

    private void createConfigFile(String pathToSplitFunctionFolder, SplitFunction splitFunction) {
        File configFile = new File(pathToSplitFunctionFolder, "split-function-config.json");
        if (!configFile.exists()) {
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(splitFunction.getSplitFunctionAsJson());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createWorkflowConfig() {
        File configFile = new File(config.getPathToSplitFunctions().toString(), "workflow_config.json");
        if (!configFile.exists()) {
            try (FileWriter writer = new FileWriter(configFile)) {
                StringBuilder sb = new StringBuilder();
                sb.append("[\n");
                abstractWorkflow.getSplitFunctions().stream().map(SplitFunction::getSplitFunctionAsJson).forEach(json -> sb.append(json).append(",\n"));
                sb.deleteCharAt(sb.length() - 2);
                sb.append("]");
                writer.write(sb.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
