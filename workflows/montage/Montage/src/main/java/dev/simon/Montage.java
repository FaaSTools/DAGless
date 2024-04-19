package dev.simon;

import dev.simon.client.ProcessClient;
import dev.simon.model.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

public class Montage {

    private final String workDir;

    private static final Logger logger = Logger.getLogger(Montage.class.getName());

    public Montage(String workDir) {
        this.workDir = addTrailSlash(workDir);
        checkWorkDir(workDir);
    }

    public Execution mProjectPPs(List<String> flags, List<String> inputFits, List<String> outputFits, String headerFile){
        if (inputFits.size() != outputFits.size()){
            throw new IllegalStateException("The number of inputFits and outputFiles must be the same.");
        }

        String executable = "mProjectPP";
        makeExecutable(getAbsolutePath(executable));

        long runtime = 0;
        String output = new String();
        for (int i = 0; i < inputFits.size(); i++) {
            String inputFit = inputFits.get(i);
            String outputFit = outputFits.get(i);
            StringBuilder command = new StringBuilder();
            command.append(getAbsolutePath(executable));
            command.append(" ");
            command.append(expandFlagSet(flags));
            command.append(" ");
            command.append(getAbsolutePath(inputFit));
            command.append(" ");
            command.append(getAbsolutePath(outputFit));
            command.append(" ");
            command.append(getAbsolutePath(headerFile));

            ProcessResult result = ProcessClient.executeCommandAsProcess(command.toString());
            runtime += result.getExecutionTimeNs();
            output += result.getOutput();
        }
        return new Execution("mProjectPP", runtime, output);
    }

    /**
     * mProjectPP creates a new projected FITS file from an input FITS file and a header file.
     * @param flags - flags as specified in the Montage documentation
     * @param projectPair - input and output FITS file relative paths
     * @param headerFile - header file relative path
     * @return Execution object containing the execution time and the output of the command
     */
    public Execution mProjectPP(List<String> flags, ProjectPair projectPair, String headerFile){
        String executable = "mProjectPP";
        // ensure that the files are local
        makeExecutable(getAbsolutePath(executable));

        StringBuilder command = new StringBuilder();
        command.append(getAbsolutePath(executable));
        command.append(" ");
        command.append(expandFlagSet(flags));
        command.append(" ");
        command.append(getAbsolutePath(projectPair.inputFit()));
        command.append(" ");
        command.append(getAbsolutePath(projectPair.outputFit()));
        command.append(" ");
        command.append(getAbsolutePath(headerFile));

        ProcessResult result = ProcessClient.executeCommandAsProcess(command.toString());

        return new Execution("mProjectPP", result.getExecutionTimeNs(), result.getOutput());
    }

    /**
     * mDiff creates a diffs.tbl file from a list of diffPairs.
     * @param flags - flags as specified in the Montage documentation
     * @param diffPairs - list of DiffPair objects containing the indexes and relative paths of the FITS files
     * @param headerFile - header file relative path
     * @return Execution object containing the execution time and the output of the command
     */
    public Execution mDiffs(List<String> flags, List<DiffPair> diffPairs, String headerFile){
        String executable = "mDiff";
        makeExecutable(getAbsolutePath(executable));

        long runtime = 0;
        String output = new String();
        // TODO check if correct type otherwise cast/parse to correct type
        for (DiffPair diffPair : diffPairs) {
            StringBuilder command = new StringBuilder();
            command.append(getAbsolutePath(executable));
            command.append(" ");
            command.append(expandFlagSet(flags));
            command.append(" ");
            command.append(getAbsolutePath(diffPair.getFirstInputFits()));
            command.append(" ");
            command.append(getAbsolutePath(diffPair.getSecondInputFits()));
            command.append(" ");
            command.append(getAbsolutePath(diffPair.getDiffOutput()));
            command.append(" ");
            command.append(getAbsolutePath(headerFile));

            ProcessResult result = ProcessClient.executeCommandAsProcess(command.toString());
            runtime += result.getExecutionTimeNs();
            output += result.getOutput();
        }
        return new Execution("mDiff", runtime, output);
    }

