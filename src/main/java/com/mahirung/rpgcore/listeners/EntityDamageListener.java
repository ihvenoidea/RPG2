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

    private final PlayerDataManager playerDataManager;
    private final DamageSkinManager damageSkinManager;
    private final DamageLogManager damageLogManager;

    private static final double DEFENSE_CONSTANT = 100.0;
    private static final double DAMAGE_PER_ENHANCE_LEVEL = 2.0;
    private static final double SKILL_DAMAGE_THRESHOLD = 5.0; // 스킬/평타 구분 기준점 (바닐라 평타 데미지보다 높게 설정)

    public EntityDamageListener(RPGCore plugin) {
        this.playerDataManager = plugin.getPlayerDataManager();
        this.damageSkinManager = plugin.getDamageSkinManager();
        this.damageLogManager = plugin.getDamageLogManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        // [중요] 데미지 스킨은 항상 표시 (데미지 스킨은 모든 공격에 대해 출력되도록 위치 변경)
        // 데미지 계산 로직의 영향을 받지 않도록 스킨 출력 코드를 분리합니다.

        PlayerData attackerData = playerDataManager.getPlayerData(attacker.getUniqueId());
        if (attackerData == null || !attackerData.hasClass()) {
            // 데미지 스킨을 위해 플레이어 데이터가 없는 경우에도 기본 표시를 원하면 여기서 처리 가능
            damageSkinManager.showDamage(victim, event.getDamage(), false, attacker); // 비RPG 평타에도 스킨 표시
            return;
        }

        // ----------------------------------------------------
        // --- RPG 데미지 계산 (스킬/평타 구분) ---
        // ----------------------------------------------------

        double currentEventDamage = event.getDamage();
        boolean isSkillAttack = currentEventDamage > SKILL_DAMAGE_THRESHOLD 
                                 || event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK;

        double finalDamage = currentEventDamage; // 초기값은 이벤트 데미지로 설정 (스킬이 아닐 경우)
        boolean isCritical = false;

        if (isSkillAttack) {
            // 스킬 공격: RPG 스탯 적용
            PlayerData victimData = (victim instanceof Player) ? playerDataManager.getPlayerData(victim.getUniqueId()) : null;
            
            double baseAttack = attackerData.getAttack();
            ItemStack weapon = attacker.getInventory().getItemInMainHand();
            int enhanceLevel = ItemUtil.getNBTInt(weapon, "enhance_level");
            double enhanceBonus = enhanceLevel * DAMAGE_PER_ENHANCE_LEVEL;
            
            // MythicMobs의 기본 데미지 + RPG 스탯
            double totalAttack = baseAttack + currentEventDamage + enhanceBonus; 

            double critChance = attackerData.getCritChance();
            double critDamage = attackerData.getCritDamage();
            double victimDefense = (victimData != null) ? victimData.getDefense() : 0.0;

            double damageReduction = victimDefense / (victimDefense + DEFENSE_CONSTANT);
            double calculatedDamage = totalAttack * (1.0 - damageReduction);

            isCritical = Math.random() < critChance;
            finalDamage = isCritical ? calculatedDamage * (1.0 + critDamage) : calculatedDamage;

            finalDamage = Math.max(0.0, finalDamage);
        } 
        // else: 평타 공격 (finalDamage는 currentEventDamage(바닐라 데미지)로 유지됩니다. 스탯 미적용)


        // 5. 최종 데미지 적용
        event.setDamage(finalDamage);

        // 6. 데미지 스킨 표시 (계산된 최종 데미지로 표시)
        damageSkinManager.showDamage(victim, finalDamage, isCritical, attacker);

        // 7. 데미지 기여도 기록
        if (!(victim instanceof Player)) {
            damageLogManager.addDamage(victim, attacker, finalDamage);
        }
    }
}