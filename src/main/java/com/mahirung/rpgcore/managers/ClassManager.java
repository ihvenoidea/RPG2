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
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

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
        
        // [추가] 직업 선택 시 기본 무기 지급
        ItemStack weapon = getClassWeapon(classId);
        if (weapon != null) {
            player.getInventory().addItem(weapon);
            player.sendMessage(ChatUtil.format("&e[아이템] &f기본 무기가 지급되었습니다."));
        }
        
        handleLevelUp(player, data);
    }

    public void handleLevelUp(Player player, PlayerData data) {
        FileConfiguration classConfig = classConfigs.get(data.getPlayerClass());
        if (classConfig == null) return;

        int level = data.getLevel();

        applyStat(data, "attack", level, classConfig);
        applyStat(data, "defense", level, classConfig);
        applyStat(data, "max-mana", level, classConfig);
        applyStat(data, "crit-chance", level, classConfig);
        applyStat(data, "crit-damage", level, classConfig);

        switch (level) {
            case 5 -> player.sendMessage(ChatUtil.format("&b[스킬] &f1번 스킬(우클릭)이 해금되었습니다!"));
            case 10 -> player.sendMessage(ChatUtil.format("&b[스킬] &f2번 스킬(쉬프트+좌클릭)이 해금되었습니다!"));
            case 20 -> player.sendMessage(ChatUtil.format("&b[스킬] &f3번 스킬(쉬프트+우클릭)이 해금되었습니다!"));
        }

        player.sendMessage(ChatUtil.format("&a[레벨업!] &f축하합니다! {0} 레벨을 달성했습니다!", level));
    }

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

    public boolean isClassWeapon(PlayerData data, ItemStack item) {
        if (!data.hasClass() || item == null) return false;
        String weaponClassId = ItemUtil.getNBTString(item, "class_weapon_id");
        return weaponClassId != null && weaponClassId.equals(data.getPlayerClass());
    }

    public double getExperienceFromMythicMob(MythicMob mob) {
        return mobExpCache.getOrDefault(mob.getInternalName(), 0.0);
    }

    public List<String> getAllClassIds() {
        return new ArrayList<>(classConfigs.keySet());
    }

    public FileConfiguration getClassConfig(String classId) {
        return classConfigs.get(classId);
    }

    /** [추가됨] 직업 무기 아이템 생성 및 반환 */
    public ItemStack getClassWeapon(String classId) {
        FileConfiguration config = classConfigs.get(classId);
        if (config == null) return null;

        ConfigurationSection weaponSec = config.getConfigurationSection("weapon");
        if (weaponSec == null) return null;

        String matName = weaponSec.getString("material", "IRON_SWORD");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.IRON_SWORD;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = weaponSec.getString("display-name");
            if (name != null) meta.setDisplayName(ChatUtil.format(name));
            
            List<String> lore = weaponSec.getStringList("lore");
            if (lore != null) {
                List<String> coloredLore = new ArrayList<>();
                for (String l : lore) coloredLore.add(ChatUtil.format(l));
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }

        // NBT 적용
        ConfigurationSection nbtSec = weaponSec.getConfigurationSection("nbt");
        if (nbtSec != null) {
            for (String key : nbtSec.getKeys(false)) {
                String value = nbtSec.getString(key);
                item = ItemUtil.setNBTString(item, key, value);
            }
        }
        return item;
    }
}