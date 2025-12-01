package com.mahirung.rpgcore.listeners;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.data.PlayerData;
import com.mahirung.rpgcore.managers.DamageLogManager;
import com.mahirung.rpgcore.managers.DamageSkinManager;
import com.mahirung.rpgcore.managers.PlayerDataManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * 엔티티 데미지 이벤트 리스너
 */
public class EntityDamageListener implements Listener {

    private final PlayerDataManager playerDataManager;
    private final DamageSkinManager damageSkinManager;
    private final DamageLogManager damageLogManager;

    private static final double DEFENSE_CONSTANT = 100.0;

    public EntityDamageListener(RPGCore plugin) {
        this.playerDataManager = plugin.getPlayerDataManager();
        this.damageSkinManager = plugin.getDamageSkinManager();
        this.damageLogManager = plugin.getDamageLogManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // 1. 공격자가 플레이어인지 확인
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        // 2. 피해자가 LivingEntity인지 확인
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }

        // 3. 공격자 데이터 로드
        PlayerData attackerData = playerDataManager.getPlayerData(attacker.getUniqueId());
        if (attackerData == null || !attackerData.hasClass()) {
            return;
        }

        // 4. 피해자 데이터 로드 (PvP)
        PlayerData victimData = (victim instanceof Player)
                ? playerDataManager.getPlayerData(victim.getUniqueId())
                : null;

        // 5. 커스텀 데미지 계산
        double baseAttack = attackerData.getAttack();
        double critChance = attackerData.getCritChance();
        double critDamage = attackerData.getCritDamage();

        double victimDefense = (victimData != null) ? victimData.getDefense() : 0.0;

        double damageReduction = victimDefense / (victimDefense + DEFENSE_CONSTANT);
        double calculatedDamage = baseAttack * (1.0 - damageReduction);

        boolean isCritical = Math.random() < critChance;
        double finalDamage = isCritical ? calculatedDamage * (1.0 + critDamage) : calculatedDamage;

        finalDamage = Math.max(0.0, finalDamage);

        // 6. 최종 데미지 적용
        event.setDamage(finalDamage);

        // 7. 데미지 스킨 표시
        damageSkinManager.showDamage(victim, finalDamage, isCritical, attacker);

        // 8. 데미지 기여도 기록 (PvE)
        if (!(victim instanceof Player)) {
            damageLogManager.addDamage(victim, attacker, finalDamage);
        }
    }
}