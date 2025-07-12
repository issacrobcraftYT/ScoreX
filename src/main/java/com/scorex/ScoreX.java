package com.scorex;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.Sound;

public class ScoreX extends JavaPlugin {
    private FileConfiguration config;
    private Map<String, Object> lang;
    private boolean placeholderApiEnabled = false;
    private boolean vaultEnabled = false;
    private Economy economy = null;
    private String[] animatedTitles = {"§bscoreX", "§cscoreX", "§dscoreX", "§escoreX"};
    private int animationStep = 0;
    private FileConfiguration scoreboardConfig;
    private String animationMode = "loop";
    private boolean bounceForward = true;
    private Map<Player, Boolean> scoreboardToggles = new HashMap<>();
    private Map<Player, String> playerScoreboards = new HashMap<>();
    private Map<Player, List<String>> playerCustomLines = new HashMap<>();
    private Map<Player, Scoreboard> playerBoards = new HashMap<>();
    private Map<Player, Objective> playerObjectives = new HashMap<>();
    private Map<String, Animation> animations = new HashMap<>();
    private boolean debugMode = false;
    private boolean cmiEnabled = false;
    private boolean worldGuardEnabled = false;
    private Map<Player, Boolean> inCombat = new HashMap<>();
    private Map<Player, Long> lastToggle = new HashMap<>();

    private ScoreXGUI gui;

