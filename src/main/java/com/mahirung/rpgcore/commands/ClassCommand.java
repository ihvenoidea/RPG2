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
        PlayerData playerData = playerDataManager.getPlayerData(player.getUniqueId());

        if (playerData == null) {
            player.sendMessage(ChatUtil.format("&c[RPGCore] &f플레이어 데이터를 불러오는 중입니다. 잠시 후 다시 시도해주세요."));
            return true;
        }

        // --- [추가됨] 1. 직업 초기화 명령어 (/class reset) ---
        if (args.length > 0 && args[0].equalsIgnoreCase("reset")) {
            // 초기화 진행
            resetClass(player, playerData);
            return true;
        }

        // --- 2. 직업 선택 GUI 열기 ---
        if (playerData.hasClass()) {
            player.sendMessage(ChatUtil.format("&c[RPGCore] &f당신은 이미 ''{0}'' 직업을 가지고 있습니다.", playerData.getPlayerClass()));
            player.sendMessage(ChatUtil.format("&7직업을 다시 선택하려면 &f/class reset &7명령어로 초기화하세요."));
            return true;
        }

        // 권한 체크 (직업 선택 시에만)
        if (!player.hasPermission("rpgcore.class.use")) {
            player.sendMessage(ChatUtil.format("&c[RPGCore] &f이 명령어를 실행할 권한이 없습니다."));
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

    /** 플레이어 직업 및 스탯 초기화 로직 */
    private void resetClass(Player player, PlayerData data) {
        if (!data.hasClass()) {
            player.sendMessage(ChatUtil.format("&c[RPGCore] &f초기화할 직업이 없습니다."));
            return;
        }

        // 1. 핵심 데이터 초기화
        String oldClass = data.getPlayerClass();
        data.setPlayerClass(null); // 직업 제거
        data.setLevel(1);          // 레벨 1로 초기화
        data.setCurrentExp(0);     // 경험치 0

        // 2. 스탯 초기화 (기본값으로 복구)
        data.setBaseAttack(0);
        data.setBaseDefense(0);
        data.setBaseMaxMana(100);
        data.setCurrentMana(100);
        data.setBaseCritChance(0);
        data.setBaseCritDamage(0);
        
        // 3. 데이터 저장
        playerDataManager.savePlayerDataAsync(player.getUniqueId(), success -> {
            if (success) {
                player.sendMessage(ChatUtil.format("&a[RPGCore] &f직업(''{0}'')과 스탯이 모두 초기화되었습니다.", oldClass));
            } else {
                player.sendMessage(ChatUtil.format("&c[RPGCore] &f데이터 저장 중 오류가 발생했습니다."));
            }
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return completions;
        }

        if (args.length == 1) {
            // [추가됨] 'reset' 자동완성 추가
            List<String> suggestions = new ArrayList<>();
            suggestions.add("reset");
            
            // 기존 직업 ID 자동완성 (필요 시 유지, 여기서는 명령어 구조상 reset만 있어도 됨)
            // suggestions.addAll(plugin.getClassManager().getAllClassIds()); 

            StringUtil.copyPartialMatches(args[0], suggestions, completions);
            Collections.sort(completions);
        }

        return completions;
    }
}