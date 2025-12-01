package com.mahirung.rpgcore.gui;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.managers.ClassManager;
import com.mahirung.rpgcore.util.ChatUtil;
import com.mahirung.rpgcore.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * 직업 선택 GUI
 * - ClassManager에서 직업 목록을 가져와 아이콘으로 표시
 * - 각 아이콘에는 class_id NBT가 붙어 있어 클릭 시 직업 선택 처리 가능
 */
public class ClassSelectorGUI implements InventoryHolder {

    private final RPGCore plugin;
    private final ClassManager classManager;
    private final Inventory inventory;

    public static final String GUI_TITLE = "§l직업 선택";
    private static final int GUI_SIZE = 27;

    public ClassSelectorGUI(RPGCore plugin) {
        this.plugin = plugin;
        this.classManager = plugin.getClassManager();
        this.inventory = Bukkit.createInventory(this, GUI_SIZE, GUI_TITLE);
        initializeItems();
    }

    /** GUI 아이템 초기화 */
    private void initializeItems() {
        List<String> classIds = classManager.getAllClassIds();
        int slot = 10;

        for (String classId : classIds) {
            if (slot > 16) break;

            FileConfiguration classConfig = classManager.getClassConfig(classId);
            if (classConfig == null) continue;

            String displayName = classConfig.getString("gui-icon.display-name", "&c" + classId);
            String materialName = classConfig.getString("gui-icon.material", "STONE");
            List<String> lore = classConfig.getStringList("gui-icon.lore");

            Material material = Material.matchMaterial(materialName);
            if (material == null) material = Material.STONE; // 안전성 강화

            ItemStack icon = new ItemStack(material, 1);
            ItemMeta meta = icon.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(ChatUtil.format(displayName));
                meta.setLore(ChatUtil.format(lore));
                icon.setItemMeta(meta);
            }

            // class_id NBT 부여
            icon = ItemUtil.setNBTString(icon, "class_id", classId);
            inventory.setItem(slot, icon);

            slot += 2; // 아이콘 간격 유지
        }
    }

    /** 플레이어에게 GUI 열기 */
    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
