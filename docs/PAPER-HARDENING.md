# Paper hardening for packet and engine-level limits

mProtect starts at the server event layer. Keep Paper's native safeguards enabled for work that must happen earlier or deeper in the engine.

## Required review before opening a public server

In `config/paper-global.yml`, review:

- `packet-limiter.all-packets` and packet-specific `overrides`;
- `spam-limiter` for incoming packets, recipes, and command suggestions;
- `chunk-loading-basic` rates and `chunk-loading-advanced` concurrent load/generation limits;
- connection throttling in the normal server configuration.

In `config/paper-world-defaults.yml`, review per-world entity spawning/despawn ranges, hopper optimization, redstone implementation, collision limits, XP merging, and unsupported-settings safeguards. Override only the worlds that need different behavior.

Paper's documented defaults evolve with the server version. Copy settings from the documentation for the exact Paper build instead of pasting an old complete configuration:

- <https://docs.papermc.io/paper/reference/global-configuration/>
- <https://docs.papermc.io/paper/reference/world-configuration/>

Use the bundled spark profiler to identify the actual tick consumer before lowering mProtect thresholds. Packet floods, decoder faults, network compression attacks, plugin task leaks, database stalls, and JVM/host exhaustion cannot be reliably fixed by a Bukkit listener.
