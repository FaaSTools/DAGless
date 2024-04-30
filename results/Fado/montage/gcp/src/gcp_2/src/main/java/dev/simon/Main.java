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
        java.lang.String inputDir = gson.fromJson(input.get("inputDir"), new com.google.gson.reflect.TypeToken<java.lang.String>(){}.getType());
        java.util.List<java.lang.String> inputFits = gson.fromJson(input.get("inputFits"), new com.google.gson.reflect.TypeToken<java.util.List<java.lang.String>>(){}.getType());
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
        // prepareMDiffFit
        // mImgTbl
        java.lang.String imagesTbl = "images.tbl";
        java.lang.String imageList = "input.imglist";
        montage.mImgtbl(java.util.List.of("-t"), imageList, inputDir, imagesTbl, inputFits);
        // mDAGTbls
        java.lang.String rImagesTbl = "rimages.tbl";
        java.lang.String pImagesTbl = "pimages.tbl";
        java.lang.String cImagesTbl = "cimages.tbl";
        montage.mDAGTbls(imagesTbl, regionHdr, rImagesTbl, pImagesTbl, cImagesTbl);
        // mOverlaps
        java.lang.String diffsTbl = "diffs.tbl";
        montage.mOverlaps(java.util.List.of(), pImagesTbl, diffsTbl);
        // mDiffFit
        // create DiffPairs
        java.util.List<dev.simon.model.DiffPair> diffPairs = montage.createDiffPairs(diffsTbl);
        long codeExecutionTimeNs = System.nanoTime() - codeStartTimeNs;
        directoryMonitor.stopMonitoring(null);
        uploadUris.forEach(uri -> fileTransfers.add(jStorage.copyTraced(dev.simon.JStorage.getLocalSourcePathForUpload(uri), dev.simon.JStorage.getLocalDestinationPathForUpload(uri))));
        java.util.HashMap<java.lang.String, java.lang.Object> output = new java.util.HashMap<>();
        output.put("cImagesTbl", cImagesTbl);
        output.put("diffPairs", diffPairs);
        output.put("inputFits", inputFits);
        output.put("montage", montage);
        output.put("pImagesTbl", pImagesTbl);
        output.put("regionHdr", regionHdr);
        output.put("fileTransfers", fileTransfers);
        output.put("monitorResult", directoryMonitor.getMonitorResults());
        output.put("codeExecutionTimeNs", codeExecutionTimeNs);
        output.put("functionExecutionTimeNs", System.nanoTime() - functionStartTimeNs);
        response.getWriter().write(gson.toJson(output));
    }
}