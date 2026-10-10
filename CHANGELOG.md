# Changelog

## 1.2.1 - 2026-10-10

- Added Minecraft 26.3 compatibility and verified the clean build and automated test suite against Paper API 26.3.

## 1.2.0

- Added bounded per-chunk redstone, piston, hopper, dispenser, fluid, fire, sculk, and block-physics protection.
- Added TNT-prime and explosion windows with configurable affected-block and yield caps.
- Added per-block/per-chunk spawner limits and bounded portal creation, search, and player-use rules.
- Added per-entity-type caps, spawn-burst protection, and cross-chunk entity counter maintenance.
- Added player inventory, interaction, block-change, item-drop, and projectile event-rate limits.
- Added bounded key registries so crafted coordinates or UUIDs cannot grow protection maps without limit.
- Bounded database/JSONL writes and player notices so detection floods cannot turn mProtect itself into a lag source.
- Added safe config version 3 defaults, full English/Russian messages, permissions, and a Paper hardening guide.
