package com.mahirung.rpgcore.listeners;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.data.PlayerData;
import com.mahirung.rpgcore.managers.ClassManager;
import com.mahirung.rpgcore.managers.PlayerDataManager;
import com.mahirung.rpgcore.managers.SkillManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 플레이어 상호작용 이벤트 리스너
 * - 직업 전용 무기를 들고 있을 때 클릭 이벤트를 감지하여 스킬 발동
 */
public class PlayerInteractListener implements Listener {

    private final PlayerDataManager playerDataManager;
    private final ClassManager classManager;
    private final SkillManager skillManager;

    public PlayerInteractListener(RPGCore plugin) {
        this.playerDataManager = plugin.getPlayerDataManager();
        this.classManager = plugin.getClassManager();
        this.skillManager = plugin.getSkillManager();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        // 1. 손에 아이템이 없거나 공기면 무시
        if (itemInHand == null || itemInHand.getType() == Material.AIR) return;

        // 2. 플레이어 데이터 확인
        PlayerData playerData = playerDataManager.getPlayerData(player.getUniqueId());
        if (playerData == null || !playerData.hasClass()) return;

        // 3. 직업 전용 무기인지 확인
        if (!classManager.isClassWeapon(playerData, itemInHand)) return;

        boolean isSneaking = player.isSneaking();

        // --- 우클릭 계열 ---
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true); // 기본 동작 차단

            if (isSneaking) {
                skillManager.executeSkill(player, playerData, SkillManager.SkillType.SKILL_3);
            } else {
                skillManager.executeSkill(player, playerData, SkillManager.SkillType.SKILL_1);
            }
        }
        // --- 좌클릭 계열 ---
        else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            if (isSneaking) {
                skillManager.executeSkill(player, playerData, SkillManager.SkillType.SKILL_2);
            } else {
                skillManager.executeSkill(player, playerData, SkillManager.SkillType.BASIC_ATTACK);
            }
        }
    }
}
