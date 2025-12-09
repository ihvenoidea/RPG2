package com.mahirung.rpgcore;

import com.mahirung.rpgcore.commands.*;
import com.mahirung.rpgcore.hooks.RPGCoreExpansion;
import com.mahirung.rpgcore.listeners.*;
import com.mahirung.rpgcore.managers.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class RPGCore extends JavaPlugin {

    private static RPGCore instance;

    // 매니저 필드 선언
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private PlayerDataManager playerDataManager;
    private ClassManager classManager;
    private RefineManager refineManager;
    private EnhanceManager enhanceManager;
    private RuneManager runeManager;
    private DamageLogManager damageLogManager;
    private DamageSkinManager damageSkinManager;
    private SkillManager skillManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        if (!checkDependencies()) {
            getLogger().severe("필수 의존성 플러그인을 찾을 수 없습니다! 플러그인을 비활성화합니다.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        configManager = new ConfigManager(this);

        if (getConfig().getBoolean("database.enabled")) {
            try {
                databaseManager = new DatabaseManager(this);
            } catch (Exception e) {
                getLogger().severe("데이터베이스 연결에 실패했습니다. 플러גי인을 비활성화합니다.");
                e.printStackTrace();
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }

        loadManagers();
        registerListeners(); // [수정] 이 메서드에서 새 리스너를 등록합니다.
        registerCommands();

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new RPGCoreExpansion(this).register();
            getLogger().info("PlaceholderAPI와 성공적으로 연동되었습니다.");
        }

        getLogger().info("RPGCore (Commercial Edition) 플러그인이 성공적으로 활성화되었습니다.");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.shutdown();
        }
        
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        getLogger().info("RPGCore 플러그인이 비활성화되었습니다.");
    }

    public void reloadPlugin() {
        reloadConfig();
        if (configManager != null) {
            configManager.reloadConfigs();
        }
        
        if (damageSkinManager != null) {
            damageSkinManager.loadDamageSkins();
        }
        if (refineManager != null) {
            refineManager.loadRefiningRecipes();
        }
        if (enhanceManager != null) {
            enhanceManager.loadEnhanceConfig();
        }
        if (runeManager != null) {
            runeManager.loadRuneConfig();
        }
        if (classManager != null) {
            classManager.loadClasses();
        }
        getLogger().info("RPGCore 설정을 리로드했습니다.");
    }

    private boolean checkDependencies() {
        String[] dependencies = {"MythicMobs", "Vault", "ItemsAdder"};
        boolean allFound = true;
        for (String dep : dependencies) {
            Plugin plugin = getServer().getPluginManager().getPlugin(dep);
            if (plugin == null || !plugin.isEnabled()) {
                getLogger().severe("필수 의존성 " + dep + "이(가) 없습니다.");
                allFound = false;
            }
        }
        return allFound;
    }

    private void loadManagers() {
        playerDataManager = new PlayerDataManager(this);
        classManager = new ClassManager(this);
        refineManager = new RefineManager(this);
        enhanceManager = new EnhanceManager(this);
        runeManager = new RuneManager(this);
        damageLogManager = new DamageLogManager(this);
        damageSkinManager = new DamageSkinManager(this);
        skillManager = new SkillManager(this);
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerJoinListener(this), this);
        pm.registerEvents(new PlayerQuitListener(this), this);
        pm.registerEvents(new InventoryCloseListener(this), this);
        pm.registerEvents(new InventoryClickListener(this), this);
        pm.registerEvents(new InventoryDragListener(this), this);
        pm.registerEvents(new EntityDamageListener(this), this);
        pm.registerEvents(new EntityDeathListener(this), this);
        pm.registerEvents(new PlayerMoveListener(this), this); 
        pm.registerEvents(new PlayerDropItemListener(this), this);
        
        // [수정] Interact 리스너들
        pm.registerEvents(new PlayerInteractListener(this), this);
        pm.registerEvents(new PlayerInteractEntityListener(this), this); // [신규] Entity 클릭 리스너 등록
        pm.registerEvents(new NPCInteractListener(this), this);
    }

    private void registerCommands() {
        getCommand("class").setExecutor(new ClassCommand(this));
        getCommand("refine").setExecutor(new RefineCommand(this));
        getCommand("enhance").setExecutor(new EnhanceCommand(this));
        getCommand("damageskin").setExecutor(new DamageSkinCommand(this));
        getCommand("rpgcore").setExecutor(new RPGCoreCommand(this));
    }

    // --- Getters ---
    public static RPGCore getInstance() { return instance; }
    
    public ConfigManager getConfigManager() { return configManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public ClassManager getClassManager() { return classManager; }
    public RefineManager getRefineManager() { return refineManager; }
    public EnhanceManager getEnhanceManager() { return enhanceManager; }
    public RuneManager getRuneManager() { return runeManager; }
    public DamageLogManager getDamageLogManager() { return damageLogManager; }
    public DamageSkinManager getDamageSkinManager() { return damageSkinManager; }
    public SkillManager getSkillManager() { return skillManager; }
}