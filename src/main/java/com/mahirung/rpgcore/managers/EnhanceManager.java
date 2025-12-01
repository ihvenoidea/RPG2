package com.mahirung.rpgcore.managers;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.gui.EnhanceGUI;
import com.mahirung.rpgcore.util.ChatUtil;
import com.mahirung.rpgcore.util.ItemUtil;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 장비 강화 시스템 매니저
 * - GUI 상호작용 로직 수정 (아이템 장착 가능하도록)
 */
public class EnhanceManager {

    private final RPGCore plugin;
    private final ConfigManager configManager;

    private final Map<Integer, EnhanceData> enhanceCache = new HashMap<>();

    public EnhanceManager(RPGCore plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        loadEnhanceConfig();
    }

    /** 강화 설정 로드 */
    public void loadEnhanceConfig() {
        enhanceCache.clear();
        FileConfiguration config = configManager.getEnhancingConfig();
        ConfigurationSection section = config.getConfigurationSection("levels");
        if (section == null) {
            plugin.getLogger().warning("enhancing.yml에 'levels' 섹션이 없습니다.");
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                int level = Integer.parseInt(key);
                double successRate = section.getDouble(key + ".success-rate", 0.5);
                double destroyRate = section.getDouble(key + ".destroy-rate", 0.0);
                int cost = section.getInt(key + ".cost", 100);

                enhanceCache.put(level, new EnhanceData(level, successRate, destroyRate, cost));
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("enhancing.yml의 잘못된 레벨 키: " + key);
            }
        }
        plugin.getLogger().info(enhanceCache.size() + "개의 강화 레벨 데이터를 로드했습니다.");
    }

    public void openEnhanceGUI(Player player) {
        new EnhanceGUI(plugin).open(player);
    }

    /** GUI 클릭 처리 (수정됨) */
    public void handleGUIClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        // GUI 제목 확인
        if (!event.getView().getTitle().equals(EnhanceGUI.GUI_TITLE)) return;

        // [Fix] 1. 내 인벤토리 클릭은 허용 (아이템 집기)
        if (event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.PLAYER) {
            event.setCancelled(false);
            return;
        }

        // 2. GUI 내부 클릭은 기본적으로 취소 (장식 아이템 빼가기 방지)
        event.setCancelled(true);

        // [Fix] 3. 장비 올리는 슬롯은 허용 (아이템 놓기/빼기)
        if (slot == EnhanceGUI.EQUIPMENT_SLOT) {
            event.setCancelled(false);
            return;
        }

        // 4. 강화 버튼 클릭 처리
        if (slot == EnhanceGUI.START_BUTTON_SLOT) {
            ItemStack equipment = event.getInventory().getItem(EnhanceGUI.EQUIPMENT_SLOT);
            if (equipment == null || equipment.getType().isAir()) {
                player.sendMessage(ChatUtil.format("&c[강화] &f강화할 장비를 올려주세요."));
                return;
            }
            ItemStack result = attemptEnhance(player, equipment);
            
            // 결과 처리 (파괴 시 null이 오므로 슬롯 비우기)
            event.getInventory().setItem(EnhanceGUI.EQUIPMENT_SLOT, result);
            
            // 강화 후 플레이어 인벤토리 업데이트 (잔상 방지)
            player.updateInventory();
        }

        // 전승 버튼 클릭
        if (slot == EnhanceGUI.SUCCESSION_BUTTON_SLOT) {
            player.sendMessage(ChatUtil.format("&e[전승] &f능력치 전승 기능은 아직 구현되지 않았습니다."));
        }
    }

    /** 장비 강화 시도 */
    public ItemStack attemptEnhance(Player player, ItemStack item) {
        if (item == null) return null;

        int currentLevel = ItemUtil.getNBTInt(item, "enhance_level");
        EnhanceData data = enhanceCache.get(currentLevel); // 현재 레벨의 '다음 단계' 정보를 가져와야 하는지, 현재 레벨 정보를 가져와야 하는지 로직 점검 필요.
        // 보통 0강 -> 1강 갈때는 '0'번 키의 데이터를 씀.
        // enhanceCache.get(currentLevel) 이 맞습니다. (0강일 때 levels.0 설정 사용)
        
        if (data == null) {
            // 만약 10강인데 10번 데이터가 설정에 있다면 '10강 -> 11강' 시도임.
            // 데이터가 없으면 만렙으로 간주
            player.sendMessage(ChatUtil.format("&c[강화] &f더 이상 강화할 수 없습니다. (최고 레벨)"));
            return item;
        }

        // TODO: Vault 연동하여 비용 차감 코드 추가 권장
        // if (!economy.has(player, data.getCost())) { ... return item; }

        Random random = new Random();
        double roll = random.nextDouble(); // 0.0 ~ 1.0

        if (roll <= data.getSuccessRate()) {
            // 성공
            int nextLevel = currentLevel + 1;
            item = ItemUtil.setNBTInt(item, "enhance_level", nextLevel);
            // 이름 업데이트 (선택 사항)
            // ItemMeta meta = item.getItemMeta();
            // meta.setDisplayName("§b+" + nextLevel + " " + ...);
            // item.setItemMeta(meta);
            
            player.sendMessage(ChatUtil.format("&a[강화 성공] &f아이템이 +{0} 레벨로 강화되었습니다!", nextLevel));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        } else if (roll <= data.getSuccessRate() + data.getDestroyRate()) {
            // 파괴
            player.sendMessage(ChatUtil.format("&c[강화 실패] &f아이템이 파괴되었습니다 ㅠㅠ"));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 0.8f);
            return null; // 아이템 삭제
        } else {
            // 실패 (유지)
            player.sendMessage(ChatUtil.format("&e[강화 실패] &f강화에 실패했지만 아이템은 무사합니다."));
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1f, 1f);
        }

        return item;
    }

    private static class EnhanceData {
        private final int level;
        private final double successRate;
        private final double destroyRate;
        private final int cost;

        public EnhanceData(int level, double successRate, double destroyRate, int cost) {
            this.level = level;
            this.successRate = successRate;
            this.destroyRate = destroyRate;
            this.cost = cost;
        }
        public double getSuccessRate() { return successRate; }
        public double getDestroyRate() { return destroyRate; }
    }
}