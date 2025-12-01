package com.mahirung.rpgcore.commands;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.gui.DamageSkinGUI;
import com.mahirung.rpgcore.managers.DamageSkinManager;
import com.mahirung.rpgcore.util.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class DamageSkinCommand implements CommandExecutor {

    private final RPGCore plugin;
    private final DamageSkinManager damageSkinManager;

    public DamageSkinCommand(RPGCore plugin) {
        this.plugin = plugin;
        this.damageSkinManager = plugin.getDamageSkinManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatUtil.format("&c플레이어만 사용할 수 있습니다."));
            return true;
        }

        Player player = (Player) sender;

        // 해금된 스킨 목록을 가져와야 함. (임시로 'default' 및 전체 스킨 포함)
        // 실제로는 PlayerData에서 unlocked skins 리스트를 불러와야 합니다.
        List<String> unlockedSkins = new ArrayList<>();
        unlockedSkins.add("default");
        // 예시: 모든 스킨을 해금 상태로 보여주려면 아래 주석 해제
        // unlockedSkins.addAll(damageSkinManager.getAllSkins().keySet());

        // [Fix] GUI 생성자에 unlockedSkins 리스트 전달
        DamageSkinGUI gui = new DamageSkinGUI(plugin, unlockedSkins, damageSkinManager.getAllSkins());
        gui.open(player);

        return true;
    }
}