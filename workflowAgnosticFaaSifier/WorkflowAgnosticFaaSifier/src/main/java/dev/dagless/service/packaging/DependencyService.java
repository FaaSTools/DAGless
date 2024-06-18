package dev.dagless.service.packaging;

import dev.dagless.model.config.Config;
import dev.dagless.model.config.DataTransferMode;
import dev.dagless.model.config.FunctionProvider;
import dev.dagless.model.splitting.AbstractWorkflow;
import dev.dagless.model.splitting.SplitFunction;
import org.apache.maven.model.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for adding the dependencies to the pom.xml file
 */
public class DependencyService {

    private final Config config;
    private final AbstractWorkflow abstractWorkflow;


    public DependencyService(Config config, AbstractWorkflow abstractWorkflow) {
        this.config = config;
        this.abstractWorkflow = abstractWorkflow;
    }


    /**
     * Adds the dependencies used by AWS to the Model of the pom.xml file
     *
     * @param model
     */
    private void addAWSDependencies(Model model) {
        List<Dependency> dependencies = new ArrayList<>();

        // Add the dependencies to interact with AWS Lambda Runtime
        Dependency awsLambdaJavaCore = new Dependency();
        awsLambdaJavaCore.setGroupId("com.amazonaws");
        awsLambdaJavaCore.setArtifactId("aws-lambda-java-core");
        awsLambdaJavaCore.setVersion("1.2.2");
        dependencies.add(awsLambdaJavaCore);

        // Add the dependencies all dependencies to the model if they are not already present
        for (Dependency dependency : dependencies) {
            if (!model.getDependencies().contains(dependency)) {
                model.addDependency(dependency);
            }
        }
    }

    /**
     * Adds the dependencies used by GCP to the Model of the pom.xml file
     *
     * @param model
     */
    private void addGCPDependencies(Model model) {
        List<Dependency> dependencies = new ArrayList<>();
        // Add the dependencies to interact with GCP Cloud Functions Runtime
        Dependency googleCloudFunctionsFramework = new Dependency();
        googleCloudFunctionsFramework.setGroupId("com.google.cloud.functions");
        googleCloudFunctionsFramework.setArtifactId("functions-framework-api");
        googleCloudFunctionsFramework.setVersion("1.1.0");
        googleCloudFunctionsFramework.setScope("provided");
        dependencies.add(googleCloudFunctionsFramework);

        // Add the dependencies all dependencies to the model if they are not already present
        for (Dependency dependency : dependencies) {
            if (!model.getDependencies().contains(dependency)) {
                model.addDependency(dependency);
            }
        }

    }

    /**
     * Adds the dependencies used by both providers to the Model of the pom.xml file
     * The following dependencies are added:
     * - GSON
     * - DirectoryMonitor
     * - JStorage
     *
     * @param model
     */
    private void addProviderAgnosticDependencies(Model model, SplitFunction splitFunction) {
        List<Dependency> dependencies = new ArrayList<>();

        // Add the dependencies serialisation and deserialization of JSON
        Dependency gson = new Dependency();
        gson.setGroupId("com.google.code.gson");
        gson.setArtifactId("gson");
        gson.setVersion("2.10.1");
        dependencies.add(gson);

        // Add the dependencies for directory monitoring (DirectoryMonitor)
        if (splitFunction.isDirectoryMonitoring()) {
            Dependency directoryMonitor = new Dependency();
            directoryMonitor.setGroupId("dev.dagless");
            directoryMonitor.setArtifactId("DirectoryMonitor");
            directoryMonitor.setVersion("1.0");
            dependencies.add(directoryMonitor);
        }

        // Add the dependencies for bucket access (JStorage)
        boolean inputStatementsEmpty = splitFunction.getSplitFunctionVariableIO().getInputVariables().isEmpty();
        boolean outputStatementsEmpty = splitFunction.getSplitFunctionVariableIO().getOutputVariables().isEmpty();
        boolean isHardcoded = config.getDataTransferMode().equals(DataTransferMode.HARDCODED);
        boolean isManual = config.getDataTransferMode().equals(DataTransferMode.MANUAL);
        boolean isComplete = config.getDataTransferMode().equals(DataTransferMode.COMPLETE);
        // only add JStorage if the data transfer mode is manual or hardcoded and there are input or output statements
        if (isComplete || isManual || (isHardcoded && (!inputStatementsEmpty || !outputStatementsEmpty))) {
            Dependency jStorage = new Dependency();
            jStorage.setGroupId("dev.dagless");
            jStorage.setArtifactId("JStorage");
            jStorage.setVersion("2.0");
            dependencies.add(jStorage);
        }

        // Add the dependencies all dependencies to the model if they are not already present
        for (Dependency dependency : dependencies) {
            if (!model.getDependencies().contains(dependency)) {
                model.addDependency(dependency);
            }
        }
    }

