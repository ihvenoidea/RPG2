package com.mahirung.rpgcore.listeners;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.managers.PlayerDataManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 플레이어 퇴장 이벤트 리스너
 * - 퇴장 시 PlayerData를 안전하게 저장하고 메모리 캐시에서 제거
 */
public class PlayerQuitListener implements Listener {

    private final PlayerDataManager playerDataManager;
    private final RPGCore plugin;

    public PlayerQuitListener(RPGCore plugin) {
        this.plugin = plugin;
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // PlayerData 비동기 저장 및 캐시 제거
        playerDataManager.savePlayerDataAsync(player.getUniqueId(), success -> {
            if (success) {
                plugin.getLogger().info(player.getName() + "님의 데이터를 성공적으로 저장했습니다.");
            } else {
                plugin.getLogger().warning(player.getName() + "님의 데이터를 저장하는 데 실패했습니다.");
            }

            // 저장 후 캐시에서 제거 (PlayerDataManager에 unloadPlayerData 메서드 필요)
            playerDataManager.unloadPlayerData(player.getUniqueId());
        });
    }
}
