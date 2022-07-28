package io.github.haappi.ducksmp.listeners;

import io.github.haappi.ducksmp.DuckSMP;
import net.minecraft.world.item.ArmorItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;

public class netheirte implements Listener {

    private final DuckSMP plugin;
    public netheirte() {

        this.plugin = DuckSMP.getInstance();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }


    @EventHandler
    public void craftItem(SmithItemEvent event) {
        SmithingInventory inv = event.getInventory();
        for (ItemStack stack : inv.getStorageContents()) {
            if (stack == null) {
                continue;
            }
            if (CraftItemStack.asNMSCopy(stack).getItem() instanceof ArmorItem) {
                inv.setResult(null);
                event.setCancelled(true);
            }
        }
    }
}