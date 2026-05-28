package me.stormweaver;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class StormSpear extends JavaPlugin implements Listener {

    private final NamespacedKey chargeKey = new NamespacedKey(this, "charges");
    private final NamespacedKey craftLimitKey = new NamespacedKey(this, "crafted");
    private final HashMap<UUID, Long> cdMap = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        registerRecipe();
    }

    private void registerRecipe() {
        ShapedRecipe r = new ShapedRecipe(new NamespacedKey(this, "storm_recipe"), getSpear());
        r.shape("BCB", "TNT", "BLB");
        r.setIngredient('B', Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE);
        r.setIngredient('C', Material.CONDUIT);
        r.setIngredient('T', Material.TRIDENT);
        // Using valueOf to bypass the "Cannot find symbol" error
        r.setIngredient('N', Material.valueOf("NETHERITE_SPEAR"));
        r.setIngredient('L', Material.LIGHTNING_ROD);
        Bukkit.addRecipe(r);
    }

    public ItemStack getSpear() {
        // Using valueOf here as well
        ItemStack s = new ItemStack(Material.valueOf("NETHERITE_SPEAR"));
        ItemMeta m = s.getItemMeta();
        if (m != null) {
            m.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "The Fulgurite Obelisk");
            List<String> l = new ArrayList<>();
            l.add(ChatColor.DARK_PURPLE + "Relic of the Primal Gale");
            l.add("");
            l.add(ChatColor.WHITE + "Charges: " + ChatColor.YELLOW + "3 / 3");
            l.add(ChatColor.GRAY + "Range: 5 Blocks");
            l.add(ChatColor.DARK_GRAY + "Cooldown: 60s");
            m.setLore(l);
            m.getPersistentDataContainer().set(chargeKey, PersistentDataType.INTEGER, 3);
            s.setItemMeta(m);
        }
        return s;
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (e.getRecipe().getResult().getItemMeta() == null) return;
        if (e.getRecipe().getResult().getItemMeta().getDisplayName().contains("Obelisk")) {
            Player p = (Player) e.getWhoClicked();
            if (p.getPersistentDataContainer().has(craftLimitKey, PersistentDataType.BYTE)) {
                p.sendMessage(ChatColor.RED + "The secret of the forge has left your mind. You can only craft this once.");
                e.setCancelled(true);
            } else {
                p.getPersistentDataContainer().set(craftLimitKey, PersistentDataType.BYTE, (byte) 1);
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.5f);
            }
        }
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getAction() != Action.LEFT_CLICK_AIR && e.getAction() != Action.LEFT_CLICK_BLOCK) return;
        
        ItemStack item = e.getItem();
        // Check if it's the spear using a string check to be safe
        if (item == null || !item.getType().name().equals("NETHERITE_SPEAR") || !item.hasItemMeta()) return;
        
        ItemMeta m = item.getItemMeta();
        if (!m.getPersistentDataContainer().has(chargeKey, PersistentDataType.INTEGER)) return;

        Player p = e.getPlayer();
        int charges = m.getPersistentDataContainer().get(chargeKey, PersistentDataType.INTEGER);
        
        if (charges <= 0) {
            long remaining = (cdMap.getOrDefault(p.getUniqueId(), 0L) + 60000) - System.currentTimeMillis();
            if (remaining > 0) {
                p.sendMessage(ChatColor.RED + "Recharging: " + (remaining/1000) + "s");
                return;
            }
            charges = 3;
        }

        Block b = p.getTargetBlockExact(5);
        if (b == null) return;

        Location loc = b.getLocation();
        loc.getWorld().strikeLightningEffect(loc);
        loc.getWorld().spawnParticle(Particle.DUST, loc.add(0.5, 1, 0.5), 50, 0.5, 0.5, 0.5, new Particle.DustOptions(Color.YELLOW, 2f));
        
        for (Entity target : loc.getWorld().getNearbyEntities(loc, 2, 2, 2)) {
            if (target instanceof LivingEntity le && !le.equals(p)) {
                le.damage(4.0, p);
            }
        }

        charges--;
        m.getPersistentDataContainer().set(chargeKey, PersistentDataType.INTEGER, charges);
        List<String> lore = m.getLore();
        if (lore != null && lore.size() > 2) {
            lore.set(2, ChatColor.WHITE + "Charges: " + ChatColor.YELLOW + charges + " / 3");
            m.setLore(lore);
        }
        item.setItemMeta(m);

        if (charges == 0) cdMap.put(p.getUniqueId(), System.currentTimeMillis());
    }
}
