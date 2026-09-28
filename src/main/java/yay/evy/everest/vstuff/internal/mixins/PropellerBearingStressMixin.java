package yay.evy.everest.vstuff.internal.mixins;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yay.evy.everest.vstuff.internal.utility.AirDensity;

@Mixin(targets = "org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlockEntity")
public abstract class PropellerBearingStressMixin extends KineticBlockEntity {
    private PropellerBearingStressMixin() {
        super(null, null, null);
    }

    @Inject(method = "calculateStressApplied", at = @At("RETURN"), cancellable = true, remap = false)
    private void vstuff$scalePropellerStress(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(AirDensity.scale(cir.getReturnValue(), AirDensity.ratioAt(getLevel(), getBlockPos())));
    }
}
