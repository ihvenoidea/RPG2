package com.mahirung.rpgcore.hooks;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.data.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RPGCoreExpansion extends PlaceholderExpansion {

    private final RPGCore plugin;

    public RPGCoreExpansion(RPGCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() { return "rpgcore"; }

    @Override
    public @NotNull String getAuthor() { return "Mahirung"; }

    @Override
    public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return "로딩중...";

        return switch (params) {
            case "level" -> String.valueOf(data.getLevel());
            case "class" -> data.hasClass() ? data.getPlayerClass() : "무직";
            case "exp" -> String.format("%.1f", data.getCurrentExp());
            case "max_exp" -> String.format("%.1f", data.getRequiredExp());
            case "hp" -> String.format("%.0f", player.getHealth());
            case "mana" -> String.format("%.0f", data.getCurrentMana());
            default -> null;
        };
    }
}