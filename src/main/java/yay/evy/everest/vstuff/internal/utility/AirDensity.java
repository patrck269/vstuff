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
            if (utils == null || dimensionId == null) {
                return 1.0;
            }
            double seaY = seaLevelY(dimensionId);
            return ratio(utils.getAirDensityForY(world.y, dimensionId), utils.getAirDensityForY(seaY, dimensionId));
        } catch (RuntimeException ignored) {
            return 1.0;
        }
    }

    private static double seaLevelY(String dimensionId) {
        var map = DimensionParametersResolver.INSTANCE.getDimensionMap();
        if (map == null) {
            return 62.0;
        }
        var params = map.get(dimensionId);
        if (params == null) {
            return 62.0;
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
