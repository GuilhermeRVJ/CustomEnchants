package pt.customenchants;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/** Arrastar um livro de encantamento para cima de um item (no inventario do jogador) aplica o encantamento. */
public class BookListener implements Listener {

    private final CustomEnchantsPlugin plugin;
    private final EnchantManager manager;

    public BookListener(CustomEnchantsPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getEnchantManager();
    }

    @EventHandler
    public void onClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (event.getView().getTopInventory().getType() != InventoryType.CRAFTING
                || event.getSlotType() == InventoryType.SlotType.RESULT) {
            return; // so no inventario normal do jogador
        }
        ItemStack cursor = event.getCursor();
        ItemStack target = event.getCurrentItem();
        if (cursor == null || target == null || target.getType() == Material.AIR || !manager.isBook(cursor)) {
            return;
        }

        final Player player = (Player) event.getWhoClicked();
        Map<CustomEnchant, Integer> bookEnchants = manager.getAll(cursor);
        CustomEnchant enchant = bookEnchants.keySet().iterator().next();
        int bookLevel = bookEnchants.get(enchant);

        if (!enchant.canApply(target)) {
            player.sendMessage(plugin.msg("not-applicable", "enchant", enchant.getDisplayName()));
            return;
        }

        int existing = manager.getLevel(target, enchant);
        int newLevel;
        if (bookLevel > existing) {
            newLevel = bookLevel;
        } else if (bookLevel == existing && existing < enchant.getMaxLevel()) {
            newLevel = existing + 1;
        } else {
            player.sendMessage(plugin.msg("book-useless", "enchant", enchant.getDisplayName()));
            return;
        }

        event.setCancelled(true);
        manager.set(target, enchant, newLevel);
        event.setCurrentItem(target);
        if (cursor.getAmount() > 1) {
            ItemStack rest = cursor.clone();
            rest.setAmount(cursor.getAmount() - 1);
            event.setCursor(rest);
        } else {
            event.setCursor(null);
        }
        player.playSound(player.getLocation(), Sound.ANVIL_USE, 1f, 1.2f);
        player.sendMessage(plugin.msg("book-applied", "enchant", enchant.getDisplayName(),
                "level", EnchantManager.toRoman(newLevel)));

        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                player.updateInventory();
            }
        });
    }
}
