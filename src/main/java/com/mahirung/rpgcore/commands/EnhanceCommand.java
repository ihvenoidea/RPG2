package com.mahirung.rpgcore.commands;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.managers.EnhanceManager;
import com.mahirung.rpgcore.util.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * 장비 강화 GUI를 여는 명령어 (/enhance)
 */
public class EnhanceCommand implements CommandExecutor, TabCompleter {

    private final EnhanceManager enhanceManager;
    private final RPGCore plugin;

    public EnhanceCommand(RPGCore plugin) {
        this.plugin = plugin;
        this.enhanceManager = plugin.getEnhanceManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatUtil.format("&c[RPGCore] &f이 명령어는 플레이어만 사용할 수 있습니다."));
            return true;
        }

        Player player = (Player) sender;

        // 권한 체크
        if (!player.hasPermission("rpgcore.enhance.use")) {
            player.sendMessage(ChatUtil.format("&c[RPGCore] &f이 명령어를 실행할 권한이 없습니다."));
            return true;
        }

        try {
            enhanceManager.openEnhanceGUI(player);
        } catch (Exception e) {
            player.sendMessage(ChatUtil.format("&c[RPGCore] &f강화 GUI를 여는 중 오류가 발생했습니다."));
            plugin.getLogger().severe("[EnhanceCommand] GUI 오류: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // 현재는 별도의 하위 명령어가 없으므로 빈 리스트 반환
        return Collections.emptyList();
    }
}
