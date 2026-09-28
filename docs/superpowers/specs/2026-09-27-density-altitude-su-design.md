# Density-altitude stress for fans and propellers

## Purpose

Fans and propellers spend stress units as if the air were always sea-level air. A fan at 1000 RPM should cost far more stress at sea level than at Y=1000. Thrust from the vstuff mechanical thruster should drop the same way. Configured stress numbers, and the thruster's current thrust formula, are the sea-level values.

## Density ratio

One helper in vstuff returns the scale factor for a block:

`ratio = airDensity(worldY, dimension) / airDensity(seaLevelY, dimension)`

Both densities come from Valkyrien Skies `AerodynamicUtils.getAirDensityForY`. That is the function Clockwork already uses for fan and propeller thrust. Sea-level Y is the sea level of that dimension's VS atmosphere. In the overworld that is Y=62, the column runs out at Y=962, and each block is about 79 meters. The ratio is 1 at sea level, about 0.58 at Y=128, about 0.15 at Y=256, and about 0 at Y=1000.

Below sea level, Valkyrien Skies already treats the air as sea-level air, so the ratio stays 1. If the air utilities are missing, or the world position cannot be resolved, the ratio is 1.

The height passed in is the machine's position in the world after the ship transform. A ship block's stored coordinates are in the shipyard, and those must not be used as altitude.

The server and the client both use this helper, so the goggles and the shaft agree.

## What changes

The shaft's RPM stays the real RPM. Other machines on that shaft are unchanged.

- **Create encased fan.** The stress it adds to the shaft is its sea-level stress cost times the ratio. vstuff mixins `EncasedFanBlockEntity` at the stress calculation. Stream length and processing (washing, blasting, smoking, haunting) stay as they are.
- **Clockwork propeller bearings** (plain, brass, and jury-rigged). The stress each adds is its sea-level stress cost times the ratio. vstuff mixins the block entity those blocks use. They share `PropellerBearingBlockEntity` unless a variant has its own. Thrust stays on Clockwork's existing density scaling and is not scaled again.
- **vstuff mechanical thruster.** Stress and thrust both multiply by the ratio. The thrust formula stays top thrust, times the config multiplier, times the speed curve, times exhaust clearance, and then times the ratio. `MechanicalThrusterBlockEntity` applies both.

## Failure behavior

A missing air lookup or an unresolvable world position uses ratio 1. The machine keeps today's sea-level cost and, for the thruster, today's sea-level thrust. No new config keys. No persisted baseline.

## Testing

A pure function owns the ratio and the two multiplications. Its tests do not need a running world. Production still asks Valkyrien Skies for the densities. The tests feed those densities in:

- Sea level is 1. Below sea level stays 1.
- Overworld samples, using the densities the current Valkyrien Skies build returns, are about 0.58 at Y=128, about 0.15 at Y=256, and about 0 at Y=1000, each within 0.01.
- A missing air lookup returns 1.
- Sea-level stress times the ratio is the stress added to the shaft.
- Sea-level thruster thrust times the ratio is the thrust that gets applied.

The fan mixin, the propeller mixin, and the thruster call that function. Tests assert those call sites. Clockwork's thrust math is not modified.

## Out of scope

Create fan stream length and processing rate. Clockwork thrust formulas. Generators, water wheels, mixers, and every other kinetic block. Gas thrusters and other non-stress propulsion. Changing shaft RPM.
