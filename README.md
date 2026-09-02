<div align="center">
  <h1>mProtect</h1>
  <p>Exploit, lag-machine, and event-spam protection for Paper, Purpur, and Folia.</p>
  <p>
    <a href="https://papermc.io/software/paper"><img alt="Paper" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/paper_vector.svg"></a>
    <a href="https://purpurmc.org"><img alt="Purpur" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/purpur_vector.svg"></a>
    <a href="https://papermc.io/software/folia"><img alt="Folia" height="56" src="https://raw.githubusercontent.com/miklires/mCommand/main/docs/assets/folia-available.png"></a>
  </p>
  <p>
    <a href="https://github.com/miklires/mProtect"><img alt="GitHub" src="https://tr7zw.github.io/uikit/social_buttons_icon/Github-Button-64.png"></a>
    <a href="https://modrinth.com/plugin/mprotect"><img alt="Modrinth" src="https://tr7zw.github.io/uikit/social_buttons_icon/Modrinth-Button-64.png"></a>
    <a href="https://discord.gg/pes25cnWKy"><img alt="Discord" src="https://tr7zw.github.io/uikit/social_buttons_icon/Discord-Button-64.png"></a>
  </p>
  <p>
    <a href="https://bstats.org/plugin/bukkit/mProtect/33359"><img alt="bStats" src="https://img.shields.io/badge/bStats-33359-2F9BE6?style=for-the-badge"></a>
    <a href="https://github.com/miklires/mProtect/releases"><img alt="Release" src="https://img.shields.io/github/v/release/miklires/mProtect?style=for-the-badge"></a>
    <img alt="Java 25" src="https://img.shields.io/badge/Java-25-5382A1?style=for-the-badge">
  </p>
</div>

## What mProtect checks

- Illegal materials, enchantments, attribute modifiers, durability, oversized components, and deeply nested containers.
- Item names/lore, custom potion effects, fireworks, book pages/authors/titles, sign text/click events, and anvil names/costs.
- Command length, frequency, blocked commands, namespaces, and roots.
- Unauthorized creative or spectator mode.
- Excessive entities per chunk and per entity type using maintained counters instead of repeated nearby-entity scans.
- Excessive player movement into new chunks.
- Redstone clocks, piston chains, hopper networks, dispensers, fluid loops, fire spread, sculk cascades, and block-physics storms.
- TNT priming, explosion bursts, oversized blast block lists, spawner bursts, and portal-search or portal-creation abuse.
- Inventory-click, interaction, block-change, item-drop, and projectile event floods.

Every detection can be written to H2 and JSONL, shown to online staff, and optionally sent to a Discord webhook. English and Russian messages are included.

<p align="center">
  <img alt="Available for Folia" src="docs/assets/folia-available.png" width="420">
</p>

## Requirements

- Java 25
- Paper, Purpur, or Folia 26.2
- No required plugins or external database

mProtect protects the server event layer. Malformed packets must be rejected before Bukkit events exist, so use Paper's native packet limiter as described in [Paper hardening](docs/PAPER-HARDENING.md). mProtect does not claim packet interception it cannot perform.

## Installation

1. Stop the server.
2. Put `mProtect-1.2.0.jar` into the server's `plugins` directory.
3. Start the server once to create `plugins/mProtect/config.yml` and the language files.
4. Review the limits before opening the server to players.
5. Run `/mprotect status` and `/mprotect test items` from the console or as an administrator.

Use `/mprotect reload` after changing checks, limits, alerts, or messages. Changing storage settings requires a restart.

## Configuration guide

The generated `config.yml` is the source of truth. Missing options are restored automatically and invalid numeric values are replaced with safe defaults.

### Items and containers

`items.blocked-materials` contains materials players must not possess. Validation also covers overstacking, enchantments, attributes, durability, names, lore, custom potion effects, fireworks, serialized size, and nested containers. Conservative defaults avoid rejecting intentional unbreakable rewards unless `items.reject-unbreakable` is enabled.

`items.action` accepts:

- `REMOVE` — remove the unsafe item.
- `REPLACE` — replace it with `items.replacement`.
- `LOG` — keep it and record the violation.
- `KICK` — cancel the action and disconnect the player.

