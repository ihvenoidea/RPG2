package com.mahirung.rpgcore.managers;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.util.ChatUtil;
import com.mahirung.rpgcore.util.ItemUtil;
import dev.lone.itemsadder.api.CustomStack; // ItemsAdder API
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class RuneManager {

    private final RPGCore plugin;
    private final ConfigManager configManager;

    private final Map<String, RuneData> runeCache = new HashMap<>();
    private String runeRemovalItemId = null;

    public static final String RUNE_LIST_NBT_KEY = "rpgcore_runes_list";
    public static final String ITEMSADDER_NBT_KEY = "itemsadder_id";

    public RuneManager(RPGCore plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        loadRuneConfig();
    }

    public void loadRuneConfig() {
        runeCache.clear();
        FileConfiguration config = configManager.getRunesConfig();
        ConfigurationSection runesSection = config.getConfigurationSection("runes");
        if (runesSection != null) {
            for (String runeIdKey : runesSection.getKeys(false)) {
                ConfigurationSection runeConfig = runesSection.getConfigurationSection(runeIdKey);
                String itemsAdderId = runeConfig.getString("itemsadder-id");
                if (itemsAdderId == null) continue;

                List<String> applicableTo = runeConfig.getStringList("applicable-to");
                Map<String, Double> stats = new HashMap<>();
                ConfigurationSection statsSection = runeConfig.getConfigurationSection("stats");
                if (statsSection != null) {
                    for (String statKey : statsSection.getKeys(false)) {
                        stats.put(statKey, statsSection.getDouble(statKey));
                    }
                }
                runeCache.put(runeIdKey, new RuneData(runeIdKey, itemsAdderId, applicableTo, stats));
            }
        }
        runeRemovalItemId = config.getString("removal-item.itemsadder-id");
        plugin.getLogger().info(runeCache.size() + "개의 룬 로드 완료.");
    }

    public RuneData getRuneData(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        String id = ItemUtil.getNBTString(item, ITEMSADDER_NBT_KEY);
        if (id == null) return null;
        for (RuneData data : runeCache.values()) {
            if (data.getItemsAdderId().equals(id)) return data;
        }
        return null;
    }

    public boolean isRune(ItemStack item) { return getRuneData(item) != null; }

    public boolean isRuneRemovalItem(ItemStack item) {
        if (item == null || runeRemovalItemId == null) return false;
        String id = ItemUtil.getNBTString(item, ITEMSADDER_NBT_KEY);
        return runeRemovalItemId.equals(id);
    }

    public ItemStack applyRune(Player player, ItemStack runeItem, ItemStack equipmentItem) {
        RuneData runeData = getRuneData(runeItem);
        if (runeData == null) return null;
        if (equipmentItem == null || equipmentItem.getType().isAir()) return null;

        final ItemStack checkItem = equipmentItem;
        boolean canApply = runeData.getApplicableTo().stream()
                .anyMatch(mat -> checkItem.getType().name().contains(mat.toUpperCase()));

        if (!canApply) {
            player.sendMessage(ChatUtil.format("&c[룬] &f장착 불가능한 장비입니다."));
            return null;
        }

        List<String> currentRunes = ItemUtil.getNBTStringList(equipmentItem, RUNE_LIST_NBT_KEY);
        if (currentRunes.size() >= 2) {
            player.sendMessage(ChatUtil.format("&c[룬] &f슬롯이 가득 찼습니다."));
            return null;
        }

        currentRunes.add(runeData.getId());
        equipmentItem = ItemUtil.setNBTStringList(equipmentItem, RUNE_LIST_NBT_KEY, currentRunes);
        
        player.sendMessage(ChatUtil.format("&a[룬] &f장착 성공: " + runeData.getId()));
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.5f);
        return equipmentItem;
    }

    public ItemStack removeRune(Player player, ItemStack removalItem, ItemStack equipmentItem) {
        if (!isRuneRemovalItem(removalItem)) return null;
        List<String> currentRunes = ItemUtil.getNBTStringList(equipmentItem, RUNE_LIST_NBT_KEY);
        if (currentRunes.isEmpty()) {
            player.sendMessage(ChatUtil.format("&c[룬] &f장착된 룬이 없습니다."));
            return null;
        }
        String removed = currentRunes.remove(currentRunes.size() - 1);
        equipmentItem = ItemUtil.setNBTStringList(equipmentItem, RUNE_LIST_NBT_KEY, currentRunes);
        player.sendMessage(ChatUtil.format("&e[룬] &f제거 완료: " + removed));
        player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1f, 1f);
        return equipmentItem;
    }

    /** 룬 아이템 생성 (ItemsAdder 연동) */
    public ItemStack getRuneItem(String runeId) {
        RuneData data = runeCache.get(runeId);
        if (data == null) return null;
        
        CustomStack stack = CustomStack.getInstance(data.getItemsAdderId());
        if (stack != null) {
            return stack.getItemStack();
        }
        return null;
    }
    
    public Set<String> getAllRuneIds() { return runeCache.keySet(); }

    public static class RuneData {
        private final String id;
        private final String itemsAdderId;
        private final List<String> applicableTo;
        private final Map<String, Double> stats;
        
        public RuneData(String id, String iaId, List<String> app, Map<String, Double> stats) { 
            this.id = id; 
            this.itemsAdderId = iaId;
            this.applicableTo = app; 
            this.stats = stats; 
        }
        public String getId() { return id; }
        public String getItemsAdderId() { return itemsAdderId; }
        public List<String> getApplicableTo() { return applicableTo; }
    }
}