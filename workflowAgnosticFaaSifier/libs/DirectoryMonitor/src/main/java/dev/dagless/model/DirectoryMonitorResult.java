package dev.dagless.model;

import java.util.Set;

public class DirectoryMonitorResult {

    public  Set<String> accessedFiles;
    public  Set<String> createdFiles;
    public  Object iterationObject;

    public DirectoryMonitorResult(Set<String> accessedFiles, Set<String> createdFiles) {
        if (accessedFiles == null) throw new IllegalArgumentException("Accessed files cannot be null");
        if (createdFiles == null) throw new IllegalArgumentException("Created files cannot be null");
        this.accessedFiles = accessedFiles;
        this.createdFiles = createdFiles;
    }

    public DirectoryMonitorResult(Object iterationObject, Set<String> accessedFiles, Set<String> createdFiles) {
        if (accessedFiles == null) throw new IllegalArgumentException("Accessed files cannot be null");
        if (createdFiles == null) throw new IllegalArgumentException("Created files cannot be null");
        this.accessedFiles = accessedFiles;
        this.createdFiles = createdFiles;
        this.iterationObject = iterationObject;
    }

    public Set<String> getAccessedFiles() {
        return accessedFiles;
    }

    public void setAccessedFiles(Set<String> accessedFiles) {
        this.accessedFiles = accessedFiles;
    }

    public Set<String> getCreatedFiles() {
        return createdFiles;
    }

    public void setCreatedFiles(Set<String> createdFiles) {
        this.createdFiles = createdFiles;
    }

    public Object getIterationObject() {
        return iterationObject;
    }

    public void setIterationObject(Object iterationObject) {
        this.iterationObject = iterationObject;
    }

    @Override
    public String toString() {
        return "DirectoryMonitorResult{" +
                "accessedFiles=" + accessedFiles +
                ", createdFiles=" + createdFiles +
                ", iterationObject=" + iterationObject +
                '}';
    }
}
