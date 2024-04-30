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
        java.util.List<dev.simon.model.AlignInput> alignInputs = gson.fromJson(input.get("alignInputs"), new com.google.gson.reflect.TypeToken<java.util.List<dev.simon.model.AlignInput>>(){}.getType());
        dev.simon.BWA bwa = gson.fromJson(input.get("bwa"), new com.google.gson.reflect.TypeToken<dev.simon.BWA>(){}.getType());
        dev.simon.JStorage jStorage = new dev.simon.JStorage();
        jStorage.clearFunctionDirectory();
        java.util.List<dev.simon.model.transfer.FileTransfer> fileTransfers = new java.util.ArrayList<>();
        downloadUris.forEach(uri -> fileTransfers.add(jStorage.copyTraced(uri, dev.simon.JStorage.getLocalFilePathForDownload(uri))));
        long codeStartTimeNs = System.nanoTime();
        java.lang.String outputSam = "NC_000913.3.sam";
        bwa.sampe(alignInputs.get(0), alignInputs.get(1), outputSam);
        java.lang.String outputSortedSam = "NC_000913.3sorted.sam";
        bwa.samtoolsSort(outputSam, outputSortedSam);
        java.lang.String outputSortedBam = "NC_000913.3sorted.bam";
        bwa.samtoolsView(outputSortedSam, outputSortedBam);
        java.lang.String outputSortedBamIndex = "NC_000913.3sorted.bam.bai";
        bwa.samtoolsIndex(outputSortedBam, outputSortedBamIndex);
        long codeExecutionTimeNs = System.nanoTime() - codeStartTimeNs;
        uploadUris.forEach(uri -> fileTransfers.add(jStorage.copyTraced(dev.simon.JStorage.getLocalSourcePathForUpload(uri), dev.simon.JStorage.getLocalDestinationPathForUpload(uri))));
        java.util.HashMap<java.lang.String, java.lang.Object> output = new java.util.HashMap<>();
        output.put("fileTransfers", fileTransfers);
        output.put("codeExecutionTimeNs", codeExecutionTimeNs);
        output.put("functionExecutionTimeNs", System.nanoTime() - functionStartTimeNs);
        response.getWriter().write(gson.toJson(output));
    }
}