# Changelog

## 1.1.0

### Added

- Validation for item names, lore, durability, custom potion effects, firework power/effects, book authors, and optional unbreakable items.
- Read-only `/mprotect inspect` and `/mprotect scan` tools for checking held items and inventories.
- Bounded pending alert buckets and validation for alert permissions, storage filenames, player filters, and update IDs.

### Fixed

- Protection listeners now start only after the H2 schema is ready; storage initialization failure disables the plugin safely.
- Storage filenames can no longer traverse outside the plugin data directory.
- Shutdown waits for queued violation writes with a bounded timeout.
- Version 1 configs migrate away from blocking every `minecraft:*` command while explicit dangerous commands remain blocked.
- Discord alerts accept only HTTPS Discord webhook hosts and violation details remove control characters.

## 1.0.0

### Added

- Configurable item, book, sign, anvil, command, creative, entity, and chunk checks.
- Safe recursive validation for shulker boxes and bundles, including overstacked items.
- H2 violation history, JSONL audit log, staff alerts, and optional Discord webhook alerts.
- English and Russian messages, bStats metrics, and Modrinth update checks.
- Verified support for Paper, Purpur, and Folia 26.2 on Java 25.
