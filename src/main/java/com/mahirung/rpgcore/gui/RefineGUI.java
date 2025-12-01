package com.mahirung.rpgcore.gui;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.managers.RefineManager.ActiveRefineTask;
import com.mahirung.rpgcore.util.ChatUtil;
import com.mahirung.rpgcore.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 광물 재련 GUI
 * - 입력 슬롯: 재료
 * - 촉매 슬롯: 촉매제
 * - 결과 슬롯: 재련 결과
 * - 버튼 슬롯: 시작/진행/수령 상태 표시
 */
public class RefineGUI implements InventoryHolder {

    private final RPGCore plugin;
    private final Inventory inventory;
    private final List<ActiveRefineTask> tasks;

    public static final String GUI_TITLE = "§l광물 재련";

    public static final int INPUT_SLOT = 10;
    public static final int CATALYST_SLOT = 12;
    public static final int START_BUTTON_SLOT = 22;
    public static final int RESULT_SLOT = 16;
    private static final int ARROW_SLOT = 13;

    public RefineGUI(RPGCore plugin, List<ActiveRefineTask> tasks) {
        this.plugin = plugin;
        this.tasks = (tasks != null) ? tasks : Collections.emptyList();
        this.inventory = Bukkit.createInventory(this, 27, GUI_TITLE);
        initializeItems();
    }

    /** GUI 아이템 초기화 */
    private void initializeItems() {
        // 기본 배경 유리판
        ItemStack glassPane = ItemUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            if (i != INPUT_SLOT && i != CATALYST_SLOT && i != RESULT_SLOT && i != START_BUTTON_SLOT) {
                inventory.setItem(i, glassPane);
            }
        }

        // 중앙 화살표
        inventory.setItem(ARROW_SLOT, ItemUtil.createItem(Material.ARROW, "§a재련"));

        ActiveRefineTask currentTask = getDisplayTask();

        if (currentTask != null) {
            if (currentTask.isComplete()) {
                // 완료 상태
                ItemStack resultItem = ItemUtil.createItem(Material.DIAMOND, "§a[재련 완료!]");
                ItemUtil.addLore(resultItem, "§e클릭하여 수령하세요.");
                inventory.setItem(RESULT_SLOT, resultItem);

                ItemStack startButton = ItemUtil.createItem(Material.GREEN_WOOL, "§a수령 가능");
                ItemUtil.addLore(startButton, "§7클릭하여 재련을 완료하세요.");
                inventory.setItem(START_BUTTON_SLOT, startButton);

            } else {
                // 진행 중 상태
                String timeString = formatTimeLeft(currentTask.getTimeLeft());
                ItemStack progressItem = ItemUtil.createItem(Material.CLOCK, "§6[재련 진행 중...]");
                ItemUtil.addLore(progressItem, "§f남은 시간: §e" + timeString);
                inventory.setItem(START_BUTTON_SLOT, progressItem);

                inventory.setItem(RESULT_SLOT, null);
            }
            // 진행 중일 때 입력/촉매 슬롯은 막기
            inventory.setItem(INPUT_SLOT, ItemUtil.createItem(Material.BARRIER, "§c재련 진행 중"));
            inventory.setItem(CATALYST_SLOT, ItemUtil.createItem(Material.BARRIER, "§c재련 진행 중"));

        } else {
            // 대기 상태
            inventory.setItem(INPUT_SLOT, null);
            inventory.setItem(CATALYST_SLOT, null);
            inventory.setItem(RESULT_SLOT, null);

            ItemStack startButton = ItemUtil.createItem(Material.RED_WOOL, "§c[재련 시작]");
            ItemUtil.addLore(startButton, "§7재료와 재련권을 올리고 클릭하세요.");
            inventory.setItem(START_BUTTON_SLOT, startButton);
        }
    }

    /** 표시할 작업 선택 (완료된 작업 우선) */
    private ActiveRefineTask getDisplayTask() {
        if (tasks.isEmpty()) return null;
        for (ActiveRefineTask task : tasks) {
            if (task.isComplete()) return task;
        }
        return tasks.get(0);
    }

    /** 남은 시간 포맷팅 */
    private String formatTimeLeft(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
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
