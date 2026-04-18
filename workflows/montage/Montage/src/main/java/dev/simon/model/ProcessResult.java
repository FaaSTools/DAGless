package dev.simon.model;

public class ProcessResult {

    long executionTimeNs;
    String output;

    public ProcessResult(long executionTimeNs, String output) {
        this.executionTimeNs = executionTimeNs;
        this.output = output;
    }

    public long getExecutionTimeNs() {
        return executionTimeNs;
    }

    public String getOutput() {
        return output;
    }
    
    @Override
    public String toString() {
        return "ExecutionTime{" +
                "executionTimeInNano=" + executionTimeNs +
                ", output=" + output +
                '}';
    }
    
}
