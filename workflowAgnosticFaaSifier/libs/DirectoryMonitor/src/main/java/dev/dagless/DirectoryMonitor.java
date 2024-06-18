package dev.dagless;

import dev.dagless.model.DirectoryMonitorResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

public class DirectoryMonitor {

    private Path functionDirectory;
    private Path monitorDirectory;
    private Path monitorExecutable;
    private Path accessedFilesLog;
    private Path modifiedFilesLog;
    private boolean isRunning = false;
    private Path directoryPathPrefix;
    private Set<String> fileSetBeforeExecution;
    private Process monitorProcess;
    private final boolean enableDirectoryMonitor;
    List<DirectoryMonitorResult> monitorResults = new ArrayList<>();
    private final Logger logger = Logger.getLogger(DirectoryMonitor.class.getName());


    // TODO maybe add parameter to deactivate the directory monitor
    public DirectoryMonitor(boolean enableDirectoryMonitor) {
        logger.info("Directory monitor enabled: " + enableDirectoryMonitor);
        this.enableDirectoryMonitor = enableDirectoryMonitor;
        if (!enableDirectoryMonitor){
            return;
        }
        directoryPathPrefix = resolveDirectoryPath();
        functionDirectory = directoryPathPrefix.resolve("function");
        monitorDirectory = directoryPathPrefix.resolve("monitor");
        monitorExecutable = monitorDirectory.resolve("dir_monitor");
        accessedFilesLog = monitorDirectory.resolve("access.log");
        modifiedFilesLog = monitorDirectory.resolve("modified.log");

    }

    private Path resolveDirectoryPath() {
        String envProvider = System.getenv("PROVIDER");
        if (envProvider == null){
            throw new RuntimeException("No provider found");
        }
        return switch (envProvider.toLowerCase()) {
            case "aws" -> Path.of("/tmp");
            case "gcp" -> Path.of("/workspace");
            default -> throw new RuntimeException("Invalid provider found");
        };
    }

    public void startMonitoring(){
        if (!enableDirectoryMonitor){
            return;
        }
        if (isRunning){
            throw new RuntimeException("Directory monitor is already running");
        }
        // Ensure that the directories and executables exist
        checkDirectoriesExistence();
        checkExecutableExistence();

        // delete the both log if they exist
        deleteLogIfExist();

        // make the dir_monitor executable
        makeFileExecutable(monitorExecutable);

        fileSetBeforeExecution = createFileSet();

        String [] command = {
                monitorExecutable.toString(), functionDirectory.toString(), accessedFilesLog.toString(), modifiedFilesLog.toString()
        };

        logger.info("Starting directory monitoring with command: " + Arrays.toString(command));

        ProcessBuilder processBuilder = new ProcessBuilder().inheritIO();
        processBuilder.command(command);

        try {
            monitorProcess = processBuilder.start();
            // This is an ugly hack to ensure that the dir_monitor has enough time to start and add the inotify watches
            // before the actual function code is executed, since we do not consider execution time in our measurements
            // this is not a problem
            sleep(2500);
        } catch (Exception e) {
            throw new RuntimeException("Error while starting directory monitor", e);
        }
        isRunning = true;
    }

    public void stopMonitoring(Object iterationObject){
        if (!enableDirectoryMonitor){
            return;
        }
        if (!isRunning){
            throw new RuntimeException("Directory monitor is not running");
        }

        // destroy the process
        monitorProcess.destroy();

        // Ensure that the logs where created
        checkAccessLogExistence();
        checkModifiedLogExistence();

        // Read in the access log line by line
        Set<String> accessedFiles = readAccessedFilesFromFile();
        // Read in the modified log line by line
        Set<String> modifiedFiles = readModifiedFilesFromFile();


        // Retain only accessed files that existed before the execution
        accessedFiles.retainAll(fileSetBeforeExecution);


        // Create a set of files that were created during the execution - used later to know which files to upload
        Set<String> createdFiles = createFileSet();
        createdFiles.removeAll(fileSetBeforeExecution);

        // Add all files that have been modified during the execution
        createdFiles.addAll(modifiedFiles);

        // delete log file to ensure that the next execution does not use the same log file
        deleteAccessLog();
        deleteModifiedLog();

        isRunning = false;
        monitorResults.add(new DirectoryMonitorResult(iterationObject, accessedFiles, createdFiles));
    }

