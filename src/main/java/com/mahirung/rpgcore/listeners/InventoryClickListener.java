package com.mahirung.rpgcore.listeners;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.gui.ClassSelectorGUI;
import com.mahirung.rpgcore.gui.DamageSkinGUI;
import com.mahirung.rpgcore.gui.EnhanceGUI;
import com.mahirung.rpgcore.gui.RefineGUI;
import com.mahirung.rpgcore.gui.SuccessionGUI;
import com.mahirung.rpgcore.managers.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

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

        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        String guiTitle = view.getTitle();
        
        boolean isCustomGUI = guiTitle.equals(ClassSelectorGUI.GUI_TITLE)
                           || guiTitle.equals(RefineGUI.GUI_TITLE)
                           || guiTitle.equals(EnhanceGUI.GUI_TITLE)
                           || guiTitle.equals(DamageSkinGUI.GUI_TITLE)
                           || guiTitle.equals(SuccessionGUI.GUI_TITLE);

        // 1. 룬 장착/해제
        if (isCustomGUI && event.getClickedInventory().equals(player.getInventory())
                && currentItem != null && cursorItem != null) {
            if (runeManager.isRune(cursorItem)) {
                event.setCancelled(true);
                ItemStack newEquipment = runeManager.applyRune(player, cursorItem, currentItem);
                if (newEquipment != null) {
                    event.setCurrentItem(newEquipment);
                    cursorItem.setAmount(cursorItem.getAmount() - 1);
                    player.setItemOnCursor(cursorItem);
                    return;
                }
            } else if (runeManager.isRuneRemovalItem(cursorItem)) {
                event.setCancelled(true);
                ItemStack newEquipment = runeManager.removeRune(player, cursorItem, currentItem);
                if (newEquipment != null) {
                    event.setCurrentItem(newEquipment);
                    cursorItem.setAmount(cursorItem.getAmount() - 1);
                    player.setItemOnCursor(cursorItem);
                    return;
                }
            }
        }

        // 2. GUI 처리 (switch 문 -> if-else if로 변경)
        if (isCustomGUI) {
            if (event.getClick().isShiftClick()) event.setCancelled(true);

            if (guiTitle.equals(ClassSelectorGUI.GUI_TITLE)) {
                event.setCancelled(true);
                classManager.handleGUIClick(event);
            } 
            else if (guiTitle.equals(RefineGUI.GUI_TITLE)) {
                refineManager.handleGUIClick(event);
            } 
            else if (guiTitle.equals(EnhanceGUI.GUI_TITLE)) {
                enhanceManager.handleGUIClick(event);
            } 
            else if (guiTitle.equals(DamageSkinGUI.GUI_TITLE)) {
                event.setCancelled(true);
                damageSkinManager.handleGUIClick(event);
            } 
            else if (guiTitle.equals(SuccessionGUI.GUI_TITLE)) {
                enhanceManager.handleSuccessionGUIClick(event);
            }
        }
    }
}