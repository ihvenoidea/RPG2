package com.mahirung.rpgcore.managers;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

public class PlayerDataManager {

    private final RPGCore plugin;
    // 동시성 문제 해결을 위해 ConcurrentHashMap 사용
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private final DatabaseManager dbManager;

    public PlayerDataManager(RPGCore plugin) {
        this.plugin = plugin;
        this.dbManager = plugin.getDatabaseManager();

        // [상업용 필수] 자동 저장 태스크 (10분마다 실행)
        long interval = 10 * 60 * 20L; 
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveAllPlayerData, interval, interval);
    }

    /**
     * 데이터 로드 (비동기 + 콜백 지원)
     * - PlayerJoinListener에서 사용
     */
    public void loadPlayerData(UUID uuid, Consumer<PlayerData> callback) {
        // 이미 메모리에 로드되어 있다면 즉시 반환
        if (playerDataMap.containsKey(uuid)) {
            if (callback != null) callback.accept(playerDataMap.get(uuid));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = null;
            try {
                // 1. DB 사용 모드
                if (plugin.getConfig().getBoolean("database.enabled") && dbManager != null) {
                    data = dbManager.loadPlayerData(uuid);
                } 
                // 2. 파일 사용 모드 (백업)
                else {
                    File playerFile = new File(plugin.getDataFolder() + "/playerdata", uuid.toString() + ".yml");
                    if (playerFile.exists()) {
                        data = new PlayerData(plugin, uuid, playerFile);
                    } else {
                        data = new PlayerData(plugin, uuid); // 신규 유저
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "데이터 로드 중 치명적 오류: " + uuid, e);
                data = new PlayerData(plugin, uuid); // 오류 시 빈 데이터로 방어
            }

            // 메인 스레드로 복귀하여 맵에 저장 및 콜백 실행
            PlayerData finalData = data;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (finalData != null) {
                    playerDataMap.put(uuid, finalData);
                    if (callback != null) callback.accept(finalData);
                }
            });
        });
    }

    // 단순 로드용 오버로딩 (콜백 없음)
    public void loadPlayerData(UUID uuid) {
        loadPlayerData(uuid, null);
    }

    /**
     * 경험치 지급 및 레벨업 처리
     * - EntityDeathListener에서 사용
     */
    public void addExperience(Player player, double exp) {
        PlayerData data = getPlayerData(player.getUniqueId());
        if (data != null) {
            boolean leveledUp = data.addExperience(exp);
            if (leveledUp) {
                player.sendMessage("§a[RPGCore] 축하합니다! 레벨이 상승했습니다. §f(Lv." + data.getLevel() + ")");
                player.sendTitle("§eLevel Up!", "§f현재 레벨: " + data.getLevel(), 10, 70, 20);
                
                // 레벨업 시 즉시 저장 (데이터 안전성 확보)
                savePlayerDataAsync(player.getUniqueId(), null);
            }
        }
    }

    /**
     * 비동기 단일 저장
     * - PlayerQuitListener 등에서 사용
     */
    public void savePlayerDataAsync(UUID uuid, Consumer<Boolean> callback) {
        PlayerData data = playerDataMap.get(uuid);
        if (data == null) {
            if (callback != null) callback.accept(false);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = false;
            try {
                if (plugin.getConfig().getBoolean("database.enabled") && dbManager != null) {
                    dbManager.savePlayerData(data);
                    success = true;
                } else {
                    File folder = new File(plugin.getDataFolder() + "/playerdata");
                    if (!folder.exists()) folder.mkdirs();
                    
                    File playerFile = new File(folder, uuid.toString() + ".yml");
                    data.saveToFile(playerFile);
                    success = true;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "데이터 저장 실패: " + uuid, e);
            }

            if (callback != null) {
                boolean finalSuccess = success;
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalSuccess));
            }
        });
    }

    /**
     * 전체 데이터 저장 (자동 저장용)
     */
    public void saveAllPlayerData() {
        if (playerDataMap.isEmpty()) return;
        
        plugin.getLogger().info("[AutoSave] 온라인 유저 데이터 저장을 시작합니다...");
        for (PlayerData data : playerDataMap.values()) {
            try {
                if (plugin.getConfig().getBoolean("database.enabled") && dbManager != null) {
                    dbManager.savePlayerData(data);
                } else {
                    // 파일 모드 저장 로직 (생략 가능하지만 안전을 위해 유지)
                    File folder = new File(plugin.getDataFolder() + "/playerdata");
                    if (!folder.exists()) folder.mkdirs();
                    File playerFile = new File(folder, data.getUuid().toString() + ".yml");
                    data.saveToFile(playerFile);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.get(uuid);
    }

    public void unloadPlayerData(UUID uuid) {
        playerDataMap.remove(uuid);
    }

    /**
     * 서버 종료 시 동기 저장 (필수)
     */
    public void shutdown() {
        plugin.getLogger().info("서버 종료: 전체 데이터를 동기화 방식으로 저장합니다.");
        for (PlayerData data : playerDataMap.values()) {
            try {
                if (plugin.getConfig().getBoolean("database.enabled") && dbManager != null) {
                    dbManager.savePlayerData(data);
                } else {
                    File folder = new File(plugin.getDataFolder() + "/playerdata");
                    if (!folder.exists()) folder.mkdirs();
                    File playerFile = new File(folder, data.getUuid().toString() + ".yml");
                    data.saveToFile(playerFile);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "종료 저장 실패: " + data.getUuid(), e);
            }
        }
        playerDataMap.clear();
    }
}