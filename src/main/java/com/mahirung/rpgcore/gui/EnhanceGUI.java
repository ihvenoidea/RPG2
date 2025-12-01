package com.mahirung.rpgcore.gui;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.util.ItemUtil;
import com.mahirung.rpgcore.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * 장비 강화/전승 GUI
 * - 왼쪽: 장비 강화
 * - 오른쪽: 능력치 전승
 */
public class EnhanceGUI implements InventoryHolder {

    private final RPGCore plugin;
    private final Inventory inventory;

    public static final String GUI_TITLE = "§l장비 강화";

    // 슬롯 정의
    public static final int EQUIPMENT_SLOT = 13;
    public static final int MATERIAL_SLOT = 22;
    public static final int START_BUTTON_SLOT = 31;
    public static final int SUCCESSION_PROOF_SLOT = 16;
    public static final int SUCCESSION_BASE_SLOT = 25;
    public static final int SUCCESSION_BUTTON_SLOT = 34;

    public EnhanceGUI(RPGCore plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 45, GUI_TITLE);
        initializeItems();
    }

    /** GUI 아이템 초기화 */
    private void initializeItems() {
        // 배경 유리판
        ItemStack grayGlass = ItemUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, grayGlass);
        }

        // 가운데 세로줄 (4, 13, 22, 31, 40)
        ItemStack blackGlass = ItemUtil.createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 5; i++) {
            inventory.setItem(i * 9 + 4, blackGlass);
        }

        // 강화 영역 (왼쪽)
        inventory.setItem(EQUIPMENT_SLOT, null);
        inventory.setItem(MATERIAL_SLOT, null);

        ItemStack enhanceButton = ItemUtil.createItem(Material.ANVIL, "§a[장비 강화]");
        ItemUtil.addLore(enhanceButton, "§7장비와 재료를 올리고 클릭하세요.");
        inventory.setItem(START_BUTTON_SLOT, enhanceButton);

        // 전승 영역 (오른쪽)
        inventory.setItem(SUCCESSION_PROOF_SLOT, null);
        inventory.setItem(SUCCESSION_BASE_SLOT, null);

        ItemStack successionButton = ItemUtil.createItem(Material.BEACON, "§b[능력치 전승]");
        ItemUtil.addLore(successionButton, "§7파괴된 장비와 새 장비를 올리고 클릭하세요.");
        inventory.setItem(SUCCESSION_BUTTON_SLOT, successionButton);

        // 슬롯 설명 아이템
        inventory.setItem(EQUIPMENT_SLOT - 9,
                ItemUtil.createItem(Material.DIAMOND_SWORD, ChatUtil.format("§f↑ 강화할 장비")));
        inventory.setItem(MATERIAL_SLOT - 9,
                ItemUtil.createItem(Material.NETHERITE_INGOT, ChatUtil.format("§f↑ 강화 재료")));
        inventory.setItem(SUCCESSION_PROOF_SLOT - 9,
                ItemUtil.createItem(Material.BARRIER, ChatUtil.format("§f↑ 파괴된 장비")));
        inventory.setItem(SUCCESSION_BASE_SLOT - 9,
                ItemUtil.createItem(Material.DIAMOND_CHESTPLATE, ChatUtil.format("§f↑ 새 장비 (베이스)")));
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
