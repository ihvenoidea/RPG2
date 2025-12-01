package com.mahirung.rpgcore.commands;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.data.PlayerData;
import com.mahirung.rpgcore.managers.ClassManager;
import com.mahirung.rpgcore.managers.RuneManager;
import com.mahirung.rpgcore.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RPGCore 메인 명령어
 * - 유저용: stats, help
 * - 관리자용: reload, inspect, setlevel, give, save
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

        // 1. 일반 유저 명령어
        if (sub.equals("stats") || sub.equals("stat") || sub.equals("info")) {
            if (sender instanceof Player player) {
                showStats(sender, player);
            } else {
                sender.sendMessage("플레이어만 사용 가능합니다.");
            }
            return true;
        }

        // 2. 관리자 명령어 권한 체크
        if (!sender.hasPermission("rpgcore.admin")) {
            sender.sendMessage(ChatUtil.format("&c[RPGCore] &f권한이 없습니다."));
            return true;
        }

        switch (sub) {
            case "reload":
                plugin.reloadPlugin();
                sender.sendMessage(ChatUtil.format("&a[RPGCore] &f모든 설정이 리로드되었습니다."));
                break;

            case "version":
                sender.sendMessage(ChatUtil.format("&a[RPGCore] &f버전: " + plugin.getDescription().getVersion()));
                break;

            case "inspect": // 다른 유저 정보 확인
                if (args.length < 2) {
                    sender.sendMessage(ChatUtil.format("&c사용법: /rpgcore inspect <닉네임>"));
                    return true;
                }
                Player targetInspect = Bukkit.getPlayer(args[1]);
                if (targetInspect == null) {
                    sender.sendMessage(ChatUtil.format("&c접속하지 않은 플레이어입니다."));
                    return true;
                }
                showStats(sender, targetInspect);
                break;

            case "setlevel": // 레벨 설정
                if (args.length < 3) {
                    sender.sendMessage(ChatUtil.format("&c사용법: /rpgcore setlevel <닉네임> <레벨>"));
                    return true;
                }
                Player targetLevel = Bukkit.getPlayer(args[1]);
                if (targetLevel == null) {
                    sender.sendMessage(ChatUtil.format("&c접속하지 않은 플레이어입니다."));
                    return true;
                }
                try {
                    int level = Integer.parseInt(args[2]);
                    PlayerData data = plugin.getPlayerDataManager().getPlayerData(targetLevel.getUniqueId());
                    if (data != null) {
                        data.setLevel(level);
                        data.setCurrentExp(0);
                        // 레벨에 따른 스탯 재계산 (ClassManager 이용)
                        plugin.getClassManager().handleLevelUp(targetLevel, data);
                        sender.sendMessage(ChatUtil.format("&a[관리자] &f" + targetLevel.getName() + "님의 레벨을 " + level + "로 설정했습니다."));
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatUtil.format("&c숫자를 입력해주세요."));
                }
                break;

            case "save": // 강제 저장
                if (args.length < 2) {
                    sender.sendMessage(ChatUtil.format("&c사용법: /rpgcore save <닉네임>"));
                    return true;
                }
                Player targetSave = Bukkit.getPlayer(args[1]);
                if (targetSave == null) {
                    sender.sendMessage(ChatUtil.format("&c접속하지 않은 플레이어입니다."));
                    return true;
                }
                plugin.getPlayerDataManager().savePlayerDataAsync(targetSave.getUniqueId(), success -> {
                    if (success) sender.sendMessage(ChatUtil.format("&a저장 성공: " + targetSave.getName()));
                    else sender.sendMessage(ChatUtil.format("&c저장 실패: " + targetSave.getName()));
                });
                break;

            case "give": // 아이템 지급
                handleGiveCommand(sender, args);
                break;

            case "help":
            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void handleGiveCommand(CommandSender sender, String[] args) {
        // /rpgcore give <player> <type> <id>
        if (args.length < 4) {
            sender.sendMessage(ChatUtil.format("&c사용법: /rpgcore give <닉네임> <rune|weapon> <ID>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatUtil.format("&c접속하지 않은 플레이어입니다."));
            return;
        }
        String type = args[2].toLowerCase();
        String id = args[3];

        if (type.equals("weapon")) {
            ClassManager cm = plugin.getClassManager();
            ItemStack item = cm.getClassWeapon(id);
            if (item != null) {
                target.getInventory().addItem(item);
                sender.sendMessage(ChatUtil.format("&a[지급] &f" + target.getName() + "님에게 무기(" + id + ")를 지급했습니다."));
            } else {
                sender.sendMessage(ChatUtil.format("&c존재하지 않는 직업ID 입니다."));
            }
        } else if (type.equals("rune")) {
            RuneManager rm = plugin.getRuneManager();
            ItemStack item = rm.getRuneItem(id);
            if (item != null) {
                target.getInventory().addItem(item);
                sender.sendMessage(ChatUtil.format("&a[지급] &f" + target.getName() + "님에게 룬(" + id + ")을 지급했습니다."));
            } else {
                sender.sendMessage(ChatUtil.format("&c존재하지 않거나 ItemsAdder 설정이 잘못된 룬ID 입니다."));
            }
        } else {
            sender.sendMessage(ChatUtil.format("&c알 수 없는 타입입니다. (weapon 또는 rune)"));
        }
    }

    private void showStats(CommandSender viewer, Player target) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(target.getUniqueId());
        if (data == null) {
            viewer.sendMessage(ChatUtil.format("&c데이터를 불러오는 중입니다."));
            return;
        }

        viewer.sendMessage(ChatUtil.format("&8&m                                       "));
        viewer.sendMessage(ChatUtil.format("  &6&l[ &e" + target.getName() + "님의 정보 &6&l]"));
        viewer.sendMessage("");
        viewer.sendMessage(ChatUtil.format("  &f직업: &e" + (data.hasClass() ? data.getPlayerClass() : "무직")));
        viewer.sendMessage(ChatUtil.format("  &f레벨: &aLv." + data.getLevel()));
        viewer.sendMessage(ChatUtil.format("  &f경험치: &7" + String.format("%.1f", data.getCurrentExp()) + " / " + String.format("%.1f", data.getRequiredExp())));
        viewer.sendMessage("");
        viewer.sendMessage(ChatUtil.format("  &c&l⚡ 공격력: &f" + String.format("%.1f", data.getAttack())));
        viewer.sendMessage(ChatUtil.format("  &9&l🛡 방어력: &f" + String.format("%.1f", data.getDefense())));
        viewer.sendMessage(ChatUtil.format("  &b&l💧 마나: &f" + String.format("%.0f", data.getCurrentMana()) + " / " + String.format("%.0f", data.getMaxMana())));
        viewer.sendMessage("");
        viewer.sendMessage(ChatUtil.format("  &4💥 치명타 확률: &f" + String.format("%.1f", data.getCritChance() * 100) + "%"));
        viewer.sendMessage(ChatUtil.format("  &4💥 치명타 피해: &f" + String.format("%.1f", data.getCritDamage() * 100) + "%"));
        viewer.sendMessage(ChatUtil.format("&8&m                                       "));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatUtil.format("&a===== [ RPGCore 명령어 ] ====="));
        sender.sendMessage(ChatUtil.format("&e/rpgcore stats &7- 내 정보 확인"));
        if (sender.hasPermission("rpgcore.admin")) {
            sender.sendMessage(ChatUtil.format("&c--- 관리자 명령어 ---"));
            sender.sendMessage(ChatUtil.format("&e/rpgcore inspect <닉네임> &7- 타인 정보 확인"));
            sender.sendMessage(ChatUtil.format("&e/rpgcore setlevel <닉네임> <Lv> &7- 레벨 설정"));
            sender.sendMessage(ChatUtil.format("&e/rpgcore give <닉네임> weapon <직업ID> &7- 무기 지급"));
            sender.sendMessage(ChatUtil.format("&e/rpgcore give <닉네임> rune <룬ID> &7- 룬 지급"));
            sender.sendMessage(ChatUtil.format("&e/rpgcore save <닉네임> &7- 강제 저장"));
            sender.sendMessage(ChatUtil.format("&e/rpgcore reload &7- 리로드"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            subs.add("stats");
            if (sender.hasPermission("rpgcore.admin")) {
                subs.add("reload");
                subs.add("inspect");
                subs.add("setlevel");
                subs.add("give");
                subs.add("save");
            }
            StringUtil.copyPartialMatches(args[0], subs, completions);
        } else if (args.length == 2 && sender.hasPermission("rpgcore.admin")) {
            // 닉네임 자동완성 (inspect, setlevel, give, save)
            if (List.of("inspect", "setlevel", "give", "save").contains(args[0].toLowerCase())) {
                return null; // 기본 플레이어 목록
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give") && sender.hasPermission("rpgcore.admin")) {
            List<String> types = new ArrayList<>();
            types.add("weapon");
            types.add("rune");
            StringUtil.copyPartialMatches(args[2], types, completions);
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give") && sender.hasPermission("rpgcore.admin")) {
            if (args[2].equalsIgnoreCase("weapon")) {
                StringUtil.copyPartialMatches(args[3], plugin.getClassManager().getAllClassIds(), completions);
            } else if (args[2].equalsIgnoreCase("rune")) {
                StringUtil.copyPartialMatches(args[3], plugin.getRuneManager().getAllRuneIds(), completions);
            }
        }
        Collections.sort(completions);
        return completions;
    }
}