package io.github.vhorvath2010.missilewars.events;

import io.github.vhorvath2010.missilewars.MissileWarsPlugin;
import io.github.vhorvath2010.missilewars.arenas.Arena;
import io.github.vhorvath2010.missilewars.arenas.ArenaManager;
import io.github.vhorvath2010.missilewars.utilities.ConfigUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.ArrayList;
import java.util.List;

/** Class to manage arena joining and pregame events. */
public class ArenaInventoryEvents implements Listener {

    /** List of players currently in arena selection. */
    public static List<Player> selectingArena = new ArrayList<>();

    /** Handle arena selection. */
    @EventHandler
    public void selectArena(InventoryClickEvent event) {
        // Check if player is selecting an Arena
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (!selectingArena.contains(player)) {
            return;
        }

        // Check for arena selection
        event.setCancelled(true);
        if (event.getClickedInventory() == null ||
                !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        ArenaManager manager = MissileWarsPlugin.getPlugin().getArenaManager();
        Arena selectedArena = manager.getArena(event.getSlot());
        if (selectedArena == null) {
            return;
        }

        // Attempt to send player to arena
        ConfigUtils.sendConfigMessage("messages.join-arena", player, selectedArena);
        if (selectedArena.joinPlayer(player)) {
            player.closeInventory();
            ConfigUtils.sendConfigMessage("messages.joined-arena", player, selectedArena);
        } else {
            ConfigUtils.sendConfigMessage("messages.arena-full", player, selectedArena);
        }
    }

    /** Replace from selectors when closed inventory. */
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        selectingArena.remove(player);
    }

}