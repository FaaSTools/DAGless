package dev.simon;

import java.util.List;
import java.util.Set;

import dev.simon.model.AlignInput;
import dev.simon.model.Execution;

public class Main {
    public static void main(String[] args) {
        String workingDir = FissionLessPathUtil.getWorkingDirByEnvironmentVariable("/tmp/function/");
        BWA bwa = new BWA(workingDir);

        String inputFasta = "NC_000913.3-hipA7.fasta";
        bwa.index(inputFasta);

        List<AlignInput> alignInputs = List.of(
                new AlignInput(inputFasta, "hipa7_reads_R1.fastq", "aln_sa1.sai"),
                new AlignInput(inputFasta, "hipa7_reads_R2.fastq", "aln_sa2.sai")
        );
        for (AlignInput alignInput : alignInputs) {
            bwa.aln(alignInput);
        }

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