package io.github.haappi.ducksmp.Utils;

import io.github.haappi.ducksmp.DuckSMP;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.util.Tuple;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.v1_19_R1.CraftServer;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.haappi.ducksmp.Cosmetics.NameTag.Common.packetsToSend;
import static io.github.haappi.ducksmp.Internals.Messages.doCountdown;
import static io.github.haappi.ducksmp.Listeners.Combat.canUseCommand;

public class Utils {

    public static final MiniMessage miniMessage = MiniMessage.miniMessage();
    public static final Random random = new Random();

    public Utils() {
        throw new UnsupportedOperationException("This class is not meant to be instantiated");
    }

    public static Component noItalics(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }

    public static Component noItalics(String content, NamedTextColor color) {
        return noItalics(Component.text(content, color));
    }

    public static Component noItalics(String content) {
        return noItalics(Component.text(content));
    }

    public static Component chain(Component... components) {
        Component thing = Component.empty();
        for (Component component : components) {
            thing = thing.append(component);
        }
        return thing;
    }

    public static String sFormat(Integer number) {
        if (number == 1) {
            return "";
        } else {
            return "s";
        }
    }

    public static TextComponent getCountdown(Integer countdown) {
        NamedTextColor color = switch (countdown) {
            case 0, 1, 2, 3 -> NamedTextColor.RED;
            case 4, 5, 6 -> NamedTextColor.YELLOW;
            default -> NamedTextColor.GREEN;
        };

        return Component.text(countdown + " second" + sFormat(countdown), color);
    }

    public static Boolean registerNewCommand(Command command) {
        return ((CraftServer) Bukkit.getServer()).getCommandMap().register("duck", command);
    }

