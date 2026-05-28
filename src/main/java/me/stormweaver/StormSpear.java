package me.stormweaver;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class StormSpear extends JavaPlugin implements Listener, CommandExecutor {

    private NamespacedKey chargeKey;
    private final HashMap<UUID, Long> rechargeTimer = new HashMap<>();

    @Override
    public void onEnable() {
        this.chargeKey = new NamespacedKey(this, "spear_charges");
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("getspear") != null) getCommand("getspear").setExecutor(this);
        registerRecipe();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player && player.isOp()) {
            player.getInventory().addItem(getSpear());
            player.sendMessage(ChatColor.GOLD + "The Storm Spear has been forged.");
            return true;
        }
        return false;
    }

    public void registerRecipe() {
        NamespacedKey recipeKey = new NamespacedKey(this, "fulgurite_obelisk");
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, getSpear());
        recipe.shape("BCB", "TIT", "BLB");
        recipe.setIngredient('B', Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE);
        recipe.setIngredient('C', Material.CONDUIT);
        recipe.setIngredient('T', Material.TRIDENT);
        recipe.setIngredient('I', Material.NETHERITE_INGOT);
        recipe.setIngredient('L', Material.LIGHTNING_ROD);
        
        // FIXED: Removed the extra string that caused the compilation error
        Bukkit.addRecipe(recipe);
    }

    public ItemStack getSpear() {
        Material mat = Material.matchMaterial("NETHERITE_SPEAR");
        if (mat == null) mat = Material.NETHERITE_HOE;
        ItemStack s = new ItemStack(mat);
        ItemMeta m = s.getItemMeta();
        if (m != null) {
            m.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "The Fulgurite Obelisk");
            m.setCustomModelData(123456);
            List<String> l = new ArrayList<>();
            l.add(ChatColor.DARK_PURPLE + "Relic of the Primal Gale");
            l.add("");
            l.add(ChatColor.WHITE + "Charges: " + ChatColor.YELLOW + "3 / 3");
            l.add(ChatColor.GRAY + "3 hits, then 60s recharge.");
            m.setLore(l);
            m.getPersistentDataContainer().set(chargeKey, PersistentDataType.INTEGER, 3);
            s.setItemMeta(m);
        }
        return s;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player player)) return;
        if (!(e.getEntity() instanceof LivingEntity victim)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta m = item.getItemMeta();
        if (!m.hasCustomModelData() || m.getCustomModelData() != 123456) return;

        int charges = m.getPersistentDataContainer().getOrDefault(chargeKey, PersistentDataType.INTEGER, 0);

        // 1. Recharge Timer Check
        if (charges <= 0) {
            if (rechargeTimer.containsKey(player.getUniqueId())) {
                long lastUsed = rechargeTimer.get(player.getUniqueId());
                long secondsPassed = (System.currentTimeMillis() - lastUsed) / 1000;
                if (secondsPassed < 60) {
                    player.sendMessage(ChatColor.RED + "Recharging... (" + (60 - secondsPassed) + "s)");
                    return;
                } else {
                    charges = 3; // Time is up, reset
                    rechargeTimer.remove(player.getUniqueId());
                }
            } else {
                charges = 3;
            }
        }

        // 2. The Strike
        if (charges > 0) {
            victim.getWorld().strikeLightning(victim.getLocation());

            // ULTIMATE DAMAGE FIX: Bypasses everything by setting health directly
            double currentHealth = victim.getHealth();
            double finalHealth = Math.max(0, currentHealth - 4.0); // 4.0 = 2 hearts
            victim.setHealth(finalHealth);
            
            player.sendMessage(ChatColor.AQUA + "⚡ THE STORM PIERCES! ⚡");

            // 3. Update State
            charges--;
            if (charges == 0) {
                rechargeTimer.put(player.getUniqueId(), System.currentTimeMillis());
                player.sendMessage(ChatColor.YELLOW + "Depleted! 60s cooldown started.");
            }

            m.getPersistentDataContainer().set(chargeKey, PersistentDataType.INTEGER, charges);
            List<String> lore = m.getLore();
            if (lore != null && lore.size() >= 3) {
                lore.set(2, ChatColor.WHITE + "Charges: " + ChatColor.YELLOW + charges + " / 3");
                m.setLore(lore);
            }
            item.setItemMeta(m);
        }
    }
}
