package com.mahirung.rpgcore.managers;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.data.PlayerData;
import com.mahirung.rpgcore.util.ChatUtil;
import com.mahirung.rpgcore.util.ItemUtil;
import io.lumine.mythic.api.mobs.MythicMob;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 직업 시스템을 관리하는 매니저 클래스
 * - 직업 선택 GUI 처리
 * - 직업별 스탯 및 레벨업 처리
 * - 몹 경험치 캐싱
 */
public class ClassManager {

    private final RPGCore plugin;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;

    private Map<String, FileConfiguration> classConfigs = new HashMap<>();
    private final Map<String, Double> mobExpCache = new HashMap<>();

    public ClassManager(RPGCore plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.playerDataManager = plugin.getPlayerDataManager();
        loadClasses();
    }

    /** 직업 및 몹 경험치 설정 로드 */
    public void loadClasses() {
        classConfigs = configManager.getClassConfigs();
        mobExpCache.clear();

        ConfigurationSection expSection = configManager.getMainConfig().getConfigurationSection("mob-experience");
        if (expSection != null) {
            for (String mobId : expSection.getKeys(false)) {
                mobExpCache.put(mobId, expSection.getDouble(mobId, 0.0));
            }
        }
        plugin.getLogger().info(classConfigs.size() + "개의 직업과 " + mobExpCache.size() + "개의 몹 경험치를 로드했습니다.");
    }

    /** GUI 클릭 이벤트 처리 */
    public void handleGUIClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        String classId = ItemUtil.getNBTString(clickedItem, "class_id");
        if (classId == null || classId.isEmpty()) return;

        player.closeInventory();
        if (classConfigs.containsKey(classId)) {
            selectClass(player, classId);
        } else {
            player.sendMessage(ChatUtil.format("&c[RPGCore] &f선택한 직업(''{0}'')을 찾을 수 없습니다.", classId));
        }
    }

    /** 직업 선택 처리 */
    private void selectClass(Player player, String classId) {
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        if (data == null) return;

        if (data.hasClass()) {
            player.sendMessage(ChatUtil.format("&c[RPGCore] &f이미 직업을 가지고 있습니다."));
            return;
        }

        FileConfiguration classConfig = classConfigs.get(classId);
        if (classConfig == null) {
            player.sendMessage(ChatUtil.format("&c[RPGCore] &f직업 설정을 찾을 수 없습니다."));
            return;
        }

        data.setPlayerClass(classId);
        data.setLevel(1);

        player.sendMessage(ChatUtil.format("&a[RPGCore] &f당신은 ''{0}'' 직업을 선택했습니다!", classConfig.getString("display-name", classId)));
        handleLevelUp(player, data);
    }

    /** 레벨업 처리 */
    public void handleLevelUp(Player player, PlayerData data) {
        FileConfiguration classConfig = classConfigs.get(data.getPlayerClass());
        if (classConfig == null) return;

        int level = data.getLevel();

        // 기본 스탯 + 레벨당 증가치 계산
        applyStat(data, "attack", level, classConfig);
        applyStat(data, "defense", level, classConfig);
        applyStat(data, "max-mana", level, classConfig);
        applyStat(data, "crit-chance", level, classConfig);
        applyStat(data, "crit-damage", level, classConfig);

        // 스킬 해금 안내
        switch (level) {
            case 5 -> player.sendMessage(ChatUtil.format("&b[스킬] &f1번 스킬(우클릭)이 해금되었습니다!"));
            case 10 -> player.sendMessage(ChatUtil.format("&b[스킬] &f2번 스킬(쉬프트+좌클릭)이 해금되었습니다!"));
            case 20 -> player.sendMessage(ChatUtil.format("&b[스킬] &f3번 스킬(쉬프트+우클릭)이 해금되었습니다!"));
        }

        player.sendMessage(ChatUtil.format("&a[레벨업!] &f축하합니다! {0} 레벨을 달성했습니다!", level));
    }

    /** 스탯 적용 헬퍼 */
    private void applyStat(PlayerData data, String statKey, int level, FileConfiguration config) {
        double base = config.getDouble("base-stats." + statKey, 0);
        double perLevel = config.getDouble("stats-per-level." + statKey, 0);
        double value = base + perLevel * (level - 1);

        switch (statKey) {
            case "attack" -> data.setBaseAttack(value);
            case "defense" -> data.setBaseDefense(value);
            case "max-mana" -> data.setBaseMaxMana(value);
            case "crit-chance" -> data.setBaseCritChance(value);
            case "crit-damage" -> data.setBaseCritDamage(value);
        }
    }

    /** 직업 무기 여부 확인 */
    public boolean isClassWeapon(PlayerData data, ItemStack item) {
        if (!data.hasClass() || item == null) return false;
        String weaponClassId = ItemUtil.getNBTString(item, "class_weapon_id");
        return weaponClassId != null && weaponClassId.equals(data.getPlayerClass());
    }

    /** 몹 경험치 반환 */
    public double getExperienceFromMythicMob(MythicMob mob) {
        return mobExpCache.getOrDefault(mob.getInternalName(), 0.0);
    }

    public List<String> getAllClassIds() {
        return new ArrayList<>(classConfigs.keySet());
    }

    public FileConfiguration getClassConfig(String classId) {
        return classConfigs.get(classId);
    }
}