    /**
     * Adds the build configuration for the maven-shade-plugin to the pom.xml file
     *
     * @param model the model of the pom.xml file
     */
    private void addBuildConfiguration(Model model) {
        // MODEL
        Build build = new Build();

        // PLUGIN (maven-shade-plugin)
        Plugin plugin = new Plugin();
        plugin.setGroupId("org.apache.maven.plugins");
        plugin.setArtifactId("maven-shade-plugin");
        plugin.setVersion("3.5.0");

        // EXECUTION
        PluginExecution pluginExecution = new PluginExecution();
        pluginExecution.setPhase("package");
        pluginExecution.addGoal("shade");

        // CONFIGURATION
        Xpp3Dom configuration = new Xpp3Dom("configuration");

        Xpp3Dom transformers = new Xpp3Dom("transformers");
        Xpp3Dom transformer = new Xpp3Dom("transformer");
        transformer.setAttribute("implementation", "org.apache.maven.plugins.shade.resource.ServicesResourceTransformer");

        transformers.addChild(transformer);

        // build createDependencyReducedPom element
        Xpp3Dom createDependencyReducedPom = new Xpp3Dom("createDependencyReducedPom");
        createDependencyReducedPom.setValue("false");

        // final name of the jar file
        Xpp3Dom finalName = new Xpp3Dom("finalName");
        finalName.setValue("split-function");

        // build filters element
        Xpp3Dom filters = new Xpp3Dom("filters");
        // build filter element
        Xpp3Dom filter = new Xpp3Dom("filter");
        // build artifact element
        Xpp3Dom artifact = new Xpp3Dom("artifact");
        artifact.setValue("*:*");
        // build excludes element
        Xpp3Dom excludes = new Xpp3Dom("excludes");
        // build exclude elements
        Xpp3Dom excludeSF = new Xpp3Dom("exclude");
        excludeSF.setValue("META-INF/*.SF");
        Xpp3Dom excludeDSA = new Xpp3Dom("exclude");
        excludeDSA.setValue("META-INF/*.DSA");
        Xpp3Dom excludeRSA = new Xpp3Dom("exclude");
        excludeRSA.setValue("META-INF/*.RSA");

        // exclude elements to excludes element
        excludes.addChild(excludeSF);
        excludes.addChild(excludeDSA);
        excludes.addChild(excludeRSA);
        // add artifact and excludes to filter
        filter.addChild(artifact);
        filter.addChild(excludes);
        // add filter to filters
        filters.addChild(filter);

        // add createDependencyReducedPom and filters to configuration
        configuration.addChild(transformers);
        configuration.addChild(createDependencyReducedPom);
        configuration.addChild(finalName);
        configuration.addChild(filters);

        // add configuration to execution
        pluginExecution.setConfiguration(configuration);
        // add execution to plugin
        plugin.addExecution(pluginExecution);
        // add plugin to build
        build.addPlugin(plugin);
        // add build to model
        model.setBuild(build);
    }

    /**
     * Exports the pom.xml file to the output directory specified in the config
     * The pom.xml file is exported for every provider and split function
     * All dependencies are added to the pom.xml file
     *
     * @throws IOException
     * @throws XmlPullParserException
     */
    public void exportModifiedPOM() throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        MavenXpp3Writer writer = new MavenXpp3Writer();

        // exporting pom.xml for every provider and split function
        for (FunctionProvider provider : config.getFunctionProviders()) {
            for (SplitFunction splitFunction : abstractWorkflow.getSplitFunctions()) {
                Path outputPath = config.getPathToSplitFunctions();
                Model model = reader.read(new FileReader(config.getPathToInputProject().resolve("pom.xml").toFile()));
                // add dependencies independent of the provider
                addBuildConfiguration(model);
                addProviderAgnosticDependencies(model, splitFunction);
                // add provider specific dependencies
                switch (provider) {
                    case AWS -> {
                        addAWSDependencies(model);
                        outputPath = Path.of(outputPath.toString(), "aws_" + splitFunction.getOrderAsString(), "pom.xml");
                    }
                    case GCP -> {
                        addGCPDependencies(model);
                        outputPath = Path.of(outputPath.toString(), "gcp_" + splitFunction.getOrderAsString(), "pom.xml");
                    }
                    default -> throw new RuntimeException("Provider for fassification not supported!");
                }
                // write the pom.xml file
                writer.write(new FileWriter(outputPath.toFile()), model);
            }
        }
    }
}

