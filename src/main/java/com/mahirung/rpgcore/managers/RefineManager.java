package com.mahirung.rpgcore.managers;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.gui.RefineGUI;
import com.mahirung.rpgcore.util.ChatUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RefineManager {

    private final RPGCore plugin;
    private final ConfigManager configManager;
    private final Map<String, RefineRecipe> recipeCache = new HashMap<>();
    private final Map<UUID, List<ActiveRefineTask>> activeTasks = new HashMap<>();
    private final File tasksFile;
    private final FileConfiguration tasksConfig;

    public RefineManager(RPGCore plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.tasksFile = new File(plugin.getDataFolder(), "refining_tasks.yml");
        if (!tasksFile.exists()) {
            try { tasksFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        this.tasksConfig = YamlConfiguration.loadConfiguration(tasksFile);
        loadRefiningRecipes();
        loadActiveTasksFromFile();
    }

    public void loadRefiningRecipes() {
        recipeCache.clear();
        FileConfiguration config = configManager.getRefiningConfig();
        ConfigurationSection sec = config.getConfigurationSection("recipes");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection r = sec.getConfigurationSection(key);
            if (r == null) continue;
            try {
                Material in = Material.matchMaterial(r.getString("input.material", "STONE"));
                int inAm = r.getInt("input.amount", 1);
                Material cat = Material.matchMaterial(r.getString("catalyst.material", "AIR"));
                int catAm = r.getInt("catalyst.amount", 0);
                Material out = Material.matchMaterial(r.getString("result.material", "DIRT"));
                int outAm = r.getInt("result.amount", 1);
                long dur = config.getLong("durations." + r.getString("duration-key", "default"), 60) * 1000L;
                recipeCache.put(key, new RefineRecipe(key, new ItemStack(in, inAm), new ItemStack(cat, catAm), new ItemStack(out, outAm), dur));
            } catch (Exception e) { plugin.getLogger().warning("레시피 오류: " + key); }
        }
    }

    public Set<String> getAllRecipeIds() { return recipeCache.keySet(); }

    public void startRefineTask(Player player, String recipeId) {
        RefineRecipe recipe = recipeCache.get(recipeId);
        if (recipe == null) {
            player.sendMessage(ChatUtil.format("&c존재하지 않는 레시피입니다."));
            return;
        }
        long endTime = System.currentTimeMillis() + recipe.getDurationMillis();
        activeTasks.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>())
                .add(new ActiveRefineTask(recipeId, endTime));
        saveActiveTasksToFile();
        player.sendMessage(ChatUtil.format("&a재련 시작: " + recipeId));
    }

    public void handleClaimItem(Player player) {
        List<ActiveRefineTask> tasks = activeTasks.get(player.getUniqueId());
        if (tasks == null || tasks.isEmpty()) {
            player.sendMessage(ChatUtil.format("&c완료된 작업이 없습니다."));
            return;
        }
        ActiveRefineTask done = tasks.stream().filter(ActiveRefineTask::isComplete).findFirst().orElse(null);
        if (done == null) {
            player.sendMessage(ChatUtil.format("&c아직 완료되지 않았습니다."));
            return;
        }
        RefineRecipe r = recipeCache.get(done.getRecipeId());
        if (r != null) player.getInventory().addItem(r.getResult().clone());
        tasks.remove(done);
        saveActiveTasksToFile();
        player.sendMessage(ChatUtil.format("&a아이템 수령 완료!"));
    }

    public void openRefineGUI(Player player) {
        new RefineGUI(plugin, activeTasks.getOrDefault(player.getUniqueId(), new ArrayList<>())).open(player);
    }

    /** GUI 클릭 처리 (수정됨) */
    public void handleGUIClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // [Fix] 1. 내 인벤토리 클릭 허용 (재료 집기)
        if (event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.PLAYER) {
            event.setCancelled(false);
            return;
        }

        // 2. GUI 내부 클릭 기본 취소
        event.setCancelled(true);

        int slot = event.getSlot();

        // [Fix] 3. 재료(Input)와 촉매(Catalyst) 슬롯 허용
        if (slot == RefineGUI.INPUT_SLOT || slot == RefineGUI.CATALYST_SLOT) {
            event.setCancelled(false);
            return;
        }

        // 4. 버튼 처리
        if (slot == RefineGUI.START_BUTTON_SLOT) {
            player.sendMessage("GUI 재련 시작 기능은 레시피 매칭 로직 구현이 필요합니다.");
        } else if (slot == RefineGUI.RESULT_SLOT) {
            handleClaimItem(player);
            player.closeInventory();
        }
    }

    private void loadActiveTasksFromFile() {
        if (!tasksConfig.contains("tasks")) return;
        ConfigurationSection sec = tasksConfig.getConfigurationSection("tasks");
        for (String uuidStr : sec.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            List<Map<?, ?>> list = sec.getMapList(uuidStr);
            List<ActiveRefineTask> tasks = new ArrayList<>();
            for (Map<?, ?> m : list) {
                String rId = (String) m.get("recipeId");
                long eTime = ((Number) m.get("endTime")).longValue();
                tasks.add(new ActiveRefineTask(rId, eTime));
            }
            activeTasks.put(uuid, tasks);
        }
    }

    public void saveActiveTasksToFile() {
        tasksConfig.set("tasks", null);
        ConfigurationSection sec = tasksConfig.createSection("tasks");
        for (Map.Entry<UUID, List<ActiveRefineTask>> e : activeTasks.entrySet()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (ActiveRefineTask t : e.getValue()) {
                Map<String, Object> m = new HashMap<>();
                m.put("recipeId", t.getRecipeId());
                m.put("endTime", t.getEndTime());
                list.add(m);
            }
            sec.set(e.getKey().toString(), list);
        }
        try { tasksConfig.save(tasksFile); } catch (IOException ex) { ex.printStackTrace(); }
    }

    public static class RefineRecipe {
        private final String id;
        private final ItemStack input, catalyst, result;
        private final long durationMillis;
        public RefineRecipe(String id, ItemStack i, ItemStack c, ItemStack r, long d) {
            this.id = id; this.input = i; this.catalyst = c; this.result = r; this.durationMillis = d;
        }
        public String getId() { return id; }
        public ItemStack getInput() { return input; }
        public ItemStack getCatalyst() { return catalyst; }
        public ItemStack getResult() { return result; }
        public long getDurationMillis() { return durationMillis; }
    }

    public static class ActiveRefineTask {
        private final String recipeId;
        private final long endTime;
        public ActiveRefineTask(String r, long e) { this.recipeId = r; this.endTime = e; }
        public String getRecipeId() { return recipeId; }
        public long getEndTime() { return endTime; }
        public boolean isComplete() { return System.currentTimeMillis() >= endTime; }
        
        // [복구됨] 이 메서드가 없어서 에러가 났습니다. 다시 추가했습니다.
        public long getTimeLeft() { return Math.max(0, endTime - System.currentTimeMillis()); }
    }
}