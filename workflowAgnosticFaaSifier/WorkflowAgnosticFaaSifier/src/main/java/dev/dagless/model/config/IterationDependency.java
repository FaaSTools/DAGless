package dev.dagless.model.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class IterationDependency {

    private List<String> downloadFilePaths;
    private List<String> uploadFilePaths;
    private Object iterationObject;

    public List<String> getDownloadFilePaths() {
        return downloadFilePaths;
    }

    public void setDownloadFilePaths(List<String> downloadFilePaths) {
        this.downloadFilePaths = downloadFilePaths;
    }

    public List<String> getUploadFilePaths() {
        return uploadFilePaths;
    }

    public void setUploadFilePaths(List<String> uploadFilePaths) {
        this.uploadFilePaths = uploadFilePaths;
    }

    public Object getIterationObject() {
        return iterationObject;
    }

    public void setIterationObject(Object iterationObject) {
        this.iterationObject = iterationObject;
    }

    public String getIterationDependencyAsJSON(){
        ObjectMapper objectMapper = new ObjectMapper();
        try{
            return "{" +
                    "\"downloadFilePaths\":" +
                    objectMapper.writeValueAsString(downloadFilePaths) +
                    "," +
                    "\"uploadFilePaths\":" +
                    objectMapper.writeValueAsString(uploadFilePaths) +
                    "," +
                    "\"iterationObject\":" +
                    objectMapper.writeValueAsString(iterationObject) +
                    "}";
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public String toString() {
        return "IterationDependency{" +
                "downloadFilesPaths=" + downloadFilePaths +
                ", uploadFilesPaths=" + uploadFilePaths +
                ", iterationObject=" + iterationObject +
                '}';
    }
}
