package com.mahirung.rpgcore.commands;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.data.PlayerData;
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
 * RPGCore 메인 관리 명령어 (/rpgcore)
 * - /rpgcore reload : 설정 리로드
 * - /rpgcore version : 버전 확인
 * - /rpgcore stats : 내 스탯 확인 (추가됨)
 * - /rpgcore help : 도움말
 */
public class RPGCoreCommand implements CommandExecutor, TabCompleter {

    private final RPGCore plugin;

    public RPGCoreCommand(RPGCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload":
                if (!sender.hasPermission("rpgcore.admin.reload")) {
                    sender.sendMessage(ChatUtil.format("&c[RPGCore] &f이 명령어를 실행할 권한이 없습니다."));
                    return true;
                }
                plugin.reloadPlugin();
                sender.sendMessage(ChatUtil.format("&a[RPGCore] &f설정을 리로드했습니다."));
                break;

            case "version":
                sender.sendMessage(ChatUtil.format("&a[RPGCore] &f버전: {0}", plugin.getDescription().getVersion()));
                break;

            // [추가됨] 스탯 확인 명령어
            case "stats":
            case "stat":
            case "info":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatUtil.format("&c플레이어만 사용할 수 있습니다."));
                    return true;
                }
                showStats(player);
                break;

            case "help":
            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    /** 플레이어 스탯 정보 출력 */
    private void showStats(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(ChatUtil.format("&c데이터를 불러오는 중입니다. 잠시 후 다시 시도해주세요."));
            return;
        }

        player.sendMessage(ChatUtil.format("&8&m                                       "));
        player.sendMessage(ChatUtil.format("  &6&l[ &e" + player.getName() + "님의 정보 &6&l]"));
        player.sendMessage("");
        player.sendMessage(ChatUtil.format("  &f직업: &e" + (data.hasClass() ? data.getPlayerClass() : "무직")));
        player.sendMessage(ChatUtil.format("  &f레벨: &aLv." + data.getLevel()));
        player.sendMessage(ChatUtil.format("  &f경험치: &7" + String.format("%.1f", data.getCurrentExp()) + " / " + String.format("%.1f", data.getRequiredExp())));
        player.sendMessage("");
        player.sendMessage(ChatUtil.format("  &c&l⚡ 공격력: &f" + String.format("%.1f", data.getAttack())));
        player.sendMessage(ChatUtil.format("  &9&l🛡 방어력: &f" + String.format("%.1f", data.getDefense())));
        player.sendMessage(ChatUtil.format("  &b&l💧 마나: &f" + String.format("%.0f", data.getCurrentMana()) + " / " + String.format("%.0f", data.getMaxMana())));
        player.sendMessage("");
        player.sendMessage(ChatUtil.format("  &4💥 치명타 확률: &f" + String.format("%.1f", data.getCritChance() * 100) + "%"));
        player.sendMessage(ChatUtil.format("  &4💥 치명타 피해: &f" + String.format("%.1f", data.getCritDamage() * 100) + "%"));
        player.sendMessage(ChatUtil.format("&8&m                                       "));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatUtil.format("&a===== [ RPGCore 명령어 도움말 ] ====="));
        sender.sendMessage(ChatUtil.format("&e/rpgcore stats &7- 내 스탯 정보 확인"));
        sender.sendMessage(ChatUtil.format("&e/rpgcore reload &7- 설정 리로드 (관리자)"));
        sender.sendMessage(ChatUtil.format("&e/rpgcore version &7- 플러그인 버전 확인"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            subs.add("stats"); // 탭 완성 추가
            subs.add("reload");
            subs.add("version");
            subs.add("help");
            return subs;
        }
        return Collections.emptyList();
    }
}