package com.mahirung.rpgcore.gui;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.managers.DamageSkinManager.DamageSkinData;
import com.mahirung.rpgcore.util.ChatUtil;
import com.mahirung.rpgcore.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 데미지 스킨 선택 GUI
 * - 플레이어가 해금한 스킨 목록을 표시
 * - 각 아이콘에는 rpgcore_skin_id NBT가 붙어 있어 클릭 시 적용 가능
 */
public class DamageSkinGUI implements InventoryHolder {

    private final RPGCore plugin;
    private final Inventory inventory;

    private final List<String> unlockedSkins;
    private final Map<String, DamageSkinData> allSkinData;

    public static final String GUI_TITLE = "§l데미지 스킨";
    private static final int GUI_SIZE = 54;

    public DamageSkinGUI(RPGCore plugin, List<String> unlockedSkins, Map<String, DamageSkinData> allSkinData) {
        this.plugin = plugin;
        this.unlockedSkins = unlockedSkins;
        this.allSkinData = allSkinData;
        this.inventory = Bukkit.createInventory(this, GUI_SIZE, GUI_TITLE);
        initializeItems();
    }

    /** GUI 아이템 초기화 */
    private void initializeItems() {
        // 기본 배경 유리판
        ItemStack glassPane = ItemUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < GUI_SIZE; i++) {
            inventory.setItem(i, glassPane);
        }

        int slot = 10;
        for (String skinId : unlockedSkins) {
            if (slot > 43) break;

            DamageSkinData skinData = allSkinData.get(skinId);
            if (skinData == null) continue;

            Material iconMat = skinId.equalsIgnoreCase("default") ? Material.IRON_SWORD : Material.GOLDEN_SWORD;
            ItemStack icon = new ItemStack(iconMat, 1);
            ItemMeta meta = icon.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(ChatUtil.format("&a[스킨] &f" + skinId));

                List<String> lore = new ArrayList<>();
                lore.add(ChatUtil.format("&7이 스킨을 적용하려면 클릭하세요."));
                lore.add(" ");
                lore.add(ChatUtil.format("&7일반: " + skinData.getNormalFormat()));
                lore.add(ChatUtil.format("&7치명: " + skinData.getCriticalFormat()));

                meta.setLore(lore);
                icon.setItemMeta(meta);
            }

            // NBT에 스킨 ID 저장
            icon = ItemUtil.setNBTString(icon, "rpgcore_skin_id", skinId);
            inventory.setItem(slot, icon);

            // 슬롯 이동 (GUI 레이아웃 유지)
            slot = ((slot + 1) % 9 == 8) ? slot + 3 : slot + 1;
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
