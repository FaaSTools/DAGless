package dev.simon;

import dev.simon.model.BackgroundPair;
import dev.simon.model.DiffPair;
import dev.simon.model.Execution;
import dev.simon.model.ProjectPair;
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
        java.lang.String cImagesTbl = gson.fromJson(input.get("cImagesTbl"), new com.google.gson.reflect.TypeToken<java.lang.String>(){}.getType());
        java.lang.String correctedDir = gson.fromJson(input.get("correctedDir"), new com.google.gson.reflect.TypeToken<java.lang.String>(){}.getType());
        java.lang.String correctionsTbl = gson.fromJson(input.get("correctionsTbl"), new com.google.gson.reflect.TypeToken<java.lang.String>(){}.getType());
        java.util.List<java.lang.String> inputFits = gson.fromJson(input.get("inputFits"), new com.google.gson.reflect.TypeToken<java.util.List<java.lang.String>>(){}.getType());
        java.lang.String modPImagesTbl = gson.fromJson(input.get("modPImagesTbl"), new com.google.gson.reflect.TypeToken<java.lang.String>(){}.getType());
        Montage montage = gson.fromJson(input.get("montage"), new com.google.gson.reflect.TypeToken<Montage>(){}.getType());
        java.lang.String pImagesTbl = gson.fromJson(input.get("pImagesTbl"), new com.google.gson.reflect.TypeToken<java.lang.String>(){}.getType());
        java.lang.String regionHdr = gson.fromJson(input.get("regionHdr"), new com.google.gson.reflect.TypeToken<java.lang.String>(){}.getType());
        dev.simon.JStorage jStorage = new dev.simon.JStorage();
        jStorage.clearFunctionDirectory();
        java.util.List<dev.simon.model.transfer.FileTransfer> fileTransfers = new java.util.ArrayList<>();
        if (enableDirectoryMonitoring)
        	jStorage.copy( monitorUtilsBucketUri + "monitor/", "/workspace/");
        downloadUris.forEach(uri -> fileTransfers.add(jStorage.copyTraced(uri, dev.simon.JStorage.getLocalFilePathForDownload(uri))));
        dev.simon.DirectoryMonitor directoryMonitor = new dev.simon.DirectoryMonitor(enableDirectoryMonitoring);
        directoryMonitor.startMonitoring();
        long codeStartTimeNs = System.nanoTime();
        montage.setAbsolutePathInImagesTbl(pImagesTbl, modPImagesTbl, "p2mass");
        long codeExecutionTimeNs = System.nanoTime() - codeStartTimeNs;
        directoryMonitor.stopMonitoring(null);
        uploadUris.forEach(uri -> fileTransfers.add(jStorage.copyTraced(dev.simon.JStorage.getLocalSourcePathForUpload(uri), dev.simon.JStorage.getLocalDestinationPathForUpload(uri))));
        java.util.HashMap<java.lang.String, java.lang.Object> output = new java.util.HashMap<>();
        output.put("cImagesTbl", cImagesTbl);
        output.put("correctedDir", correctedDir);
        output.put("correctionsTbl", correctionsTbl);
        output.put("inputFits", inputFits);
        output.put("modPImagesTbl", modPImagesTbl);
        output.put("montage", montage);
        output.put("regionHdr", regionHdr);
        output.put("fileTransfers", fileTransfers);
        output.put("monitorResult", directoryMonitor.getMonitorResults());
        output.put("codeExecutionTimeNs", codeExecutionTimeNs);
        output.put("functionExecutionTimeNs", System.nanoTime() - functionStartTimeNs);
        response.getWriter().write(gson.toJson(output));
    }
}