package pt.customenchants;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/** Aplica o efeito do encantamento Pressa aos jogadores que seguram a ferramenta. */
public class HasteTask extends BukkitRunnable {

    private final EnchantManager manager;

    public HasteTask(CustomEnchantsPlugin plugin) {
        this.manager = plugin.getEnchantManager();
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack item = player.getItemInHand();
            int level = manager.getLevel(item, CustomEnchant.HASTE);
            if (level <= 0) {
                continue;
            }
            int amplifier = level - 1;
            boolean strongerPresent = false;
            for (PotionEffect active : player.getActivePotionEffects()) {
                if (active.getType().equals(PotionEffectType.FAST_DIGGING) && active.getAmplifier() > amplifier) {
                    strongerPresent = true;
                    break;
                }
            }
            if (!strongerPresent) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 60, amplifier, true), true);
            }
        }
    }
}
