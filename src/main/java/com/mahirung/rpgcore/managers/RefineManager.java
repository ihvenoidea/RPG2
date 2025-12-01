package com.mahirung.rpgcore.managers;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.gui.RefineGUI;
import com.mahirung.rpgcore.util.ChatUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
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
        
        player.sendMessage(ChatUtil.format("&a[재련] &f작업이 시작되었습니다! (" + (recipe.getDurationMillis() / 1000) + "초 소요)"));
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
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
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
    }

    public void openRefineGUI(Player player) {
        new RefineGUI(plugin, activeTasks.getOrDefault(player.getUniqueId(), new ArrayList<>())).open(player);
    }

    /** GUI 클릭 처리 */
    public void handleGUIClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // 1. 내 인벤토리 클릭 허용 (재료 집기)
        if (event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.PLAYER) {
            event.setCancelled(false);
            return;
        }

        // 2. GUI 내부 클릭 기본 취소
        event.setCancelled(true);

        int slot = event.getSlot();

        // 3. 재료(Input)와 촉매(Catalyst) 슬롯 허용
        if (slot == RefineGUI.INPUT_SLOT || slot == RefineGUI.CATALYST_SLOT) {
            event.setCancelled(false);
            return;
        }

        // 4. 버튼 처리
        if (slot == RefineGUI.START_BUTTON_SLOT) {
            // [추가] 제작 버튼 클릭 시 레시피 확인 및 시작
            tryStartRefine(player, event.getInventory());
            
        } else if (slot == RefineGUI.RESULT_SLOT) {
            handleClaimItem(player);
            player.closeInventory();
        }
    }

    /** [신규] GUI 아이템을 확인하고 재련 시작 시도 */
    private void tryStartRefine(Player player, Inventory gui) {
        ItemStack input = gui.getItem(RefineGUI.INPUT_SLOT);
        ItemStack catalyst = gui.getItem(RefineGUI.CATALYST_SLOT);

        if (input == null || input.getType().isAir()) {
            player.sendMessage(ChatUtil.format("&c[재련] &f재료 아이템을 넣어주세요."));
            return;
        }

        // 레시피 찾기
        RefineRecipe matchedRecipe = null;
        for (RefineRecipe recipe : recipeCache.values()) {
            if (isRecipeMatch(recipe, input, catalyst)) {
                matchedRecipe = recipe;
                break;
            }
        }

        if (matchedRecipe == null) {
            player.sendMessage(ChatUtil.format("&c[재련] &f일치하는 레시피가 없습니다."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // 아이템 소모
        input.setAmount(input.getAmount() - matchedRecipe.getInput().getAmount());
        gui.setItem(RefineGUI.INPUT_SLOT, input); // GUI 업데이트

        if (matchedRecipe.getCatalyst() != null && matchedRecipe.getCatalyst().getType() != Material.AIR) {
            if (catalyst != null) {
                catalyst.setAmount(catalyst.getAmount() - matchedRecipe.getCatalyst().getAmount());
                gui.setItem(RefineGUI.CATALYST_SLOT, catalyst);
            }
        }

        // 재련 시작
        startRefineTask(player, matchedRecipe.getId());
        player.closeInventory(); // 오류 방지를 위해 닫기
    }

    /** 레시피 매칭 여부 확인 */
    private boolean isRecipeMatch(RefineRecipe recipe, ItemStack input, ItemStack catalyst) {
        // 1. 입력 재료 확인
        if (input.getType() != recipe.getInput().getType()) return false;
        if (input.getAmount() < recipe.getInput().getAmount()) return false;

        // 2. 촉매 확인
        ItemStack recipeCat = recipe.getCatalyst();
        boolean recipeHasCat = (recipeCat != null && recipeCat.getType() != Material.AIR);
        
        if (recipeHasCat) {
            // 촉매가 필요한 레시피인데 촉매 슬롯이 비었거나 틀린 경우
            if (catalyst == null || catalyst.getType() != recipeCat.getType()) return false;
            if (catalyst.getAmount() < recipeCat.getAmount()) return false;
        } else {
            // 촉매가 필요 없는 레시피인데 촉매 슬롯에 무언가 있는 경우 (엄격한 매칭)
            if (catalyst != null && catalyst.getType() != Material.AIR) return false;
        }

        return true;
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
        public long getTimeLeft() { return Math.max(0, endTime - System.currentTimeMillis()); }
    }
}