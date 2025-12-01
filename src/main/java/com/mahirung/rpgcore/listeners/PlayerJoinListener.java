package com.mahirung.rpgcore.listeners;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.data.PlayerData;
import com.mahirung.rpgcore.managers.PlayerDataManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final RPGCore plugin;
    private final PlayerDataManager playerDataManager;

    public PlayerJoinListener(RPGCore plugin) {
        this.plugin = plugin;
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 비동기로 데이터 로드 후, 콜백으로 환영 메시지 출력
        playerDataManager.loadPlayerData(player.getUniqueId(), (data) -> {
            if (data != null) {
                if (data.isNewPlayer()) {
                    player.sendMessage("§e[RPGCore] §f서버에 오신 것을 환영합니다! /class 로 직업을 선택하세요.");
                } else {
                    player.sendMessage("§a[RPGCore] §f데이터가 성공적으로 로드되었습니다. (Lv." + data.getLevel() + ")");
                }
                
                // 필요하다면 여기서 체력/스피드 등 스탯 초기화 로직 추가 가능
                // updatePlayerStats(player, data);
            } else {
                player.sendMessage("§c[RPGCore] §f데이터를 불러오는 중 오류가 발생했습니다.");
            }
        });
    }
}