# Density-Altitude Stress Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Scale sea-level stress for the Create encased fan, Clockwork propeller bearings, and the vstuff mechanical thruster by Valkyrien Skies air density, and scale the thruster's thrust by that same ratio.

**Architecture:** A pure scaler turns two densities into a ratio and applies it to a sea-level number. A level lookup asks Valkyrien Skies for those densities at the machine's world height and at that dimension's sea level. The thruster calls the lookup directly. Mixins multiply the stress returned by the encased fan and the propeller bearing.

**Tech Stack:** Minecraft 1.20.1, Forge 47.4.16, Create 6.0.8, Valkyrien Skies 2.4.11, Clockwork 0.5.6, Java 17, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-09-27-density-altitude-su-design.md`

## Global Constraints

- Ratio is `airDensity(worldY, dimension) / airDensity(seaLevelY, dimension)` from `AerodynamicUtils.getAirDensityForY`.
- Sea-level Y is that dimension's VS atmosphere sea level. Overworld default is Y=62. The air column runs out at Y=962.
- Below sea level the ratio stays 1. A missing lookup or unresolvable position returns 1.
- World height is the position after the ship transform, never the shipyard block Y.
- Configured stress numbers and the thruster thrust formula are the sea-level values.
- Shaft RPM stays the real RPM. Other kinetic blocks are unchanged.
- Create fan stream length and processing stay as they are.
- Clockwork thrust is not scaled again.
- Server and client both use the helper.
- No new config keys. No persisted baseline.
- Overworld sample ratios, within 0.01: Y=128 about 0.58, Y=256 about 0.15, Y=1000 about 0.

---

## File structure

- Create `src/main/java/yay/evy/everest/vstuff/internal/utility/AirDensity.java` — pure ratio, pure scale, and the level lookup.
- Create `src/test/java/yay/evy/everest/vstuff/internal/utility/AirDensityTest.java` — ratio and scale tests.
- Create `src/test/java/yay/evy/everest/vstuff/internal/utility/DensityCallSiteTest.java` — asserts the thruster and mixins call the helper.
- Modify `src/main/java/yay/evy/everest/vstuff/content/ships/thrust/MechanicalThrusterBlockEntity.java` — stress and thrust.
- Create `src/main/java/yay/evy/everest/vstuff/internal/mixins/EncasedFanStressMixin.java` — fan stress.
- Create `src/main/java/yay/evy/everest/vstuff/internal/mixins/PropellerBearingStressMixin.java` — propeller stress.
- Modify `src/main/resources/vstuff.mixins.json` — register the mixins.
- Modify `build.gradle` — JUnit test task.

### Task 1: Pure ratio and scale

**Files:**
- Create: `src/main/java/yay/evy/everest/vstuff/internal/utility/AirDensity.java`
- Test: `src/test/java/yay/evy/everest/vstuff/internal/utility/AirDensityTest.java`
- Modify: `build.gradle`

**Interfaces:**
- Consumes: nothing
- Produces: `AirDensity.ratio(double densityHere, double densityAtSeaLevel): double` and `AirDensity.scale(float seaLevelValue, double ratio): float`

- [ ] **Step 1: Add JUnit and write the failing test**

In `build.gradle`, inside `dependencies`, add:

```gradle
testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
```

After the `dependencies` block, add:

```gradle
tasks.named('test', Test).configure {
    useJUnitPlatform()
}
```

Create `src/test/java/yay/evy/everest/vstuff/internal/utility/AirDensityTest.java`:

```java
package yay.evy.everest.vstuff.internal.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `.\gradlew.bat test --tests yay.evy.everest.vstuff.internal.utility.AirDensityTest`

Expected: compile failure, `AirDensity` does not exist.

- [ ] **Step 3: Write the minimal implementation**

Create `src/main/java/yay/evy/everest/vstuff/internal/utility/AirDensity.java`:

```java
package yay.evy.everest.vstuff.internal.utility;

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
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `.\gradlew.bat test --tests yay.evy.everest.vstuff.internal.utility.AirDensityTest`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add build.gradle src/main/java/yay/evy/everest/vstuff/internal/utility/AirDensity.java src/test/java/yay/evy/everest/vstuff/internal/utility/AirDensityTest.java
git commit -m "Add the sea-level air-density ratio."
```

### Task 2: World-height lookup

**Files:**
- Modify: `src/main/java/yay/evy/everest/vstuff/internal/utility/AirDensity.java`
- Test: `src/test/java/yay/evy/everest/vstuff/internal/utility/AirDensityTest.java`