    /**
     * mDiff creates a diffs.tbl file from a list of diffPairs.
     * @param flags - flags as specified in the Montage documentation
     * @param diffPair - single DiffPair objects containing the indexes and relative paths of the FITS files
     * @param headerFile - header file relative path
     * @return Execution object containing the execution time and the output of the command
     */
    public Execution mDiff(List<String> flags, DiffPair diffPair, String headerFile){
        String executable = "mDiff";
        makeExecutable(getAbsolutePath(executable));

        StringBuilder command = new StringBuilder();
        command.append(getAbsolutePath(executable));
        command.append(" ");
        command.append(expandFlagSet(flags));
        command.append(" ");
        command.append(getAbsolutePath(diffPair.getFirstInputFits()));
        command.append(" ");
        command.append(getAbsolutePath(diffPair.getSecondInputFits()));
        command.append(" ");
        command.append(getAbsolutePath(diffPair.getDiffOutput()));
        command.append(" ");
        command.append(getAbsolutePath(headerFile));

        ProcessResult result = ProcessClient.executeCommandAsProcess(command.toString());

        return new Execution("mDiff", result.getExecutionTimeNs(), result.getOutput());
    }

    /**
     * mFitplane creates a text file containing the coefficients of a plane fit to the input FITS file. // TODO check if correct
     * @param flags - flags as specified in the Montage documentation
     * @param inputFits -
     * @param outputTexts
     * @return
     */
    public Execution mFitplanes(List<String> flags, List<String> inputFits, List<String> outputTexts){
        if (inputFits.size() != outputTexts.size()){
            throw new IllegalArgumentException("The number of inputFits and outputFiles must be the same.");
        }
        String executable = "mFitplane";
        makeExecutable(getAbsolutePath(executable));

        long runtime = 0;
        String output = new String();
        for (int i = 0; i < inputFits.size(); i++) {
            String inputFit = inputFits.get(i);
            String outputFile = outputTexts.get(i);
            StringBuilder command = new StringBuilder();
            command.append(getAbsolutePath(executable));
            command.append(" ");
            command.append(expandFlagSet(flags));
            command.append(" ");
            command.append(getAbsolutePath(inputFit));
            command.append(" ");
            command.append("-s");
            command.append(" ");
            command.append(getAbsolutePath(outputFile));

            ProcessResult result = ProcessClient.executeCommandAsProcess(command.toString());
            runtime += result.getExecutionTimeNs();
            output += result.getOutput();

        }
        return new Execution("mFitplane", runtime, output);
    }

    /**
     * mFitplane uses least squares to fit a plane (excluding outlier pixels) to an image.
     * @param flags - flags as specified in the Montage documentation
     * @param diffPair - DiffPair object containing the indexes and relative paths of the FITS files
     * @return Execution object containing the execution time and the output of the command
     */
    public Execution mFitplane(List<String> flags, DiffPair diffPair){
        String executable = "mFitplane";
        makeExecutable(getAbsolutePath(executable));


        StringBuilder command = new StringBuilder();
        command.append(getAbsolutePath(executable));
        command.append(" ");
        command.append(expandFlagSet(flags));
        command.append(" ");
        command.append(getAbsolutePath(diffPair.getDiffOutput()));
        command.append(" ");
        command.append("-s");
        command.append(" ");
        command.append(getAbsolutePath(diffPair.getFitTextFile()));

        ProcessResult result = ProcessClient.executeCommandAsProcess(command.toString());

        return new Execution("mFitplane", result.getExecutionTimeNs(), result.getOutput());
    }

