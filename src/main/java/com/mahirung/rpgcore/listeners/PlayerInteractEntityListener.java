package com.mahirung.rpgcore.listeners;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.data.PlayerData;
import com.mahirung.rpgcore.managers.ClassManager;
import com.mahirung.rpgcore.managers.PlayerDataManager;
import com.mahirung.rpgcore.managers.SkillManager;
import com.mahirung.rpgcore.managers.SkillManager.SkillType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class PlayerInteractEntityListener implements Listener {

    private final PlayerDataManager playerDataManager;
    private final SkillManager skillManager;
    private final ClassManager classManager;

    public PlayerInteractEntityListener(RPGCore plugin) {
        this.playerDataManager = plugin.getPlayerDataManager();
        this.skillManager = plugin.getSkillManager();
        this.classManager = plugin.getClassManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // 1. 기본 검증 (오른손 클릭만 처리)
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(EquipmentSlot.HAND);
        if (item == null || item.getType() == Material.AIR) return;

        // 2. RPG 데이터 확인 및 직업 전용 무기 체크
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        if (data == null || !data.hasClass()) return;
        if (!classManager.isClassWeapon(data, item)) return;

        boolean isShift = player.isSneaking();

        // 3. Right-Click Entity 처리 (우클릭은 PlayerInteractEntityEvent로 들어옵니다.)
        if (isShift) {
            // Shift + 우클릭 -> SKILL_3
            skillManager.executeSkill(player, data, SkillType.SKILL_3);
        } else {
            // 일반 우클릭 -> SKILL_1
            skillManager.executeSkill(player, data, SkillType.SKILL_1);
        }
        
        // 4. 이벤트 취소 (바닐라 엔티티 상호작용 방지)
        event.setCancelled(true);
    }
}