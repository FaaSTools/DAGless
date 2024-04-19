package dev.simon.client;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;

import dev.simon.model.ProcessResult;
import java.util.logging.Logger;
public class ProcessClient {

    private static final Logger logger = Logger.getLogger(ProcessClient.class.getName());

    /**
     * Executes a command as a process
     * @param command
     * @return
     */
    public static ProcessResult executeCommandAsProcess(String command) {
        try {
            String [] commandList = command.split("\s+");

           logger.info("Executing command: " + command);

            long startTimeNs = System.nanoTime();

            Process process = new ProcessBuilder(commandList).start();
            String result = new BufferedReader(new InputStreamReader(process.getInputStream())).readLine();
            process.waitFor();

            long endTimeNs = System.nanoTime();

            if (result != null) {
                logger.info("Command result: " + result);
            }

            return new ProcessResult(endTimeNs - startTimeNs, result);
        } catch (Exception e) {
            logger.severe("Error while executing command: " + command);
            logger.severe(e.getMessage());
            return new ProcessResult(0, "");
        }
    }

    /**
     * Executes a command as a process and redirects the output to a file
     * @param command
     * @param outputFile
     * @return
     */
     public static ProcessResult executeCommandAsProcess(String command, String outputFile) {
        try {
            String [] commandList = command.split("\s+");

            logger.info("Executing command: " + command);

            long startTimeNs = System.nanoTime();

            ProcessBuilder processBuilder = new ProcessBuilder(commandList);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.to(Path.of(outputFile).toFile()));
            Process process = processBuilder.start();	
            process.waitFor();

            long endTimeNs = System.nanoTime();

            return new ProcessResult(endTimeNs - startTimeNs, "");
        } catch (Exception e) {
            logger.severe("Error while executing command: " + command);
            logger.severe(e.getMessage());
            return new ProcessResult(0, "");
        }
    }
}
