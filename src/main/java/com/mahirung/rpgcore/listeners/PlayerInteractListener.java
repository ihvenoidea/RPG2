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
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class PlayerInteractListener implements Listener {

    private final PlayerDataManager playerDataManager;
    private final SkillManager skillManager;
    private final ClassManager classManager;

    public PlayerInteractListener(RPGCore plugin) {
        this.playerDataManager = plugin.getPlayerDataManager();
        this.skillManager = plugin.getSkillManager();
        this.classManager = plugin.getClassManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 1. 기본 검증 (오른손 클릭만 처리, 아이템 없으면 무시)
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        // 2. 데이터 로드
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        if (data == null || !data.hasClass()) return;

        // 3. 직업 전용 무기인지 확인
        if (!classManager.isClassWeapon(data, item)) return;

        Action action = event.getAction();
        boolean isShift = player.isSneaking();

        // 4. 조작법에 따른 스킬 매핑
        // - 좌클릭: 평타 (BASIC_ATTACK) -> 보통 EntityDamageListener에서 처리하지만, 스킬 평타라면 여기서 처리 가능
        // - 우클릭: 1번 스킬 (SKILL_1)
        // - 쉬프트 + 좌클릭: 2번 스킬 (SKILL_2)
        // - 쉬프트 + 우클릭: 3번 스킬 (SKILL_3)

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            if (isShift) {
                // Shift + 좌클릭 -> SKILL_2
                skillManager.executeSkill(player, data, SkillType.SKILL_2);
            } else {
                // 일반 좌클릭 -> 기본 공격 (MythicMobs 스킬로 평타를 대체하고 싶을 때 사용)
                // 만약 바닐라 공격을 유지하려면 이 부분은 비워두세요.
                skillManager.executeSkill(player, data, SkillType.BASIC_ATTACK);
            }
        } 
        else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            if (isShift) {
                // Shift + 우클릭 -> SKILL_3
                skillManager.executeSkill(player, data, SkillType.SKILL_3);
            } else {
                // 일반 우클릭 -> SKILL_1
                skillManager.executeSkill(player, data, SkillType.SKILL_1);
            }
        }
    }
}