    /**
     * mDiffFits is the consecutive execution of mDiff and mFitplane.
     * @param mDiffFlags - flags as specified in the Montage documentation for mDiff
     * @param mFitplaneFlags - flags as specified in the Montage documentation for mFitplane
     * @param diffPairs - list of DiffPair objects containing the indexes and relative paths of the FITS files
     * @param headerFile - header file relative path
     * @return Execution object containing the execution time and the output of the command
     */
    public Execution mDiffFits(List<String> mDiffFlags, List<String> mFitplaneFlags, List<DiffPair> diffPairs, String headerFile){
        Execution mDiff = mDiffs(mDiffFlags, diffPairs, headerFile);

        // create input for mFitplane
        List<String> diffFits = diffPairs.stream().map(DiffPair::getDiffOutput).toList();
        List<String> fitTxtFiles = diffPairs.stream().map(DiffPair::getFitTextFile).toList();

        Execution mFitplane = mFitplanes(mFitplaneFlags, diffFits, fitTxtFiles);

        return new Execution("mDiffFits", mDiff.executionTimeNs() + mFitplane.executionTimeNs(), mDiff.output() + mFitplane.output());
    }

    /**
     * mDiffFit is the consecutive execution of mDiff and mFitplane.
     * @param mDiffFlags - flags as specified in the Montage documentation for mDiff
     * @param mFitplaneFlags - flags as specified in the Montage documentation for mFitplane
     * @param diffPair - DiffPair object containing the indexes and relative paths of the FITS files
     * @param headerFile - header file relative path
     * @return Execution object containing the execution time and the output of the command
     */
    public Execution mDiffFit(List<String> mDiffFlags, List<String> mFitplaneFlags, DiffPair diffPair, String headerFile){
        Execution mDiff = mDiff(mDiffFlags, diffPair, headerFile);

        Execution mFitplane = mFitplane(mFitplaneFlags, diffPair);

        return new Execution("mDiffFit", mDiff.executionTimeNs() + mFitplane.executionTimeNs(), mDiff.output() + mFitplane.output());
    }

    /**
     * mStatfile creates a statfile.tbl from a diffs.tbl file.
     * This method only works if the specified path, in this case the working directory, contains a diffs.tbl file.
     * Otherwise, the executable will have a segmentation fault.
     * @return ExecutionTime of mStatfile
     */
    public Execution mStatFile(){
        String executable = "mStatFile";
        String diffsTbl = "diffs.tbl";
        makeExecutable(getAbsolutePath(executable));

        StringBuilder command = new StringBuilder();
        command.append(getAbsolutePath(executable));
        command.append(" ");
        command.append(workDir);

        ProcessResult result = ProcessClient.executeCommandAsProcess(command.toString());

        return new Execution("mStatFile", result.getExecutionTimeNs(), result.getOutput());
    }

    /**
     * mConcatFit creates a fits.tbl file from a statfile.tbl file and a list of diffPairs.
     * @param statFile - relative path of the statfile.tbl file
     * @param outputFitTable - relative path of the fits.tbl file that will be created
     * @param diffPairs - list of DiffPair objects containing the indexes and relative paths of the FITS files
     * @return
     */
    public Execution mConcatFit(String statFile, String outputFitTable, List<DiffPair> diffPairs){
        String executable = "mConcatFit";
        makeExecutable(getAbsolutePath(executable));

        StringBuilder command = new StringBuilder();
        command.append(getAbsolutePath(executable));
        command.append(" ");
        command.append(getAbsolutePath(statFile));
        command.append(" ");
        command.append(getAbsolutePath(outputFitTable));
        command.append(" ");
        command.append(workDir);

        ProcessResult result = ProcessClient.executeCommandAsProcess(command.toString());

        return new Execution("mConcatFit", result.getExecutionTimeNs(), result.getOutput());
    }

