package yay.evy.everest.vstuff.internal.mixins;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yay.evy.everest.vstuff.internal.utility.AirDensity;

@Mixin(KineticBlockEntity.class)
public abstract class EncasedFanStressMixin {
    @Inject(method = "calculateStressApplied", at = @At("RETURN"), cancellable = true, remap = false)
    private void vstuff$scaleEncasedFanStress(CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof EncasedFanBlockEntity)) {
            return;
        }
        KineticBlockEntity self = (KineticBlockEntity) (Object) this;
        cir.setReturnValue(AirDensity.scale(cir.getReturnValue(), AirDensity.ratioAt(self.getLevel(), self.getBlockPos())));
    }
}
