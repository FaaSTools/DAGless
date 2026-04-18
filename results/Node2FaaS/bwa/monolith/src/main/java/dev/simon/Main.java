package dev.simon;

import dev.simon.BWA;
import dev.simon.FissionLessPathUtil;
import dev.simon.model.AlignInput;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        String workingDir = FissionLessPathUtil.getWorkingDirByEnvironmentVariable("/tmp/function/");
        String storageUri = "gs://bwa-results-bucket/";
        BWA bwa = new BWA(workingDir);
        DynamicJStorage storage = new DynamicJStorage(workingDir, storageUri);

        List<String> downloadFiles = List.of(
                "NC_000913.3-hipA7.fasta",
                "hipa7_reads_R1.fastq",
                "hipa7_reads_R2.fastq",
                "bwa"
        );
        downloadFiles.forEach(storage::ensureLocal);

        String inputFasta = "NC_000913.3-hipA7.fasta";
        bwa.index(inputFasta);

        List<AlignInput> alignInputs = List.of(
                new AlignInput(inputFasta, "hipa7_reads_R1.fastq", "aln_sa1.sai"),
                new AlignInput(inputFasta, "hipa7_reads_R2.fastq", "aln_sa2.sai")
        );

        // upload file to storage
        storage.ensureStorage("NC_000913.3-hipA7.fasta.bwt");
        
        // INVOKE ALIGN

        alignInputs.forEach(alignInput -> storage.ensureLocal(alignInput.getOutputSai()));

        String outputSam = "NC_000913.3.sam";
        bwa.sampe(alignInputs.get(0), alignInputs.get(1), outputSam);

        String outputSortedSam = "NC_000913.3sorted.sam";
        bwa.samtoolsSort(outputSam, outputSortedSam);

        String outputSortedBam = "NC_000913.3sorted.bam";
        bwa.samtoolsView(outputSortedSam, outputSortedBam);

        String outputSortedBamIndex = "NC_000913.3sorted.bam.bai";
        bwa.samtoolsIndex(outputSortedBam, outputSortedBamIndex);
    }
}