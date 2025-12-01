package com.mahirung.rpgcore.util;

import org.bukkit.ChatColor;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 채팅 유틸리티 클래스
 * - 색상 코드 변환 (&a, &c 등)
 * - MessageFormat 지원 ({0}, {1} 등)
 * - Lore 리스트 변환
 */
public final class ChatUtil {

    private ChatUtil() {
        throw new UnsupportedOperationException("이 클래스는 인스턴스화할 수 없습니다.");
    }

    /**
     * 색상 코드 변환 및 MessageFormat 적용
     * 예: format("&c에러! {0}님", player.getName())
     */
    public static String format(String message, Object... args) {
        if (message == null) return "";

        // 색상 코드 변환
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);

        // MessageFormat 적용
        if (args != null && args.length > 0) {
            try {
                return MessageFormat.format(coloredMessage, args);
            } catch (IllegalArgumentException e) {
                // 포맷팅 오류 시 원본 반환
                return coloredMessage;
            }
        }
        return coloredMessage;
    }

    /**
     * Lore 리스트의 색상 코드 변환
     */
    public static List<String> format(List<String> lore) {
        if (lore == null || lore.isEmpty()) return new ArrayList<>();
        return lore.stream()
                   .map(ChatUtil::format) // 인수 없는 format(String) 호출
                   .collect(Collectors.toList());
    }
}
