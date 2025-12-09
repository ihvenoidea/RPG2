package com.mahirung.rpgcore.managers;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.gui.DamageSkinGUI;
import com.mahirung.rpgcore.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Random;

public class DamageSkinManager {

    private final RPGCore plugin;
    private final Random random = new Random();

    public DamageSkinManager(RPGCore plugin) {
        this.plugin = plugin;
    }

    public void loadDamageSkins() {
        // [Fix] 컴파일 오류 방지용 (현재는 기능 없음)
    }

    public void openGUI(Player player) {
        new DamageSkinGUI().open(player);
    }

    public void handleGUIClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true); 

        // 현재는 선택 기능이 없으므로, GUI 클릭 시 아이템 가져가기만 막음
    }

    /**
     * * 핵심: 데미지 스킨 표시 (TextDisplay 사용) 
     */
    public void showDamage(LivingEntity victim, double damage, boolean isCritical, Player attacker) {
        if (!plugin.getConfig().getBoolean("damage-skins.enable", true)) return;

        // 1. 위치 설정 (머리 위)
        Location loc = victim.getLocation().add(0, 1.2, 0); 
        
        // 2. 랜덤 오프셋 (숫자가 겹치지 않게 살짝 흩뿌림)
        double offsetX = (random.nextDouble() - 0.5) * 0.5;
        double offsetZ = (random.nextDouble() - 0.5) * 0.5;
        double offsetY = (random.nextDouble() - 0.5) * 0.3;
        loc.add(offsetX, offsetY, offsetZ);

        // 3. TextDisplay 엔티티 소환
        TextDisplay display = (TextDisplay) victim.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
        
        // 4. 텍스트 꾸미기
        String damageStr = String.format("%.0f", damage);
        String text;
        
        if (isCritical) {
            text = ChatUtil.format("&c&l💥 " + damageStr); // 크리티컬: 빨강 + 굵게 + 이모지
            display.setBackgroundColor(Color.fromARGB(100, 255, 0, 0)); // 배경: 붉은 반투명
        } else {
            text = ChatUtil.format("&f" + damageStr); // 일반: 흰색
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0)); // 배경: 투명
        }

        display.setText(text);
        display.setBillboard(Display.Billboard.CENTER);
        display.setSeeThrough(true);
        display.setShadowed(true);

        // 5. 크기 조절 (크리티컬은 1.5배)
        float scale = isCritical ? 1.5f : 1.0f;
        display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f(0, 0, 0, 1)
        ));

        // 6. 삭제 스케줄러 (1초 뒤 삭제)
        plugin.getServer().getScheduler().runTaskLater(plugin, display::remove, 20L); 
        
        // 7. 애니메이션 (둥실둥실 위로 올라감)
        animateText(display);
    }

    private void animateText(TextDisplay display) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (!display.isValid()) {
                task.cancel();
                return;
            }
            Location current = display.getLocation();
            current.add(0, 0.05, 0);
            display.teleport(current);
        }, 0L, 1L);
    }
}