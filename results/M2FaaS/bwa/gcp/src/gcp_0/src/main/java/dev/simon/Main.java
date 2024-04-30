package dev.simon;

import dev.simon.model.AlignInput;
import dev.simon.model.Execution;
public class Main implements com.google.cloud.functions.HttpFunction {
    @java.lang.Override
    public void service(final com.google.cloud.functions.HttpRequest request, final com.google.cloud.functions.HttpResponse response) throws java.io.IOException {
        long functionStartTimeNs = System.nanoTime();
        com.google.gson.Gson gson = new com.google.gson.Gson();
        java.util.Map<java.lang.String, java.lang.String> input = gson.fromJson(request.getReader(), java.util.Map.class);
        java.util.List<String> downloadUris = gson.fromJson(input.get("downloadUris"), new com.google.gson.reflect.TypeToken<java.util.List<java.lang.String>>(){}.getType());
        java.util.List<String> uploadUris = gson.fromJson(input.get("uploadUris"), new com.google.gson.reflect.TypeToken<java.util.List<java.lang.String>>(){}.getType());
        java.lang.Boolean enableDirectoryMonitoring = gson.fromJson(input.get("enableDirectoryMonitoring"), new com.google.gson.reflect.TypeToken<java.lang.Boolean>(){}.getType());
        java.lang.String monitorUtilsBucketUri = gson.fromJson(input.get("monitorUtilsBucketUri"), new com.google.gson.reflect.TypeToken<java.lang.String>(){}.getType());
        dev.simon.JStorage jStorage = new dev.simon.JStorage();
        jStorage.clearFunctionDirectory();
        java.util.List<dev.simon.model.transfer.FileTransfer> fileTransfers = new java.util.ArrayList<>();
        if (enableDirectoryMonitoring)
        	jStorage.copy( monitorUtilsBucketUri + "monitor/", "/workspace/");
        downloadUris.forEach(uri -> fileTransfers.add(jStorage.copyTraced(uri, dev.simon.JStorage.getLocalFilePathForDownload(uri))));
        dev.simon.DirectoryMonitor directoryMonitor = new dev.simon.DirectoryMonitor(enableDirectoryMonitoring);
        directoryMonitor.startMonitoring();
        long codeStartTimeNs = System.nanoTime();
        java.lang.String workingDir = dev.simon.FissionLessPathUtil.getWorkingDirByEnvironmentVariable("/tmp/function/");
        dev.simon.BWA bwa = new dev.simon.BWA(workingDir);
        java.lang.String inputFasta = "NC_000913.3-hipA7.fasta";
        bwa.index(inputFasta);
        java.util.List<dev.simon.model.AlignInput> alignInputs = java.util.List.of(new dev.simon.model.AlignInput(inputFasta, "hipa7_reads_R1.fastq", "aln_sa1.sai"), new dev.simon.model.AlignInput(inputFasta, "hipa7_reads_R2.fastq", "aln_sa2.sai"));
        long codeExecutionTimeNs = System.nanoTime() - codeStartTimeNs;
        directoryMonitor.stopMonitoring(null);
        uploadUris.forEach(uri -> fileTransfers.add(jStorage.copyTraced(dev.simon.JStorage.getLocalSourcePathForUpload(uri), dev.simon.JStorage.getLocalDestinationPathForUpload(uri))));
        java.util.HashMap<java.lang.String, java.lang.Object> output = new java.util.HashMap<>();
        output.put("alignInputs", alignInputs);
        output.put("bwa", bwa);
        output.put("fileTransfers", fileTransfers);
        output.put("monitorResult", directoryMonitor.getMonitorResults());
        output.put("codeExecutionTimeNs", codeExecutionTimeNs);
        output.put("functionExecutionTimeNs", System.nanoTime() - functionStartTimeNs);
        response.getWriter().write(gson.toJson(output));
    }
}