package com.mahirung.rpgcore.gui;

import com.mahirung.rpgcore.util.ChatUtil;
import com.mahirung.rpgcore.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class DamageSkinGUI {

    public static final String GUI_TITLE = ChatUtil.format("&8[ 데미지 스킨 설정 ]");

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, GUI_TITLE);

        ItemStack info = ItemUtil.createItem(Material.NAME_TAG, "&e[기본 스킨]");
        ItemUtil.addLore(info, 
                "&7현재 기본 데미지 스킨이 적용되어 있습니다.",
                "&7(크리티컬 시 붉은색 효과)");
        
        inv.setItem(4, info);

        player.openInventory(inv);
    }
}