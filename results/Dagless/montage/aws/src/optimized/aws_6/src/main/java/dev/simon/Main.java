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
        java.util.List<dev.simon.model.BackgroundPair> backgroundPairs = gson.fromJson(input.get("backgroundPairs"), new com.google.gson.reflect.TypeToken<java.util.List<dev.simon.model.BackgroundPair>>(){}.getType());
        java.lang.String cImagesTbl = gson.fromJson(input.get("cImagesTbl"), new com.google.gson.reflect.TypeToken<java.lang.String>(){}.getType());
        java.lang.String correctedDir = gson.fromJson(input.get("correctedDir"), new com.google.gson.reflect.TypeToken<java.lang.String>(){}.getType());
        java.lang.String correctionsTbl = gson.fromJson(input.get("correctionsTbl"), new com.google.gson.reflect.TypeToken<java.lang.String>(){}.getType());
        java.lang.String modPImagesTbl = gson.fromJson(input.get("modPImagesTbl"), new com.google.gson.reflect.TypeToken<java.lang.String>(){}.getType());
        dev.simon.Montage montage = gson.fromJson(input.get("montage"), new com.google.gson.reflect.TypeToken<dev.simon.Montage>(){}.getType());
        java.lang.String regionHdr = gson.fromJson(input.get("regionHdr"), new com.google.gson.reflect.TypeToken<java.lang.String>(){}.getType());
        dev.simon.JStorage jStorage = new dev.simon.JStorage();
        jStorage.clearFunctionDirectory();
        java.util.List<dev.simon.model.transfer.FileTransfer> fileTransfers = new java.util.ArrayList<>();
        downloadUris.forEach(uri -> fileTransfers.add(jStorage.copyTraced(uri, dev.simon.JStorage.getLocalFilePathForDownload(uri))));
        long codeStartTimeNs = System.nanoTime();
        for (dev.simon.model.BackgroundPair pair : backgroundPairs) {
            montage.mBackground(java.util.List.of("-t"), pair, modPImagesTbl, correctionsTbl);
        }
        long codeExecutionTimeNs = System.nanoTime() - codeStartTimeNs;
        uploadUris.forEach(uri -> fileTransfers.add(jStorage.copyTraced(dev.simon.JStorage.getLocalSourcePathForUpload(uri), dev.simon.JStorage.getLocalDestinationPathForUpload(uri))));
        java.util.HashMap<java.lang.String, java.lang.Object> output = new java.util.HashMap<>();
        output.put("backgroundPairs", backgroundPairs);
        output.put("cImagesTbl", cImagesTbl);
        output.put("correctedDir", correctedDir);
        output.put("montage", montage);
        output.put("regionHdr", regionHdr);
        output.put("fileTransfers", fileTransfers);
        output.put("codeExecutionTimeNs", codeExecutionTimeNs);
        output.put("functionExecutionTimeNs", System.nanoTime() - functionStartTimeNs);
        return output;
    }
}