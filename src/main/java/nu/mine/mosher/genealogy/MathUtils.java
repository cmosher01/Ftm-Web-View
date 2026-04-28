package nu.mine.mosher.genealogy;

public class MathUtils {
    public static double clamp(final double min, final double n, final double max) {
        if (max < min) {
            return n;
        }
        if (n < min) {
            return min;
        }
        if (max < n) {
            return max;
        }
        return n;
    }
}
