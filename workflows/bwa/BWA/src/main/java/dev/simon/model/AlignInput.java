package dev.simon.model;

public class AlignInput {

    String inputFasta;
    String inputFastq;
    String outputSai;

    public AlignInput(String inputFasta, String inputFastq, String outputSai) {
        this.inputFasta = inputFasta;
        this.inputFastq = inputFastq;
        this.outputSai = outputSai;
    }

    public String getInputFasta() {
        return inputFasta;
    }

    public void setInputFasta(String inputFasta) {
        this.inputFasta = inputFasta;
    }

    public String getInputFastq() {
        return inputFastq;
    }

    public void setInputFastq(String inputFastq) {
        this.inputFastq = inputFastq;
    }

    public String getOutputSai() {
        return outputSai;
    }

    public void setOutputSai(String outputSai) {
        this.outputSai = outputSai;
    }
}
