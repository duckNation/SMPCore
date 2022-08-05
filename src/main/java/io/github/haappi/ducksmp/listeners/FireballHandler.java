package io.github.haappi.ducksmp.listeners;

import io.github.haappi.ducksmp.DuckSMP;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

import static io.github.haappi.ducksmp.utils.Utils.noItalics;

public class FireballHandler implements Listener {
    private final DuckSMP plugin;
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();

    public FireballHandler() {
        this.plugin = DuckSMP.getInstance();
        Bukkit.getPluginManager().registerEvents(this, plugin);

        Bukkit.addRecipe(fireballRecipe());
    }

    private ItemStack createFireball() {
        ItemStack fireball = new ItemStack(Material.FIRE_CHARGE, 16);
        ItemMeta fireballMeta = fireball.getItemMeta();
        fireballMeta.displayName(noItalics("Fireball", NamedTextColor.RED));
        fireballMeta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "fireball"), PersistentDataType.INTEGER, 1);
        fireball.setItemMeta(fireballMeta);
        return fireball;
    }

    private ShapelessRecipe fireballRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "fireball_recipe");

        ShapelessRecipe recipe = new ShapelessRecipe(key, createFireball());

        recipe.addIngredient(Material.SUNFLOWER); // im a bit nice okay
        recipe.addIngredient(Material.HONEY_BLOCK);
        recipe.addIngredient(Material.BEEF);

        recipe.addIngredient(Material.WARPED_TRAPDOOR);
        recipe.addIngredient(Material.FIRE_CHARGE);
        recipe.addIngredient(Material.EGG);

        recipe.addIngredient(Material.GOLDEN_LEGGINGS);
        recipe.addIngredient(Material.GHAST_TEAR);
        recipe.addIngredient(Material.NOTE_BLOCK);

        return recipe;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (cooldowns.containsKey(event.getPlayer().getUniqueId())) {
            if (cooldowns.get(event.getPlayer().getUniqueId()) > System.currentTimeMillis()) {
                return;
            }
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {

        } else {
            return;
        }
        if (event.getItem() == null) {
            return;
        }
        ItemStack item = event.getItem();
        if (item.getItemMeta().getPersistentDataContainer().getOrDefault(new org.bukkit.NamespacedKey(plugin, "fireball"), PersistentDataType.INTEGER, 0) == 0) {
            return;
        }
        Player player = event.getPlayer();
        player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);

        Fireball fireball = player.getWorld().spawn(player.getEyeLocation(), Fireball.class);
        fireball.setYield(2);

        new BukkitRunnable() {
            public void run() {
                if (!fireball.isDead()) {
                    fireball.getWorld().spawnParticle(Particle.TOTEM, fireball.getLocation(), 5);
                } else {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);

        fireball.setVelocity(player.getEyeLocation().getDirection().multiply(1.5));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 40, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 1));

        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + 400);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (event.getDamager() instanceof Fireball) {
            event.setDamage(event.getDamage() + (int) (Math.random() * 4));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onExplosion(EntityExplodeEvent event) {
        if (event.isCancelled()) {
            return;
        }
        event.blockList().forEach(this::bounceBlock);
    }

    private void bounceBlock(Block block) {
        Entity fb;
        if (block == null) {
            return;
        }
        if (block.getType() == Material.TNT) {
            fb = block.getWorld().spawn(block.getLocation().add(0, 1, 0), TNTPrimed.class);
        } else {
            fb = block.getWorld()
                    .spawnFallingBlock(block.getLocation().add(0, 1, 0), block.getType().createBlockData());
            ((FallingBlock) fb).setDropItem(false);
        }

        block.setType(Material.AIR);

        float x = (float) 0.3 + (float) (Math.random() * ((0.6 + 0.6) + 1));
        float y = (float) 0.5;  //(float) -5 + (float)(Math.random() * ((5 - -5) + 1));
        float z = (float) -0.3 + (float) (Math.random() * ((0.6 + 0.6) + 1));
        fb.setVelocity(new Vector(x, y, z));
    }
}