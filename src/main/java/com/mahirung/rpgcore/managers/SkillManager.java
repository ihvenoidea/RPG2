package com.mahirung.rpgcore.managers;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.data.PlayerData;
import com.mahirung.rpgcore.util.ChatUtil;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 스킬 시스템 매니저
 * - 직업별 스킬 설정 로드
 * - 마나 소모, 쿨타임 관리
 * - MythicMobs 스킬 시전 연동
 */
public class SkillManager {

    private final RPGCore plugin;
    private final PlayerDataManager playerDataManager;
    private final ClassManager classManager;

    private final Map<UUID, Map<String, Long>> playerCooldowns = new HashMap<>();

    public enum SkillType {
        BASIC_ATTACK("basic-attack", 1),
        SKILL_1("skill-1", 5),
        SKILL_2("skill-2", 10),
        SKILL_3("skill-3", 20);

        private final String configKey;
        private final int levelRequirement;

        SkillType(String configKey, int levelRequirement) {
            this.configKey = configKey;
            this.levelRequirement = levelRequirement;
        }

        public String getConfigKey() { return configKey; }
        public int getLevelRequirement() { return levelRequirement; }
    }

    public SkillManager(RPGCore plugin) {
        this.plugin = plugin;
        this.playerDataManager = plugin.getPlayerDataManager();
        this.classManager = plugin.getClassManager();
    }

    /** 스킬 실행 */
    public void executeSkill(Player player, PlayerData playerData, SkillType skillType) {
        FileConfiguration classConfig = classManager.getClassConfig(playerData.getPlayerClass());
        if (classConfig == null) return;

        checkAndCast(player, playerData, classConfig, skillType);
    }

    /** 조건 확인 후 스킬 시전 */
    private void checkAndCast(Player player, PlayerData playerData, FileConfiguration classConfig, SkillType skillType) {
        if (playerData.getLevel() < skillType.getLevelRequirement()) {
            player.sendMessage(ChatUtil.format("&c[스킬] &f레벨 {0} 이상만 사용할 수 있습니다.", skillType.getLevelRequirement()));
            return;
        }

        ConfigurationSection skillSection = classConfig.getConfigurationSection("skills." + skillType.getConfigKey());
        if (skillSection == null) return;

        String mythicSkillId = skillSection.getString("mythic-skill-id");
        double manaCost = skillSection.getDouble("mana-cost", 0);
        double cooldown = skillSection.getDouble("cooldown", 0);

        if (mythicSkillId == null || mythicSkillId.isEmpty()) return;

        long cooldownLeft = getCooldownLeft(player.getUniqueId(), mythicSkillId);
        if (cooldownLeft > 0) {
            player.sendMessage(ChatUtil.format("&c[스킬] &f쿨타임이 {0}초 남았습니다.", String.format("%.1f", cooldownLeft / 1000.0)));
            return;
        }

        if (playerData.getCurrentMana() < manaCost) {
            player.sendMessage(ChatUtil.format("&c[스킬] &f마나가 부족합니다. ({0}/{1})", playerData.getCurrentMana(), manaCost));
            return;
        }

        playerData.spendMana(manaCost);
        setCooldown(player.getUniqueId(), mythicSkillId, cooldown);

        MythicBukkit.inst().getAPIHelper().castSkill(player, mythicSkillId);
    }

    /** 쿨타임 설정 */
    private void setCooldown(UUID uuid, String skillId, double seconds) {
        if (seconds <= 0) return;
        long endTime = System.currentTimeMillis() + (long) (seconds * 1000);
        playerCooldowns.computeIfAbsent(uuid, k -> new HashMap<>()).put(skillId, endTime);
    }

    /** 남은 쿨타임 확인 */
    private long getCooldownLeft(UUID uuid, String skillId) {
        Map<String, Long> cooldowns = playerCooldowns.get(uuid);
        if (cooldowns == null) return 0;
        Long endTime = cooldowns.get(skillId);
        if (endTime == null) return 0;
        long timeLeft = endTime - System.currentTimeMillis();
        if (timeLeft <= 0) {
            cooldowns.remove(skillId);
            return 0;
        }
        return timeLeft;
    }
}
