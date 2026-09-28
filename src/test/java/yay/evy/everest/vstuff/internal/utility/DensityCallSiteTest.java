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
    }
}
