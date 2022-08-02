package io.github.haappi.ducksmp.listeners;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import io.github.haappi.ducksmp.DuckSMP;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

import static io.github.haappi.ducksmp.Cosemetics.NameTag.Common.chatColors;
import static io.github.haappi.ducksmp.Cosemetics.NameTag.Common.getFormattedPrefix;
import static io.github.haappi.ducksmp.PacketInjector.injectPlayer;
import static io.github.haappi.ducksmp.PacketInjector.removePlayer;
import static io.github.haappi.ducksmp.utils.Encryption.encrypt;
import static io.github.haappi.ducksmp.utils.Utils.miniMessage;

public class JoinLeave implements Listener {

    private final DuckSMP plugin;
    private final ConcurrentHashMap<String, String> IPNameMapping = new ConcurrentHashMap<>();

    public JoinLeave() {
        this.plugin = DuckSMP.getInstance();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        removePlayer(player);
        if (player.getUniqueId().toString().replaceAll("-", "").equalsIgnoreCase("1ca6d48fc4f4438781f79158a209d60d")) {
            return;
        }

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
        if (chatColors.get(event.getPlayer().getUniqueId()) != null) {
            event.renderer((source, sourceDisplayName, message, viewer) -> Component.text()
                    .append(
                            getFormattedPrefix(event.getPlayer()),
                            Component.text("[", chatColors.get(event.getPlayer().getUniqueId())),
                            sourceDisplayName.color(chatColors.get(event.getPlayer().getUniqueId())),
                            Component.text("] ", chatColors.get(event.getPlayer().getUniqueId())),
                            Component.text()
                                    .color(NamedTextColor.WHITE)
                                    .append(message)
                                    .build()
                    )
                    .build());
        } else if (event.getPlayer().isOp()) {
            event.renderer((source, sourceDisplayName, message, viewer) -> Component.text()
                    .append(
                            Component.text("[", NamedTextColor.GREEN),
                            sourceDisplayName.color(NamedTextColor.GREEN),
                            Component.text("] ", NamedTextColor.GREEN),
                            Component.text()
                                    .color(NamedTextColor.WHITE)
                                    .append(message)
                                    .build()
                    )
                    .build());
        } else { // todo make non-linked players gray
            event.renderer((source, sourceDisplayName, message, viewer) -> Component.text()
                    .append(
                            Component.text("[", NamedTextColor.YELLOW),
                            sourceDisplayName.color(NamedTextColor.YELLOW),
                            Component.text("] ", NamedTextColor.YELLOW),
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
        injectPlayer(player);
        if (player.getUniqueId().toString().replaceAll("-", "").equalsIgnoreCase("1ca6d48fc4f4438781f79158a209d60d")) {
            return;
        }
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
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> saveStuffInDB(player));
    }

    private void saveStuffInDB(Player player) {
        Document doc = new Document();
        doc.put("playerIP", encrypt(player.getAddress().getAddress().getHostAddress())); // it's encrypted with the key in the config
        doc.put("playerName", player.getName().replaceAll("\\.", "")); // Doubt Bedrock players want to see the prepended '.'

        Bson filter = Filters.eq("_id", player.getUniqueId().toString().replaceAll("-", ""));

        DuckSMP.getMongoClient().getDatabase("duckMinecraft").getCollection("playerData")
                .updateOne(
                        filter,
                        new Document("$set", doc),
                        new UpdateOptions().upsert(true)
                );
    }

    private @Nullable String getPlayerMOTDFromIP(String ipAddress) {
        String mapping = IPNameMapping.get(ipAddress);
        if (mapping != null) {
            if (mapping.equals("")) {
                return "";
            }
            return String.format("\n<gray>Welcome Back</gray> <aqua><bold>%s</bold></aqua><gray>", IPNameMapping.get(ipAddress));
        }
        Document doc = DuckSMP.getMongoClient().getDatabase("duckMinecraft")
                .getCollection("playerData")
                .find(new Document("playerIP", encrypt(ipAddress))).first();
        if (doc == null) {
            return null;
        } else {
            return String.format("\n<gray>Welcome Back</gray> <aqua><bold>%s</bold></aqua><gray>", doc.getString("playerName"));
        }
    }

    @EventHandler
    public void onPing(PaperServerListPingEvent event) {
        String motd = "<bold><gold>Duck</gold><yellow>Nation</yellow><green> SMP</green></bold>";
        event.motd(miniMessage.deserialize(motd));

        Component component = miniMessage.deserialize(motd + getPlayerMOTDFromIP(event.getAddress().getHostAddress()));
        event.motd(component);

    }
}