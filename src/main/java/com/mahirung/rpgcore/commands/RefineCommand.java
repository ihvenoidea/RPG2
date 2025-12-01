package com.mahirung.rpgcore.commands;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.managers.RefineManager;
import com.mahirung.rpgcore.util.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 재련 관련 명령어 (/refine)
 * - /refine gui : 재련 GUI 열기
 * - /refine start <레시피ID> : 재련 작업 시작
 * - /refine claim : 완료된 아이템 수령
 */
public class RefineCommand implements CommandExecutor, TabCompleter {

    private final RPGCore plugin;
    private final RefineManager refineManager;

    public RefineCommand(RPGCore plugin) {
        this.plugin = plugin;
        this.refineManager = plugin.getRefineManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatUtil.format("&c[RPGCore] &f이 명령어는 플레이어만 사용할 수 있습니다."));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "gui":
                refineManager.openRefineGUI(player);
                break;

            case "start":
                if (args.length < 2) {
                    player.sendMessage(ChatUtil.format("&c[재련] &f레시피 ID를 입력하세요."));
                    return true;
                }
                String recipeId = args[1];
                refineManager.startRefineTask(player, recipeId);
                break;

            case "claim":
                refineManager.handleClaimItem(player);
                break;

            default:
                sendHelp(player);
                break;
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatUtil.format("&a===== [ 재련 명령어 도움말 ] ====="));
        player.sendMessage(ChatUtil.format("&e/refine gui &7- 재련 GUI 열기"));
        player.sendMessage(ChatUtil.format("&e/refine start <레시피ID> &7- 재련 작업 시작"));
        player.sendMessage(ChatUtil.format("&e/refine claim &7- 완료된 아이템 수령"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();
        Player player = (Player) sender;

        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            subs.add("gui");
            subs.add("start");
            subs.add("claim");
            return subs;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return new ArrayList<>(refineManager.getAllRecipeIds());
        }

        return Collections.emptyList();
    }
}
