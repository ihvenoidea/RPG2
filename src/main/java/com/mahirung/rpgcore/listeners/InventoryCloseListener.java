package com.mahirung.rpgcore.listeners;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.gui.RefineGUI;
import com.mahirung.rpgcore.gui.EnhanceGUI;
import com.mahirung.rpgcore.gui.DamageSkinGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * 인벤토리 닫기 이벤트 리스너
 * - 커스텀 GUI 닫을 때 후처리
 */
public class InventoryCloseListener implements Listener {

    private final RPGCore plugin;

    public InventoryCloseListener(RPGCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        String title = event.getView().getTitle();

        // 재련 GUI 닫기
        if (title.equals(RefineGUI.GUI_TITLE)) {
            // 필요 시 재련 작업 저장/갱신 로직 추가 가능
            plugin.getLogger().info(player.getName() + " 님이 재련 GUI를 닫았습니다.");
        }

        // 강화 GUI 닫기
        else if (title.equals(EnhanceGUI.GUI_TITLE)) {
            plugin.getLogger().info(player.getName() + " 님이 강화 GUI를 닫았습니다.");
        }

        // 데미지 스킨 GUI 닫기
        else if (title.equals(DamageSkinGUI.GUI_TITLE)) {
            plugin.getLogger().info(player.getName() + " 님이 데미지 스킨 GUI를 닫았습니다.");
        }
    }
}
