package yay.evy.everest.vstuff.internal.utility;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DensityCallSiteTest {
    @Test
    void thrusterScalesThrustAndStress() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/yay/evy/everest/vstuff/content/ships/thrust/MechanicalThrusterBlockEntity.java"));
        assertTrue(source.contains("AirDensity.scale"));
        assertTrue(source.contains("AirDensity.ratioAt"));
        assertTrue(source.contains("calculateStressApplied"));
        assertTrue(!source.contains("propellerStress"));
    }

    @Test
    void encasedFanMixinScalesStress() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/yay/evy/everest/vstuff/internal/mixins/EncasedFanStressMixin.java"));
        String config = Files.readString(Path.of("src/main/resources/vstuff.mixins.json"));
        assertTrue(source.contains("EncasedFanBlockEntity"));
        assertTrue(source.contains("calculateStressApplied"));
        assertTrue(source.contains("AirDensity.scale"));
        assertTrue(source.contains("AirDensity.ratioAt"));
        assertTrue(!source.contains("propellerStress"));
        assertTrue(config.contains("EncasedFanStressMixin"));
    }

    @Test
    void kineticDensityRefreshMixinResamplesStress() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/yay/evy/everest/vstuff/internal/mixins/KineticDensityRefreshMixin.java"));
        String config = Files.readString(Path.of("src/main/resources/vstuff.mixins.json"));
        assertTrue(source.contains("updateStressFor"));
        assertTrue(source.contains("ratioMoved"));
        assertTrue(source.contains("EncasedFanBlockEntity"));
        assertTrue(!config.contains("PropellerBearingStressMixin"));
        assertTrue(source.contains("MechanicalThrusterBlockEntity"));
        assertTrue(source.contains("tick"));
        assertTrue(config.contains("KineticDensityRefreshMixin"));
        assertTrue(!source.contains("AirDensity.scale"));
    }
}
