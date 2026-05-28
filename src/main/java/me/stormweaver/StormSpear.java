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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class StormSpear extends JavaPlugin implements Listener, CommandExecutor {

    private NamespacedKey chargeKey;

    @Override
    public void onEnable() {
        this.chargeKey = new NamespacedKey(this, "spear_charges");
        getServer().getPluginManager().registerEvents(this, this);
        
        if (getCommand("getspear") != null) {
            getCommand("getspear").setExecutor(this);
        }
        
        registerRecipe();
        getLogger().info("StormSpear Final Build: Infinite Loop Patch applied.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (player.isOp()) {
            player.getInventory().addItem(getSpear());
            player.sendMessage(ChatColor.GOLD + "The Fulgurite Obelisk is yours.");
        } else {
            player.sendMessage(ChatColor.RED + "You aren't powerful enough to summon this.");
        }
        return true;
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
            l.add(ChatColor.AQUA + "Passive: " + ChatColor.WHITE + "Armor-Piercing Strike");
            m.setLore(l);

            m.getPersistentDataContainer().set(chargeKey, PersistentDataType.INTEGER, 3);
            s.setItemMeta(m);
        }
        return s;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        // 1. Only trigger if a PLAYER is the one hitting
        if (!(e.getDamager() instanceof Player player)) return;

        // 2. THE LOOP FIX: Only run if the damage is from a physical arm swing.
        // When the plugin deals damage, the cause is NOT ENTITY_ATTACK, so it stops here.
        if (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        // 3. Ensure the target is a Living Entity
        if (!(e.getEntity() instanceof LivingEntity victim)) return;

        // 4. Verify the item in hand is our spear
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta m = item.getItemMeta();
        if (!m.hasCustomModelData() || m.getCustomModelData() != 123456) return;

        int charges = m.getPersistentDataContainer().getOrDefault(chargeKey, PersistentDataType.INTEGER, 0);

        if (charges > 0) {
            // Visual/Sound Effect
            victim.getWorld().strikeLightning(victim.getLocation());

            // TRUE DAMAGE (4.0 = 2 Hearts) 
            // We don't pass 'player' here to make the damage 'anonymous' to the event system
            victim.damage(4.0); 

            // Update Charges
            charges--;
            m.getPersistentDataContainer().set(chargeKey, PersistentDataType.INTEGER, charges);

            List<String> lore = m.getLore();
            if (lore != null && lore.size() >= 3) {
                lore.set(2, ChatColor.WHITE + "Charges: " + ChatColor.YELLOW + charges + " / 3");
                m.setLore(lore);
            }
            item.setItemMeta(m);
            player.sendMessage(ChatColor.AQUA + "⚡ THE STORM STRIKES! ⚡");
        } else {
            player.sendMessage(ChatColor.RED + "The spear's core is cold...");
        }
    }
}
