<div align="center">
  <h1>mProtect</h1>
  <p>Configurable item, command, entity, and content hardening for Paper servers.</p>
</div>

## Development status

mProtect 1.0.0 is under active development. The current build targets Java 25 and Paper 26.2. Platform support and installation instructions will be published after the server test matrix is complete.

## Implemented checks

- Blocked materials, illegal enchantments and attributes, oversized item components, and nested containers.
- Book, sign, and anvil content limits.
- Command namespace, root, length, and rate limits.
- Creative and spectator permission enforcement.
- Per-chunk entity counters without nearby-entity scans.
- Player chunk-movement rate limits.
- H2 violation history, JSONL audit log, staff alerts, and optional Discord alerts.

## Build

```bash
./gradlew clean build
```

The server artifact is `build/libs/mProtect-1.0.0.jar`.

Licensed under the MIT License.
