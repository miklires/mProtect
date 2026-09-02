package io.github.miklires.mprotect;

import io.github.miklires.mprotect.command.ProtectCommand;
import io.github.miklires.mprotect.config.ConfigManager;
import io.github.miklires.mprotect.listener.AnvilProtectionListener;
import io.github.miklires.mprotect.listener.BookProtectionListener;
import io.github.miklires.mprotect.listener.ChunkLoadProtectionListener;
import io.github.miklires.mprotect.listener.CommandProtectionListener;
import io.github.miklires.mprotect.listener.CreativeProtectionListener;
import io.github.miklires.mprotect.listener.EntityLimitListener;
import io.github.miklires.mprotect.listener.ExplosionProtectionListener;
import io.github.miklires.mprotect.listener.ItemProtectionListener;
import io.github.miklires.mprotect.listener.PhysicsProtectionListener;
import io.github.miklires.mprotect.listener.PlayerActivityProtectionListener;
import io.github.miklires.mprotect.listener.RedstoneAutomationListener;
import io.github.miklires.mprotect.listener.SignProtectionListener;
import io.github.miklires.mprotect.listener.SpawnerPortalProtectionListener;
import io.github.miklires.mprotect.message.Messages;
import io.github.miklires.mprotect.service.ViolationService;
import io.github.miklires.mprotect.storage.ViolationStore;
import io.github.miklires.mprotect.update.UpdateChecker;
import io.github.miklires.mprotect.util.PluginScheduler;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bstats.bukkit.Metrics;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

public final class MProtectPlugin extends JavaPlugin {
    private ConfigManager config;
    private Messages messages;
    private PluginScheduler scheduler;
    private ViolationStore store;
    private ViolationService violations;

    @Override
    public void onEnable() {
        scheduler = new PluginScheduler(this);
        config = new ConfigManager(this);
        config.load();
        messages = new Messages(this);
        messages.load();
        store = new ViolationStore(this);
        violations = new ViolationService(this, store);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
                event -> event.registrar().register("mprotect", List.of("mpr"), new ProtectCommand(this)));
        store.start().whenComplete((ignored, exception) -> scheduler.global(() -> finishEnable(exception)));

        if (config.bool("metrics.enabled", true)) {
            int id = config.integer("metrics.bstats-id", 33359);
            if (id > 0) new Metrics(this, id);
        }
        new UpdateChecker(this).start();
        getLogger().info("mProtect " + getPluginMeta().getVersion() + " is starting");
    }

    private void finishEnable(Throwable exception) {
        if (!isEnabled()) return;
        if (exception != null) {
            Throwable failure = exception instanceof java.util.concurrent.CompletionException && exception.getCause() != null
                    ? exception.getCause() : exception;
            getLogger().log(Level.SEVERE, "Could not initialize violation storage", failure);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        ItemProtectionListener items = new ItemProtectionListener(this);
        EntityLimitListener entities = new EntityLimitListener(this);
        List<Listener> listeners = List.of(items, new BookProtectionListener(this), new SignProtectionListener(this),
                new AnvilProtectionListener(this), new CommandProtectionListener(this), new CreativeProtectionListener(this),
                entities, new ChunkLoadProtectionListener(this), new RedstoneAutomationListener(this),
                new PhysicsProtectionListener(this), new ExplosionProtectionListener(this),
                new SpawnerPortalProtectionListener(this), new PlayerActivityProtectionListener(this));
        listeners.forEach(listener -> getServer().getPluginManager().registerEvents(listener, this));
        items.startFallbackScan();
        entities.initializeLoadedChunks();

        getLogger().info("mProtect " + getPluginMeta().getVersion() + " enabled");
    }

    @Override
    public void onDisable() {
        if (store != null) store.close();
        getLogger().info("mProtect disabled");
    }

    public void reloadSafeSettings() {
        config.load();
        messages.load();
    }

    public ConfigManager config() { return config; }
    public Messages messages() { return messages; }
    public PluginScheduler scheduler() { return scheduler; }
    public ViolationStore store() { return store; }
    public ViolationService violations() { return violations; }
}
