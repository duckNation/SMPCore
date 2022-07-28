package io.github.haappi.ducksmp;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.CreateCollectionOptions;
import io.github.haappi.ducksmp.Commands.*;
import io.github.haappi.ducksmp.LifeSteal.ArmorStandPlayer;
import io.github.haappi.ducksmp.LifeSteal.Listeners;
import io.github.haappi.ducksmp.LifeSteal.signup;
import io.github.haappi.ducksmp.internals.Messages;
import io.github.haappi.ducksmp.listeners.*;
import io.github.haappi.ducksmp.utils.CustomHolder;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.world.item.ArmorItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Objects;

import static io.github.haappi.ducksmp.utils.Utils.*;

public final class DuckSMP extends JavaPlugin implements Listener {

    public static ArrayList<Integer> taskIds = new ArrayList<>();
    private static DuckSMP instance;
    private MongoClient mongoClient;
    private boolean hasListenerLoaded = false;

    public static DuckSMP getInstance() {
        return instance;
    }

    public static MongoClient getMongoClient() {
        return instance.mongoClient;
    }

    @Override
    public void onEnable() {
        if (!checkMongoConfig()) {
            Bukkit.getPluginManager().disablePlugin(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin(this.getName())));
            return;
        }

        this.getLogger().info(ChatColor.GREEN + "Connected to MongoDB.");
        instance = this;

        new Messages();
        new Villager();
//        new totem(); // enable in like 2 days
        new crystal();
        new stats();
        new netheirte();

        new ArmorStandPlayer();
        new enchantLore();

        Bukkit.getPluginManager().registerEvents(this, this);