**Interfaces:**
- Consumes: `AirDensity.ratio(double, double)`
- Produces: `AirDensity.ratioAt(Level level, BlockPos pos): double`. Null level, null pos, a missing ship world, or a missing `getAerodynamicUtils` returns 1. World Y comes from `VSGameUtilsKt.toWorldCoordinates(level, pos)`. Sea-level Y comes from `DimensionParametersResolver.INSTANCE.getDimensionMap().get(dimensionId).getSeaLevel()`, or 62 when that dimension has no entry. Dimension id comes from `VSGameUtilsKt.getDimensionId(level)`.

- [ ] **Step 1: Write the failing test**

Add to `AirDensityTest`:

```java
@Test
void missingLevelReturnsOne() {
    assertEquals(1.0, AirDensity.ratioAt(null, null), 0.0);
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `.\gradlew.bat test --tests yay.evy.everest.vstuff.internal.utility.AirDensityTest.missingLevelReturnsOne`

Expected: compile failure, `ratioAt` does not exist.

- [ ] **Step 3: Write the lookup**

Add these methods to `AirDensity`. Keep `ratio` and `scale` unchanged.

```java
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
```

Imports:

```java
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.core.api.util.AerodynamicUtils;
import org.valkyrienskies.core.internal.world.VsiServerShipWorld;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.config.DimensionParametersResolver;

import java.lang.reflect.Method;
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `.\gradlew.bat test --tests yay.evy.everest.vstuff.internal.utility.AirDensityTest`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/yay/evy/everest/vstuff/internal/utility/AirDensity.java src/test/java/yay/evy/everest/vstuff/internal/utility/AirDensityTest.java
git commit -m "Look up the air-density ratio at a block's world height."
```

### Task 3: Thruster stress and thrust

**Files:**
- Modify: `src/main/java/yay/evy/everest/vstuff/content/ships/thrust/MechanicalThrusterBlockEntity.java` (method `updateThrust` around line 89, and a new `calculateStressApplied` override)
- Test: `src/test/java/yay/evy/everest/vstuff/internal/utility/DensityCallSiteTest.java`

**Interfaces:**
- Consumes: `AirDensity.ratioAt(Level, BlockPos)`, `AirDensity.scale(float, double)`
- Produces: thruster thrust `scale(seaLevelThrust, ratioAt(level, worldPosition))` and stress `scale(super.calculateStressApplied(), ratioAt(getLevel(), getBlockPos()))`

- [ ] **Step 1: Write the failing call-site test**

Create `src/test/java/yay/evy/everest/vstuff/internal/utility/DensityCallSiteTest.java`:

```java
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
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `.\gradlew.bat test --tests yay.evy.everest.vstuff.internal.utility.DensityCallSiteTest.thrusterScalesThrustAndStress`

Expected: FAIL because `calculateStressApplied` is absent and `AirDensity` is not referenced.

- [ ] **Step 3: Scale thrust and stress**

In `updateThrust`, replace the thrust assignment:

```java
float seaLevelThrust = BASE_MAX_THRUST * thrustMultiplier * softPower * obstructionEffect;
float thrust = AirDensity.scale(seaLevelThrust, AirDensity.ratioAt(level, worldPosition));
thrusterData.setThrust(thrust);
```

Add this override. The kinetic network uses the returned value.

```java
@Override
public float calculateStressApplied() {
    float seaLevelStress = super.calculateStressApplied();
    return AirDensity.scale(seaLevelStress, AirDensity.ratioAt(getLevel(), getBlockPos()));
}
```

Import `yay.evy.everest.vstuff.internal.utility.AirDensity`.

- [ ] **Step 4: Run the tests**

Run: `.\gradlew.bat test --tests yay.evy.everest.vstuff.internal.utility.DensityCallSiteTest --tests yay.evy.everest.vstuff.internal.utility.AirDensityTest`

