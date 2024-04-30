package dev.simon;

import dev.simon.model.BackgroundPair;
import dev.simon.model.DiffPair;
import dev.simon.model.ProjectPair;
public class Main implements com.amazonaws.services.lambda.runtime.RequestHandler<java.util.HashMap<java.lang.String, java.lang.String>, java.util.HashMap<java.lang.String, java.lang.Object>> {
    @java.lang.Override
    public java.util.HashMap<java.lang.String, java.lang.Object> handleRequest(java.util.HashMap<java.lang.String, java.lang.String> input, com.amazonaws.services.lambda.runtime.Context context) {
        long functionStartTimeNs = System.nanoTime();
        com.google.gson.Gson gson = new com.google.gson.Gson();
        java.util.List<String> downloadUris = gson.fromJson(input.get("downloadUris"), new com.google.gson.reflect.TypeToken<java.util.List<java.lang.String>>(){}.getType());
        java.util.List<String> uploadUris = gson.fromJson(input.get("uploadUris"), new com.google.gson.reflect.TypeToken<java.util.List<java.lang.String>>(){}.getType());
        dev.simon.JStorage jStorage = new dev.simon.JStorage();
        jStorage.clearFunctionDirectory();
        java.util.List<dev.simon.model.transfer.FileTransfer> fileTransfers = new java.util.ArrayList<>();
        downloadUris.forEach(uri -> fileTransfers.add(jStorage.copyTraced(uri, dev.simon.JStorage.getLocalFilePathForDownload(uri))));
        long codeStartTimeNs = System.nanoTime();
        java.lang.String workingDir = dev.simon.FissionLessPathUtil.getWorkingDirByEnvironmentVariable("/tmp/");
        dev.simon.Montage montage = new dev.simon.Montage(workingDir);
        // mProjectPPs
        java.util.List<java.lang.String> inputFits = java.util.List.of("input/2mass-atlas-001020s-k0860033.fits", "input/2mass-atlas-001020s-k0860044.fits", "input/2mass-atlas-001020s-k0860056.fits", "input/2mass-atlas-001020s-k0870221.fits", "input/2mass-atlas-001020s-k0870233.fits", "input/2mass-atlas-001020s-k0870245.fits", "input/2mass-atlas-001021s-k0490221.fits", "input/2mass-atlas-001021s-k0490233.fits", "input/2mass-atlas-001021s-k0490245.fits", "input/2mass-atlas-001021s-k0560033.fits", "input/2mass-atlas-001021s-k0560044.fits", "input/2mass-atlas-001021s-k0560056.fits", "input/2mass-atlas-001021s-k0570221.fits", "input/2mass-atlas-001021s-k0570233.fits", "input/2mass-atlas-001021s-k0570245.fits", "input/2mass-atlas-980914s-k0800033.fits", "input/2mass-atlas-980914s-k0800044.fits", "input/2mass-atlas-980914s-k0800056.fits", "input/2mass-atlas-980914s-k0810221.fits", "input/2mass-atlas-980914s-k0810233.fits", "input/2mass-atlas-980914s-k0810245.fits", "input/2mass-atlas-980914s-k0820033.fits", "input/2mass-atlas-980914s-k0820044.fits", "input/2mass-atlas-980914s-k0820056.fits", "input/2mass-atlas-980914s-k0830221.fits", "input/2mass-atlas-980914s-k0830233.fits", "input/2mass-atlas-980914s-k0830245.fits", "input/2mass-atlas-980914s-k0840033.fits", "input/2mass-atlas-980914s-k0840044.fits", "input/2mass-atlas-980914s-k0840056.fits");
        java.lang.String regionHdr = "region.hdr";
        java.lang.String inputDir = "input/";
        java.util.List<dev.simon.model.ProjectPair> projectPairs = inputFits.stream().map(str -> new dev.simon.model.ProjectPair(str, "p" + str.substring(6))).toList();
        long codeExecutionTimeNs = System.nanoTime() - codeStartTimeNs;
        uploadUris.forEach(uri -> fileTransfers.add(jStorage.copyTraced(dev.simon.JStorage.getLocalSourcePathForUpload(uri), dev.simon.JStorage.getLocalDestinationPathForUpload(uri))));
        java.util.HashMap<java.lang.String, java.lang.Object> output = new java.util.HashMap<>();
        output.put("inputDir", inputDir);
        output.put("inputFits", inputFits);
        output.put("montage", montage);
        output.put("projectPairs", projectPairs);
        output.put("regionHdr", regionHdr);
        output.put("fileTransfers", fileTransfers);
        output.put("codeExecutionTimeNs", codeExecutionTimeNs);
        output.put("functionExecutionTimeNs", System.nanoTime() - functionStartTimeNs);
        return output;
    }
}