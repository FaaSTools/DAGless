package dev.simon.model;

public class Execution {

    private final String executable;
    private final long executionTimeNs;
    private final String output;

    public Execution(String executable, long executionTimeNs, String output) {
        this.executable = executable;
        this.executionTimeNs = executionTimeNs;
        this.output = output;
    }

    public String getExecutable() {
        return executable;
    }

    public long getExecutionTimeNs() {
        return executionTimeNs;
    }

    public String getOutput() {
        return output;
    }

    @Override
    public String toString() {
        return "Execution{" +
                "executable='" + executable + '\'' +
                ", executionTimeNs=" + executionTimeNs +
                ", output='" + output + '\'' +
                '}';
    }
}
