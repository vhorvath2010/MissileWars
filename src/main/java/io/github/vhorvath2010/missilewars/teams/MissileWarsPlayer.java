package io.github.vhorvath2010.missilewars.teams;

import io.github.vhorvath2010.missilewars.decks.Deck;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

/** Represents a Missile Wars Player. */
public class MissileWarsPlayer {

    /** The UUID of the Spigot player this player represents. */
    private UUID playerId;
    /** The current deck the player has selected. */
    private Deck deck;

    /**
     * Create a MissileWarsPlayer from a Minecraft player.
     *
     * @player the Minecraft player
     */
    public MissileWarsPlayer(UUID playerID) {
        this.playerId = playerID;
    }

    /**
     * Set the user's current Deck.
     *
     * @param deck the deck to let this MissileWarsPlayer use
     */
    public void setDeck(Deck deck) {
        this.deck = deck;
    }

    /**
     * Obtain the UUID of the Minecraft player associated with the MissileWarsPlayer.
     *
     * @return the UUID of the Minecraft player associated with the MissileWarsPlayer
     */
    public UUID getMCPlayerId() {
        return playerId;
    }

    /**
     * Return the MC player this MissileWarsPlayer represents.
     *
     * @return the MC player this MissileWarsPlayer represents
     */
    public Player getMCPlayer() {
        return Bukkit.getPlayer(playerId);
    }

    /**
     * Checks to see if this MissileWarsPlayer is equal to another Object.
     *
     * @param o the object
     * @return true if o is a MissileWarsPlayer with the same playerId
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MissileWarsPlayer player = (MissileWarsPlayer) o;
        return Objects.equals(playerId, player.playerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId);
    }

    /** Give the MC player an item from their Deck. */
    public void givePoolItem() {
        if (deck != null && getMCPlayer() != null) {
            deck.givePoolItem(getMCPlayer());
        }
    }

    /** Give the MC player their Deck gear. */
    public void giveDeckGear() {
        if (deck != null && getMCPlayer() != null) {
            deck.giveGear(getMCPlayer());
        }
    }

}
