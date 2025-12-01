package com.mahirung.rpgcore.managers;

import com.mahirung.rpgcore.RPGCore;
import com.mahirung.rpgcore.data.PlayerData;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final RPGCore plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(RPGCore plugin) {
        this.plugin = plugin;
        connect();
        createTables();
    }

    private void connect() {
        String host = plugin.getConfig().getString("database.host");
        int port = plugin.getConfig().getInt("database.port");
        String user = plugin.getConfig().getString("database.user");
        String password = plugin.getConfig().getString("database.password");
        String database = plugin.getConfig().getString("database.database");

        HikariConfig config = new HikariConfig();
        
        // [추가됨] 드라이버 클래스를 강제로 지정 (오류 해결의 핵심!)
        config.setDriverClassName("org.postgresql.Driver");
        
        config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database + "?sslmode=require");
        config.setUsername(user);
        config.setPassword(password);
        
        config.setMaximumPoolSize(plugin.getConfig().getInt("database.pool-size", 10));
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setMaxLifetime(1800000);
        config.setConnectionTimeout(10000);

        try {
            dataSource = new HikariDataSource(config);
            plugin.getLogger().info("Supabase(PostgreSQL)에 성공적으로 연결되었습니다.");
        } catch (Exception e) {
            plugin.getLogger().severe("DB 연결 실패! config.yml 정보를 확인해주세요.");
            e.printStackTrace();
        }
    }

    private void createTables() {
        if (dataSource == null) return;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS rpg_player_data (" +
                             "uuid VARCHAR(36) PRIMARY KEY, " +
                             "player_class VARCHAR(32), " +
                             "level INT DEFAULT 1, " +
                             "exp DOUBLE PRECISION DEFAULT 0, " +
                             "base_attack DOUBLE PRECISION DEFAULT 0, " +
                             "base_defense DOUBLE PRECISION DEFAULT 0, " +
                             "base_mana DOUBLE PRECISION DEFAULT 100, " +
                             "current_mana DOUBLE PRECISION DEFAULT 100, " +
                             "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                             ")"
             )) {
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "테이블 생성 중 오류 발생", e);
        }
    }

    public void savePlayerData(PlayerData data) {
        if (dataSource == null) return;

        String sql = "INSERT INTO rpg_player_data " +
                     "(uuid, player_class, level, exp, base_attack, base_defense, base_mana, current_mana) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON CONFLICT (uuid) DO UPDATE SET " +
                     "player_class = EXCLUDED.player_class, " +
                     "level = EXCLUDED.level, " +
                     "exp = EXCLUDED.exp, " +
                     "base_attack = EXCLUDED.base_attack, " +
                     "base_defense = EXCLUDED.base_defense, " +
                     "base_mana = EXCLUDED.base_mana, " +
                     "current_mana = EXCLUDED.current_mana, " +
                     "last_updated = CURRENT_TIMESTAMP";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, data.getUuid().toString());
            ps.setString(2, data.getPlayerClass());
            ps.setInt(3, data.getLevel());
            ps.setDouble(4, data.getCurrentExp());
            ps.setDouble(5, data.getBaseAttack());
            ps.setDouble(6, data.getBaseDefense());
            ps.setDouble(7, data.getBaseMaxMana());
            ps.setDouble(8, data.getCurrentMana());

            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "데이터 저장 실패: " + data.getUuid(), e);
        }
    }

    public PlayerData loadPlayerData(UUID uuid) {
        if (dataSource == null) return null;

        PlayerData data = null;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM rpg_player_data WHERE uuid = ?"
             )) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data = new PlayerData(plugin, uuid);
                    data.setPlayerClass(rs.getString("player_class"));
                    data.setLevel(rs.getInt("level"));
                    data.setCurrentExp(rs.getDouble("exp"));
                    data.setBaseAttack(rs.getDouble("base_attack"));
                    data.setBaseDefense(rs.getDouble("base_defense"));
                    data.setBaseMaxMana(rs.getDouble("base_mana"));
                    data.setNewPlayer(false);
                } else {
                    data = new PlayerData(plugin, uuid);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "데이터 로드 실패: " + uuid, e);
        }
        return data;
    }

    public HikariDataSource getDataSource() { return dataSource; }
    
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}