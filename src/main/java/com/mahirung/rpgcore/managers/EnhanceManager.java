package com.mahirung.rpgcore.managers;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.data.PlayerData;
import com.mahirung.rpgcore.gui.EnhanceGUI;
import com.mahirung.rpgcore.gui.SuccessionGUI;
import com.mahirung.rpgcore.util.ChatUtil;
import com.mahirung.rpgcore.util.ItemUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.*;

public class EnhanceManager {

    private final RPGCore plugin;
    private final ConfigManager configManager;
    private Economy economy = null;

    private final Map<Integer, EnhanceData> enhanceCache = new HashMap<>();
    
    // 전승 관련 변수
    private int successionPenalty = 1;
    private int successionCostPerLevel = 1000;
    private ItemStack traceItemTemplate;

    public EnhanceManager(RPGCore plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        setupEconomy();
        loadEnhanceConfig();
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public void loadEnhanceConfig() {
        enhanceCache.clear();
        FileConfiguration config = configManager.getEnhancingConfig();
        
        ConfigurationSection levels = config.getConfigurationSection("levels");
        if (levels != null) {
            for (String key : levels.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    double success = levels.getDouble(key + ".success-rate", 0.5);
                    double destroy = levels.getDouble(key + ".destroy-rate", 0.0);
                    int cost = levels.getInt(key + ".cost", 100);
                    enhanceCache.put(level, new EnhanceData(level, success, destroy, cost));
                } catch (NumberFormatException ignored) {}
            }
        }

        // 전승 설정 로드 (흔적 아이템)
        ConfigurationSection succ = config.getConfigurationSection("succession");
        if (succ != null) {
            this.successionPenalty = succ.getInt("level-penalty", 1);
            this.successionCostPerLevel = succ.getInt("cost-per-level", 5000);
            
            String matName = succ.getString("trace-item.material", "NETHER_STAR");
            Material mat = Material.matchMaterial(matName);
            if (mat == null) mat = Material.NETHER_STAR;
            
            traceItemTemplate = new ItemStack(mat);
            ItemMeta meta = traceItemTemplate.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatUtil.format(succ.getString("trace-item.name", "&c흔적")));
                List<String> lore = new ArrayList<>();
                for(String l : succ.getStringList("trace-item.lore")) {
                    lore.add(ChatUtil.format(l));
                }
                meta.setLore(lore);
                traceItemTemplate.setItemMeta(meta);
            }
        }
        
        plugin.getLogger().info("강화 및 전승 데이터 로드 완료.");
    }

    public void openEnhanceGUI(Player player) { new EnhanceGUI(plugin).open(player); }
    public void openSuccessionGUI(Player player) { new SuccessionGUI(plugin).open(player); }

    public void handleGUIClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (!event.getView().getTitle().equals(EnhanceGUI.GUI_TITLE)) return;

        if (event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.PLAYER) {
            event.setCancelled(false);
            return;
        }
        event.setCancelled(true);

        if (slot == EnhanceGUI.EQUIPMENT_SLOT) {
            event.setCancelled(false);
            return;
        }

        if (slot == EnhanceGUI.START_BUTTON_SLOT) {
            ItemStack equipment = event.getInventory().getItem(EnhanceGUI.EQUIPMENT_SLOT);
            
            if (!isValidClassWeapon(player, equipment)) {
                player.sendMessage(ChatUtil.format("&c[강화] &f자신의 직업 무기만 강화할 수 있습니다."));
                return;
            }

            // 강화 시도
            ItemStack result = attemptEnhance(player, equipment);
            
            // [Fix] 고스트 아이템 방지를 위해 1틱 뒤에 GUI 업데이트
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                event.getInventory().setItem(EnhanceGUI.EQUIPMENT_SLOT, result);
                player.updateInventory();
            });
        }

        if (slot == EnhanceGUI.SUCCESSION_BUTTON_SLOT) {
            player.closeInventory();
            openSuccessionGUI(player);
        }
    }

    public void handleSuccessionGUIClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (!event.getView().getTitle().equals(SuccessionGUI.GUI_TITLE)) return;

        if (event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.PLAYER) {
            event.setCancelled(false);
            return;
        }
        event.setCancelled(true);

        if (slot == SuccessionGUI.TARGET_SLOT || slot == SuccessionGUI.TRACE_SLOT) {
            event.setCancelled(false);
            return;
        }

        if (slot == SuccessionGUI.START_BUTTON_SLOT) {
            Inventory inv = event.getInventory();
            ItemStack target = inv.getItem(SuccessionGUI.TARGET_SLOT);
            ItemStack trace = inv.getItem(SuccessionGUI.TRACE_SLOT);
            
            attemptSuccession(player, target, trace, inv);
        }
    }

    private boolean isValidClassWeapon(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null || !data.hasClass()) return false;
        return plugin.getClassManager().isClassWeapon(data, item);
    }

    public ItemStack attemptEnhance(Player player, ItemStack item) {
        if (item == null) return null;

        int currentLevel = ItemUtil.getNBTInt(item, "enhance_level");
        EnhanceData data = enhanceCache.get(currentLevel);
        
        if (data == null) {
            player.sendMessage(ChatUtil.format("&c[강화] &f최고 레벨입니다."));
            return item;
        }

        if (economy != null) {
            if (!economy.has(player, data.getCost())) {
                player.sendMessage(ChatUtil.format("&c[강화] &f돈이 부족합니다."));
                return item;
            }
            economy.withdrawPlayer(player, data.getCost());
        }

        Random random = new Random();
        double roll = random.nextDouble();

        if (roll <= data.getSuccessRate()) {
            int nextLevel = currentLevel + 1;
            item = ItemUtil.setNBTInt(item, "enhance_level", nextLevel);
            updateItemLore(item, nextLevel);
            
            player.sendMessage(ChatUtil.format("&a[강화 성공] &f+{0}강 강화에 성공했습니다!", nextLevel));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        } else if (roll <= data.getSuccessRate() + data.getDestroyRate()) {
            player.sendMessage(ChatUtil.format("&c[강화 실패] &f장비가 파괴되었습니다..."));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 0.5f);
            
            // [중요] 파괴 시 흔적 아이템 지급
            giveTraceItem(player, currentLevel, item);
            
            return null; // null을 리턴해야 GUI에서 사라짐
        } else {
            player.sendMessage(ChatUtil.format("&e[강화 실패] &f강화에 실패했습니다."));
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1f, 1f);
        }
        return item;
    }

    private void attemptSuccession(Player player, ItemStack target, ItemStack trace, Inventory inv) {
        if (target == null || target.getType().isAir() || trace == null || trace.getType().isAir()) {
            player.sendMessage(ChatUtil.format("&c[전승] &f장비와 흔적을 모두 넣어주세요."));
            return;
        }

        int traceLevel = ItemUtil.getNBTInt(trace, "trace_level");
        String originId = ItemUtil.getNBTString(trace, "trace_origin_id");
        
        if (traceLevel <= 0 || originId == null) {
            player.sendMessage(ChatUtil.format("&c[전승] &f유효하지 않은 흔적 아이템입니다."));
            return;
        }

        if (!isValidClassWeapon(player, target)) {
            player.sendMessage(ChatUtil.format("&c[전승] &f직업 전용 무기에만 전승할 수 있습니다."));
            return;
        }
        
        String targetId = ItemUtil.getNBTString(target, "class_weapon_id");
        if (targetId == null || !targetId.equals(originId)) {
            player.sendMessage(ChatUtil.format("&c[전승] &f같은 종류의 장비에만 전승할 수 있습니다."));
            return;
        }

        int targetLevel = ItemUtil.getNBTInt(target, "enhance_level");
        if (targetLevel >= traceLevel) {
            player.sendMessage(ChatUtil.format("&c[전승] &f이미 흔적보다 높은 등급의 장비입니다."));
            return;
        }

        int finalLevel = Math.max(0, traceLevel - successionPenalty);
        double cost = finalLevel * successionCostPerLevel;
        
        if (economy != null) {
            if (!economy.has(player, cost)) {
                player.sendMessage(ChatUtil.format("&c[전승] &f비용이 부족합니다. (필요: " + (int)cost + "원)"));
                return;
            }
            economy.withdrawPlayer(player, cost);
        }

        target = ItemUtil.setNBTInt(target, "enhance_level", finalLevel);
        updateItemLore(target, finalLevel);
        
        trace.setAmount(trace.getAmount() - 1);
        inv.setItem(SuccessionGUI.TRACE_SLOT, trace);
        inv.setItem(SuccessionGUI.TARGET_SLOT, target);

        player.sendMessage(ChatUtil.format("&a[전승 성공] &f능력치가 계승되어 +{0}강이 되었습니다!", finalLevel));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
    }

    private void giveTraceItem(Player player, int level, ItemStack originalItem) {
        if (traceItemTemplate == null) return;
        
        ItemStack trace = traceItemTemplate.clone();
        ItemMeta meta = trace.getItemMeta();
        if (meta != null) {
            String name = meta.getDisplayName().replace("{0}", String.valueOf(level));
            meta.setDisplayName(name);
            
            List<String> lore = new ArrayList<>();
            int recoverLevel = Math.max(0, level - successionPenalty);
            if (meta.hasLore()) {
                for (String l : meta.getLore()) {
                    lore.add(l.replace("{0}", String.valueOf(level)).replace("{1}", String.valueOf(recoverLevel)));
                }
            }
            meta.setLore(lore);
            trace.setItemMeta(meta);
        }
        
        trace = ItemUtil.setNBTInt(trace, "trace_level", level);
        
        String classWeaponId = ItemUtil.getNBTString(originalItem, "class_weapon_id");
        if (classWeaponId != null) {
            trace = ItemUtil.setNBTString(trace, "trace_origin_id", classWeaponId);
        }
        
        player.getInventory().addItem(trace);
        player.sendMessage(ChatUtil.format("&e[전승] &f파괴된 장비의 영혼이 담긴 흔적을 획득했습니다."));
    }
    
    private void updateItemLore(ItemStack item, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        lore.removeIf(l -> l.contains("◆ 강화 등급:"));
        lore.add(0, ChatUtil.format("&6◆ 강화 등급: &e+" + level));
        
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private static class EnhanceData {
        private final int level;
        private final double successRate, destroyRate;
        private final int cost;
        public EnhanceData(int l, double s, double d, int c) { level=l; successRate=s; destroyRate=d; cost=c; }
        public double getSuccessRate() { return successRate; }
        public double getDestroyRate() { return destroyRate; }
        public int getCost() { return cost; }
    }
}