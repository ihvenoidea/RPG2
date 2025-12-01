package com.mahirung.rpgcore.gui;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Arrays;

public class SuccessionGUI {
    public static final String GUI_TITLE = ChatUtil.format("&8[ 전승 시스템 ]");
    public static final int TARGET_SLOT = 11;
    public static final int TRACE_SLOT = 15;
    public static final int START_BUTTON_SLOT = 13;

    private final RPGCore plugin;

    public SuccessionGUI(RPGCore plugin) { this.plugin = plugin; }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, "&7");
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        inv.setItem(TARGET_SLOT, null);
        inv.setItem(TRACE_SLOT, null);
        inv.setItem(START_BUTTON_SLOT, createItem(Material.ANVIL, "&a&l[ 전승 시작 ]", "&7왼쪽에 새 장비, 오른쪽에 흔적을 넣으세요."));

        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatUtil.format(name));
            if (lore.length > 0) meta.setLore(Arrays.stream(lore).map(ChatUtil::format).toList());
            item.setItemMeta(meta);
        }
        return item;
    }
}