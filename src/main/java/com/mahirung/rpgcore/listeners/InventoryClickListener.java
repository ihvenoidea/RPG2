package com.mahirung.rpgcore.listeners;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.gui.ClassSelectorGUI;
import com.mahirung.rpgcore.gui.DamageSkinGUI;
import com.mahirung.rpgcore.gui.EnhanceGUI;
import com.mahirung.rpgcore.gui.RefineGUI;
import com.mahirung.rpgcore.managers.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

/**
 * 인벤토리 클릭 리스너
 * - 커스텀 GUI 클릭 처리
 * - 룬 장착/해제 처리
 */
public class InventoryClickListener implements Listener {

    private final ClassManager classManager;
    private final RefineManager refineManager;
    private final EnhanceManager enhanceManager;
    private final DamageSkinManager damageSkinManager;
    private final RuneManager runeManager;

    public InventoryClickListener(RPGCore plugin) {
        this.classManager = plugin.getClassManager();
        this.refineManager = plugin.getRefineManager();
        this.enhanceManager = plugin.getEnhanceManager();
        this.damageSkinManager = plugin.getDamageSkinManager();
        this.runeManager = plugin.getRuneManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        InventoryView view = player.getOpenInventory();
        if (view == null || event.getClickedInventory() == null) return;

        ItemStack currentItem = event.getCurrentItem(); // 클릭된 아이템
        ItemStack cursorItem = event.getCursor();       // 커서 아이템

        String guiTitle = view.getTitle();
        boolean isCustomGUI = guiTitle.equals(ClassSelectorGUI.GUI_TITLE)
                           || guiTitle.equals(RefineGUI.GUI_TITLE)
                           || guiTitle.equals(EnhanceGUI.GUI_TITLE)
                           || guiTitle.equals(DamageSkinGUI.GUI_TITLE);

        // --- 1. 룬 장착/해제 처리 ---
        if (isCustomGUI && event.getClickedInventory().equals(player.getInventory())
                && currentItem != null && cursorItem != null) {

            if (runeManager.isRune(cursorItem)) {
                event.setCancelled(true);
                ItemStack newEquipment = runeManager.applyRune(player, cursorItem, currentItem);
                if (newEquipment != null) {
                    event.setCurrentItem(newEquipment);
                    consumeCursorItem(player, cursorItem);
                    return;
                }
            } else if (runeManager.isRuneRemovalItem(cursorItem)) {
                event.setCancelled(true);
                ItemStack newEquipment = runeManager.removeRune(player, cursorItem, currentItem);
                if (newEquipment != null) {
                    event.setCurrentItem(newEquipment);
                    consumeCursorItem(player, cursorItem);
                    return;
                }
            }
        }

        // --- 2. 커스텀 GUI 처리 ---
        if (isCustomGUI) {
            // 쉬프트 클릭 방지
            if (event.getClick().isShiftClick()) {
                event.setCancelled(true);
            }

            // GUI별 매니저 위임
            switch (guiTitle) {
                case ClassSelectorGUI.GUI_TITLE -> {
                    event.setCancelled(true);
                    classManager.handleGUIClick(event);
                }
                case RefineGUI.GUI_TITLE -> refineManager.handleGUIClick(event);
                case EnhanceGUI.GUI_TITLE -> enhanceManager.handleGUIClick(event);
                case DamageSkinGUI.GUI_TITLE -> {
                    event.setCancelled(true);
                    damageSkinManager.handleGUIClick(event);
                }
            }
        }
    }

    /** 커서 아이템 1개 소모 처리 */
    private void consumeCursorItem(Player player, ItemStack cursorItem) {
        cursorItem.setAmount(cursorItem.getAmount() - 1);
        player.setItemOnCursor(cursorItem);
    }
}
