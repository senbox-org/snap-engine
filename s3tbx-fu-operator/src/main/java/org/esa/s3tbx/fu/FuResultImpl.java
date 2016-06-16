package org.esa.s3tbx.fu;

/**
 * @author Marco Peters
 */
class FuResultImpl implements FuResult {


    double hueAngle;
    byte fuValue;

    // intermediate results
    double x3;
    double y3;
    double z3;
    double chrX;
    double chrY;
    double hue;
    double polyCorr;

    @Override
    public double getHueAngle() {
        return hueAngle;
    }

    @Override
    public byte getFuValue() {
        return fuValue;
    }

    @Override
    public double getX3() {
        return x3;
    }

    @Override
    public double getY3() {
        return y3;
    }

    @Override
    public double getZ3() {
        return z3;
    }

    @Override
    public double getChrX() {
        return chrX;
    }

    @Override
    public double getChrY() {
        return chrY;
    }

    @Override
    public double getHue() {
        return hue;
    }

    @Override
    public double getPolyCorr() {
        return polyCorr;
    }
}