    /**
     * mBgModel creates a corrections.tbl file from a fits.tbl file.
     * @param flags - flags as specified in the Montage documentation
     * @param imagesTbl - relative path of the images.tbl file
     * @param fitsTbl - relative path of the fits.tbl file
     * @param correctionsTbl - relative path of the corrections.tbl file that will be created
     * @return
     */
    public Execution mBgModel(List<String> flags, String imagesTbl, String fitsTbl, String correctionsTbl){
        String executable = "mBgModel";
        makeExecutable(getAbsolutePath(executable));

        StringBuilder command = new StringBuilder();
        command.append(getAbsolutePath(executable));
        command.append(" ");
        command.append(expandFlagSet(flags));
        command.append(" ");
        command.append(getAbsolutePath(imagesTbl));
        command.append(" ");
        command.append(getAbsolutePath(fitsTbl));
        command.append(" ");
        command.append(getAbsolutePath(correctionsTbl));

        ProcessResult result = ProcessClient.executeCommandAsProcess(command.toString());

        return new Execution("mBgModel", result.getExecutionTimeNs(), result.getOutput());
    }

    /**
     * mBackgrounds removes a background plane from a FITS image. The background correction applied to the image is
     * specified as Ax+By+C, where (x,y) is the pixel coordinate using the image center as the origin, and (A,B,C) are
     * the background plane parameters specified as linear coefficients.
     * @param flags - flags as specified in the Montage documentation
     * @param backgroundPairs -
     * @param imageTbl
     * @param correctionsTbl
     * @return
     */
    public Execution mBackgrounds(List<String> flags, List<BackgroundPair> backgroundPairs, String imageTbl, String correctionsTbl){
        String executable = "mBackground";
        makeExecutable(getAbsolutePath(executable));

        long runtime = 0;
        StringBuilder output = new StringBuilder();
        for (BackgroundPair backgroundPair : backgroundPairs) {

            StringBuilder command = new StringBuilder();
            command.append(getAbsolutePath(executable));
            command.append(" ");
            command.append(expandFlagSet(flags));
            command.append(" ");
            command.append(getAbsolutePath(backgroundPair.getProjectedFit()));
            command.append(" ");
            command.append(getAbsolutePath(backgroundPair.getCorrectedFit()));
            command.append(" ");
            command.append(getAbsolutePath(imageTbl));
            command.append(" ");
            command.append(getAbsolutePath(correctionsTbl));

            ProcessResult result = ProcessClient.executeCommandAsProcess(command.toString());
            runtime += result.getExecutionTimeNs();
            output.append(result.getOutput());
        }
        return new Execution("mBackground", runtime, output.toString());
    }

    /**
     * mBackground removes a background plane from a FITS image. The background correction applied to the image is
     * specified as Ax+By+C, where (x,y) is the pixel coordinate using the image center as the origin, and (A,B,C) are
     * the background plane parameters specified as linear coefficients.
     * @param flags - flags as specified in the Montage documentation
     * @param backgroundPair - object containing the relative paths of the projectFit and correctedFit
     * @param imageTbl - relative path of the images.tbl file
     * @param correctionsTbl - relative path of the corrections.tbl file
     * @return
     */
    public Execution mBackground(List<String> flags, BackgroundPair backgroundPair, String imageTbl, String correctionsTbl){
        // create dir for corrected fits if it does not exist
        File correctedDir = new File(getAbsolutePath("corrected"));
        if (!correctedDir.exists()){
            if(!correctedDir.mkdir()){
                logger.warning("Could not create directory: " + correctedDir.getAbsolutePath());
            }
        }

        String executable = "mBackground";
        makeExecutable(getAbsolutePath(executable));


        StringBuilder command = new StringBuilder();
        command.append(getAbsolutePath(executable));
        command.append(" ");
        command.append(expandFlagSet(flags));
        command.append(" ");
        command.append(getAbsolutePath(backgroundPair.getProjectedFit()));
        command.append(" ");
        command.append(getAbsolutePath(backgroundPair.getCorrectedFit()));
        command.append(" ");
        command.append(getAbsolutePath(imageTbl));
        command.append(" ");
        command.append(getAbsolutePath(correctionsTbl));

        ProcessResult result = ProcessClient.executeCommandAsProcess(command.toString());

        return new Execution("mBackground", result.getExecutionTimeNs(), result.getOutput());
    }

