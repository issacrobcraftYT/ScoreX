package com.scorex;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;

public class ScoreXGUI implements Listener {
    private final ScoreX plugin;
    public ScoreXGUI(ScoreX plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openSelector(Player player, List<String> boards) {
        Inventory inv = Bukkit.createInventory(null, 27, "Select Scoreboard");
        int i = 0;
        for (String board : boards) {
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§b" + board);
            item.setItemMeta(meta);
            inv.setItem(i++, item);
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("Select Scoreboard")) {
            e.setCancelled(true);
            if (e.getCurrentItem() != null && e.getCurrentItem().hasItemMeta()) {
                String board = e.getCurrentItem().getItemMeta().getDisplayName().replace("§b", "");
                if (e.getWhoClicked() instanceof Player) {
                    Player p = (Player) e.getWhoClicked();
                    plugin.setScoreboardForPlayer(p, board);
                    p.closeInventory();
                    p.sendMessage("§aScoreX §7» §fScoreboard set to §b" + board);
                }
            }
        }
    }
}
