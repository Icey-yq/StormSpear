package me.stormweaver;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
        
        // Register the command
        getCommand("getspear").setExecutor(this);
        
        registerRecipe();
        getLogger().info("StormSpear (Fulgurite Obelisk) has been awakened!");
    }

    // This handles the /getspear command
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "You do not have permission to summon the Storm Spear!");
            return true;
        }

        player.getInventory().addItem(getSpear());
        player.sendMessage(ChatColor.GOLD + "The sky darkens as the " + ChatColor.BOLD + "Fulgurite Obelisk" + ChatColor.GOLD + " appears in your hands!");
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
            l.add(ChatColor.GRAY + "Effect: Direct Lightning Strike");
            m.setLore(l);

            m.getPersistentDataContainer().set(chargeKey, PersistentDataType.INTEGER, 3);
            s.setItemMeta(m);
        }
        return s;
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta m = item.getItemMeta();
        if (!m.hasCustomModelData() || m.getCustomModelData() != 123456) return;

        int charges = m.getPersistentDataContainer().getOrDefault(chargeKey, PersistentDataType.INTEGER, 0);

        if (charges > 0) {
            e.getEntity().getWorld().strikeLightning(e.getEntity().getLocation());
            e.setDamage(e.getDamage() + 4.0);

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
            player.sendMessage(ChatColor.RED + "The spear's energy is depleted...");
        }
    }
}
