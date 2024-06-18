package dev.dagless;

import dev.dagless.io.CommandLineHandler;
import dev.dagless.io.ConfigReader;
import dev.dagless.model.config.Config;
import dev.dagless.model.splitting.AbstractWorkflow;
import dev.dagless.model.splitting.VariableOptimizedWorkflow;
import dev.dagless.service.generation.CodeGenerationService;
import dev.dagless.service.packaging.DependencyService;
import dev.dagless.service.splitting.AbstractSplitService;
import dev.dagless.service.splitting.SplitServiceFactory;
import org.apache.commons.cli.ParseException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

public class Main {

    public static void main(String[] args) throws ParseException, IOException, XmlPullParserException {
        Logger logger = Logger.getLogger(Main.class.getName());
        logger.info("Starting the faasification process...");

        logger.info("Parsing command-line arguments");
        CommandLineHandler handler = new CommandLineHandler();
        Path configFile = handler.parseArguments(args);

        logger.info("Reading config file");
        Config config = ConfigReader.readConfig(configFile);
        logger.info("Finished reading config file");

        logger.info("Starting the splitting process...");
        AbstractSplitService splitService = SplitServiceFactory.createSplitService(config);
        AbstractWorkflow abstractWorkflow = splitService.createWorkflow();
        logger.info("Finished the splitting process...");

        logger.info("Starting the code generation process...");
        CodeGenerationService codeTransformer = new CodeGenerationService(abstractWorkflow, config);
        codeTransformer.exportSplitFunctions();
        logger.info("Finished the code generation process...");

        logger.info("Starting the dependency transformation process...");
        DependencyService dependencyTransformer = new DependencyService(config, abstractWorkflow);
        dependencyTransformer.exportModifiedPOM();
        logger.info("Finished the dependency transformation process...");

        logger.info("Finished the fassification process...");
    }

}