    /**
     * mImgtbl creates an images.tbl file from a list of FITS files.
     * @param flags - flags as specified in the Montage documentation
     * @param imageList - relative path of the input.imglist file
     * @param dir - relative path of the directory containing the FITS files
     * @param imageTbl - relative path of the images.tbl file that will be created
     * @return
     */
    public Execution mImgtbl(List<String> flags, String imageList, String dir, String imageTbl){
        String executable = "mImgtbl";
        makeExecutable(getAbsolutePath(executable));

        StringBuilder command = new StringBuilder();
        command.append(getAbsolutePath(executable));
        command.append(" ");
        command.append(expandFlagSet(flags));
        command.append(" ");
        command.append(getAbsolutePath(imageList));
        command.append(" ");
        command.append(getAbsolutePath(dir));
        command.append(" ");
        command.append(getAbsolutePath(imageTbl));

        ProcessResult result = ProcessClient.executeCommandAsProcess(command.toString());

        return new Execution("mImgtbl", result.getExecutionTimeNs(), result.getOutput());
    }

    /**
     * mImgtbl creates an images.tbl file from a list of FITS files.
     * @param flags - flags as specified in the Montage documentation
     * @param dir - relative path of the directory containing the FITS files
     * @param imageTbl - relative path of the images.tbl file that will be created
     * @return
     */
    public Execution mImgtbl(List<String> flags, String dir, String imageTbl){
        String executable = "mImgtbl";
        makeExecutable(getAbsolutePath(executable));

        StringBuilder command = new StringBuilder();
        command.append(getAbsolutePath(executable));
        command.append(" ");
        command.append(expandFlagSet(flags));
        command.append(" ");
        command.append(getAbsolutePath(dir));
        command.append(" ");
        command.append(getAbsolutePath(imageTbl));

        ProcessResult result = ProcessClient.executeCommandAsProcess(command.toString());

        return new Execution("mImgtbl", result.getExecutionTimeNs(), result.getOutput());
    }

    /**
     * Coadds a set of images into a single mosaic FITS file.
     * @param flags - flags as specified in the Montage documentation
     * @param imageTbl - relative path of the images.tbl file
     * @param headerFiles - relative path of the header files
     * @param outputFit - relative path of the output FITS file
     * @return
     */
    public Execution mAdd(List<String> flags, String imageTbl, String headerFiles, String outputFit){
        String executable = "mAdd";
        makeExecutable(getAbsolutePath(executable));

        StringBuilder command = new StringBuilder();
        command.append(getAbsolutePath(executable));
        command.append(" ");
        command.append(expandFlagSet(flags));
        command.append(" ");
        command.append(getAbsolutePath(imageTbl));
        command.append(" ");
        command.append(getAbsolutePath(headerFiles));
        command.append(" ");
        command.append(getAbsolutePath(outputFit));

        ProcessResult result = ProcessClient.executeCommandAsProcess(command.toString());

        return new Execution("mAdd", result.getExecutionTimeNs(), result.getOutput());
    }

    /**
     * mShrink reduces the size of a FITS file by a specified scaling factor.
     * @param flags - flags as specified in the Montage documentation
     * @param inputFit - relative path of the input FITS file
     * @param outputFit - relative path of the output FITS file
     * @param scalingFactor - scaling factor
     * @return
     */
    public Execution mShrink(List<String> flags, String inputFit, String outputFit, float scalingFactor){
        if (scalingFactor <= 0){
            throw new IllegalArgumentException("The scaling factor must be greater than 0.");
        }
        String executable = "mShrink";
        makeExecutable(getAbsolutePath(executable));

        StringBuilder command = new StringBuilder();
        command.append(getAbsolutePath(executable));
        command.append(" ");
        command.append(expandFlagSet(flags));
        command.append(" ");
        command.append(getAbsolutePath(inputFit));
        command.append(" ");
        command.append(getAbsolutePath(outputFit));
        command.append(" ");
        command.append(scalingFactor);

        ProcessResult result = ProcessClient.executeCommandAsProcess(command.toString());

        return new Execution("mShrink", result.getExecutionTimeNs(), result.getOutput());
    }

