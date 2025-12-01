package com.mahirung.rpgcore.commands;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.data.PlayerData;
import com.mahirung.rpgcore.gui.ClassSelectorGUI; 
import com.mahirung.rpgcore.managers.PlayerDataManager;
import com.mahirung.rpgcore.util.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClassCommand implements CommandExecutor, TabCompleter {

    private final RPGCore plugin;
    private final PlayerDataManager playerDataManager;

    public ClassCommand(RPGCore plugin) {
        this.plugin = plugin;
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatUtil.format("&c[RPGCore] &f이 명령어는 플레이어만 사용할 수 있습니다."));
            return true;
        }

        Player player = (Player) sender;

        // 권한 체크
        if (!player.hasPermission("rpgcore.class.use")) {
            player.sendMessage(ChatUtil.format("&c[RPGCore] &f이 명령어를 실행할 권한이 없습니다."));
            return true;
        }

        PlayerData playerData = playerDataManager.getPlayerData(player.getUniqueId());

        if (playerData == null) {
            player.sendMessage(ChatUtil.format("&c[RPGCore] &f플레이어 데이터를 불러오는 중입니다. 잠시 후 다시 시도해주세요."));
            return true;
        }

        if (playerData.hasClass()) {
            player.sendMessage(ChatUtil.format("&c[RPGCore] &f당신은 이미 ''{0}'' 직업을 가지고 있습니다.", playerData.getPlayerClass()));
            return true;
        }

        try {
            new ClassSelectorGUI(plugin).open(player);
        } catch (Exception e) {
            player.sendMessage(ChatUtil.format("&c[RPGCore] &fGUI를 여는 중 오류가 발생했습니다. 관리자에게 문의하세요."));
            plugin.getLogger().severe("[ClassCommand] GUI 오류: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return completions;
        }

        if (args.length == 1) {
            // 직업 ID 자동완성 (ClassManager에서 가져오기)
            List<String> classIds = plugin.getClassManager().getAllClassIds();
            StringUtil.copyPartialMatches(args[0], classIds, completions);
            Collections.sort(completions);
        }

        return completions;
    }
}
