package com.mahirung.rpgcore.listeners;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.util.ChatUtil;
import com.mahirung.rpgcore.util.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 플레이어 아이템 드롭 이벤트 리스너
 * - 특정 아이템(강화/룬 장비) 드롭 방지
 */
public class PlayerDropItemListener implements Listener {

    private final RPGCore plugin;

    public PlayerDropItemListener(RPGCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack dropped = event.getItemDrop().getItemStack();

        if (dropped == null) return;

        // 예시 1: 강화/룬 아이템은 드롭 금지
        String enhanceLevel = ItemUtil.getNBTString(dropped, "enhance_level");
        String runeId = ItemUtil.getNBTString(dropped, "rpgcore_runes_list");
        if (enhanceLevel != null || runeId != null) {
            event.setCancelled(true);
            player.sendMessage(ChatUtil.format("&c[RPGCore] &f강화/룬 장비는 버릴 수 없습니다."));
        }
    }
}