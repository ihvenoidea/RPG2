package com.mahirung.rpgcore.gui;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * 장비 강화/전승 GUI
 * - 불필요한 장식 아이콘 제거
 * - 슬롯 겹침 버그 수정
 */
public class EnhanceGUI implements InventoryHolder {

    private final RPGCore plugin;
    private final Inventory inventory;

    public static final String GUI_TITLE = "§l장비 강화";

    // 슬롯 정의
    public static final int EQUIPMENT_SLOT = 13;      // 강화할 장비
    public static final int MATERIAL_SLOT = 22;       // (사용 안 함 / 여유 슬롯)
    public static final int START_BUTTON_SLOT = 31;   // 강화 시작 버튼
    
    public static final int SUCCESSION_PROOF_SLOT = 16; // 파괴된 장비(흔적)
    public static final int SUCCESSION_BASE_SLOT = 25;  // 새 장비
    public static final int SUCCESSION_BUTTON_SLOT = 34; // 전승 시작 버튼

    public EnhanceGUI(RPGCore plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 45, GUI_TITLE);
        initializeItems();
    }

    /** GUI 아이템 초기화 */
    private void initializeItems() {
        // 1. 배경 유리판 (회색)
        ItemStack grayGlass = ItemUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, grayGlass);
        }

        // 2. 가운데 세로줄 구분선 (검은색)
        ItemStack blackGlass = ItemUtil.createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 5; i++) {
            inventory.setItem(i * 9 + 4, blackGlass);
        }

        // 3. 입력 슬롯 비우기 (중요: 여기가 비어있어야 아이템을 넣을 수 있음)
        inventory.setItem(EQUIPMENT_SLOT, null);        // 13번
        // inventory.setItem(MATERIAL_SLOT, null);      // 22번 (현재 강화 재료 슬롯은 사용 안 하면 막아두거나 비워둠)
        
        inventory.setItem(SUCCESSION_PROOF_SLOT, null); // 16번
        inventory.setItem(SUCCESSION_BASE_SLOT, null);  // 25번

        // 4. 기능 버튼 배치
        
        // [강화 버튼]
        ItemStack enhanceButton = ItemUtil.createItem(Material.ANVIL, "§a[장비 강화]");
        ItemUtil.addLore(enhanceButton, "§7위쪽 슬롯에 장비를 올리고 클릭하세요.");
        inventory.setItem(START_BUTTON_SLOT, enhanceButton);

        // [전승 버튼]
        ItemStack successionButton = ItemUtil.createItem(Material.BEACON, "§b[능력치 전승]");
        ItemUtil.addLore(successionButton, "§7위쪽 슬롯에 흔적과 새 장비를 올리고 클릭하세요.");
        inventory.setItem(SUCCESSION_BUTTON_SLOT, successionButton);
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}