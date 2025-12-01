package com.mahirung.rpgcore.managers;

import com.mahirung.rpgcore.RPGCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 설정 매니저 클래스
 * - 기본 config.yml 및 messages.yml
 * - 커스텀 YML (refining, enhancing, runes)
 * - classes/ 폴더 기반 설정
 */
public class ConfigManager {

    private final RPGCore plugin;

    // 기본 설정
    private FileConfiguration mainConfig;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    // 커스텀 설정
    private FileConfiguration refiningConfig;
    private File refiningFile;

    private FileConfiguration enhancingConfig;
    private File enhancingFile;

    private FileConfiguration runesConfig;
    private File runesFile;

    // 폴더 기반 설정
    private final Map<String, FileConfiguration> classConfigs = new HashMap<>();
    // 던전 관련 Map 삭제됨
    private File classFolder;
    // 던전 관련 File 삭제됨

    public ConfigManager(RPGCore plugin) {
        this.plugin = plugin;

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        plugin.saveDefaultConfig();
        mainConfig = plugin.getConfig();

        loadCustomConfigs();
    }

    /** 커스텀 설정 파일 및 폴더 로드 */
    private void loadCustomConfigs() {
        messagesFile = ensureFile("messages.yml");
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        refiningFile = ensureFile("refining.yml");
        refiningConfig = YamlConfiguration.loadConfiguration(refiningFile);

        enhancingFile = ensureFile("enhancing.yml");
        enhancingConfig = YamlConfiguration.loadConfiguration(enhancingFile);

        runesFile = ensureFile("runes.yml");
        runesConfig = YamlConfiguration.loadConfiguration(runesFile);

        // classes/
        classFolder = ensureFolder("classes", new String[]{"warrior.yml", "mage.yml"});
        loadConfigsFromFolder(classFolder, classConfigs);

        // 던전 로드 로직 삭제됨 (여기서 에러가 발생했었음)
    }

    /** 파일 없으면 리소스에서 복사 */
    private File ensureFile(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            try {
                plugin.saveResource(name, false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("리소스 파일을 찾을 수 없습니다: " + name);
            }
        }
        return file;
    }

    /** 폴더 없으면 생성 및 기본 리소스 복사 */
    private File ensureFolder(String folderName, String[] defaults) {
        File folder = new File(plugin.getDataFolder(), folderName);
        if (!folder.exists()) {
            folder.mkdirs();
            for (String def : defaults) {
                try {
                    plugin.saveResource(folderName + "/" + def, false);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("기본 리소스 파일을 찾을 수 없습니다: " + folderName + "/" + def);
                }
            }
        }
        return folder;
    }

    /** 폴더 내 모든 YML 로드 */
    private void loadConfigsFromFolder(File folder, Map<String, FileConfiguration> configMap) {
        configMap.clear();
        if (folder == null || !folder.exists()) return;

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));

        if (files == null) {
            plugin.getLogger().warning(folder.getName() + " 폴더를 읽는 데 실패했습니다.");
            return;
        }

        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            String configId = file.getName().replace(".yml", "");
            configMap.put(configId, config);
            plugin.getLogger().info(folder.getName() + " 설정 파일 로드: " + file.getName());
        }
    }

    /** 모든 설정 리로드 */
    public void reloadAllConfigs() {
        plugin.reloadConfig();
        mainConfig = plugin.getConfig();

        if (messagesFile != null) messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        if (refiningFile != null) refiningConfig = YamlConfiguration.loadConfiguration(refiningFile);
        if (enhancingFile != null) enhancingConfig = YamlConfiguration.loadConfiguration(enhancingFile);
        if (runesFile != null) runesConfig = YamlConfiguration.loadConfiguration(runesFile);

        loadConfigsFromFolder(classFolder, classConfigs);
        // 던전 리로드 로직 삭제됨
    }

    /** 외부에서 호출하는 reloadConfigs() → 내부적으로 reloadAllConfigs() 실행 */
    public void reloadConfigs() {
        reloadAllConfigs();
    }

    // Getter
    public FileConfiguration getMainConfig() { return mainConfig; }
    public FileConfiguration getMessagesConfig() { return messagesConfig; }
    public FileConfiguration getRefiningConfig() { return refiningConfig; }
    public FileConfiguration getEnhancingConfig() { return enhancingConfig; }
    public FileConfiguration getRunesConfig() { return runesConfig; }

    public Map<String, FileConfiguration> getClassConfigs() { return classConfigs; }
    // 던전 Config Getter 삭제됨
}