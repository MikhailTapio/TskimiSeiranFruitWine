package club.someoneice.vine.common.item.group;

import com.google.common.collect.ImmutableSet;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.function.Supplier;

public enum CreativeModeTabDef {
    TAB,
    WINE_TAB,
    COCKTAIL_TAB;

    private static Map<CreativeModeTabDef, Set<Supplier<ItemStack>>> itemsInTab = new HashMap<>();

    public void addIntoTab(Supplier<ItemStack> stack) {
        if (!itemsInTab.containsKey(this)) itemsInTab.put(this, new HashSet<>());
        itemsInTab.get(this).add(stack);
    }

    public static List<ItemStack> getItemsInTab(CreativeModeTabDef def) {
        return ImmutableSet.copyOf(Objects.requireNonNullElseGet(itemsInTab.get(def), ImmutableSet::of)).stream().map(Supplier::get).toList();
    }
}