    /**
     * mViewer creates a JPEG image from a FITS file.
     * @param inputFlags - flags as specified in the Montage documentation
     * @param inputFit - relative path of the input FITS file
     * @param outputFlags - flags as specified in the Montage documentation
     * @param outputImage - relative path of the output JPEG file
     * @return
     */
    public Execution mViewer(List<String> inputFlags, String inputFit, List<String> outputFlags, String outputImage){
        String executable = "mViewer";
        makeExecutable(getAbsolutePath(executable));

        StringBuilder command = new StringBuilder();
        command.append(getAbsolutePath(executable));
        command.append(" ");
        command.append(expandFlagSet(inputFlags));
        command.append(" ");
        command.append(getAbsolutePath(inputFit));
        command.append(" ");
        command.append(expandFlagSet(outputFlags));
        command.append(" ");
        command.append(getAbsolutePath(outputImage));

        ProcessResult result = ProcessClient.executeCommandAsProcess(command.toString());

        return new Execution("mViewer", result.getExecutionTimeNs(), result.getOutput());
    }

    /**
     * Creates multiple tables from a single images.tbl and a header file.
     * @param imageTbl - relative path of the images.tbl file
     * @param headerFile - relative path of the header file
     * @param outputRImagesTbl - relative path of the rImages.tbl file
     * @param outputPImagesTbl - relative path of the pImages.tbl file
     * @param outputCImagesTbl - relative path of the cImages.tbl file
     * @return
     */
    public Execution mDAGTbls(String imageTbl, String headerFile, String outputRImagesTbl, String outputPImagesTbl, String outputCImagesTbl){
        String executable = "mDAGTbls";
        makeExecutable(getAbsolutePath(executable));

        StringBuilder command = new StringBuilder();
        command.append(getAbsolutePath(executable));
        command.append(" ");
        command.append(getAbsolutePath(imageTbl));
        command.append(" ");
        command.append(getAbsolutePath(headerFile));
        command.append(" ");
        command.append(getAbsolutePath(outputRImagesTbl));
        command.append(" ");
        command.append(getAbsolutePath(outputPImagesTbl));
        command.append(" ");
        command.append(getAbsolutePath(outputCImagesTbl));

        ProcessResult result = ProcessClient.executeCommandAsProcess(command.toString());

        return new Execution("mDAGTbls", result.getExecutionTimeNs(), result.getOutput());
    }

    /**
     * Analyzes an image metadata table to determine a list of overlapping images. Creates a diffs.tbl file.
     * @param flags - flags as specified in the Montage documentation
     * @param imageTbl - relative path of the images.tbl file
     * @param outputDiffTbl - relative path of the diffs.tbl file
     * @return
     */
    public Execution mOverlaps(List<String> flags, String imageTbl, String outputDiffTbl){
        String executable = "mOverlaps";
        makeExecutable(getAbsolutePath(executable));

        StringBuilder command = new StringBuilder();
        command.append(getAbsolutePath(executable));
        command.append(" ");
        command.append(expandFlagSet(flags));
        command.append(" ");
        command.append(getAbsolutePath(imageTbl));
        command.append(" ");
        command.append(getAbsolutePath(outputDiffTbl));

        ProcessResult result = ProcessClient.executeCommandAsProcess(command.toString());

        return new Execution("mOverlaps", result.getExecutionTimeNs(), result.getOutput());
    }

