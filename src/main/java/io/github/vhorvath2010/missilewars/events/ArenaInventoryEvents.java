package io.github.vhorvath2010.missilewars.events;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.scheduler.BukkitRunnable;

import io.github.vhorvath2010.missilewars.MissileWarsPlugin;
import io.github.vhorvath2010.missilewars.arenas.Arena;
import io.github.vhorvath2010.missilewars.arenas.ArenaManager;
import io.github.vhorvath2010.missilewars.teams.MissileWarsPlayer;
import io.github.vhorvath2010.missilewars.utilities.ConfigUtils;

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
        if (selectedArena.joinPlayer(player)) {
            player.closeInventory();
        } else {
            ConfigUtils.sendConfigMessage("messages.arena-full", player, selectedArena, null);
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

    /** Stop players from changing their armor/bow items. */
    @EventHandler
    public void stopItemMoving(InventoryClickEvent event) {
        // Obtain player
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        // Check if player is in an active arena
        ArenaManager manager = MissileWarsPlugin.getPlugin().getArenaManager();
        Arena arena = manager.getArena(player.getUniqueId());
        if (arena == null || !arena.isRunning()) {
            return;
        }

        // Stop armor removals and first slot changes
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            event.setCancelled(true);
        }
    }

    /** Manage item dropping. */
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        // Check if player is in Arena
        Player player = event.getPlayer();
        ArenaManager manager = MissileWarsPlugin.getPlugin().getArenaManager();
        Arena arena = manager.getArena(player.getUniqueId());
        if (arena == null) {
            return;
        }

        // Stop drops entirely if player not on team
        if ((arena.getTeam(player.getUniqueId()).equals("no team"))) {
            event.setCancelled(true);
            return;
        }

        // Stop drops of gear items
        MissileWarsPlayer mwPlayer = arena.getPlayerInArena(player.getUniqueId()); 
        if (mwPlayer.getDeck().getGear().contains(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    /** Manage item pickups. */
    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        // Check if player is in Arena
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        ArenaManager manager = MissileWarsPlugin.getPlugin().getArenaManager();
        Arena arena = manager.getArena(player.getUniqueId());
        if (arena == null) {
            return;
        }
        MissileWarsPlayer mwPlayer = arena.getPlayerInArena(player.getUniqueId());

        // Cancel event if player cannot pick up item based on their given deck
        if (mwPlayer.getDeck() != null && !mwPlayer.getDeck().hasInventorySpace(mwPlayer.getMCPlayer())) {
            event.setCancelled(true);
        }
    }

    /** Stop swapping to offhand if in arena. */
    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        ArenaManager manager = MissileWarsPlugin.getPlugin().getArenaManager();
        Arena arena = manager.getArena(event.getPlayer().getUniqueId());
        if (arena != null) {
            event.setCancelled(true);
        }
    }
    
    /** Remove glass bottles after drinking potions */
    @EventHandler
    public void onDrink(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (event.getItem().getType() == Material.POTION) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.getInventory().setItemInMainHand(null);
                }
            }.runTaskLater(MissileWarsPlugin.getPlugin(), 1L);
        }
    }

}
