package dev.simon.client;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import dev.simon.model.ProcessResult;

public class ProcessClient {

    static Logger logger = Logger.getLogger(ProcessClient.class.getName());

    public static ProcessResult executeCommandAsProcess(String command) {
        try {
            String [] commandList = command.split("\s+");

            System.out.println("Executing command: " + command);

            long startTimeNs = System.nanoTime();

            Process process = new ProcessBuilder(commandList).start();
            String result = new BufferedReader(new InputStreamReader(process.getInputStream())).readLine();
            process.waitFor();

            long endTimeNs = System.nanoTime();

            if (result != null) {
                System.out.println("Result: " + result);
            }

            return new ProcessResult(endTimeNs - startTimeNs, result);
        } catch (Exception e) {
            logger.severe("Error while executing command: " + command);
            logger.severe(e.getMessage());
            return new ProcessResult(0, "");
        }
    }
}