    public List<DirectoryMonitorResult> getMonitorResults() {
        logger.info("Getting monitor results");
        logger.info("Number of monitor results: " + monitorResults.size());
        return monitorResults;
    }

    private Set<String> readAccessedFilesFromFile() {
        return getFilesFromFile(accessedFilesLog);
    }

    private Set<String> readModifiedFilesFromFile() {
        return getFilesFromFile(modifiedFilesLog);
    }

    private Set<String> getFilesFromFile(Path modifiedFilesLog) {
        Set<String> accessedFiles = new HashSet<>();
        try {
            BufferedReader reader = new BufferedReader(new java.io.FileReader(modifiedFilesLog.toString()));
            String line;

            while ((line = reader.readLine()) != null) {
                // remove the prefix of the directory path
                accessedFiles.add(line.replace(directoryPathPrefix.toString(), ""));
            }

            reader.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // this filter is necessary since Montage creates a lot of temporary files that are not relevant
        return accessedFiles.stream().filter(s -> !s.endsWith(".temp") && !s.endsWith(".tmp")).collect(Collectors.toSet());
    }

    private void checkDirectoriesExistence(){
        if (!functionDirectory.toFile().exists()){
            // create the function directory if it does not exist
            if (!functionDirectory.toFile().mkdirs()){
                throw new RuntimeException("Could not create function directory");
            }
        }
        if (!monitorDirectory.toFile().exists()){
            throw new RuntimeException("Monitor directory does not exist");
        }
    }

    private void checkExecutableExistence(){
        if (!monitorExecutable.toFile().exists()){
            throw new RuntimeException("Monitor executable does not exist");
        }
    }

    private void checkAccessLogExistence(){
        if (!accessedFilesLog.toFile().exists()){
            throw new RuntimeException("Access log does not exist");
        }
    }

    private void checkModifiedLogExistence(){
        if (!modifiedFilesLog.toFile().exists()){
            throw new RuntimeException("Modified log does not exist");
        }
    }

    private void deleteLogIfExist(){
        if (accessedFilesLog.toFile().exists()) {
            deleteAccessLog();
        }
        if (modifiedFilesLog.toFile().exists()) {
            deleteModifiedLog();
        }
    }

    private void checkIfFilesExist(Set<String> accessedFiles){
        for (String pathString : accessedFiles){
            System.out.println("Checking if file exists: " + pathString);
            Path path = Path.of(pathString);
            if (!path.toFile().exists()){
                // This case acts as a fallback in case the dir_monitor PathResolver fails to resolve the correct relative path
                String searchResult = searchFile(functionDirectory.toFile(), path.getFileName().toString());
                if (searchResult == null){
                    System.out.println("File " + path + " does not exist");
                    throw new RuntimeException("File " + path + " does not exist");
                } else {
                    //accessedFiles.remove(pathString);
                    //accessedFiles.add(searchResult);
                }
            }
        }
    }

    private String searchFile(File directory, String fileName) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        // Recursively search in subdirectories
                        String filePath = searchFile(file, fileName);
                        if (filePath != null) {
                            return filePath;
                        }
                    } else if (file.getName().equals(fileName)) {
                        // Found the file
                        return file.getAbsolutePath();
                    }
                }
            }
        }
        return null; // File not found in this directory
    }

    private void deleteAccessLog(){
        logger.info("Trying to delete access log");
        if (!accessedFilesLog.toFile().delete()){
            throw new RuntimeException("Could not delete access log");
        }
        logger.info("Deleted access log");
    }

    private void deleteModifiedLog(){
        logger.info("Trying to delete modified log");
        if (!modifiedFilesLog.toFile().delete()){
            throw new RuntimeException("Could not delete modified log");
        }
        logger.info("Deleted modified log");
    }

    private Set<String> createFileSet(){
        Set<String> fileSet = new HashSet<>();
        try {
            Files.walk(Path.of(functionDirectory.toString())).forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    // I do not want the provider specific directory prefix in the file paths
                    fileSet.add(filePath.toString().replace(directoryPathPrefix.toString(), ""));
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return fileSet;
    }

    private void makeFileExecutable(Path path){
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("chmod", "+x", path.toString());
            Process process = processBuilder.start();
            process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
