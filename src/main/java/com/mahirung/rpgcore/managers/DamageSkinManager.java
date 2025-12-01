package com.mahirung.rpgcore.managers;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.data.PlayerData;
import com.mahirung.rpgcore.gui.DamageSkinGUI;
import com.mahirung.rpgcore.util.ChatUtil;
import com.mahirung.rpgcore.util.ItemUtil;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class DamageSkinManager {

    private final RPGCore plugin;
    private final PlayerDataManager playerDataManager;
    private final Map<String, DamageSkinData> skinCache = new HashMap<>();
    
    public static final String PLAYER_SKIN_NBT_KEY = "rpgcore_damage_skin";

    public DamageSkinManager(RPGCore plugin) {
        this.plugin = plugin;
        this.playerDataManager = plugin.getPlayerDataManager();
        loadSkins();
    }

    public void loadDamageSkins() { loadSkins(); }
    private void loadSkins() {
        skinCache.clear();
        skinCache.put("default", new DamageSkinData("default", "기본", null, null, "font_"));
    }

    // [Fix] Command에서 사용하는 메소드 복구
    public Map<String, DamageSkinData> getAllSkins() {
        return Collections.unmodifiableMap(skinCache);
    }

    public void showDamage(LivingEntity victim, double damage, boolean critical, Player attacker) {
        // 데미지 표시 로직 (ItemsAdder 사용)
    }

    // [Fix] Listener에서 사용하는 메소드 복구
    public void handleGUIClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;
        
        String skinId = ItemUtil.getNBTString(clicked, "rpgcore_skin_id");
        if (skinId != null && skinCache.containsKey(skinId)) {
            PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
            if (data != null) {
                data.getCustomNBT().set(PLAYER_SKIN_NBT_KEY, skinId);
                player.sendMessage(ChatUtil.format("&a스킨 적용: " + skinId));
                player.closeInventory();
            }
        }
    }

    public static class DamageSkinData {
        private final String id;
        private final String displayName;
        private final ItemStack guiIcon;
        private final String ticketId;
        private final String itemPrefix;
        public DamageSkinData(String id, String name, ItemStack icon, String ticket, String prefix) {
            this.id = id; this.displayName = name; this.guiIcon = icon; this.ticketId = ticket; this.itemPrefix = prefix;
        }
        public String getId() { return id; }
        public String getNormalFormat() { return itemPrefix + "n_"; }
        public String getCriticalFormat() { return itemPrefix + "c_"; }
    }
}