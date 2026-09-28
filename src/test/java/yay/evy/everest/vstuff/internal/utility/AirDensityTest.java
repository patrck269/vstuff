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
}
