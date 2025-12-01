package com.mahirung.rpgcore.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mahirung.rpgcore.RPGCore;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Type;
import java.util.*;

/**
 * 아이템 유틸리티 클래스
 * - 아이템 생성, Lore 수정
 * - NBT 태그(PersistentDataContainer) 관리
 */
public final class ItemUtil {

    private static final Gson gson = new Gson();
    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {}.getType();
    private static final Type DOUBLE_MAP_TYPE = new TypeToken<Map<String, Double>>() {}.getType();

    private ItemUtil() {
        throw new UnsupportedOperationException("이 클래스는 인스턴스화할 수 없습니다.");
    }

    /** 네임스페이스 키 생성 */
    private static NamespacedKey getKey(String key) {
        return new NamespacedKey(RPGCore.getInstance(), key.toLowerCase());
    }

    // --- 1. 기본 아이템 생성 ---

    public static ItemStack createItem(Material material, String displayName) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null && displayName != null) {
            meta.setDisplayName(ChatUtil.format(displayName));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack setLore(ItemStack item, List<String> loreLines) {
        if (item == null || loreLines == null) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        List<String> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(ChatUtil.format(line));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack addLore(ItemStack item, String... loreLines) {
        if (item == null || loreLines == null) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        for (String line : loreLines) {
            lore.add(ChatUtil.format(line));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // --- 2. NBT String ---

    public static ItemStack setNBTString(ItemStack item, String key, String value) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        meta.getPersistentDataContainer().set(getKey(key), PersistentDataType.STRING, value);
        item.setItemMeta(meta);
        return item;
    }

    public static String getNBTString(ItemStack item, String key) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey namespacedKey = getKey(key);
        return pdc.has(namespacedKey, PersistentDataType.STRING)
                ? pdc.get(namespacedKey, PersistentDataType.STRING)
                : null;
    }

    // --- 3. NBT Integer ---

    public static ItemStack setNBTInt(ItemStack item, String key, int value) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        meta.getPersistentDataContainer().set(getKey(key), PersistentDataType.INTEGER, value);
        item.setItemMeta(meta);
        return item;
    }

    public static int getNBTInt(ItemStack item, String key, int defaultValue) {
        if (item == null) return defaultValue;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return defaultValue;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.getOrDefault(getKey(key), PersistentDataType.INTEGER, defaultValue);
    }

    /** 오버로드: 기본값 0으로 정수 NBT 조회 */
    public static int getNBTInt(ItemStack item, String key) {
        return getNBTInt(item, key, 0);
    }

    // --- 4. NBT Boolean ---

    public static ItemStack setNBTBoolean(ItemStack item, String key, boolean value) {
        return setNBTInt(item, key, value ? 1 : 0);
    }

    public static boolean getNBTBoolean(ItemStack item, String key, boolean defaultValue) {
        return getNBTInt(item, key, defaultValue ? 1 : 0) == 1;
    }

    // --- 5. NBT List<String> (JSON 기반) ---

    public static ItemStack setNBTStringList(ItemStack item, String key, List<String> value) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        String json = gson.toJson(value);
        meta.getPersistentDataContainer().set(getKey(key), PersistentDataType.STRING, json);
        item.setItemMeta(meta);
        return item;
    }

    public static List<String> getNBTStringList(ItemStack item, String key) {
        if (item == null) return new ArrayList<>();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return new ArrayList<>();

        String json = meta.getPersistentDataContainer().get(getKey(key), PersistentDataType.STRING);
        if (json == null || json.isEmpty()) return new ArrayList<>();

        try {
            return gson.fromJson(json, STRING_LIST_TYPE);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // --- 6. NBT Map<String, Double> (룬/강화 스탯용) ---

    public static ItemStack setNBTDoubleMap(ItemStack item, String key, Map<String, Double> map) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        String json = gson.toJson(map);
        meta.getPersistentDataContainer().set(getKey(key), PersistentDataType.STRING, json);
        item.setItemMeta(meta);
        return item;
    }

    public static Map<String, Double> getNBTDoubleMap(ItemStack item, String key) {
        if (item == null) return new HashMap<>();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return new HashMap<>();

        String json = meta.getPersistentDataContainer().get(getKey(key), PersistentDataType.STRING);
        if (json == null || json.isEmpty()) return new HashMap<>();

        try {
            return gson.fromJson(json, DOUBLE_MAP_TYPE);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