Expected: PASS. Also compile the main sources with `.\gradlew.bat compileJava`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/yay/evy/everest/vstuff/content/ships/thrust/MechanicalThrusterBlockEntity.java src/test/java/yay/evy/everest/vstuff/internal/utility/DensityCallSiteTest.java
git commit -m "Scale mechanical thruster stress and thrust by air density."
```

### Task 4: Encased fan stress

**Files:**
- Create: `src/main/java/yay/evy/everest/vstuff/internal/mixins/EncasedFanStressMixin.java`
- Modify: `src/main/resources/vstuff.mixins.json`
- Test: `src/test/java/yay/evy/everest/vstuff/internal/utility/DensityCallSiteTest.java`

**Interfaces:**
- Consumes: `AirDensity.scale`, `AirDensity.ratioAt`
- Produces: a mixin on `KineticBlockEntity.calculateStressApplied` that scales the return value only when the instance is `EncasedFanBlockEntity`. The encased fan does not override that method, so the parent method is the one that runs.

- [ ] **Step 1: Extend the failing test**

Add to `DensityCallSiteTest`:

```java
@Test
void encasedFanMixinScalesStress() throws Exception {
    String source = Files.readString(Path.of(
            "src/main/java/yay/evy/everest/vstuff/internal/mixins/EncasedFanStressMixin.java"));
    String config = Files.readString(Path.of("src/main/resources/vstuff.mixins.json"));
    assertTrue(source.contains("EncasedFanBlockEntity"));
    assertTrue(source.contains("calculateStressApplied"));
    assertTrue(source.contains("AirDensity.scale"));
    assertTrue(source.contains("AirDensity.ratioAt"));
    assertTrue(config.contains("EncasedFanStressMixin"));
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `.\gradlew.bat test --tests yay.evy.everest.vstuff.internal.utility.DensityCallSiteTest.encasedFanMixinScalesStress`

Expected: FAIL because the mixin file does not exist.

- [ ] **Step 3: Add the mixin**

Create `src/main/java/yay/evy/everest/vstuff/internal/mixins/EncasedFanStressMixin.java`:

```java
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
```

In `vstuff.mixins.json`, add `"EncasedFanStressMixin"` to the `mixins` array next to `"ShipAssemblyMixin"`.

- [ ] **Step 4: Run the tests and compile**

Run: `.\gradlew.bat test --tests yay.evy.everest.vstuff.internal.utility.DensityCallSiteTest` and `.\gradlew.bat compileJava`

Expected: PASS and BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add src/main/java/yay/evy/everest/vstuff/internal/mixins/EncasedFanStressMixin.java src/main/resources/vstuff.mixins.json src/test/java/yay/evy/everest/vstuff/internal/utility/DensityCallSiteTest.java
git commit -m "Scale encased fan stress by air density."
```

### Task 5: Propeller bearing stress

**Files:**
- Create: `src/main/java/yay/evy/everest/vstuff/internal/mixins/PropellerBearingStressMixin.java`
- Modify: `src/main/resources/vstuff.mixins.json`
- Test: `src/test/java/yay/evy/everest/vstuff/internal/utility/DensityCallSiteTest.java`

**Interfaces:**
- Consumes: `AirDensity.scale`, `AirDensity.ratioAt`
- Produces: a mixin on `org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlockEntity.calculateStressApplied` that replaces the returned stress with the scaled value. Plain, brass, and jury-rigged bearings share this entity. Do not change Clockwork thrust classes.

- [ ] **Step 1: Extend the failing test**

Add to `DensityCallSiteTest`:

```java
@Test
void propellerBearingMixinScalesStress() throws Exception {
    String source = Files.readString(Path.of(
            "src/main/java/yay/evy/everest/vstuff/internal/mixins/PropellerBearingStressMixin.java"));
    String config = Files.readString(Path.of("src/main/resources/vstuff.mixins.json"));
    assertTrue(source.contains("PropellerBearingBlockEntity"));
    assertTrue(source.contains("calculateStressApplied"));
    assertTrue(source.contains("AirDensity.scale"));
    assertTrue(source.contains("AirDensity.ratioAt"));
    assertTrue(config.contains("PropellerBearingStressMixin"));
    assertTrue(!source.contains("PropellerController"));
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `.\gradlew.bat test --tests yay.evy.everest.vstuff.internal.utility.DensityCallSiteTest.propellerBearingMixinScalesStress`

Expected: FAIL because the mixin file does not exist.

- [ ] **Step 3: Add the mixin**

Clockwork is not a compile dependency. Target the class by name and extend `KineticBlockEntity`, which is on the Create classpath.

Create `src/main/java/yay/evy/everest/vstuff/internal/mixins/PropellerBearingStressMixin.java`:

```java
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
```

Add `"PropellerBearingStressMixin"` to the `mixins` array in `vstuff.mixins.json`.

- [ ] **Step 4: Run the tests and compile**

Run: `.\gradlew.bat test --tests yay.evy.everest.vstuff.internal.utility.DensityCallSiteTest --tests yay.evy.everest.vstuff.internal.utility.AirDensityTest` and `.\gradlew.bat compileJava`

Expected: PASS and BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add src/main/java/yay/evy/everest/vstuff/internal/mixins/PropellerBearingStressMixin.java src/main/resources/vstuff.mixins.json src/test/java/yay/evy/everest/vstuff/internal/utility/DensityCallSiteTest.java
git commit -m "Scale propeller bearing stress by air density."
```
