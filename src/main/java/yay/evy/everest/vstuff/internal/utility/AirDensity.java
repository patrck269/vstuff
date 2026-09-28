package yay.evy.everest.vstuff.internal.utility;

public final class AirDensity {
    private AirDensity() {}

    public static double ratio(double densityHere, double densityAtSeaLevel) {
        if (!(densityAtSeaLevel > 0.0) || !(densityHere >= 0.0) || Double.isNaN(densityHere)) {
            return 1.0;
        }
        double ratio = densityHere / densityAtSeaLevel;
        if (ratio > 1.0) {
            return 1.0;
        }
        return ratio;
    }

    public static float scale(float seaLevelValue, double ratio) {
        return (float) (seaLevelValue * ratio);
    }
}
