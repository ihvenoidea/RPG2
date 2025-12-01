package com.mahirung.rpgcore.data;

import com.mahirung.rpgcore.RPGCore;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 플레이어 데이터 객체
 * - 직업, 레벨, 경험치, 스탯, 마나 관리
 * - YML 파일 저장/로드 및 DB 저장 지원
 */
public class PlayerData {

    private final RPGCore plugin;
    private final UUID uuid;

    // --- 1. 핵심 정보 ---
    private String playerClass;
    private int level;
    private double currentExp;
    private double requiredExp;

    // --- 2. 스탯 (Base: 직업/레벨, Bonus: 룬/버프) ---
    private double baseAttack, bonusAttack;
    private double baseMaxMana, bonusMaxMana, currentMana;
    private double baseDefense, bonusDefense;
    private double baseCritChance, bonusCritChance;
    private double baseCritDamage, bonusCritDamage;

    // --- 3. 기타 데이터 ---
    private boolean isNewPlayer;

    // --- 4. 커스텀 NBT (스킨 등 부가 데이터) ---
    private MemorySection customNBT;

    // --- 경험치 계산 상수 ---
    private static final double EXP_BASE = 100.0;
    private static final double EXP_MULTIPLIER = 1.15;
    private static final int MAX_LEVEL = 100;

    /** 신규 유저 생성자 */
    public PlayerData(RPGCore plugin, UUID uuid) {
        this.plugin = plugin;
        this.uuid = uuid;
        this.isNewPlayer = true;

        this.level = 1;
        this.currentExp = 0;
        this.requiredExp = calculateRequiredExpForLevel(1);

        this.baseMaxMana = 100.0;
        this.currentMana = 100.0;

        this.customNBT = new MemoryConfiguration();
    }

    /** 기존 유저 생성자 (파일 로드용) */
    public PlayerData(RPGCore plugin, UUID uuid, File playerFile) {
        this.plugin = plugin;
        this.uuid = uuid;
        this.isNewPlayer = false;

        loadFromFile(playerFile);
        if (this.customNBT == null) this.customNBT = new MemoryConfiguration();
    }

    /** YML 파일에서 데이터 로드 */
    private void loadFromFile(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        this.playerClass = config.getString("class", null);
        this.level = config.getInt("level", 1);
        this.currentExp = config.getDouble("experience.current", 0);
        this.requiredExp = config.getDouble("experience.required", calculateRequiredExpForLevel(this.level));

        this.baseAttack = config.getDouble("stats.base.attack", 0);
        this.bonusAttack = config.getDouble("stats.bonus.attack", 0);
        this.baseMaxMana = config.getDouble("stats.base.max-mana", 100);
        this.bonusMaxMana = config.getDouble("stats.bonus.max-mana", 0);
        this.currentMana = config.getDouble("stats.current-mana", 100);
        this.baseDefense = config.getDouble("stats.base.defense", 0);
        this.bonusDefense = config.getDouble("stats.bonus.defense", 0);
        this.baseCritChance = config.getDouble("stats.base.crit-chance", 0);
        this.bonusCritChance = config.getDouble("stats.bonus.crit-chance", 0);
        this.baseCritDamage = config.getDouble("stats.base.crit-damage", 0);
        this.bonusCritDamage = config.getDouble("stats.bonus.crit-damage", 0);

        this.customNBT = (MemorySection) config.get("custom-nbt", new MemoryConfiguration());
    }

    /** YML 파일에 데이터 저장 */
    public void saveToFile(File file) throws IOException {
        YamlConfiguration config = new YamlConfiguration();

        config.set("class", playerClass);
        config.set("level", level);
        config.set("experience.current", currentExp);
        config.set("experience.required", requiredExp);

        config.set("stats.base.attack", baseAttack);
        config.set("stats.bonus.attack", bonusAttack);
        config.set("stats.base.max-mana", baseMaxMana);
        config.set("stats.bonus.max-mana", bonusMaxMana);
        config.set("stats.current-mana", currentMana);
        config.set("stats.base.defense", baseDefense);
        config.set("stats.bonus.defense", bonusDefense);
        config.set("stats.base.crit-chance", baseCritChance);
        config.set("stats.bonus.crit-chance", bonusCritChance);
        config.set("stats.base.crit-damage", baseCritDamage);
        config.set("stats.bonus.crit-damage", bonusCritDamage);

        config.set("custom-nbt", customNBT);

        config.save(file);
    }

