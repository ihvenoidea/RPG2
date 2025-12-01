package com.mahirung.rpgcore.listeners;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.data.PlayerData;
import com.mahirung.rpgcore.managers.DamageLogManager;
import com.mahirung.rpgcore.managers.DamageSkinManager;
import com.mahirung.rpgcore.managers.PlayerDataManager;
import com.mahirung.rpgcore.util.ItemUtil; // NBT 유틸
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 엔티티 데미지 이벤트 리스너
 * - 강화 레벨에 따른 추가 데미지 적용
 */
public class EntityDamageListener implements Listener {

    private final PlayerDataManager playerDataManager;
    private final DamageSkinManager damageSkinManager;
    private final DamageLogManager damageLogManager;

    private static final double DEFENSE_CONSTANT = 100.0;
    private static final double DAMAGE_PER_ENHANCE_LEVEL = 2.0; // 1강당 데미지 증가량

    public EntityDamageListener(RPGCore plugin) {
        this.playerDataManager = plugin.getPlayerDataManager();
        this.damageSkinManager = plugin.getDamageSkinManager();
        this.damageLogManager = plugin.getDamageLogManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        // 바닐라 평타 무시 (스킬만 적용)
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        PlayerData attackerData = playerDataManager.getPlayerData(attacker.getUniqueId());
        if (attackerData == null || !attackerData.hasClass()) return;

        PlayerData victimData = (victim instanceof Player)
                ? playerDataManager.getPlayerData(victim.getUniqueId())
                : null;

        // 1. 공격력 계산
        double baseAttack = attackerData.getAttack();
        double skillDamage = event.getDamage();
        
        // [추가됨] 무기 강화 추가 데미지
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        int enhanceLevel = ItemUtil.getNBTInt(weapon, "enhance_level");
        double enhanceBonus = enhanceLevel * DAMAGE_PER_ENHANCE_LEVEL;

        double totalAttack = baseAttack + skillDamage + enhanceBonus;

        // 2. 방어/치명타 계산
        double critChance = attackerData.getCritChance();
        double critDamage = attackerData.getCritDamage();
        double victimDefense = (victimData != null) ? victimData.getDefense() : 0.0;

        double damageReduction = victimDefense / (victimDefense + DEFENSE_CONSTANT);
        double calculatedDamage = totalAttack * (1.0 - damageReduction);

        boolean isCritical = Math.random() < critChance;
        double finalDamage = isCritical ? calculatedDamage * (1.0 + critDamage) : calculatedDamage;

        finalDamage = Math.max(0.0, finalDamage);

        // 3. 적용
        event.setDamage(finalDamage);
        damageSkinManager.showDamage(victim, finalDamage, isCritical, attacker);

        if (!(victim instanceof Player)) {
            damageLogManager.addDamage(victim, attacker, finalDamage);
        }
    }
}