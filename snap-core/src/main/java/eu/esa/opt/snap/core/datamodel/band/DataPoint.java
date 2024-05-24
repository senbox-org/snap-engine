package eu.esa.opt.snap.core.datamodel.band;

public class DataPoint {

    private int x;
    private int y;
    private double value;

    public DataPoint(int x, int y, double value) {
        this.x = x;
        this.y = y;
        this.value = value;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public double getValue() {
        return value;
    }
}
