package com.mahirung.rpgcore.listeners;

import com.mahirung.rpgcore.RPGCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * 플레이어 이동 이벤트 리스너
 * - (던전/파티 시스템 삭제로 인해 현재 기능 없음)
 */
public class PlayerMoveListener implements Listener {

    private final RPGCore plugin;

    public PlayerMoveListener(RPGCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // 던전 및 파티 관련 로직이 삭제되었습니다.
        // 추후 특정 지역 이동 이벤트가 필요할 경우 여기에 추가하세요.
    }
}