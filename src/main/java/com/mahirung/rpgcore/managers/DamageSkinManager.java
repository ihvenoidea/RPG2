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
import java.util.Map;
import java.util.HashMap;

public class DamageSkinManager {

    private final RPGCore plugin;
    private final Random random = new Random();

    private final Map<String, Object> skinCache = new HashMap<>();

    public DamageSkinManager(RPGCore plugin) {
        this.plugin = plugin;
        loadDamageSkins();
    }

    public void loadDamageSkins() {
        skinCache.clear();
    }

    public void openGUI(Player player) {
        new DamageSkinGUI().open(player);
    }

    public void handleGUIClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);
    }

    /**
     * * 핵심: 데미지 스킨 표시 (TextDisplay 사용) 
     */
    public void showDamage(LivingEntity victim, double damage, boolean isCritical, Player attacker) {
        if (!plugin.getConfig().getBoolean("damage-skins.enable", true)) return;

        // [Fix] try-catch 블록으로 감싸서 TextDisplay 오류 발생 시에도 메인 이벤트가 멈추지 않게 함
        try {
            // 1. 위치 설정
            Location loc = victim.getLocation().add(0, 1.8, 0); 
            
            // 2. 랜덤 오프셋 
            double offsetX = (random.nextDouble() - 0.5) * plugin.getConfig().getDouble("damage-skins.font-offset-x", 0.25);
            double offsetZ = (random.nextDouble() - 0.5) * plugin.getConfig().getDouble("damage-skins.font-offset-x", 0.25);
            double offsetY = (random.nextDouble() - 0.5) * plugin.getConfig().getDouble("damage-skins.font-offset-y", 0.5);
            loc.add(offsetX, offsetY, offsetZ);

            // 3. TextDisplay 엔티티 소환
            TextDisplay display = (TextDisplay) victim.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
            
            // 4. 텍스트 꾸미기
            String damageStr = String.format("%.0f", damage);
            String text;
            
            if (isCritical) {
                text = ChatUtil.format("&c&l💥 " + damageStr); 
                display.setBackgroundColor(Color.fromARGB(100, 255, 0, 0));
            } else {
                text = ChatUtil.format("&f" + damageStr);
                display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            }

            display.setText(text);
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(true);
            display.setShadowed(true);

            // 5. 크기 조절
            float scale = isCritical ? 1.5f : 1.0f;
            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0, 0, 0, 1)
            ));

            // 6. 삭제 스케줄러 (config.yml 설정값 사용)
            long duration = plugin.getConfig().getLong("damage-skins.display-duration-ticks", 20L);
            plugin.getServer().getScheduler().runTaskLater(plugin, display::remove, duration); 
            
            // 7. 애니메이션
            animateText(display);
            
        } catch (Exception e) {
            // TextDisplay 관련 오류가 발생하면 로그를 남기고 종료 (이벤트 충돌 방지)
            plugin.getLogger().warning("데미지 스킨 표시 중 오류 발생: " + e.getMessage());
        }
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