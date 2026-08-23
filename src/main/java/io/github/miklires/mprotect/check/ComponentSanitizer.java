package io.github.miklires.mprotect.check;

import net.kyori.adventure.text.Component;

import java.util.List;

public final class ComponentSanitizer {
    private ComponentSanitizer() {}

    public static boolean hasClickEvent(Component component) {
        if (component.clickEvent() != null) return true;
        return component.children().stream().anyMatch(ComponentSanitizer::hasClickEvent);
    }

    public static Component removeClickEvents(Component component) {
        List<Component> children = component.children().stream().map(ComponentSanitizer::removeClickEvents).toList();
        return component.clickEvent(null).children(children);
    }
}
