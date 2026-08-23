package io.github.miklires.mprotect.check;

import io.github.miklires.mprotect.MProtectPlugin;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Container;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ItemValidator {
    private final MProtectPlugin plugin;
    private final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
    private final GsonComponentSerializer gson = GsonComponentSerializer.gson();

    public ItemValidator(MProtectPlugin plugin) {
        this.plugin = plugin;
    }

    public ValidationResult validate(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return ValidationResult.pass();
        if (plugin.config().blockedMaterials().contains(stack.getType())) return ValidationResult.fail("blocked material " + stack.getType());
        if (plugin.config().bool("items.reject-overstacked", true) && stack.getAmount() > stack.getMaxStackSize()) {
            return ValidationResult.fail("stack amount " + stack.getAmount() + " > " + stack.getMaxStackSize());
        }
        ValidationResult enchantments = enchantments(stack);
        if (!enchantments.safe()) return enchantments;
        ValidationResult attributes = attributes(stack);
        if (!attributes.safe()) return attributes;
        ValidationResult book = book(stack);
        if (!book.safe()) return book;
        if (plugin.config().bool("items.components.enabled", true)) {
            int maxBytes = plugin.config().integer("items.components.max-serialized-bytes", 65_536);
            try {
                int bytes = stack.serializeAsBytes().length;
                if (bytes > maxBytes) return ValidationResult.fail("serialized item size " + bytes + " > " + maxBytes);
            } catch (RuntimeException exception) {
                return ValidationResult.fail("item components could not be serialized");
            }
            return containers(stack, 0, new Counter());
        }
        return ValidationResult.pass();
    }

    private ValidationResult enchantments(ItemStack stack) {
        if (!plugin.config().bool("items.enchantments.enabled", true)) return ValidationResult.pass();
        boolean rejectIncompatible = plugin.config().bool("items.enchantments.reject-incompatible", true);
        for (Map.Entry<Enchantment, Integer> entry : stack.getEnchantments().entrySet()) {
            int level = entry.getValue();
            if (level < 1 || level > entry.getKey().getMaxLevel()) {
                return ValidationResult.fail("illegal enchantment " + entry.getKey().getKey().getKey() + " level " + level);
            }
            if (rejectIncompatible && !entry.getKey().canEnchantItem(stack)) {
                return ValidationResult.fail("incompatible enchantment " + entry.getKey().getKey().getKey());
            }
        }
        return ValidationResult.pass();
    }

    private ValidationResult attributes(ItemStack stack) {
        if (!plugin.config().bool("items.attributes.enabled", true) || !stack.hasItemMeta()) return ValidationResult.pass();
        ItemMeta meta = stack.getItemMeta();
        if (!meta.hasAttributeModifiers()) return ValidationResult.pass();
        double max = plugin.config().decimal("items.attributes.max-absolute-amount", 2048.0);
        boolean duplicates = plugin.config().bool("items.attributes.reject-duplicate-uuids", true);
        Set<String> keys = new HashSet<>();
        var modifiers = meta.getAttributeModifiers();
        if (modifiers == null) return ValidationResult.pass();
        for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entries()) {
            AttributeModifier modifier = entry.getValue();
            if (!Double.isFinite(modifier.getAmount()) || Math.abs(modifier.getAmount()) > max) {
                return ValidationResult.fail("attribute amount exceeds limit for " + entry.getKey().getKey().getKey());
            }
            String key = modifier.getKey().asString();
            if (duplicates && !keys.add(key)) return ValidationResult.fail("duplicate attribute modifier " + key);
        }
        return ValidationResult.pass();
    }

    private ValidationResult book(ItemStack stack) {
        if (!(stack.getItemMeta() instanceof BookMeta meta)) return ValidationResult.pass();
        int maxPages = plugin.config().integer("books.max-pages", 50);
        int maxPage = plugin.config().integer("books.max-page-characters", 1024);
        int maxTotal = plugin.config().integer("books.max-total-characters", 20_000);
        int maxJson = plugin.config().integer("books.max-component-json-characters", 32_768);
        if (meta.pages().size() > maxPages) return ValidationResult.fail("book page count " + meta.pages().size() + " > " + maxPages);
        int total = 0;
        for (var page : meta.pages()) {
            int length = plain.serialize(page).length();
            total += length;
            if (length > maxPage) return ValidationResult.fail("book page length " + length + " > " + maxPage);
            if (gson.serialize(page).length() > maxJson) return ValidationResult.fail("book component JSON exceeds limit");
        }
        if (total > maxTotal) return ValidationResult.fail("book total length " + total + " > " + maxTotal);
        String title = meta.getTitle();
        int maxTitle = plugin.config().integer("books.max-title-characters", 32);
        if (title != null && title.length() > maxTitle) return ValidationResult.fail("book title exceeds limit");
        return ValidationResult.pass();
    }

    private ValidationResult containers(ItemStack stack, int depth, Counter counter) {
        int maxDepth = plugin.config().integer("items.components.max-container-depth", 2);
        int maxItems = plugin.config().integer("items.components.max-container-items", 1728);
        if (!stack.hasItemMeta()) return ValidationResult.pass();
        Iterable<ItemStack> contents = null;
        if (stack.getItemMeta() instanceof BlockStateMeta blockStateMeta && blockStateMeta.getBlockState() instanceof Container container) {
            contents = Arrays.asList(container.getInventory().getContents());
        } else if (stack.getItemMeta() instanceof BundleMeta bundleMeta) {
            contents = bundleMeta.getItems();
        }
        if (contents == null) return ValidationResult.pass();
        if (depth >= maxDepth) return ValidationResult.fail("nested container depth exceeds " + maxDepth);
        for (ItemStack child : contents) {
            if (child == null || child.getType().isAir()) continue;
            counter.value += child.getAmount();
            if (counter.value > maxItems) return ValidationResult.fail("container item count exceeds " + maxItems);
            ValidationResult nested = validateNested(child, depth + 1, counter);
            if (!nested.safe()) return nested;
        }
        return ValidationResult.pass();
    }

    private ValidationResult validateNested(ItemStack stack, int depth, Counter counter) {
        if (plugin.config().blockedMaterials().contains(stack.getType())) return ValidationResult.fail("blocked material inside container " + stack.getType());
        if (plugin.config().bool("items.reject-overstacked", true) && stack.getAmount() > stack.getMaxStackSize()) {
            return ValidationResult.fail("overstacked item inside container " + stack.getType());
        }
        ValidationResult enchantments = enchantments(stack);
        if (!enchantments.safe()) return enchantments;
        ValidationResult attributes = attributes(stack);
        if (!attributes.safe()) return attributes;
        ValidationResult book = book(stack);
        if (!book.safe()) return book;
        int maxBytes = plugin.config().integer("items.components.max-serialized-bytes", 65_536);
        try {
            if (stack.serializeAsBytes().length > maxBytes) return ValidationResult.fail("nested item size exceeds " + maxBytes);
        } catch (RuntimeException exception) {
            return ValidationResult.fail("nested item components could not be serialized");
        }
        return containers(stack, depth, counter);
    }

    private static final class Counter { private int value; }
}
