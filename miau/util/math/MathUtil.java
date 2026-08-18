package miau.util.math;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;
import miau.util.vector.Vector3d;
import net.minecraft.util.MathHelper;

public final class MathUtil {
    public static double getRandom(double min, double max) {
        if (min == max) {
            return min;
        }

        if (min > max) {
            double d = min;
            min = max;
            max = d;
        }

        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static double roundToPlace(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        }

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static float calculateGaussianValue(float x, float sigma) {
        double output = 1.0 / Math.sqrt((Math.PI * 2) * (sigma * sigma));
        return (float)(output * Math.exp(-(x * x) / (2.0 * (sigma * sigma))));
    }

    public static double roundToHalf(double d) {
        return Math.round(d * 2.0) / 2.0;
    }

    public static double round(double value, int places) {
        try {
            BigDecimal bigDecimal = BigDecimal.valueOf(value);
            return bigDecimal.setScale(places, RoundingMode.HALF_UP).doubleValue();
        } catch (Exception exception) {
            return 0.0;
        }
    }

    public static float getClosestMultipleOfDivisor(float valueToRound, float divisor) {
        float quotient = Math.round(valueToRound / divisor);
        return divisor * quotient;
    }

    public static double round(double value, int scale, double inc) {
        double halfOfInc = inc / 2.0;
        double floored = Math.floor(value / inc) * inc;
        return value >= floored + halfOfInc
            ? new BigDecimal(Math.ceil(value / inc) * inc).setScale(scale, RoundingMode.HALF_UP).doubleValue()
            : new BigDecimal(floored).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    public static double roundWithSteps(double value, double steps) {
        double a = Math.round(value / steps) * steps;
        a *= 1000.0;
        a = (int)a;
        return a / 1000.0;
    }

    public static double lerp(double a, double b, double c) {
        return a + c * (b - a);
    }

    public static float lerp(float a, float b, float c) {
        return a + c * (b - a);
    }

    public static double getDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double d0 = x2 - x1;
        double d1 = y2 - y1;
        double d2 = z2 - z1;
        return MathHelper.func_76133_a(d0 * d0 + d1 * d1 + d2 * d2);
    }

    public static double clamp(double min, double max, double n) {
        return Math.max(min, Math.min(max, n));
    }

    public static double wrappedDifference(double number1, double number2) {
        return Math.min(
            Math.abs(number1 - number2),
            Math.min(
                Math.abs(number1 - 360.0) - Math.abs(number2 - 0.0),
                Math.abs(number2 - 360.0) - Math.abs(number1 - 0.0)
            )
        );
    }

    public static double getAngleBetweenLocations(Vector3d location1, Vector3d location2) {
        double deltaX = location2.x - location1.x;
        double deltaZ = location2.z - location1.z;
        double yawToLocation = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0;
        return MathHelper.func_76142_g((float)yawToLocation);
    }

    private MathUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
