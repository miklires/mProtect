<div align="center">
  <h1>mProtect</h1>
  <p>Configurable exploit checks and server hardening for Paper, Purpur, and Folia.</p>
  <p>
    <a href="https://github.com/miklires/mProtect/actions/workflows/build.yml"><img alt="Build" src="https://img.shields.io/github/actions/workflow/status/miklires/mProtect/build.yml?branch=main&style=for-the-badge&label=build"></a>
    <a href="https://bstats.org/plugin/bukkit/mProtect/33359"><img alt="bStats" src="https://img.shields.io/bstats/servers/33359?style=for-the-badge&label=servers"></a>
    <img alt="Java 25" src="https://img.shields.io/badge/Java-25-f89820?style=for-the-badge&logo=openjdk&logoColor=white">
  </p>
  <p>
    <a href="https://github.com/miklires/mProtect"><img alt="GitHub" src="https://img.shields.io/badge/GitHub-source-181717?style=for-the-badge&logo=github"></a>
    <a href="https://github.com/miklires/mProtect/issues"><img alt="Issues" src="https://img.shields.io/badge/GitHub-issues-181717?style=for-the-badge&logo=github"></a>
    <a href="https://discord.gg/pes25cnWKy"><img alt="Discord" src="https://img.shields.io/badge/Discord-support-5865F2?style=for-the-badge&logo=discord&logoColor=white"></a>
  </p>
</div>

## What mProtect checks

- Illegal materials, enchantments, attribute modifiers, oversized components, and deeply nested containers.
- Book pages, titles, total content, serialized components, sign text and click events, and anvil names and costs.
- Command length, frequency, blocked commands, namespaces, and roots.
- Unauthorized creative or spectator mode.
- Excessive entities per chunk and per entity type using maintained counters instead of repeated nearby-entity scans.
- Excessive player movement into new chunks.

Every detection can be written to H2 and JSONL, shown to online staff, and optionally sent to a Discord webhook. English and Russian messages are included.

<p align="center">
  <img alt="Available for Folia" src="docs/assets/folia-available.png" width="420">
</p>

## Requirements

- Java 25
- Paper, Purpur, or Folia 26.2
- No required plugins or external database

Packet-level exploit filtering is not part of mProtect 1.0. Server software, proxies, and a properly configured firewall should still be kept current.

## Installation

1. Stop the server.
2. Put `mProtect-1.0.0.jar` into the server's `plugins` directory.
3. Start the server once to create `plugins/mProtect/config.yml` and the language files.
4. Review the limits before opening the server to players.
5. Run `/mprotect status` and `/mprotect test items` from the console or as an administrator.

Use `/mprotect reload` after changing checks, limits, alerts, or messages. Changing storage settings requires a restart.

## Configuration guide

The generated `config.yml` is the source of truth. Missing options are restored automatically and invalid numeric values are replaced with safe defaults.

### Items and containers

`items.blocked-materials` contains materials players must not possess. Item validation also rejects unsafe enchantments, duplicate or excessive attribute modifiers, oversized serialized data, and nested shulker boxes or bundles beyond the configured depth and item count.

`items.action` accepts:

- `REMOVE` — remove the unsafe item.
- `REPLACE` — replace it with `items.replacement`.
- `LOG` — keep it and record the violation.
- `KICK` — cancel the action and disconnect the player.

Most checks run when an inventory is actually touched. `items.fallback-scan-minutes` controls the low-frequency safety scan; it is not a per-tick scan.

### Books, signs, and anvils

The `books`, `signs`, and `anvils` sections set character, component, and repair-cost limits. Set `books.strip-formatting` or `anvils.strip-formatting` only if formatting should be removed. Sign click events can be stripped independently with `signs.strip-click-events`.

### Commands

`commands.blocked` matches command names. `commands.blocked-namespaces` blocks namespaced forms such as `minecraft:op`. `commands.blocked-roots` is intended for command roots whose arguments may execute another command. Length and rate limits are applied before command dispatch.

### Entities and chunks

`entities.max-per-chunk` limits the total tracked entities in a chunk, while `entities.max-per-type-per-chunk` limits a single type. Existing chunks are counted as they load and counters are updated on spawn and removal.

`chunk-loads` limits how quickly a player may cross into new chunks. Increase the limit for servers where fast elytra travel is expected.

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
| `/mprotect reload` | Reload safe settings and language files | `mprotect.command.reload` |

The alias `/mpr` is also available.

## Permissions

`mprotect.admin` grants all administrative commands, alerts, and player-attributable bypasses. Individual bypass permissions are available for `items`, `commands`, `books`, `signs`, `anvils`, `creative`, and `chunks`, using the form `mprotect.bypass.<check>`.

Entity spawn limits intentionally have no bypass permission because many spawn events do not have a reliable player initiator.

## Telemetry and updates

mProtect uses [bStats](https://bstats.org/plugin/bukkit/mProtect/33359) to collect anonymous usage statistics when `metrics.enabled` is `true`. Server owners can opt out in the global bStats configuration. The collected data and privacy details are documented in the [bStats server owner guide](https://bstats.org/docs/server-owners).

The update checker only requests public release metadata from Modrinth when `updates.enabled` is `true` and a project ID is configured. It never downloads or installs updates.

## Building

```bash
./gradlew clean build
```

The deployable artifact is `build/libs/mProtect-1.0.0.jar`. Automated tests cover rate windows, configured protection actions, and semantic version ordering.

## Support

Report reproducible problems through [GitHub Issues](https://github.com/miklires/mProtect/issues) or ask for help in [Discord](https://discord.gg/pes25cnWKy). Include the server software, Java version, mProtect version, relevant configuration, and the complete error from the log.

Licensed under the [MIT License](LICENSE).
