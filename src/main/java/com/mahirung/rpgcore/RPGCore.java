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
    private DatabaseManager databaseManager; // [추가] DB 매니저
    private PlayerDataManager playerDataManager;
    private ClassManager classManager;
    private RefineManager refineManager;
    private EnhanceManager enhanceManager;
    private RuneManager runeManager;
    private DamageLogManager damageLogManager; // [추가] 데미지 기여도/메모리 관리
    private DamageSkinManager damageSkinManager;
    private SkillManager skillManager;

    @Override
    public void onEnable() {
        instance = this;

        // 1. 기본 설정 파일 로드 (config.yml 존재 확인)
        saveDefaultConfig();

        // 2. 의존성 플러그인 확인
        if (!checkDependencies()) {
            getLogger().severe("필수 의존성 플러그인을 찾을 수 없습니다! 플러그인을 비활성화합니다.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 3. Config 매니저 초기화
        configManager = new ConfigManager(this);

        // 4. 데이터베이스 연결 (설정에 따라 활성화)
        if (getConfig().getBoolean("database.enabled")) {
            try {
                databaseManager = new DatabaseManager(this);
            } catch (Exception e) {
                getLogger().severe("데이터베이스 연결에 실패했습니다. 플러그인을 비활성화합니다.");
                e.printStackTrace();
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }

        // 5. 나머지 매니저 및 리스너/명령어 로드
        loadManagers();
        registerListeners();
        registerCommands();

        // 6. PlaceholderAPI 연동 (PAPI가 서버에 있을 경우에만 등록)
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new RPGCoreExpansion(this).register();
            getLogger().info("PlaceholderAPI와 성공적으로 연동되었습니다.");
        }

        getLogger().info("RPGCore (Commercial Edition) 플러그인이 성공적으로 활성화되었습니다.");
    }

    @Override
    public void onDisable() {
        // 플레이어 데이터 안전 저장
        if (playerDataManager != null) {
            playerDataManager.shutdown(); // 동기 방식으로 전체 저장
        }
        
        // 데이터베이스 연결 종료
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        getLogger().info("RPGCore 플러그인이 비활성화되었습니다.");
    }

    public void reloadPlugin() {
        // 설정 리로드
        reloadConfig();
        if (configManager != null) {
            configManager.reloadConfigs();
        }
        
        // 각 매니저 리로드 로직
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
        // 파티/던전 제거로 FastAsyncWorldEdit 제거됨
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
        // DatabaseManager는 onEnable에서 먼저 로드됨
        
        // PlayerDataManager는 내부에서 DBManager를 사용하므로 순서 주의
        playerDataManager = new PlayerDataManager(this);
        
        classManager = new ClassManager(this);
        refineManager = new RefineManager(this);
        enhanceManager = new EnhanceManager(this);
        runeManager = new RuneManager(this);
        
        // 데미지 로그 매니저 (메모리 누수 방지 포함)
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
        pm.registerEvents(new PlayerMoveListener(this), this); // 빈 껍데기지만 유지
        pm.registerEvents(new PlayerDropItemListener(this), this);
        pm.registerEvents(new PlayerInteractListener(this), this);
        pm.registerEvents(new NPCInteractListener(this), this);
    }

    private void registerCommands() {
        getCommand("class").setExecutor(new ClassCommand(this));
        getCommand("refine").setExecutor(new RefineCommand(this));
        getCommand("enhance").setExecutor(new EnhanceCommand(this));
        // party, dungeon 명령어 제거됨
        getCommand("damageskin").setExecutor(new DamageSkinCommand(this));
        getCommand("rpgcore").setExecutor(new RPGCoreCommand(this));
    }

    // --- Getters ---
    public static RPGCore getInstance() { return instance; }
    
    public ConfigManager getConfigManager() { return configManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; } // [추가]
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public ClassManager getClassManager() { return classManager; }
    public RefineManager getRefineManager() { return refineManager; }
    public EnhanceManager getEnhanceManager() { return enhanceManager; }
    public RuneManager getRuneManager() { return runeManager; }
    public DamageLogManager getDamageLogManager() { return damageLogManager; } // [추가]
    public DamageSkinManager getDamageSkinManager() { return damageSkinManager; }
    public SkillManager getSkillManager() { return skillManager; }
}