package com.mahirung.rpgcore.commands;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.util.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class MainCommand implements CommandExecutor, TabCompleter {

    private final RPGCore plugin;

    public MainCommand(RPGCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        if (args.length == 0) {
            sender.sendMessage(ChatUtil.format("&a[RPGCore] &f플러그인 버전: " + plugin.getDescription().getVersion()));
            sender.sendMessage(ChatUtil.format("&7/rpgcore reload - 플러그인 설정을 리로드합니다."));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                return handleReloadCommand(sender);
            default:
                sender.sendMessage(ChatUtil.format("&c[RPGCore] &f알 수 없는 명령어입니다."));
                return true;
        }
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("rpgcore.admin.reload")) {
            sender.sendMessage(ChatUtil.format("&c[RPGCore] &f이 명령어를 실행할 권한이 없습니다."));
            return true;
        }

        try {
            plugin.reloadPlugin();
            sender.sendMessage(ChatUtil.format("&a[RPGCore] &f모든 설정을 성공적으로 리로드했습니다."));
        } catch (Exception e) {
            sender.sendMessage(ChatUtil.format("&c[RPGCore] &f설정 리로드 중 오류가 발생했습니다. (콘솔 로그 확인)"));
            plugin.getLogger().severe("설정 리로드 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            if ("reload".startsWith(args[0].toLowerCase()) && sender.hasPermission("rpgcore.admin.reload")) {
                completions.add("reload");
            }
        }
        return completions;
    }
}