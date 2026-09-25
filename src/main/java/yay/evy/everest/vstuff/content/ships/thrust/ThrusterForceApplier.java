package yay.evy.everest.vstuff.content.ships.thrust;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import net.minecraft.core.BlockPos;

public class ThrusterForceApplier {
    ThrusterData data;

    @JsonCreator
    public ThrusterForceApplier(@JsonProperty("data") ThrusterData data) {
        this.data = data;
    }

    public ThrusterForceApplier() {}

    @JsonIgnore private final Vector3d worldForceDirection = new Vector3d();
    @JsonIgnore private final Vector3d worldForce = new Vector3d();

    public void applyForces(BlockPos pos, PhysShipImpl ship) {
        float thrust = data.getThrust();
        if (thrust == 0) return;

        final ShipTransform transform = ship.getTransform();
        final Vector3dc shipCenterOfMass = transform.getPositionInShip();

        Vector3d relativePos = VectorConversionsMCKt.toJOMLD(pos)
                .add(0.5, 0.5, 0.5)
                .sub(shipCenterOfMass);

        Vector3d thrusterDir = new Vector3d(data.getDirection());
        if (thrusterDir.lengthSquared() < 1e-6) {
            thrusterDir.set(0, 0, 1);
        }

        transform.getShipToWorld().transformDirection(thrusterDir, worldForceDirection);
        worldForceDirection.normalize();
        worldForce.set(worldForceDirection).mul(thrust);
        ship.applyInvariantForceToPos(worldForce, relativePos);
    }
}