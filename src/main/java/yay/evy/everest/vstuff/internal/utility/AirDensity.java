package yay.evy.everest.vstuff.internal.utility;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.core.api.util.AerodynamicUtils;
import org.valkyrienskies.core.internal.world.VsiServerShipWorld;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.config.DimensionParametersResolver;

import java.lang.reflect.Method;

public final class AirDensity {
    private static final double DEFAULT_SEA_LEVEL = 62.0;
    private static final double DEFAULT_MAX_Y = 962.0;
    private static final double DEFAULT_GRAVITY = 10.0;
    private static final double COLUMN_HEIGHT_METERS = 71000.0;
    private static final double ATMOSPHERE_TOP_METERS = 84852.0;
    private static final double MOLAR_MASS = 0.0289644;
    private static final double GAS_CONSTANT = 8.314;
    private static final double[] LAYER_BASE_METERS = {0.0, 11000.0, 20000.0, 32000.0, 47000.0, 51000.0, 71000.0};
    private static final double[] LAYER_DENSITY = {1.225, 0.36391, 0.08803, 0.01322, 0.00143, 8.6e-4, 6.4e-5};
    private static final double[] LAYER_TEMPERATURE = {288.15, 216.65, 216.65, 228.65, 270.65, 270.65, 214.65};
    private static final double[] LAYER_LAPSE = {0.0065, 0.0, -0.001, -0.0028, 0.0, 0.0028, 0.002};

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

    public static double ratioAt(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return 1.0;
        }
        try {
            Vec3 world = VSGameUtilsKt.toWorldCoordinates(level, pos);
            String dimensionId = VSGameUtilsKt.getDimensionId(level);
            AerodynamicUtils utils = aerodynamicUtils(level);
            if (utils != null && dimensionId != null) {
                double seaY = seaLevelY(dimensionId);
                return ratio(utils.getAirDensityForY(world.y, dimensionId), utils.getAirDensityForY(seaY, dimensionId));
            }
            if (utils != null) {
                return 1.0;
            }
            // Client ship worlds do not implement getAerodynamicUtils.
            return standardAtmosphereRatio(world.y, dimensionId);
        } catch (RuntimeException ignored) {
            return 1.0;
        }
    }

    static double standardAtmosphereRatio(double worldY) {
        return standardAtmosphereRatio(worldY, null);
    }

    private static double standardAtmosphereRatio(double worldY, String dimensionId) {
        double seaLevel = DEFAULT_SEA_LEVEL;
        double maxY = DEFAULT_MAX_Y;
        double gravity = DEFAULT_GRAVITY;
        if (dimensionId != null) {
            var map = DimensionParametersResolver.INSTANCE.getDimensionMap();
            if (map != null) {
                var params = map.get(dimensionId);
                if (params != null) {
                    seaLevel = params.getSeaLevel();
                    maxY = params.getMaxY();
                    var gravityVector = params.getGravity();
                    if (gravityVector != null) {
                        gravity = gravityVector.length();
                    }
                }
            }
        }
        if (maxY <= seaLevel) {
            return 1.0;
        }
        double metersPerBlock = COLUMN_HEIGHT_METERS / (maxY - seaLevel);
        double altitude = Math.max(0.0, (worldY - seaLevel) * metersPerBlock);
        return ratio(densityAtAltitude(altitude, gravity), densityAtAltitude(0.0, gravity));
    }

    private static double densityAtAltitude(double altitudeMeters, double gravity) {
        if (altitudeMeters >= ATMOSPHERE_TOP_METERS) {
            return 0.0;
        }
        int layer = 0;
        for (int i = 1; i < LAYER_BASE_METERS.length; i++) {
            if (altitudeMeters >= LAYER_BASE_METERS[i]) {
                layer = i;
            }
        }
        double rise = altitudeMeters - LAYER_BASE_METERS[layer];
        double rho0 = LAYER_DENSITY[layer];
        double temperature = LAYER_TEMPERATURE[layer];
        double lapse = LAYER_LAPSE[layer];
        if (lapse == 0.0) {
            return rho0 * Math.exp(-gravity * MOLAR_MASS / (GAS_CONSTANT * temperature) * rise);
        }
        double scaled = (temperature - lapse * rise) / temperature;
        double exponent = gravity * MOLAR_MASS / (GAS_CONSTANT * lapse) - 1.0;
        return rho0 * Math.pow(scaled, exponent);
    }

    private static double seaLevelY(String dimensionId) {
        var map = DimensionParametersResolver.INSTANCE.getDimensionMap();
        if (map == null) {
            return DEFAULT_SEA_LEVEL;
        }
        var params = map.get(dimensionId);
        if (params == null) {
            return DEFAULT_SEA_LEVEL;
        }
        return params.getSeaLevel();
    }

    private static AerodynamicUtils aerodynamicUtils(Level level) {
        Object shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
        if (shipWorld instanceof VsiServerShipWorld serverWorld) {
            return serverWorld.getAerodynamicUtils();
        }
        if (shipWorld == null) {
            return null;
        }
        try {
            Method method = shipWorld.getClass().getMethod("getAerodynamicUtils");
            Object value = method.invoke(shipWorld);
            if (value instanceof AerodynamicUtils utils) {
                return utils;
            }
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
        return null;
    }
}
