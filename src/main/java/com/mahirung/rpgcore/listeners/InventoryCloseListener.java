package com.mahirung.rpgcore.listeners;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.gui.RefineGUI;
import com.mahirung.rpgcore.gui.EnhanceGUI;
import com.mahirung.rpgcore.gui.SuccessionGUI; // [중요] 전승 GUI import
import com.mahirung.rpgcore.gui.DamageSkinGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

/**
 * 인벤토리 닫기 이벤트 리스너
 * - GUI를 닫을 때 올려둔 아이템을 플레이어에게 반환 (증발 방지)
 */
public class InventoryCloseListener implements Listener {

    private final RPGCore plugin;

    public InventoryCloseListener(RPGCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        Inventory inv = event.getInventory();
        String title = event.getView().getTitle();

        // 1. 강화 GUI 닫기
        if (title.equals(EnhanceGUI.GUI_TITLE)) {
            returnItem(player, inv, EnhanceGUI.EQUIPMENT_SLOT);
        }

        // 2. 전승 GUI 닫기
        else if (title.equals(SuccessionGUI.GUI_TITLE)) {
            returnItem(player, inv, SuccessionGUI.TARGET_SLOT);
            returnItem(player, inv, SuccessionGUI.TRACE_SLOT);
        }

        // 3. 재련 GUI 닫기
        else if (title.equals(RefineGUI.GUI_TITLE)) {
            returnItem(player, inv, RefineGUI.INPUT_SLOT);
            returnItem(player, inv, RefineGUI.CATALYST_SLOT);
        }
    }

    /**
     * 특정 슬롯의 아이템을 플레이어에게 반환하는 메서드
     */
    private void returnItem(Player player, Inventory inv, int slot) {
        ItemStack item = inv.getItem(slot);
        if (item != null && !item.getType().isAir()) {
            // 인벤토리에 아이템 추가 (남은 아이템 반환)
            HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(item);
            
            // 인벤토리가 꽉 차서 못 넣은 아이템은 바닥에 드롭
            if (!leftOver.isEmpty()) {
                for (ItemStack drop : leftOver.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                player.sendMessage("§e[주의] §f인벤토리가 가득 차서 아이템이 바닥에 떨어졌습니다.");
            }
            
            // GUI 슬롯 비우기 (중복 반환 방지)
            inv.setItem(slot, null); 
        }
    }
}