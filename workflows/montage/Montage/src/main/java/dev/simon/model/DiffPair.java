package dev.simon.model;

public class DiffPair {
    private int firstIndex;
    private int secondIndex;
    private String firstInputFits;
    private String secondInputFits;
    private String diffOutput;

    public DiffPair(int firstIndex, int secondIndex, String firstInputFits, String secondInputFits) {
        this.firstIndex = firstIndex;
        this.secondIndex = secondIndex;
        this.firstInputFits = firstInputFits;
        this.secondInputFits = secondInputFits;
        this.diffOutput = "diff." + leftZeroPadding(String.valueOf(firstIndex), 6) + "." + leftZeroPadding(String.valueOf(secondIndex), 6) + ".fits";
    }

    public String getFirstInputFits() {
        return firstInputFits;
    }

    public String getFirstAreaFits() {
        return firstInputFits.replace(".fits", "_area.fits");
    }

    public String getSecondInputFits() {
        return secondInputFits;
    }

    public String getSecondAreaFits() {
        return secondInputFits.replace(".fits", "_area.fits");
    }

    public String getDiffOutput() {
        return diffOutput;
    }

    private String leftZeroPadding(String string, int numberOfDigits) {
        StringBuilder stringBuilder = new StringBuilder(string);
        while (stringBuilder.length() < numberOfDigits) {
            stringBuilder.insert(0, "0");
        }
        return stringBuilder.toString();
    }

    public String getFitTextFile(){
        return "fit." + leftZeroPadding(String.valueOf(firstIndex), 6) + "." + leftZeroPadding(String.valueOf(secondIndex), 6) + ".txt";
    }

    public int getFirstIndex() {
        return firstIndex;
    }

    public int getSecondIndex() {
        return secondIndex;
    }

    public void setFirstIndex(int firstIndex) {
        this.firstIndex = firstIndex;
    }

    public void setSecondIndex(int secondIndex) {
        this.secondIndex = secondIndex;
    }

    public void setFirstInputFits(String firstInputFits) {
        this.firstInputFits = firstInputFits;
    }

    public void setSecondInputFits(String secondInputFits) {
        this.secondInputFits = secondInputFits;
    }

    public void setDiffOutput(String diffOutput) {
        this.diffOutput = diffOutput;
    }

    @Override
    public String toString() {
        return "DiffPair{" +
                "firstIndex=" + firstIndex +
                ", secondIndex=" + secondIndex +
                ", firstInputFits='" + firstInputFits + '\'' +
                ", secondInputFits='" + secondInputFits + '\'' +
                ", diffOutput='" + diffOutput + '\'' +
                '}';
    }
}
