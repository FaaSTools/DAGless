package dev.simon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import dev.simon.client.ProcessClient;
import dev.simon.model.AlignInput;
import dev.simon.model.Execution;
import dev.simon.model.ProcessResult;
import htsjdk.samtools.*;

public class BWA {
    private final String workDir;
    private static final Logger logger = Logger.getLogger(BWA.class.getName());

    public BWA(String workDir) {
        checkWorkDir(workDir);
        this.workDir = addTrailSlash(workDir);
    }

    public Execution index(String inputFasta){
        makeExecutable(getAbsolutePath("bwa"));

        StringBuilder command = new StringBuilder();
        command.append(getAbsolutePath("bwa"));
        command.append(" ");
        command.append("index");
        command.append(" ");
        command.append(getAbsolutePath(inputFasta));

        ProcessResult result = ProcessClient.executeCommandAsProcess(command.toString());
        return new Execution("index", result.getExecutionTimeNs(), result.getOutput());
    }

    public Execution aln(AlignInput alignInput){
        makeExecutable(getAbsolutePath("bwa"));

        StringBuilder command = new StringBuilder();
        command.append(getAbsolutePath("bwa"));
        command.append(" ");
        command.append("aln");
        command.append(" ");
        command.append(getAbsolutePath(alignInput.getInputFasta()));
        command.append(" ");
        command.append(getAbsolutePath(alignInput.getInputFastq()));

        ProcessResult result = ProcessClient.executeCommandAsProcess(command.toString(), getAbsolutePath(alignInput.getOutputSai()));
        return new Execution("aln", result.getExecutionTimeNs(), result.getOutput());
    }

    public Execution sampe(AlignInput alignInput1, AlignInput alignInput2,  String outputSam){
        makeExecutable(getAbsolutePath("bwa"));

        StringBuilder command = new StringBuilder();
        command.append(getAbsolutePath("bwa"));
        command.append(" ");
        command.append("sampe");
        command.append(" ");
        command.append(getAbsolutePath(alignInput1.getInputFasta()));
        command.append(" ");
        command.append(getAbsolutePath(alignInput1.getOutputSai()));
        command.append(" ");
        command.append(getAbsolutePath(alignInput2.getOutputSai()));
        command.append(" ");
        command.append(getAbsolutePath(alignInput1.getInputFastq()));
        command.append(" ");
        command.append(getAbsolutePath(alignInput2.getInputFastq()));

        ProcessResult result = ProcessClient.executeCommandAsProcess(command.toString(), getAbsolutePath(outputSam));
        return new Execution("sampe", result.getExecutionTimeNs(), result.getOutput());
    }

    public Execution samtoolsSort(String inputSamFile, String outputSamFile){
        logger.info("Executing samtools sort");
        long startTimeNs = System.nanoTime();
        inputSamFile = getAbsolutePath(inputSamFile);
        outputSamFile = getAbsolutePath(outputSamFile);
        try (SamReader samReader = SamReaderFactory.makeDefault().open(new File(inputSamFile))) {

            SAMFileHeader header = samReader.getFileHeader();
            header.setSortOrder(SAMFileHeader.SortOrder.coordinate);
            header.setGroupOrder(SAMFileHeader.GroupOrder.none);

            SAMFileWriterFactory factory = new SAMFileWriterFactory();
            SAMFileWriter samWriter = factory.makeSAMWriter(header, false, new File(outputSamFile));

            // Iterate over reads and write them to the sorted SAM file
            for (SAMRecord samRecord : samReader) {
                samWriter.addAlignment(samRecord);
            }

            // Close the readers and writers
            samReader.close();
            samWriter.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        long endTimeNs = System.nanoTime();
        return new Execution("sort", endTimeNs-startTimeNs, "");
    }

    public Execution samtoolsView(String sortedSamFile, String outputBamFile){
        logger.info("Executing samtools view");
        long startTimeNs = System.nanoTime();
        sortedSamFile = getAbsolutePath(sortedSamFile);
        outputBamFile = getAbsolutePath(outputBamFile);
        try (SamReader samReader = SamReaderFactory.makeDefault().open(new File(sortedSamFile))){

            // Create a SAM file header for the output BAM file
            SAMFileHeader header = samReader.getFileHeader();

            // Create a SAM file writer for the output BAM file
            SAMFileWriter writer = new SAMFileWriterFactory().makeBAMWriter(header, true, new File(outputBamFile));

            // Iterate over the reads in the input SAM file and write them to the output BAM file
            for (SAMRecord record : samReader) {
                writer.addAlignment(record);
            }

            // Close the SAM file reader and writer
            samReader.close();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        long endTimeNs = System.nanoTime();
        return new Execution("view", endTimeNs-startTimeNs, "");
    }

    public Execution samtoolsIndex(String inputBamFile, String outputBamIndexFile){
        logger.info("Executing samtools index");
        long startTimeNs = System.nanoTime();
        inputBamFile = getAbsolutePath(inputBamFile);
        outputBamIndexFile = getAbsolutePath(outputBamIndexFile);
        try {
            // Open the sorted BAM file
            SamReader reader = SamReaderFactory
                    .makeDefault()
                    .enable(SamReaderFactory.Option.INCLUDE_SOURCE_IN_RECORDS)
                    .open(new File(inputBamFile));

            // Create an index for the sorted BAM file
            BAMIndexer.createIndex(reader, new File(outputBamIndexFile));

            // Close the SAM file reader
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        long endTimeNs = System.nanoTime();
        return new Execution("index", endTimeNs-startTimeNs, "");
    }

    private String expandFlags(List<String> flags){
        StringBuilder sb = new StringBuilder();
        for (String flag : flags){
            sb.append(flag);
            sb.append(" ");
        }
        return sb.toString();
    }

    private String getAbsolutePath(String relativePath){
        return Path.of(workDir + relativePath).toString();
    }

    private void checkWorkDir(String workDir) {
        Path workDirPath = Paths.get(workDir);

        // check if the path exists
        if (!workDirPath.toFile().exists()) {
            if (workDirPath.toFile().mkdirs()) {
                System.out.println("Created working directory.");
            } else {
                throw new IllegalArgumentException("The working directory does not exist and could not be created.");
            }
        }

        // check if the path is a directory
        if (!workDirPath.toFile().isDirectory()) {
            throw new IllegalArgumentException("The working directory is not a directory.");
        }
    }

    private String addTrailSlash(String path){
        if (!path.endsWith("/")){
            return path + "/";
        }
        return path;
    }

    private void makeExecutable(String executable){
        ProcessClient.executeCommandAsProcess("chmod +x " + executable);
    }

    public String getWorkDir() {
        return workDir;
    }
}
