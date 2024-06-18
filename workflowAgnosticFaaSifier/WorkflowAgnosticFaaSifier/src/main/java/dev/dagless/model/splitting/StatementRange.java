package dev.dagless.model.splitting;

public class StatementRange {

    private int start;
    private int end;

    public StatementRange() {
        this.start = 1;
        this.end = 1;
    }

    private StatementRange(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public void incrementEnd() {
        this.end++;
    }

    public void setStartToEnd() {
        this.start = this.end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public StatementRange getCurrentRange() {
        return new StatementRange(this.start, this.end-1);
    }

    public String getStatementRangeAsJson() {
        return "{\"start\":" + this.start + ",\"end\":" + this.end + "}";
    }

    @Override
    public String toString() {
        return "StatementRange{" +
                "start=" + start +
                ", end=" + end +
                '}';
    }
}