    @SuppressWarnings("SameParameterValue")
    private static Object getPrivateField(Object object, String field) throws SecurityException,
            NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Class<?> clazz = object.getClass();
        Field objectField = clazz.getDeclaredField(field);
        objectField.setAccessible(true);
        Object result = objectField.get(object);
        objectField.setAccessible(false);
        return result;
    }

    public static void unRegisterBukkitCommand(org.bukkit.command.Command command) {
        try {
            Object result = getPrivateField(Bukkit.getServer(), "commandMap");
            SimpleCommandMap commandMap = (SimpleCommandMap) result;
            HashMap<String, Command> thing = (HashMap<String, org.bukkit.command.Command>) commandMap.getKnownCommands();
            thing.remove(command.getName());
            for (String alias : command.getAliases()) {
                if (thing.containsKey(alias) && thing.get(alias).toString().contains(Bukkit.getName())) {
                    thing.remove(alias);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void unRegisterBukkitCommand(String command) {
        org.bukkit.command.Command cmd = (((CraftServer) Bukkit.getServer()).getCommandMap()).getCommand(command);
        if (cmd != null) {
            unRegisterBukkitCommand(cmd);
        }
    }

    public static String formatLocation(Location location) {
        return String.format("%s,%s,%s,%s", location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static Location getLocation(String location) {
        String[] split = location.split(",");
        return new Location(Bukkit.getWorld(UUID.fromString(split[0])), Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3]));
    }

    public static Component formattedLocation(Location location) {
        return Component.text(location.getWorld().getName(), NamedTextColor.GOLD).append(Component.text(" ", NamedTextColor.GOLD))
                .append(Component.text(location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ(), NamedTextColor.GOLD));
    }

    public static void loadChunksAsync(Location starting, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                starting.getWorld().getChunkAtAsync(starting.getBlockX() + (x * 16), starting.getBlockZ() + (z * 16));
            }
        }
    }

    public static @NotNull ItemStack getHeart(int count, Player owner) {
        ItemStack thing = new ItemStack(Material.NETHER_STAR, count);
        ItemMeta meta = thing.getItemMeta();
        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(DuckSMP.getInstance(), "life_steal"), PersistentDataType.STRING, "true");
        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(DuckSMP.getInstance(), "owner"), PersistentDataType.STRING, owner.getUniqueId().toString());

        List<Component> lore = Arrays.asList(
                noItalics(""),
                chain(noItalics("Click to claim a heart "), noItalics("❤", NamedTextColor.RED)),
//                noItalics(""),
                chain(noItalics(owner.getName(), NamedTextColor.YELLOW), noItalics("'s heart", NamedTextColor.GRAY))
        );

        meta.lore(lore);
        meta.displayName(miniMessage.deserialize("<rainbow>Life Steal Heart</rainbow>").decoration(TextDecoration.ITALIC, false));
        thing.setItemMeta(meta);

        return thing;
    }

    public static boolean isLifeStealItem(@Nullable ItemStack item) {
        if (item == null) {
            return false;
        }
        if (item.getItemMeta() == null) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(DuckSMP.getInstance(), "life_steal"), PersistentDataType.STRING);
    }

    public static ArmorStand createStand(Entity owner, int count) {
        ArmorStand as = getStand(owner.getWorld(), owner.getLocation());
        as.customName(Component.text(count + "x", NamedTextColor.YELLOW).append(Component.text(" Heart ❤", NamedTextColor.RED)));
        return as;
    }

    public static ArmorStand createStand(Player owner) {
        return getStand(owner.getWorld(), owner.getLocation());
    }

    private static ArmorStand getStand(World world, Location location) {
        ArmorStand as = world.spawn(location, ArmorStand.class);
        as.setInvisible(true);
        as.setInvulnerable(true);
        as.setMarker(true);
        as.setCustomNameVisible(true);
        as.setGravity(false);
        as.setPersistent(false);

        return as;
    }

    public static void sendTeamPackets() {
        AtomicInteger current = new AtomicInteger(1);
        for (Player p : Bukkit.getOnlinePlayers()) {
            Bukkit.getScheduler().runTaskLater(DuckSMP.getInstance(), () -> {
                for (Map.Entry<UUID, Tuple<ClientboundSetPlayerTeamPacket, ClientboundSetPlayerTeamPacket>> entry : packetsToSend.entrySet()) {
                    ((CraftPlayer) p).getHandle().connection.send(entry.getValue().getA());
                    ((CraftPlayer) p).getHandle().connection.send(entry.getValue().getB());
                }
            }, 2L * current.getAndIncrement());
        }
    }

    public static boolean canRunAway(Player player) {
        if (!canUseCommand(player)) {
            player.sendMessage(noItalics("You can't do this in combat!", NamedTextColor.RED));
            return false;
        }
        if (player.isInLava() || player.isInPowderedSnow() || player.isInWaterOrBubbleColumn()) {
            player.sendMessage(noItalics("Hmmmm, it looks like you're in a ...liquid of some sort... I can't allow you to use this command right now.", NamedTextColor.RED));
            return false;
        }
        if (player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR) {
            player.sendMessage(noItalics("Hmmmm, it looks like you're in the air. I can't allow you to use this command right now.", NamedTextColor.RED));
            return false;
        }
        if (player.getNearbyEntities(5, 5, 5).stream().anyMatch(entity -> entity instanceof Monster)) {
            player.sendMessage(noItalics("Hmmmm, it looks like there are mobs near you. I can't allow you to use this command right now.", NamedTextColor.RED));
            return false;
        }
        if (player.isInsideVehicle()) {
            player.sendMessage(noItalics("Hmmmm, it looks like you're in a vehicle.. I can't allow you to use this command right now.", NamedTextColor.RED));
            return false;
        }
        return true;
    }

    public static void restartWarning(int time, boolean minutes) {
        String appended;
        if (minutes) {
            appended = " minutes. ";
        } else {
            appended = " seconds. ";
        }
        if ((time == 5) && minutes) {
            DuckSMP.showRestartBar = true;
            DuckSMP.restartBar = BossBar.bossBar(chain(Component.text("Server restarts in ", NamedTextColor.GREEN), Component.text("5:00")), 1f, BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10);
        }
        Component message = chain(Component.text("[!] ", NamedTextColor.YELLOW), Component.text("Server will be restarting in ", NamedTextColor.GREEN), Component.text(time + appended, NamedTextColor.AQUA).append(Component.text("[!]", NamedTextColor.YELLOW)));
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (DuckSMP.showRestartBar) {
                player.showBossBar(DuckSMP.restartBar);
            }
            player.sendMessage(message);
        }

        if (DuckSMP.showRestartBar) {
            stuff(300);
        }

        if (time == 60 && minutes) {
            Bukkit.getScheduler().runTaskLater(DuckSMP.getInstance(), () -> restartWarning(30, true), 20 * 60 * 30); // 30 minutes
        } else if (time == 30 && minutes) {
            Bukkit.getScheduler().runTaskLater(DuckSMP.getInstance(), () -> restartWarning(10, true), 20 * 60 * 20); // Runs after 20 minutes
        } else if (time == 10 && minutes) {
            Bukkit.getScheduler().runTaskLater(DuckSMP.getInstance(), () -> restartWarning(5, true), 20 * 60 * 5); // 5 minutes
        }
    }

    private static void stuff(int time) {
        AtomicInteger countdown = new AtomicInteger(time);
        new BukkitRunnable() {
            @Override
            public void run() {
                int time = countdown.getAndDecrement();
                updateBossBar(time, DuckSMP.restartBar);
                if (time <= 10) {
                    doCountdown("Server will restart in ", DuckSMP.getInstance(), time);
                    cancel();
                }
            }
        }.runTaskTimer(DuckSMP.getInstance(), 0, 20);
    }

    public static int updateBossBar(int seconds, BossBar bossBar) {
        NamedTextColor color;
        if (seconds > 240) { // 3 minutes & 20 seconds
            color = NamedTextColor.GREEN;
        } else if (seconds > 150) { //
            color = NamedTextColor.AQUA;
        } else if (seconds > 80) {
            color = NamedTextColor.YELLOW;
        } else if (seconds > 20) {
            color = NamedTextColor.RED;
        } else {
            color = NamedTextColor.DARK_RED;
        }

        String thing;

        if (seconds % 60 < 10) {
            thing = "0" + seconds % 60;
        } else {
            thing = "" + seconds % 60;
        }

        String time = (seconds / 60) + ":" + thing;

        final Component text = chain(Component.text("Server restarts in ", NamedTextColor.GREEN), Component.text(time, color));

        bossBar.name(text);
        float progress = seconds / (5 * 60f);
        int holder = Math.round(progress);
        if (holder > 1) {
            bossBar.progress(1);
        } else if (holder < 0) {
            bossBar.progress(0);
        } else {
            bossBar.progress(progress);
            bossBar.color(getBossBarColor(color));
        }

        return seconds - 1;
    }

    private static BossBar.Color getBossBarColor(NamedTextColor color) {
        if (NamedTextColor.AQUA.equals(color)) {
            return BossBar.Color.BLUE;
        } else if (NamedTextColor.YELLOW.equals(color)) {
            return BossBar.Color.YELLOW;
        } else if (NamedTextColor.RED.equals(color) || NamedTextColor.DARK_RED.equals(color)) {
            return BossBar.Color.RED;
        }
        return BossBar.Color.GREEN;
    }
}
