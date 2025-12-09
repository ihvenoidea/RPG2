package com.mahirung.rpgcore.listeners;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.data.PlayerData;
import com.mahirung.rpgcore.managers.DamageLogManager;
import com.mahirung.rpgcore.managers.DamageSkinManager;
import com.mahirung.rpgcore.managers.PlayerDataManager;
import com.mahirung.rpgcore.util.ItemUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

public class EntityDamageListener implements Listener {

    private final RPGCore plugin;
    private final PlayerDataManager playerDataManager;
    private final DamageSkinManager damageSkinManager;
    private final DamageLogManager damageLogManager;

    private static final double DEFENSE_CONSTANT = 100.0;
    private static final double DAMAGE_PER_ENHANCE_LEVEL = 2.0;

    public EntityDamageListener(RPGCore plugin) {
        this.plugin = plugin;
        this.playerDataManager = plugin.getPlayerDataManager();
        this.damageSkinManager = plugin.getDamageSkinManager();
        this.damageLogManager = plugin.getDamageLogManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        // 1. RPG 데이터 확인
        PlayerData attackerData = playerDataManager.getPlayerData(attacker.getUniqueId());
        if (attackerData == null || !attackerData.hasClass()) return;

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        boolean isClassWeaponHeld = plugin.getClassManager().isClassWeapon(attackerData, weapon);

        // ----------------------------------------------------
        // --- 2. 바닐라 평타 처리 (ENTITY_ATTACK) ---
        // ----------------------------------------------------
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            if (isClassWeaponHeld) {
                // [요청 3 해결] 전용 무기를 들고 평타를 치면 데미지 0으로 취소
                event.setDamage(0.0);
                event.setCancelled(true); 
            }
            // 전용 무기가 아니면 바닐라 데미지 그대로 진행
            return;
        }

        // ----------------------------------------------------
        // --- 3. 스킬 공격 처리 (NOT ENTITY_ATTACK) ---
        // ----------------------------------------------------
        
        // 이 로직은 MythicMobs 스킬 이벤트(CUSTOM, PROJECTILE 등)에만 해당됩니다.
        
        PlayerData victimData = (victim instanceof Player) ? playerDataManager.getPlayerData(victim.getUniqueId()) : null;
        
        double currentEventDamage = event.getDamage();
        double finalDamage;
        boolean isCritical = false;

        // RPG 스탯 계산
        double baseAttack = attackerData.getAttack();
        int enhanceLevel = ItemUtil.getNBTInt(weapon, "enhance_level");
        double enhanceBonus = enhanceLevel * DAMAGE_PER_ENHANCE_LEVEL;
        
        double totalAttack = baseAttack + currentEventDamage + enhanceBonus; 

        double critChance = attackerData.getCritChance();
        double critDamage = attackerData.getCritDamage();
        double victimDefense = (victimData != null) ? victimData.getDefense() : 0.0;

        double damageReduction = victimDefense / (victimDefense + DEFENSE_CONSTANT);
        double calculatedDamage = totalAttack * (1.0 - damageReduction);

        isCritical = Math.random() < critChance;
        finalDamage = isCritical ? calculatedDamage * (1.0 + critDamage) : calculatedDamage;

        finalDamage = Math.max(0.0, finalDamage);


        // 4. 최종 데미지 적용
        event.setDamage(finalDamage);

        // 5. 데미지 스킨 출력 (요청 1 해결)
        damageSkinManager.showDamage(victim, finalDamage, isCritical, attacker);

        // 6. 데미지 기여도 기록
        if (!(victim instanceof Player)) {
            damageLogManager.addDamage(victim, attacker, finalDamage);
        }
    }
}