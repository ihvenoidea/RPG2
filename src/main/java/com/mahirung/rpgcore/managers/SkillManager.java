package com.mahirung.rpgcore.managers;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.data.PlayerData;
import com.mahirung.rpgcore.util.ChatUtil;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

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

    public void executeSkill(Player player, PlayerData playerData, SkillType skillType) {
        // MythicMobs 플러그인이 없으면 실행 불가
        if (!plugin.getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
            return;
        }

        FileConfiguration classConfig = classManager.getClassConfig(playerData.getPlayerClass());
        if (classConfig == null) return;

        checkAndCast(player, playerData, classConfig, skillType);
    }

    private void checkAndCast(Player player, PlayerData playerData, FileConfiguration classConfig, SkillType skillType) {
        // 1. 레벨 제한 확인
        if (playerData.getLevel() < skillType.getLevelRequirement()) {
            player.sendMessage(ChatUtil.format("&c[스킬] &f레벨 {0} 이상만 사용할 수 있습니다.", skillType.getLevelRequirement()));
            return;
        }

        // 2. 설정 확인
        String path = "skills." + skillType.getConfigKey();
        ConfigurationSection skillSection = classConfig.getConfigurationSection(path);
        
        if (skillSection == null) {
            // 해당 스킬이 설정되지 않은 경우 조용히 무시 (또는 디버그 메시지)
            return;
        }

        String mythicSkillId = skillSection.getString("mythic-skill-id");
        if (mythicSkillId == null || mythicSkillId.isEmpty()) return;

        double manaCost = skillSection.getDouble("mana-cost", 0);
        double cooldown = skillSection.getDouble("cooldown", 0);

        // 3. 쿨타임 확인
        long cooldownLeft = getCooldownLeft(player.getUniqueId(), mythicSkillId);
        if (cooldownLeft > 0) {
            player.sendMessage(ChatUtil.format("&c[스킬] &f쿨타임이 {0}초 남았습니다.", String.format("%.1f", cooldownLeft / 1000.0)));
            return;
        }

        // 4. 마나 확인
        if (playerData.getCurrentMana() < manaCost) {
            player.sendMessage(ChatUtil.format("&c[스킬] &f마나가 부족합니다. ({0}/{1})", 
                    (int)playerData.getCurrentMana(), (int)manaCost));
            return;
        }

        // 5. 실행 (마나 소모 -> 쿨타임 적용 -> 스킬 시전)
        playerData.spendMana(manaCost);
        setCooldown(player.getUniqueId(), mythicSkillId, cooldown);

        // MythicMobs API 호출
        boolean castSuccess = MythicBukkit.inst().getAPIHelper().castSkill(player, mythicSkillId);
        
        if (!castSuccess) {
            plugin.getLogger().warning("MythicMobs 스킬 시전 실패: " + mythicSkillId);
            player.sendMessage(ChatUtil.format("&c[오류] &f스킬을 시전할 수 없습니다. 관리자에게 문의하세요."));
        }
    }

    private void setCooldown(UUID uuid, String skillId, double seconds) {
        if (seconds <= 0) return;
        long endTime = System.currentTimeMillis() + (long) (seconds * 1000);
        playerCooldowns.computeIfAbsent(uuid, k -> new HashMap<>()).put(skillId, endTime);
    }

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