    /** 경험치 추가 및 레벨업 여부 반환 */
    public boolean addExperience(double exp) {
        if (level >= MAX_LEVEL) return false;

        this.currentExp += exp;
        boolean leveledUp = false;

        while (this.currentExp >= this.requiredExp && this.level < MAX_LEVEL) {
            this.currentExp -= this.requiredExp;
            this.level++;
            this.requiredExp = calculateRequiredExpForLevel(this.level);
            leveledUp = true;
        }
        return leveledUp;
    }

    /** 해당 레벨에 필요한 경험치 계산 */
    private double calculateRequiredExpForLevel(int level) {
        if (level >= MAX_LEVEL) return Double.MAX_VALUE;
        return Math.floor(EXP_BASE * Math.pow(EXP_MULTIPLIER, level - 1));
    }

    // --- 마나 관리 ---
    public void spendMana(double amount) {
        this.currentMana = Math.max(0, this.currentMana - amount);
    }

    public void regenMana(double amount) {
        this.currentMana = Math.min(getMaxMana(), this.currentMana + amount);
    }

    // --- Getters ---
    public UUID getUuid() { return uuid; }
    public String getPlayerClass() { return playerClass; }
    public boolean hasClass() { return playerClass != null && !playerClass.isEmpty(); }
    public int getLevel() { return level; }
    public double getCurrentExp() { return currentExp; }
    public double getRequiredExp() { return requiredExp; }
    public boolean isNewPlayer() { return isNewPlayer; }

    // 총합 스탯 반환 (기본 + 보너스)
    public double getAttack() { return baseAttack + bonusAttack; }
    public double getMaxMana() { return baseMaxMana + bonusMaxMana; }
    public double getCurrentMana() { return currentMana; }
    public double getDefense() { return baseDefense + bonusDefense; }
    public double getCritChance() { return baseCritChance + bonusCritChance; }
    public double getCritDamage() { return baseCritDamage + bonusCritDamage; }

    // --- [추가됨] 기본 스탯 Getter (DB 저장 시 사용) ---
    public double getBaseAttack() { return baseAttack; }
    public double getBaseDefense() { return baseDefense; }
    public double getBaseMaxMana() { return baseMaxMana; }

    // --- Setters ---
    public void setPlayerClass(String playerClass) { this.playerClass = playerClass; }
    public void setLevel(int level) {
        this.level = level;
        this.requiredExp = calculateRequiredExpForLevel(level);
    }
    public void setCurrentExp(double currentExp) { this.currentExp = currentExp; }
    public void setRequiredExp(double requiredExp) { this.requiredExp = requiredExp; }
    public void setNewPlayer(boolean newPlayer) { this.isNewPlayer = newPlayer; }

    public void setBaseAttack(double baseAttack) { this.baseAttack = baseAttack; }
    public void setBaseMaxMana(double baseMaxMana) { this.baseMaxMana = baseMaxMana; }
    public void setBaseDefense(double baseDefense) { this.baseDefense = baseDefense; }
    public void setBaseCritChance(double baseCritChance) { this.baseCritChance = baseCritChance; }
    public void setBaseCritDamage(double baseCritDamage) { this.baseCritDamage = baseCritDamage; }
    public void setCurrentMana(double currentMana) { this.currentMana = currentMana; }

    public void setBonusAttack(double bonusAttack) { this.bonusAttack = bonusAttack; }
    public void setBonusMaxMana(double bonusMaxMana) { this.bonusMaxMana = bonusMaxMana; }
    public void setBonusDefense(double bonusDefense) { this.bonusDefense = bonusDefense; }
    public void setBonusCritChance(double bonusCritChance) { this.bonusCritChance = bonusCritChance; }
    public void setBonusCritDamage(double bonusCritDamage) { this.bonusCritDamage = bonusCritDamage; }

    public MemorySection getCustomNBT() { return customNBT; }
}