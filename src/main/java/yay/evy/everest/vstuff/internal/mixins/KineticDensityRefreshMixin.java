package yay.evy.everest.vstuff.internal.mixins;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yay.evy.everest.vstuff.content.ships.thrust.MechanicalThrusterBlockEntity;
import yay.evy.everest.vstuff.internal.utility.AirDensity;

@Mixin(KineticBlockEntity.class)
public abstract class KineticDensityRefreshMixin {
    private static final String PROPELLER_BEARING =
            "org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlockEntity";

    @Inject(method = "tick", at = @At("RETURN"), remap = false)
    private void vstuff$refreshStressWhenAirDensityMoves(CallbackInfo ci) {
        Object self = this;
        if (!(self instanceof EncasedFanBlockEntity)
                && !(self instanceof MechanicalThrusterBlockEntity)
                && !self.getClass().getName().equals(PROPELLER_BEARING)) {
            return;
        }
        KineticBlockEntity kinetic = (KineticBlockEntity) self;
        if (AirDensity.ratioMoved(this, AirDensity.ratioAt(kinetic.getLevel(), kinetic.getBlockPos()))) {
            kinetic.networkDirty = true;
        }
    }
}
