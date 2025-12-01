package com.mahirung.rpgcore.listeners;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.managers.ClassManager;
import com.mahirung.rpgcore.managers.DamageLogManager;
import com.mahirung.rpgcore.managers.PlayerDataManager;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 엔티티 사망 이벤트 리스너
 * - 몹 처치 시 경험치 지급
 * - 아이템 드롭 처리 (데미지 기여도 1위 독식)
 */
public class EntityDeathListener implements Listener {

    private final PlayerDataManager playerDataManager;
    private final ClassManager classManager;
    private final DamageLogManager damageLogManager;

    public EntityDeathListener(RPGCore plugin) {
        this.playerDataManager = plugin.getPlayerDataManager();
        this.classManager = plugin.getClassManager();
        this.damageLogManager = plugin.getDamageLogManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Player killer = victim.getKiller();

        // 1. 데미지 기여도 1등(Top Dealer) 확인
        Player topDealer = damageLogManager.getTopDealer(victim);

        // 2. 보상 받을 대상 결정 (1등 -> 없으면 막타 -> 둘 다 없으면 종료)
        final Player receiver = (topDealer != null) ? topDealer : killer;

        if (receiver == null) {
            damageLogManager.removeLog(victim); // 로그 정리 후 종료
            return;
        }

        // --- 경험치 계산 ---
        double totalExperience;
        Optional<MythicMob> mythicMobOpt = Optional.empty();

        // MythicMobs 인스턴스 확인
        if (MythicBukkit.inst().getMobManager().isActiveMob(victim.getUniqueId())) {
            ActiveMob am = MythicBukkit.inst().getMobManager().getMythicMobInstance(victim);
            if (am != null) {
                mythicMobOpt = Optional.ofNullable(am.getType());
            }
        }

        if (mythicMobOpt.isPresent()) {
            totalExperience = classManager.getExperienceFromMythicMob(mythicMobOpt.get());
        } else {
            totalExperience = event.getDroppedExp();
            event.setDroppedExp(0); // 기본 경험치 드롭 제거
        }

        // --- 아이템 분배 (대상자에게 지급) ---
        handleItemDistribution(event, receiver);

        // --- 경험치 지급 (대상자에게 지급) ---
        playerDataManager.addExperience(receiver, totalExperience);
        
        // --- 로그 정리 ---
        damageLogManager.removeLog(victim);
    }

    /** 아이템 드롭 처리 */
    private void handleItemDistribution(EntityDeathEvent event, Player receiver) {
        List<ItemStack> drops = new ArrayList<>(event.getDrops());
        event.getDrops().clear();

        if (receiver.isOnline()) {
            // 대상자가 온라인이면 인벤토리에 지급, 공간 부족 시 발 밑에 드롭
            receiver.getInventory().addItem(drops.toArray(new ItemStack[0]))
                    .forEach((slot, item) -> receiver.getWorld().dropItemNaturally(receiver.getLocation(), item));
        } else {
            // 대상자가 오프라인이면 몬스터가 죽은 위치에 드롭
            drops.forEach(item -> event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), item));
        }
    }
}