Most checks run when an inventory is actually touched. `items.fallback-scan-minutes` controls the low-frequency safety scan; it is not a per-tick scan.

### Books, signs, and anvils

The `books`, `signs`, and `anvils` sections set character, component, and repair-cost limits. Set `books.strip-formatting` or `anvils.strip-formatting` only if formatting should be removed. Sign click events can be stripped independently with `signs.strip-click-events`.

### Commands

Explicit blocked names are checked after removing a namespace, so `/minecraft:op` cannot bypass the `op` rule. Config version 2 no longer blocks every vanilla namespaced command by default. `blocked-namespaces` remains available when an entire plugin namespace must be prohibited.

### Entities and chunks

`entities.max-per-chunk` limits the total tracked entities in a chunk, while `entities.max-per-type-per-chunk` limits a single type. Existing chunks are counted as they load and counters are updated on spawn and removal.

`chunk-loads` limits how quickly a player may cross into new chunks. Increase the limit for servers where fast elytra travel is expected.

### Lag-machine protection

Every hot-path rule is a bounded O(1) window keyed by chunk, block, or player. No rule scans nearby entities or the whole world:

- `redstone` freezes additional current changes after the per-chunk threshold.
- `automation` limits piston movement, hopper transfers/pickups, and dispenser actions.
- `physics` limits block updates, fluids, spreading fire/sculk, and oversized multi-block growth.
- `explosions` limits TNT priming, explosion frequency, affected blocks, and drop yield.
- `spawners` applies both per-spawner and per-chunk spawn windows.
- `portals` bounds portal creation, player use, search radius, and creation radius.
- `activity` cancels excessive player-generated inventory, interaction, block, drop, and projectile events.

The defaults are intentionally generous for ordinary survival servers. Technical servers should tune one section at a time using `/mprotect status` and `/mprotect violations`. Administrative bypasses exist only for attributable player limits; machine limits have no bypass because most world events have no trustworthy owner.

### Alerts and storage

- `alerts.staff-chat` sends deduplicated alerts to players with `mprotect.alerts`.
- `alerts.file-log` writes `plugins/mProtect/violations.jsonl`.
- `alerts.discord` can send alerts to an HTTPS Discord webhook. Keep its URL private.
- `storage.retention-days` controls cleanup of the embedded H2 history.

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/mprotect status` | Show enabled checks and today's counts | `mprotect.command.status` |
| `/mprotect violations [player]` | Show the ten newest stored violations | `mprotect.command.violations` |
| `/mprotect test <check>` | Validate a configured check | `mprotect.command.test` |
| `/mprotect inspect` | Check the held item without modifying it | `mprotect.command.inspect` |
| `/mprotect scan` | Audit your inventory without modifying it | `mprotect.command.scan` |
| `/mprotect reload` | Reload safe settings and language files | `mprotect.command.reload` |

The alias `/mpr` is also available.

## Permissions

`mprotect.admin` grants all administrative commands, alerts, and player-attributable bypasses. Individual bypass permissions are available for `items`, `commands`, `books`, `signs`, `anvils`, `creative`, `chunks`, `portals`, and `activity`, using the form `mprotect.bypass.<check>`.

Entity spawn limits intentionally have no bypass permission because many spawn events do not have a reliable player initiator.

## Telemetry and updates

mProtect uses [bStats](https://bstats.org/plugin/bukkit/mProtect/33359) to collect anonymous usage statistics when `metrics.enabled` is `true`. Server owners can opt out in the global bStats configuration. The collected data and privacy details are documented in the [bStats server owner guide](https://bstats.org/docs/server-owners).

The update checker only requests public release metadata from Modrinth when `updates.enabled` is `true` and a project ID is configured. It never downloads or installs updates.

## Building

```bash
./gradlew clean build
```

The deployable artifact is `build/libs/mProtect-1.2.0.jar`. Automated tests cover bounded keyed rate windows, configured actions, semantic versions, and storage path containment.

## Support

Report reproducible problems through [GitHub Issues](https://github.com/miklires/mProtect/issues) or ask for help in [Discord](https://discord.gg/pes25cnWKy). Include the server software, Java version, mProtect version, relevant configuration, and the complete error from the log.

Licensed under the [MIT License](LICENSE).
