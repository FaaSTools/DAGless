package dev.simon;

import dev.simon.model.BackgroundPair;
import dev.simon.model.DiffPair;
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
        java.util.List<dev.simon.model.BackgroundPair> backgroundPairs = gson.fromJson(input.get("backgroundPairs"), new com.google.gson.reflect.TypeToken<java.util.List<dev.simon.model.BackgroundPair>>(){}.getType());
        java.lang.String cImagesTbl = gson.fromJson(input.get("cImagesTbl"), new com.google.gson.reflect.TypeToken<java.lang.String>(){}.getType());
        java.lang.String correctedDir = gson.fromJson(input.get("correctedDir"), new com.google.gson.reflect.TypeToken<java.lang.String>(){}.getType());
        dev.simon.Montage montage = gson.fromJson(input.get("montage"), new com.google.gson.reflect.TypeToken<dev.simon.Montage>(){}.getType());
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
        // mImgTbl
        java.util.List<java.lang.String> correctedFits = java.util.stream.Stream.concat(backgroundPairs.stream().map(BackgroundPair::getCorrectedFit), backgroundPairs.stream().map(BackgroundPair::getCorrectedAreaFit)).toList();
        montage.mImgtbl(java.util.List.of(), correctedDir, cImagesTbl, correctedFits);
        // mAdd
        java.lang.String mosaicFits = "mosaic.fits";
        montage.mAdd(java.util.List.of(), cImagesTbl, regionHdr, correctedFits, mosaicFits);
        // mShrink
        java.lang.String mosaicShrinked = "mosaic_shrinked.fits";
        montage.mShrink(java.util.List.of(), mosaicFits, mosaicShrinked, 1.5F);
        // mViewer
        java.lang.String finalImage = "final.png";
        montage.mViewer(java.util.List.of("-ct 1", "-gray"), mosaicShrinked, java.util.List.of("-2s max gaussian-log", "-out"), finalImage);
        long codeExecutionTimeNs = System.nanoTime() - codeStartTimeNs;
        directoryMonitor.stopMonitoring(null);
        uploadUris.forEach(uri -> fileTransfers.add(jStorage.copyTraced(dev.simon.JStorage.getLocalSourcePathForUpload(uri), dev.simon.JStorage.getLocalDestinationPathForUpload(uri))));
        java.util.HashMap<java.lang.String, java.lang.Object> output = new java.util.HashMap<>();
        output.put("fileTransfers", fileTransfers);
        output.put("monitorResult", directoryMonitor.getMonitorResults());
        output.put("codeExecutionTimeNs", codeExecutionTimeNs);
        output.put("functionExecutionTimeNs", System.nanoTime() - functionStartTimeNs);
        response.getWriter().write(gson.toJson(output));
    }
}