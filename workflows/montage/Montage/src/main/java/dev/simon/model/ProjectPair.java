package dev.simon.model;

public record ProjectPair(String inputFit, String outputFit) {

    @Override
    public String toString() {
        return "ProjectPair{" +
                "inputFit=" + inputFit +
                ", outputFit=" + outputFit +
                '}';
    }
}
