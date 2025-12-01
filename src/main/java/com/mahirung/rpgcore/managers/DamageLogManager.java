package com.mahirung.rpgcore.managers;

import com.mahirung.rpgcore.RPGCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class DamageLogManager {

    private final Map<UUID, Map<UUID, Double>> damageMap = new HashMap<>();

    public DamageLogManager(RPGCore plugin) {
        // 메모리 누수 방지: 주기적으로 죽거나 사라진 몹 데이터 청소
        long interval = plugin.getConfig().getLong("gameplay.memory-cleanup-interval", 300) * 20L;
        Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupLogs, interval, interval);
    }

    public void addDamage(Entity entity, Player player, double damage) {
        damageMap.computeIfAbsent(entity.getUniqueId(), k -> new HashMap<>())
                .merge(player.getUniqueId(), damage, Double::sum);
    }

    public Player getTopDealer(Entity entity) {
        Map<UUID, Double> logs = damageMap.get(entity.getUniqueId());
        if (logs == null || logs.isEmpty()) return null;

        return logs.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> Bukkit.getPlayer(entry.getKey()))
                .orElse(null);
    }

    public void removeLog(Entity entity) {
        damageMap.remove(entity.getUniqueId());
    }

    private void cleanupLogs() {
        // 현재 월드에 존재하지 않는 엔티티(디스폰됨)의 로그 제거
        damageMap.keySet().removeIf(uuid -> Bukkit.getEntity(uuid) == null || !Bukkit.getEntity(uuid).isValid());
    }
}