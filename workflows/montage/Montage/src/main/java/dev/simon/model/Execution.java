package dev.simon.model;

public record Execution(String executable, long executionTimeNs, String output) {

    @Override
    public String toString() {
        return "Execution{" +
                "executable='" + executable + '\'' +
                ", executionTimeNs=" + executionTimeNs +
                ", output='" + output + '\'' +
                '}';
    }
}
