package pt.customenchants;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Telepatia e Experiencia ao matar mobs, e Vampirismo. */
public class CombatListener implements Listener {

    private final CustomEnchantsPlugin plugin;
    private final EnchantManager manager;

    public CombatListener(CustomEnchantsPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getEnchantManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead instanceof Player) {
            return; // nunca roubar o inventario de jogadores mortos
        }
        Player killer = dead.getKiller();
        if (killer == null) {
            return;
        }
        ItemStack weapon = killer.getItemInHand();
        if (weapon == null || weapon.getType() == Material.AIR) {
            return;
        }
        Map<CustomEnchant, Integer> enchants = manager.getAll(weapon);
        if (enchants.isEmpty()) {
            return;
        }

        int exp = event.getDroppedExp();
        Integer expLevel = enchants.get(CustomEnchant.EXPERIENCE);
        if (expLevel != null && exp > 0) {
            double bonus = plugin.getConfig().getDouble("experience.bonus-per-level", 0.5);
            exp = (int) Math.round(exp * (1 + bonus * expLevel));
            event.setDroppedExp(exp);
        }

        if (enchants.containsKey(CustomEnchant.TELEPATHY)) {
            List<ItemStack> drops = new ArrayList<ItemStack>(event.getDrops());
            event.getDrops().clear();
            BlockBreakListener.give(plugin, killer, drops);
            if (exp > 0) {
                killer.giveExp(exp);
                event.setDroppedExp(0);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (!(damager instanceof Player) || !(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        Player player = (Player) damager;
        ItemStack weapon = player.getItemInHand();
        int level = manager.getLevel(weapon, CustomEnchant.LIFESTEAL);
        if (level <= 0) {
            return;
        }
        double percent = plugin.getConfig().getDouble("lifesteal.percent-per-level", 10) / 100.0;
        double heal = event.getFinalDamage() * percent * level;
        if (heal > 0 && !player.isDead()) {
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + heal));
        }
    }
}
