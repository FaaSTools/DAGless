package dev.simon.model;

import java.util.Objects;

public final class BackgroundPair {
    private  String projectedFit;
    private  String projectedAreaFit;
    private  String correctedAreaFit;
    private  String correctedFit;

    public BackgroundPair(String projectedFit, String correctedFit) {
        this.projectedFit = projectedFit;
        this.correctedFit = correctedFit;
        this.projectedAreaFit = projectedFit.replace(".fits", "_area.fits");
        this.correctedAreaFit = correctedFit.replace(".fits", "_area.fits");
    }

    public String getProjectedFit() {
        return projectedFit;
    }

    public String getProjectedAreaFit() {
        return projectedAreaFit;
    }

    public String getCorrectedAreaFit() {
        return correctedAreaFit;
    }

    public String getCorrectedFit() {
        return correctedFit;
    }

    public void setProjectedFit(String projectedFit) {
        this.projectedFit = projectedFit;
    }

    public void setProjectedAreaFit(String projectedAreaFit) {
        this.projectedAreaFit = projectedAreaFit;
    }

    public void setCorrectedAreaFit(String correctedAreaFit) {
        this.correctedAreaFit = correctedAreaFit;
    }

    public void setCorrectedFit(String correctedFit) {
        this.correctedFit = correctedFit;
    }

    @Override
    public String toString() {
        return "BackgroundPair{" +
                "projectedFit='" + projectedFit + '\'' +
                ", projectedAreaFit='" + projectedAreaFit + '\'' +
                ", correctedAreaFit='" + correctedAreaFit + '\'' +
                ", correctedFit='" + correctedFit + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BackgroundPair) obj;
        return Objects.equals(this.projectedFit, that.projectedFit) &&
                Objects.equals(this.correctedFit, that.correctedFit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectedFit, correctedFit);
    }

}
