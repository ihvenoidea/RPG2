package com.mahirung.rpgcore.listeners;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.managers.RuneManager;
import com.mahirung.rpgcore.util.ChatUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 인벤토리 드래그 이벤트 리스너
 * - 룬 아이템을 드래그하여 잘못 분배하는 것을 방지
 */
public class InventoryDragListener implements Listener {

    private final RuneManager runeManager;

    public InventoryDragListener(RPGCore plugin) {
        this.runeManager = plugin.getRuneManager();
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        ItemStack draggedItem = event.getOldCursor();

        // 아이템이 없거나 공기(AIR)면 무시
        if (draggedItem == null || draggedItem.getType().isAir()) return;

        // 룬 아이템이 아니면 무시
        if (!runeManager.isRune(draggedItem)) return;

        // 룬 아이템 드래그 방지
        event.setCancelled(true);

        if (event.getWhoClicked() instanceof Player player) {
            player.sendMessage(ChatUtil.format("&c[RPGCore] &f룬은 드래그로 적용할 수 없습니다. 장비 아이템 위에 클릭하세요."));
        }
    }
}
