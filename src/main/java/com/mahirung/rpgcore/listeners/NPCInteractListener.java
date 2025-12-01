package com.mahirung.rpgcore.listeners;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.managers.EnhanceManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * NPC 상호작용 이벤트 리스너
 * - 특정 NPC(강화 NPC)를 우클릭했을 때 강화 GUI를 오픈
 */
public class NPCInteractListener implements Listener {

    private final EnhanceManager enhanceManager;

    // 강화 NPC 이름 (config.yml에서 불러오도록 개선 가능)
    private static final String ENHANCE_NPC_NAME = "§6[강화] 대장장이";

    public NPCInteractListener(RPGCore plugin) {
        this.enhanceManager = plugin.getEnhanceManager();
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        // 1. 주 손(MAIN_HAND) 상호작용만 처리 (이중 호출 방지)
        if (event.getHand() != EquipmentSlot.HAND) return;

        Entity clickedEntity = event.getRightClicked();
        if (!(clickedEntity instanceof LivingEntity)) return;

        Player player = event.getPlayer();
        String entityName = clickedEntity.getCustomName();
        if (entityName == null) return;

        // 색상 코드 제거 후 비교 (권장)
        String strippedName = ChatColor.stripColor(entityName);
        String targetName = ChatColor.stripColor(ENHANCE_NPC_NAME);

        if (strippedName.equals(targetName)) {
            // 기본 상호작용 차단
            event.setCancelled(true);

            // 강화 GUI 열기
            enhanceManager.openEnhanceGUI(player);
        }
    }
}