    @Override
    public void onEnable() {
        loadConfig();
        loadLanguage();
        loadAnimations();
        loadScoreboard();
        checkDependencies();
        sendConsole(getLang("plugin.started"));
        setupAnimatedTitles();
        setupAnimationSettings();
        startScoreboardAnimation();
        getCommand("scorex").setExecutor(this);
        debugMode = config.getBoolean("debug_mode", false);
        gui = new ScoreXGUI(this);
        boolean luckPermsEnabled = Bukkit.getPluginManager().getPlugin("LuckPerms") != null;
        if (!luckPermsEnabled) {
            getLogger().warning("LuckPerms not found. Prefix/suffix/rank placeholders will not work.");
        }
        worldGuardEnabled = Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
        cmiEnabled = Bukkit.getPluginManager().getPlugin("CMI") != null;
        if (!worldGuardEnabled) getLogger().info("[ScoreX] WorldGuard not found. Region features disabled.");
        if (!cmiEnabled) getLogger().info("[ScoreX] CMI not found. AFK features disabled.");
        // Register event listeners
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlayerQuit(PlayerQuitEvent e) {
                scoreboardToggles.remove(e.getPlayer());
                playerScoreboards.remove(e.getPlayer());
                playerCustomLines.remove(e.getPlayer());
                inCombat.remove(e.getPlayer());
            }
            @EventHandler
            public void onPvP(EntityDamageByEntityEvent e) {
                if (e.getEntity() instanceof Player && e.getDamager() instanceof Player) {
                    Player p = (Player) e.getEntity();
                    inCombat.put(p, true);
                    Bukkit.getScheduler().runTaskLater(ScoreX.this, () -> inCombat.put(p, false), 200L);
                }
            }
        }, this);
    }
    private void loadAnimations() {
        animations.clear();
        File animDir = new File(getDataFolder(), "animations");
        if (!animDir.exists()) animDir.mkdirs();
        File[] files = animDir.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files != null) {
            for (File file : files) {
                FileConfiguration animYaml = YamlConfiguration.loadConfiguration(file);
                for (String key : animYaml.getKeys(false)) {
                    int interval = animYaml.getInt(key + ".interval", 20);
                    List<String> frames = animYaml.getStringList(key + ".frames");
                    if (!frames.isEmpty()) {
                        animations.put(key, new Animation(frames, interval));
                    }
                }
            }
        }
    }

    private String getLuckPermsPrefix(Player player) {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) return "";
        try {
            Class<?> lpProvider = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object luckPerms = lpProvider.getMethod("get").invoke(null);
            Object userManager = luckPerms.getClass().getMethod("getUserManager").invoke(luckPerms);
            Object user = userManager.getClass().getMethod("getUser", java.util.UUID.class).invoke(userManager, player.getUniqueId());
            if (user == null) return "";
            Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
            Object metaData = cachedData.getClass().getMethod("getMetaData").invoke(cachedData);
            Object prefix = metaData.getClass().getMethod("getPrefix").invoke(metaData);
            return prefix != null ? prefix.toString() : "";
        } catch (Throwable t) {
            getLogger().warning("[ScoreX] Error using LuckPerms: " + t.getMessage());
            return "";
        }
    }
    private void checkDependencies() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderApiEnabled = config.getBoolean("use_placeholderapi", true);
            getLogger().info("[scoreX] PlaceholderAPI hooked!");
        } else {
            placeholderApiEnabled = false;
            getLogger().warning("[scoreX] PlaceholderAPI not found. Placeholders will not work.");
        }
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            vaultEnabled = config.getBoolean("use_vault", true);
            economy = getServer().getServicesManager().getRegistration(Economy.class) != null ?
                getServer().getServicesManager().getRegistration(Economy.class).getProvider() : null;
            if (economy != null) {
                getLogger().info("[scoreX] Vault hooked! Economy support enabled.");
            } else {
                getLogger().warning("[scoreX] Vault found but no economy provider detected.");
            }
        } else {
            vaultEnabled = false;
            getLogger().warning("[scoreX] Vault not found. Economy placeholders will not work.");
        }
    }

    private void startScoreboardAnimation() {
        int interval = getAnimationInterval();
        new BukkitRunnable() {
            @Override
            public void run() {
                updateAnimationStep();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (shouldShowScoreboard(player)) {
                        updateScoreboard(player);
                    } else {
                        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
                    }
                }
            }
        }.runTaskTimer(this, 0L, interval);
    }

    private int getAnimationInterval() {
        String animName = scoreboardConfig.getString("animation", null);
        if (animName != null && animations.containsKey(animName)) {
            return animations.get(animName).interval;
        }
        return scoreboardConfig.getInt("animation_settings.interval", config.getInt("animation_interval", 20));
    }

    private void updateAnimationStep() {
        String animName = scoreboardConfig.getString("animation", null);
        if (animName != null && animations.containsKey(animName)) {
            Animation anim = animations.get(animName);
            animationStep = (animationStep + 1) % anim.frames.size();
            animatedTitles = anim.frames.toArray(new String[0]);
        } else {
            if (animationMode.equalsIgnoreCase("loop")) {
                animationStep = (animationStep + 1) % animatedTitles.length;
            } else if (animationMode.equalsIgnoreCase("bounce")) {
                if (bounceForward) {
                    animationStep++;
                    if (animationStep >= animatedTitles.length - 1) {
                        bounceForward = false;
                    }
                } else {
                    animationStep--;
                    if (animationStep <= 0) {
                        bounceForward = true;
                    }
                }
            } else if (animationMode.equalsIgnoreCase("random")) {
                animationStep = (int) (Math.random() * animatedTitles.length);
            }
        }
    }

    private void updateScoreboard(Player player) {
        if (worldGuardEnabled) {
            try {
                org.bukkit.plugin.Plugin wg = Bukkit.getPluginManager().getPlugin("WorldGuard");
                if (wg != null) {
                }
            } catch (Throwable t) {
                getLogger().warning("[ScoreX] Error using WorldGuard: " + t.getMessage());
            }
        }
        // PvP/AFK/Combat/Auto-hide logic
        if (isAfk(player) || isInCombat(player)) return;
        Scoreboard board = playerBoards.get(player);
        Objective obj = playerObjectives.get(player);
        String objectiveName = "scorex";
        String pluginVersion = getDescription().getVersion();
        if (board == null) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            playerBoards.put(player, board);
        }
        String scoreboardName = getScoreboardForPlayer(player);
        FileConfiguration customConfig = scoreboardConfig;
        if (!scoreboardName.equals(config.getString("scoreboard", "default"))) {
            File scoreboardFile = new File(getDataFolder(), "scoreboards/" + scoreboardName + ".yaml");
            if (scoreboardFile.exists()) {
                customConfig = YamlConfiguration.loadConfiguration(scoreboardFile);
            }
        }
        String title = customConfig.getString("title", "&bscoreX");
        if (title.contains("{animation}") && animatedTitles.length > 0) {
            title = title.replace("{animation}", animatedTitles[animationStep]);
        }
        title = title.replace("%scorex_version%", pluginVersion);
        title = title.replace("%luckperms_prefix%", getLuckPermsPrefix(player));
        title = title.replace("%bungee_server%", "Main");
        title = title.replace("%bungee_total%", String.valueOf(Bukkit.getOnlinePlayers().size()));
        if (placeholderApiEnabled) {
            title = PlaceholderAPI.setPlaceholders(player, title);
        }
        title = ChatColor.translateAlternateColorCodes('&', title);
        title = color(title);
        if (obj == null || !obj.getDisplayName().equals(title)) {
            if (obj != null) obj.unregister();
            obj = board.registerNewObjective(objectiveName, "dummy", title);
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            playerObjectives.put(player, obj);
        }
        List<String> lines = playerCustomLines.getOrDefault(player, customConfig.getStringList("lines"));
        int score = lines.size();
        for (String line : lines) {
            String processed = line;
            processed = processed.replace("%scorex_version%", pluginVersion);
            processed = processed.replace("%luckperms_prefix%", getLuckPermsPrefix(player));
            processed = processed.replace("%bungee_server%", "Main");
            processed = processed.replace("%bungee_total%", String.valueOf(Bukkit.getOnlinePlayers().size()));
            if (placeholderApiEnabled) {
                processed = PlaceholderAPI.setPlaceholders(player, processed);
            }
            if (vaultEnabled && economy != null) {
                processed = processed.replace("%vault_eco_balance%", String.valueOf(economy.getBalance(player)));
            } else {
                processed = processed.replace("%vault_eco_balance%", "N/A");
            }
            if (!placeholderApiEnabled) {
                processed = processed.replace("%player_name%", player.getName());
                processed = processed.replace("%server_online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
                processed = processed.replace("%player_world%", player.getWorld().getName());
                processed = processed.replace("%player_ping%", getPing(player));
                processed = processed.replace("%player_health%", String.valueOf(player.getHealth()));
                processed = processed.replace("%player_max_health%", String.valueOf(player.getMaxHealth()));
                processed = processed.replace("%player_gamemode%", player.getGameMode().toString());
                processed = processed.replace("%player_level%", String.valueOf(player.getLevel()));
                processed = processed.replace("%player_uuid%", player.getUniqueId().toString());
            }
            processed = animateLine(processed, animationStep);
            processed = ChatColor.translateAlternateColorCodes('&', processed);
            processed = color(processed);
            processed = makeClickable(processed, "/scorex toggle");
            if (debugMode) {
                Pattern p = Pattern.compile("%([^%]+)%");
                Matcher m = p.matcher(processed);
                while (m.find()) {
                    String ph = m.group();
                    getLogger().log(Level.INFO, "[ScoreX][DEBUG] Missing placeholder: " + ph + " in line: " + line);
                }
            }
            obj.getScore(processed).setScore(score--);
        }
        player.setScoreboard(board);
    }

    // Per-group/permission scoreboard logic
    private String getScoreboardForPlayer(Player player) {
        // Per-player override
        if (playerScoreboards.containsKey(player)) {
            return playerScoreboards.get(player);
        }
        // Per-group/permission
        for (String perm : player.getEffectivePermissions().stream().map(p -> p.getPermission()).toList()) {
            if (perm.startsWith("scorex.board.")) {
                String board = perm.substring("scorex.board.".length());
                File f = new File(getDataFolder(), "scoreboards/" + board + ".yaml");
                if (f.exists()) return board;
            }
        }
        // Per-world
        String worldBoard = "world_" + player.getWorld().getName().toLowerCase();
        File f = new File(getDataFolder(), "scoreboards/" + worldBoard + ".yaml");
        if (f.exists()) return worldBoard;
        // Default
        return config.getString("scoreboard", "default");
    }
    private void setupAnimatedTitles() {
        List<String> anim = scoreboardConfig.getStringList("animated_titles");
        if (anim != null && !anim.isEmpty()) {
            animatedTitles = anim.toArray(new String[0]);
        }
    }

    private void setupAnimationSettings() {
        if (scoreboardConfig.contains("animation_settings.mode")) {
            animationMode = scoreboardConfig.getString("animation_settings.mode", "loop");
        }
    }
    private boolean shouldShowScoreboard(Player player) {
        if (scoreboardToggles.containsKey(player)) {
            return scoreboardToggles.get(player);
        }
        List<String> hideWorlds = config.getStringList("hide_in_worlds");
        if (hideWorlds != null && hideWorlds.contains(player.getWorld().getName())) {
            return false;
        }
        return config.getBoolean("scoreboard_enabled", true);
    }
    private String getPing(Player player) {
        try {
            return String.valueOf(player.getClass().getMethod("getPing").invoke(player));
        } catch (Exception e) {
            return "?";
        }
    }
    private void loadScoreboard() {
        String scoreboardName = config.getString("scoreboard", "default");
        File scoreboardFile = new File(getDataFolder(), "scoreboards/" + scoreboardName + ".yaml");
        if (!scoreboardFile.exists()) {
            saveResource("scoreboards/" + scoreboardName + ".yaml", false);
        }
        scoreboardConfig = YamlConfiguration.loadConfiguration(scoreboardFile);
    }

    private void loadConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void loadLanguage() {
        String langCode = config != null ? config.getString("language", "en") : "en";
        File langFile = new File(getDataFolder(), "lang/" + langCode + ".yaml");
        if (!langFile.exists()) {
            saveResource("lang/" + langCode + ".yaml", false);
        }
        FileConfiguration langYaml = YamlConfiguration.loadConfiguration(langFile);
        lang = langYaml.getValues(true);
    }



    public String getLang(String key) {
        if (lang != null && lang.containsKey(key)) {
            Object val = lang.get(key);
            if (val instanceof List) {
                return String.join("\n", ((List<?>) val).stream().map(Object::toString).toArray(String[]::new));
            }
            return val != null ? val.toString() : key;
        }
        return key;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("scorex")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("reload")) {
                    loadConfig();
                    loadLanguage();
                    loadAnimations();
                    loadScoreboard();
                    setupAnimatedTitles();
                    setupAnimationSettings();
                    sendMessage(sender, getLang("plugin.reloaded"));
                    return true;
                } else if (args[0].equalsIgnoreCase("toggle")) {
                    if (sender instanceof Player) {
                        Player p = (Player) sender;
                        boolean enabled = !shouldShowScoreboard(p);
                        scoreboardToggles.put(p, enabled);
                        saveToggleStatus(p, enabled);
                        playToggleSound(p, enabled);
                        sendMessage(sender, getLang(enabled ? "scoreboard.enabled" : "scoreboard.disabled"));
                        return true;
                    } else {
                        sendMessage(sender, getLang("error.players_only"));
                        return true;
                    }
                } else if (args[0].equalsIgnoreCase("set") && args.length > 1) {
                    String newBoard = args[1];
                    if (sender instanceof Player) {
                        Player p = (Player) sender;
                        playerScoreboards.put(p, newBoard);
                        sendMessage(sender, getLang("scoreboard.set_player").replace("{board}", newBoard));
                        return true;
                    } else {
                        config.set("scoreboard", newBoard);
                        loadScoreboard();
                        setupAnimatedTitles();
                        setupAnimationSettings();
                        sendMessage(sender, getLang("scoreboard.set_global").replace("{board}", newBoard));
                        return true;
                    }
                } else if (args[0].equalsIgnoreCase("list")) {
                    File dir = new File(getDataFolder(), "scoreboards");
                    StringBuilder sb = new StringBuilder("&aScoreX &7» &fAvailable scoreboards: ");
                    if (dir.exists() && dir.isDirectory()) {
                        String[] files = dir.list((d, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
                        if (files != null) {
                            for (String file : files) {
                                sb.append("&b").append(file.replace(".yaml", "").replace(".yml", "")).append("&7, ");
                            }
                        }
                    }
                    sendMessage(sender, getLang("scoreboard.list") + sb.toString().replaceAll(", $", ""));
                    return true;
                } else if (args[0].equalsIgnoreCase("gui") && sender instanceof Player) {
                    Player p = (Player) sender;
                    File dir = new File(getDataFolder(), "scoreboards");
                    List<String> boards = new java.util.ArrayList<>();
                    if (dir.exists() && dir.isDirectory()) {
                        String[] files = dir.list((d, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
                        if (files != null) {
                            for (String file : files) {
                                boards.add(file.replace(".yaml", "").replace(".yml", ""));
                            }
                        }
                    }
                    gui.openSelector(p, boards);
                    return true;
                } else if (args[0].equalsIgnoreCase("customlines") && args.length > 1 && sender instanceof Player) {
                    Player p = (Player) sender;
                    List<String> customLines = new java.util.ArrayList<>();
                    for (int i = 1; i < args.length; i++) {
                        customLines.add(args[i].replace("_", " "));
                    }
                    playerCustomLines.put(p, customLines);
                    sendMessage(sender, getLang("scoreboard.custom_lines_set"));
                    return true;
                } else if (args[0].equalsIgnoreCase("reset") && sender instanceof Player) {
                    Player p = (Player) sender;
                    playerScoreboards.remove(p);
                    playerCustomLines.remove(p);
                    sendMessage(sender, getLang("scoreboard.reset"));
                    return true;
                }
            }
            sendMessage(sender, getLang("scoreboard.usage"));
            return true;
        }
        return false;
    }
    private void sendMessage(CommandSender sender, String msg) {
        sender.sendMessage(color(msg));
    }

    private void sendConsole(String msg) {
        getServer().getConsoleSender().sendMessage(color(msg));
    }
    private static class Animation {
        List<String> frames;
        int interval;
        Animation(List<String> frames, int interval) {
            this.frames = frames;
            this.interval = interval;
        }
    }

    private String color(String msg) {
        // Hex color support: &#RRGGBB
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(msg);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(sb, "§x" + hex.chars().mapToObj(c -> "§" + (char)c).reduce("", String::concat));
        }
        matcher.appendTail(sb);
        return sb.toString().replace('&', '§');
    }
    // API for other plugins
    public void setScoreboardForPlayer(Player player, String scoreboardName) {
        File scoreboardFile = new File(getDataFolder(), "scoreboards/" + scoreboardName + ".yaml");
        if (scoreboardFile.exists()) {
            FileConfiguration customBoard = YamlConfiguration.loadConfiguration(scoreboardFile);
            List<String> lines = customBoard.getStringList("lines");
            String title = customBoard.getString("title", "scoreX");
            Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective obj = board.registerNewObjective("scorex", "dummy", color(title));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            int score = lines.size();
            for (String line : lines) {
                obj.getScore(color(line)).setScore(score--);
            }
            player.setScoreboard(board);
        }
    }
    private String animateLine(String line, int tick) {
        return line;
    }

    private boolean isAfk(Player player) {
        if (cmiEnabled) {
            try {
                org.bukkit.plugin.Plugin cmi = Bukkit.getPluginManager().getPlugin("CMI");
                if (cmi != null) {
                    java.lang.reflect.Method getInstance = cmi.getClass().getMethod("getInstance");
                    Object cmiInstance = getInstance.invoke(null);
                    Object afkManager = cmiInstance.getClass().getMethod("getAfkManager").invoke(cmiInstance);
                    java.lang.reflect.Method isAfk = afkManager.getClass().getMethod("isAfk", Player.class);
                    Object result = isAfk.invoke(afkManager, player);
                    if (result instanceof Boolean) return (Boolean) result;
                }
            } catch (Throwable t) {
                getLogger().warning("[ScoreX] Error using CMI AFK: " + t.getMessage());
            }
        }
        return false;
    }

    private boolean isInCombat(Player player) {
        return inCombat.getOrDefault(player, false);
    }

    private void playToggleSound(Player player, boolean enabled) {
        player.playSound(player.getLocation(), enabled ? Sound.UI_BUTTON_CLICK : Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
    }

    private void saveToggleStatus(Player player, boolean enabled) {
        // TODO: Save to file/db for persistence
        lastToggle.put(player, System.currentTimeMillis());
    }
    private boolean loadToggleStatus(Player player) {
        // TODO: Load from file/db for persistence
        return scoreboardToggles.getOrDefault(player, true);
    }

    // Clickable lines (stub, Adventure API required for full impl)
    private String makeClickable(String line, String command) {
        // TODO: Use Adventure API to make line clickable
        return line;
    }
}