    public List<DiffPair> createDiffPairs(String diffsTbl){
        File diffTbl = new File(getAbsolutePath(diffsTbl));
        if (!diffTbl.exists()){
            throw new IllegalArgumentException("The diffs.tbl file does not exist.");
        }

        List<DiffPair> diffPairs = new ArrayList<>();

        // read file line by line
        try {
            Scanner scanner = new Scanner(diffTbl);

            int lineCount = 0;
			while (scanner.hasNextLine()) {
                lineCount++;
                // skip first two lines since they are the header
                if (lineCount <= 2){
                    scanner.nextLine();
                    continue;
                }

                String [] lineSplit = scanner.nextLine().split("\s+");
                if (lineSplit.length < 6){
                    System.out.println("Skipping Line " + lineCount + " has " + lineSplit.length + " elements.");
                    continue;
                }
                int firstIndex = Integer.parseInt(lineSplit[1]);
                int secondIndex = Integer.parseInt(lineSplit[2]);
                String firstInputFit = lineSplit[3];
                String secondInputFit = lineSplit[4];
                System.out.println(firstIndex + " " + secondIndex + " " + firstInputFit + " " + secondInputFit);
                diffPairs.add(new DiffPair(firstIndex, secondIndex, firstInputFit, secondInputFit));

			}

			scanner.close();
        } catch(Exception e){
            throw new RuntimeException(e);
        }

        return diffPairs;
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

    private String expandFlagSet(List<String> flags) {
        StringBuilder sb = new StringBuilder();
        for (String flag : flags) {
            sb.append(flag);
            sb.append(" ");
        }
        return sb.toString();
    }

    public String getAbsolutePath(String relativePath){
        return Path.of(workDir + relativePath).toString();
    }

    private String addTrailSlash(String path){
        if (!path.endsWith("/")){
            return path + "/";
        }
        return path;
    }

    public void setAbsolutePathInImagesTbl(String inputImagesTbl, String modifiedImagesTbl, String stringToReplaceWithAbsolutePath){
        File file = new File(getAbsolutePath(inputImagesTbl));
        if (!file.exists()){
            throw new IllegalArgumentException("The images.tbl file does not exist.");
        }

        // read file line by line
        try {
            Scanner scanner = new Scanner(file);

            // list of lines to write to the new file
            List<String> lines = new ArrayList<>();
            int lineCount = 0;
            while(scanner.hasNextLine()){
                lineCount++;
                String line = scanner.nextLine();
                // skip first line since it is the header
                if (lineCount == 1){
                    lines.add(line);
                    continue;
                // end header of this line needs to be at the same position as the file in the next rows
                } else if (lineCount == 2){
                    line = adjustSpacesInHeaderLine(line);
                    lines.add(line);
                    continue;
                // end header of this line needs to be at the same position as the char in the next rows
                } else if (lineCount == 3){
                    line = adjustSpacesInHeaderLine(line);
                    lines.add(line);
                    continue;
                }

                // replace the relative path with the absolute path
                line = line.replaceAll(stringToReplaceWithAbsolutePath, getAbsolutePath(stringToReplaceWithAbsolutePath));
                lines.add(line);
            }
            scanner.close();

            // write the new file
            File outputFile = new File(getAbsolutePath(modifiedImagesTbl));
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
            writer.close();

        } catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    private String adjustSpacesInHeaderLine(String line){
        String adjustedHeader = new String();
        for (int i = 0; i < line.length()-5; i++){
            adjustedHeader += line.charAt(i);
        }
        adjustedHeader += getNWhitespaces(workDir.length()) + line.substring(line.length()-5);
        return adjustedHeader;
    }

    private String getNWhitespaces(int n){
        return " ".repeat(Math.max(0, n));
    }

    private void makeExecutable(String executable){
       ProcessClient.executeCommandAsProcess("chmod +x " + executable);
    }

    public String getWorkDir() {
        return workDir;
    }

    @Override
    public String toString() {
        return "Montage{" +
                "workDir='" + workDir + '\'' +
                '}';
    }
}