        registerNewCommand(new signup("signup"));
        registerNewCommand(new stoprestart("stoprestart"));
        registerNewCommand(new nv("nv"));
        registerNewCommand(new vanish("v"));
        unRegisterBukkitCommand("tell");
        registerNewCommand(new tell("tell"));
        registerNewCommand(new reply("reply"));

    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof CustomHolder) {
            if (event.getCurrentItem() == null) {
                return;
            }
            event.setCancelled(true);

            switch (event.getCurrentItem().getType()) {
                case GREEN_TERRACOTTA: // no
                    event.getWhoClicked().sendMessage(Component.text("Alright, you didn't join LifeSteal. Guess you live for another day", NamedTextColor.RED));
                    break;
                case RED_TERRACOTTA: // yes
                    event.getWhoClicked().getPersistentDataContainer().set(new NamespacedKey(DuckSMP.getInstance(), "claimed_hearts"), PersistentDataType.INTEGER, 0);
                    event.getWhoClicked().sendMessage(Component.text("You have joined LifeSteal! Now make sure you don't drop to zero hearts.", NamedTextColor.GREEN));
                    Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(Component.text("" + event.getWhoClicked().getName() + " has joined LifeSteal!", NamedTextColor.GREEN)));
                    break;
                default:
                    return;
            }
            Bukkit.getScheduler().runTask(DuckSMP.getInstance(), () -> event.getWhoClicked().closeInventory());
        }

    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (!hasListenerLoaded) {
            new Listeners();
            hasListenerLoaded = true;
        }
    }

    @EventHandler
    public void onCommandRun(PlayerCommandPreprocessEvent event) {
        if (event.getMessage().contains("rg define")) {
            String claimName = event.getMessage().split(" ")[2];
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), String.format("/rg flag -w %s %s pvp -g everyone allow", event.getPlayer().getLocation().getWorld().getName(), claimName));
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), String.format("/rg flag -w %s %s use -g everyone allow", event.getPlayer().getLocation().getWorld().getName(), claimName));
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (!(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        if (event.getItem() == null) {
            return;
        }
        if (event.getItem().getType().toString().toLowerCase().contains("elytra")) {
            org.bukkit.inventory.ItemStack item = event.getPlayer().getInventory().getChestplate();
            event.getPlayer().getInventory().setChestplate(event.getItem());
            if (event.getHand() == EquipmentSlot.HAND) {
                event.getPlayer().getInventory().setItemInMainHand(item);
            } else {
                event.getPlayer().getInventory().setItemInOffHand(item);
            }
            return;
        }
        if (!(CraftItemStack.asNMSCopy(event.getItem()).getItem() instanceof ArmorItem)) {
            return;
        }
        if (event.getItem().getType().toString().toLowerCase().contains("helmet")) {
            org.bukkit.inventory.ItemStack item = event.getPlayer().getInventory().getHelmet();
            event.getPlayer().getInventory().setHelmet(event.getItem());
            if (event.getHand() == EquipmentSlot.HAND) {
                event.getPlayer().getInventory().setItemInMainHand(item);
            } else {
                event.getPlayer().getInventory().setItemInOffHand(item);
            }
        } else if (event.getItem().getType().toString().toLowerCase().contains("boots")) {
            org.bukkit.inventory.ItemStack item = event.getPlayer().getInventory().getBoots();
            event.getPlayer().getInventory().setBoots(event.getItem());
            if (event.getHand() == EquipmentSlot.HAND) {
                event.getPlayer().getInventory().setItemInMainHand(item);
            } else {
                event.getPlayer().getInventory().setItemInOffHand(item);
            }
        } else if (event.getItem().getType().toString().toLowerCase().contains("leggings")) {
            org.bukkit.inventory.ItemStack item = event.getPlayer().getInventory().getLeggings();
            event.getPlayer().getInventory().setLeggings(event.getItem());
            if (event.getHand() == EquipmentSlot.HAND) {
                event.getPlayer().getInventory().setItemInMainHand(item);
            } else {
                event.getPlayer().getInventory().setItemInOffHand(item);
            }
        } else if (event.getItem().getType().toString().toLowerCase().contains("chestplate")) {
            org.bukkit.inventory.ItemStack item = event.getPlayer().getInventory().getChestplate();
            event.getPlayer().getInventory().setChestplate(event.getItem());
            if (event.getHand() == EquipmentSlot.HAND) {
                event.getPlayer().getInventory().setItemInMainHand(item);
            } else {
                event.getPlayer().getInventory().setItemInOffHand(item);
            }
        }
    }

    @Override
    public void onDisable() {
        for (int taskId : taskIds) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        this.mongoClient.getDatabase("duckMinecraft").getCollection("messages").drop();
        this.mongoClient.getDatabase("duckMinecraft").createCollection("messages", new CreateCollectionOptions().capped(true).sizeInBytes(1024 * 1024 * 10)); // 10 MB
    }

    private boolean checkMongoConfig() {
        FileConfiguration config = this.getConfig();
        if ((config.getString("mongo-uri") == null) ||
                (Objects.requireNonNull(config.getString("mongo-uri")).equalsIgnoreCase("your-mongo-uri")) ||
                (!Objects.requireNonNull(config.getString("mongo-uri")).startsWith("mongodb"))) {

            config.addDefault("mongo-uri", "your-mongo-uri");
            config.addDefault("secretKey", "this-is-not-secure-until-you-set-it");
            config.options().copyDefaults(true);
            this.saveConfig();
            this.getLogger().severe("A proper Mongo URI is required to run this plugin.");
            return false;
        }
        try {
            mongoClient = MongoClients.create(Objects.requireNonNull(config.getString("mongo-uri")));
        } catch (Exception e) {
            this.getLogger().severe("Could not connect to MongoDB. Please check your Mongo URI.");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (player.isOp()) {
            event.quitMessage(Component.text()
                    .append(
                            Component.text("-", NamedTextColor.DARK_RED)
                                    .append(Component.text(" " + player.getName(), NamedTextColor.GREEN))).build()
            );
        } else {
            event.quitMessage(Component.text()
                    .append(
                            Component.text("-", NamedTextColor.DARK_RED)
                                    .append(Component.text(" " + player.getName(), NamedTextColor.YELLOW))).build()
            );
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (event.getPlayer().isOp()) {
            event.renderer((source, sourceDisplayName, message, viewer) -> Component.text()
                    .append(
                            Component.text("<", NamedTextColor.GREEN),
                            sourceDisplayName.color(NamedTextColor.GREEN),
                            Component.text("> ", NamedTextColor.GREEN),
                            Component.text()
                                    .color(NamedTextColor.WHITE)
                                    .append(message)
                                    .build()
                    )
                    .build());
        } else {
            event.renderer((source, sourceDisplayName, message, viewer) -> Component.text()
                    .append(
                            Component.text("<", NamedTextColor.YELLOW),
                            sourceDisplayName.color(NamedTextColor.YELLOW),
                            Component.text("> ", NamedTextColor.YELLOW),
                            Component.text()
                                    .color(NamedTextColor.WHITE)
                                    .append(message)
                                    .build()
                    )
                    .build());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) {
            event.joinMessage(Component.text()
                    .append(
                            Component.text("+", NamedTextColor.DARK_GREEN)
                                    .append(Component.text(" " + player.getName(), NamedTextColor.GREEN))).build()
            );
        } else {
            event.joinMessage(Component.text()
                    .append(
                            Component.text("+", NamedTextColor.DARK_GREEN)
                                    .append(Component.text(" " + player.getName(), NamedTextColor.YELLOW))).build()
            );
        }
    }

    @EventHandler
    public void onPing(PaperServerListPingEvent event) {
        final Component component = miniMessage.deserialize("<bold><gold>Duck</gold><yellow>Nation</yellow><green> SMP</green></bold>");
        event.motd(component);
    }

}
