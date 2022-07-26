package io.github.haappi.ducksmp.Listeners;

import io.github.haappi.ducksmp.DuckSMP;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.world.item.ArmorItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.persistence.PersistentDataType;

public class Netherite implements Listener {

    private final DuckSMP plugin;

    public Netherite() {

        this.plugin = DuckSMP.getInstance();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onFuelUse(FurnaceBurnEvent event) {
        if (event.getFuel().getType() == Material.WOODEN_AXE) {
            event.setBurnTime(0);
        }
    }

    @EventHandler
    public void onWoodenDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (player.getActiveItem().getType() == Material.WOODEN_AXE) {
                event.setCancelled(true);
                player.sendMessage(Component.text("No!", NamedTextColor.RED));
            }
        }
    }


    //    @EventHandler
    public void craftItem(SmithItemEvent event) {
        SmithingInventory inv = event.getInventory();
        for (ItemStack stack : inv.getStorageContents()) {
            if (stack == null) {
                continue;
            }
            if (CraftItemStack.asNMSCopy(stack).getItem() instanceof ArmorItem) {
                inv.setResult(null);
                event.setCancelled(true);
                return;
            }
        }
        if (inv.getResult() == null) {
            return;
        }
        if (inv.getResult().getType() == Material.NETHERITE_AXE || inv.getResult().getType() == Material.NETHERITE_SWORD) {
            inv.getResult().getItemMeta().getPersistentDataContainer().set(new NamespacedKey(plugin, "no_pvp_tool"), PersistentDataType.BYTE, (byte) 1);
        }
    }


    //    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (event.getEntity() instanceof Player) {
                if (player.getInventory().getItemInMainHand().getItemMeta() == null) {
                    return;
                }
                if (player.getInventory().getItemInMainHand().getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "np_pvp_tool"))) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("You can't damage players with this tool.", NamedTextColor.RED));
                }
            }
        }
    }
}
