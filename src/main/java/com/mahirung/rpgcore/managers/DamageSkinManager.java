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
import org.bukkit.event.inventory.InventoryClickEvent; // [중요] import 추가
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

    // RPGCore.java에서 호출하는 리로드 메서드
    public void loadDamageSkins() {
        // 추후 스킨 설정을 파일에서 불러오는 로직이 들어갈 자리입니다.
    }

    public void openGUI(Player player) {
        new DamageSkinGUI().open(player);
    }

    // [Fix] 누락되었던 메서드를 다시 추가했습니다!
    public void handleGUIClick(InventoryClickEvent event) {
        // 1. 클릭 취소 (아이템 못 가져가게)
        event.setCancelled(true);

        // 2. 추가 로직 (현재는 정보만 보여주는 GUI라 기능 없음)
        // 만약 스킨을 선택하는 기능을 넣는다면 여기에 작성하면 됩니다.
        if (event.getCurrentItem() != null) {
            // Player player = (Player) event.getWhoClicked();
            // player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
        }
    }

    /**
     * 데미지 스킨 표시 (TextDisplay 사용 - 1.21+ 최적화)
     */
    public void showDamage(LivingEntity victim, double damage, boolean isCritical, Player attacker) {
        if (!plugin.getConfig().getBoolean("damage-skins.enable", true)) return;

        Location loc = victim.getLocation().add(0, 1.2, 0); // 머리 위
        
        // 랜덤 오프셋 (숫자가 겹치지 않게)
        double offsetX = (random.nextDouble() - 0.5) * 0.5;
        double offsetZ = (random.nextDouble() - 0.5) * 0.5;
        double offsetY = (random.nextDouble() - 0.5) * 0.3;
        loc.add(offsetX, offsetY, offsetZ);

        // TextDisplay 소환
        TextDisplay display = (TextDisplay) victim.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
        
        // 텍스트 포맷팅
        String damageStr = String.format("%.0f", damage); // 소수점 제거
        String text;
        
        if (isCritical) {
            text = ChatUtil.format("&c&l💥 " + damageStr); // 크리티컬
            display.setBackgroundColor(Color.fromARGB(100, 255, 0, 0)); // 붉은 배경
        } else {
            text = ChatUtil.format("&f" + damageStr); // 일반
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0)); // 투명 배경
        }

        display.setText(text);
        display.setBillboard(Display.Billboard.CENTER); // 항상 플레이어를 바라봄
        display.setSeeThrough(true); // 벽 뒤에서도 보임
        display.setShadowed(true); // 그림자 효과

        // 크기 조절 (크리티컬은 더 크게)
        float scale = isCritical ? 1.5f : 1.0f;
        display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f(0, 0, 0, 1)
        ));

        // 애니메이션 (위로 떠오르며 삭제)
        Bukkit.getScheduler().runTaskLater(plugin, display::remove, 20L); // 1초 뒤 삭제
        animateText(display);
    }

    private void animateText(TextDisplay display) {
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!display.isValid()) {
                task.cancel();
                return;
            }
            // 위로 천천히 이동
            Location current = display.getLocation();
            current.add(0, 0.05, 0);
            display.teleport(current);
        }, 0L, 1L);
    }
}