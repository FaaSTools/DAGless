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
        java.util.List<dev.simon.model.AlignInput> alignInputs = gson.fromJson(input.get("alignInputs"), new com.google.gson.reflect.TypeToken<java.util.List<dev.simon.model.AlignInput>>(){}.getType());
        dev.simon.BWA bwa = gson.fromJson(input.get("bwa"), new com.google.gson.reflect.TypeToken<dev.simon.BWA>(){}.getType());
        dev.simon.JStorage jStorage = new dev.simon.JStorage();
        jStorage.clearFunctionDirectory();
        java.util.List<dev.simon.model.transfer.FileTransfer> fileTransfers = new java.util.ArrayList<>();
        if (enableDirectoryMonitoring)
        	jStorage.copy( monitorUtilsBucketUri + "monitor/", "/workspace/");
        downloadUris.forEach(uri -> fileTransfers.add(jStorage.copyTraced(uri, dev.simon.JStorage.getLocalFilePathForDownload(uri))));
        dev.simon.DirectoryMonitor directoryMonitor = new dev.simon.DirectoryMonitor(enableDirectoryMonitoring);
        long codeStartTimeNs = System.nanoTime();
        for (dev.simon.model.AlignInput alignInput : alignInputs) {
            directoryMonitor.startMonitoring();
            bwa.aln(alignInput);
            directoryMonitor.stopMonitoring(alignInput);
        }
        long codeExecutionTimeNs = System.nanoTime() - codeStartTimeNs;
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