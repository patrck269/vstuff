package yay.evy.everest.vstuff.internal.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirDensityTest {
    @Test
    void seaLevelRatioIsOne() {
        assertEquals(1.0, AirDensity.ratio(1.225, 1.225), 1.0e-9);
    }

    @Test
    void equalDensityStaysOne() {
        assertEquals(1.0, AirDensity.ratio(1.225, 1.225), 1.0e-9);
    }

    @Test
    void overworldSamplesMatchValkyrienSkies() {
        assertEquals(0.58, AirDensity.ratio(0.71052, 1.225), 0.01);
        assertEquals(0.15, AirDensity.ratio(0.18213, 1.225), 0.01);
        assertEquals(0.0, AirDensity.ratio(0.00004, 1.225), 0.01);
    }

    @Test
    void missingLookupReturnsOne() {
        assertEquals(1.0, AirDensity.ratio(Double.NaN, 1.225), 0.0);
        assertEquals(1.0, AirDensity.ratio(1.0, 0.0), 0.0);
        assertEquals(1.0, AirDensity.ratio(-1.0, 1.225), 0.0);
    }

    @Test
    void ratioDoesNotExceedSeaLevel() {
        assertEquals(1.0, AirDensity.ratio(2.0, 1.225), 0.0);
    }

    @Test
    void scaleMultipliesSeaLevelValue() {
        assertEquals(50.0f, AirDensity.scale(100.0f, 0.5), 1.0e-4f);
        assertEquals(100_000.0f, AirDensity.scale(100_000.0f, 1.0), 1.0e-3f);
    }

    @Test
    void missingLevelReturnsOne() {
        assertEquals(1.0, AirDensity.ratioAt(null, null), 0.0);
    }

    @Test
    void ratioMovedIgnoresFirstObservationUntilThreshold() {
        Object key = new Object();
        assertFalse(AirDensity.ratioMoved(key, 0.0));
        assertFalse(AirDensity.ratioMoved(key, 0.005));
        assertTrue(AirDensity.ratioMoved(key, 0.015));
        assertFalse(AirDensity.ratioMoved(key, 0.015));
        assertFalse(AirDensity.ratioMoved(new Object(), 0.2));
    }

    @Test
    void overworldCurveMatchesValkyrienSkies() {
        assertEquals(1.0, AirDensity.standardAtmosphereRatio(62.0), 0.01);
        assertEquals(0.58, AirDensity.standardAtmosphereRatio(128.0), 0.01);
        assertEquals(0.15, AirDensity.standardAtmosphereRatio(256.0), 0.01);
        assertEquals(0.0, AirDensity.standardAtmosphereRatio(1000.0), 0.01);
    }

    @Test
    void propellerStressCostsMoreAsAirThins() {
        float seaLevelStress = 64.0f;
        double seaDensity = 1.225;
        double[] densities = {1.225, 0.71052, 0.18213, 0.00004};
        float[] stress = new float[densities.length];
        double[] ratio = new double[densities.length];
        double[] hover = new double[densities.length];
        for (int i = 0; i < densities.length; i++) {
            ratio[i] = AirDensity.ratio(densities[i], seaDensity);
            assertTrue(ratio[i] > 0.0);
            stress[i] = AirDensity.propellerStress(seaLevelStress, ratio[i]);
            assertTrue(Float.isFinite(stress[i]));
            float quotient = (float) (seaLevelStress / ratio[i]);
            assertEquals(quotient, stress[i], Math.max(1.0e-3f, Math.abs(quotient) * 1.0e-4f));
            hover[i] = stress[i] * (1.0 / Math.sqrt(ratio[i]));
        }
        assertEquals(seaLevelStress, stress[0], 0.0f);
        assertEquals(1.72f, stress[1] / seaLevelStress, 0.02f);
        assertEquals(6.67f, stress[2] / seaLevelStress, 0.08f);
        assertTrue(hover[1] > hover[0]);
        assertTrue(hover[2] > hover[1]);
        assertTrue(hover[3] > hover[2]);

        float vacuum = AirDensity.propellerStress(seaLevelStress, AirDensity.ratio(0.0, seaDensity));
        assertTrue(Float.isFinite(vacuum));
        assertTrue(vacuum > stress[2]);
        assertTrue(vacuum > stress[3]);
        assertTrue(vacuum > seaLevelStress);

        assertEquals(seaLevelStress, AirDensity.propellerStress(seaLevelStress, AirDensity.ratio(Double.NaN, seaDensity)), 0.0f);
        assertEquals(seaLevelStress, AirDensity.propellerStress(seaLevelStress, AirDensity.ratio(-1.0, seaDensity)), 0.0f);
        assertEquals(seaLevelStress, AirDensity.propellerStress(seaLevelStress, AirDensity.ratio(1.0, 0.0)), 0.0f);
        assertEquals(seaLevelStress, AirDensity.propellerStress(seaLevelStress, AirDensity.ratio(2.0, seaDensity)), 0.0f);
    }
}
