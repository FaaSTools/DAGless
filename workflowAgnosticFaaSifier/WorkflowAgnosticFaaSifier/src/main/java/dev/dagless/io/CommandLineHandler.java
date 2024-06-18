package dev.dagless.io;

import org.apache.commons.cli.*;

import java.nio.file.Path;
import java.util.logging.Logger;


public class CommandLineHandler {
    CommandLineParser parser;
    Options options;
    Logger logger = Logger.getLogger(CommandLineHandler.class.getName());

    public CommandLineHandler() {
        parser = new DefaultParser();
        options = new Options();
        options.addOption("h", "help", false, "This is a prototype for a automatic serverless profiler");
        options.addOption("v", "version", false, "0.1");
        options.addOption("c", "config", true, "Path to config file");
    }

    public Path parseArguments(String[] args) throws ParseException {
        // Parse command-line arguments
        logger.fine("Parsing command-line arguments");
        CommandLine cmd = parser.parse(options, args);

        // Check for specific options
        if (cmd.hasOption("config")) {
            String inputFile = cmd.getOptionValue("config");
            logger.fine("Config file: " + inputFile);
            return Path.of(inputFile);
        } else {
            throw new ParseException("No config file specified");
        }
    }
}
