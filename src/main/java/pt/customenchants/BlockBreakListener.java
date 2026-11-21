package pt.customenchants;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Telepatia, Plantacao Automatica, Fundicao, Lenhador, Mineiro de Veios e Experiencia (blocos).
 * Quando um destes encantamentos e relevante, o evento e cancelado e o bloco e partido manualmente,
 * porque a 1.8 nao tem BlockDropItemEvent.
 */
public class BlockBreakListener implements Listener {

    private final CustomEnchantsPlugin plugin;
    private final EnchantManager manager;
    private final Set<UUID> busy = new HashSet<UUID>();
    private final Random random = new Random();

    public BlockBreakListener(CustomEnchantsPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getEnchantManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (busy.contains(player.getUniqueId())) {
            return; // evento sintetico que nos proprios lancamos para respeitar protecoes
        }
        GameMode mode = player.getGameMode();
        if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
            return;
        }
        ItemStack tool = player.getItemInHand();
        if (tool == null || tool.getType() == Material.AIR) {
            return;
        }
        Map<CustomEnchant, Integer> enchants = manager.getAll(tool);
        if (enchants.isEmpty()) {
            return;
        }

        Block block = event.getBlock();
        if (BlockUtil.isUnhandleable(block)) {
            return;
        }
        Material type = block.getType();

        boolean telepathy = enchants.containsKey(CustomEnchant.TELEPATHY);
        boolean smelting = enchants.containsKey(CustomEnchant.SMELTING);
        int expLevel = enchants.containsKey(CustomEnchant.EXPERIENCE) ? enchants.get(CustomEnchant.EXPERIENCE) : 0;

        // ---- escolher os blocos a partir
        List<Block> targets = new ArrayList<Block>();
        targets.add(block);
        boolean replant = false;

        if (autoPlant != null && BlockUtil.isCrop(type)) {
            if (!BlockUtil.isMature(block)) {
                event.setCancelled(true); // protege culturas que ainda nao estao 100% crescidas
                return;
            }
            replant = true; // so replanta o proprio bloco partido
        }

        Integer timber = enchants.get(CustomEnchant.TIMBER);
        if (timber != null && BlockUtil.isLog(type) && sneakAllowed("timber", player)) {
            final int species = block.getData() & 3;
            final Material logType = type;
            int max = timber * plugin.getConfig().getInt("timber.blocks-per-level", 40);
            targets = BlockUtil.floodFill(block, max, b -> b.getType() == logType && (b.getData() & 3) == species);
        }

        Integer vein = enchants.get(CustomEnchant.VEINMINER);
        if (vein != null && BlockUtil.isOre(type) && sneakAllowed("vein-miner", player)) {
            final Material ore = BlockUtil.normalizeOre(type);
            int max = vein * plugin.getConfig().getInt("vein-miner.blocks-per-level", 8);
            targets = BlockUtil.floodFill(block, max, b -> BlockUtil.normalizeOre(b.getType()) == ore);
        }

        boolean needsHandling = telepathy || smelting || replant || targets.size() > 1
                || (expLevel > 0 && event.getExpToDrop() > 0);
        if (!needsHandling) {
            return;
        }

        // ---- partir os blocos manualmente
        event.setCancelled(true);
        UUID id = player.getUniqueId();
        busy.add(id);
        try {
            boolean silk = tool.containsEnchantment(Enchantment.SILK_TOUCH);
            List<ItemStack> telepathyDrops = new ArrayList<ItemStack>();
            int totalExp = 0;
            boolean toolBroken = false;

            for (int i = 0; i < targets.size() && !toolBroken; i++) {
                Block target = targets.get(i);
                Material targetType = target.getType();

                int blockExp;
                if (i == 0) {
                    blockExp = event.getExpToDrop();
                } else {
                    // respeitar plugins de protecao nos blocos extra
                    BlockBreakEvent sub = new BlockBreakEvent(target, player);
                    Bukkit.getPluginManager().callEvent(sub);
                    if (sub.isCancelled()) {
                        continue;
                    }
                    blockExp = silk ? 0 : BlockUtil.baseExp(targetType);
                }

                List<ItemStack> drops = new ArrayList<ItemStack>();
                if (BlockUtil.canHarvest(targetType, tool)) {
                    drops = BlockUtil.getDrops(target, tool);
                } else {
                    blockExp = 0;
                }

                if (smelting) {
                    List<ItemStack> smelted = new ArrayList<ItemStack>();
                    for (ItemStack drop : drops) {
                        ItemStack result = BlockUtil.smelt(drop);
                        smelted.add(result != null ? result : drop);
                    }
                    drops = smelted;
                }

                target.getWorld().playEffect(target.getLocation(), Effect.STEP_SOUND, target.getTypeId());

                boolean planted = false;
                if (replant && BlockUtil.isCrop(targetType)) {
                    Material seed = BlockUtil.seedFor(targetType);
                    planted = takeOne(drops, seed) || takeFromInventory(player, seed);
                }
                if (planted) {
                    target.setTypeIdAndData(targetType.getId(), (byte) 0, true);
                } else {
                    target.setType(Material.AIR);
                }

                if (expLevel > 0 && blockExp > 0) {
                    double bonus = plugin.getConfig().getDouble("experience.bonus-per-level", 0.5);
                    blockExp = (int) Math.round(blockExp * (1 + bonus * expLevel));
                }

                if (telepathy) {
                    telepathyDrops.addAll(drops);
                    totalExp += blockExp;
                } else {
                    Location loc = target.getLocation();
                    for (ItemStack drop : BlockUtil.normalize(drops)) {
                        target.getWorld().dropItemNaturally(loc, drop);
                    }
                    if (blockExp > 0) {
                        ExperienceOrb orb = (ExperienceOrb) target.getWorld()
                                .spawnEntity(loc.clone().add(0.5, 0.5, 0.5), EntityType.EXPERIENCE_ORB);
                        orb.setExperience(blockExp);
                    }
                }

                if (!BlockUtil.isInstantBreak(targetType)) {
                    toolBroken = damageTool(player, tool);
                }
            }

            if (telepathy) {
                give(player, telepathyDrops);
                if (totalExp > 0) {
                    player.giveExp(totalExp);
                }
            }
            if (!toolBroken) {
                player.setItemInHand(tool);
            }
            player.updateInventory();
        } finally {
            busy.remove(id);
        }
    }

    private boolean sneakAllowed(String section, Player player) {
        return !plugin.getConfig().getBoolean(section + ".require-sneak", false) || player.isSneaking();
    }

    private boolean takeOne(List<ItemStack> drops, Material material) {
        Iterator<ItemStack> it = drops.iterator();
        while (it.hasNext()) {
            ItemStack drop = it.next();
            if (drop.getType() == material && drop.getAmount() > 0) {
                if (drop.getAmount() == 1) {
                    it.remove();
                } else {
                    drop.setAmount(drop.getAmount() - 1);
                }
                return true;
            }
        }
        return false;
    }

    private boolean takeFromInventory(Player player, Material material) {
        if (!player.getInventory().contains(material)) {
            return false;
        }
        player.getInventory().removeItem(new ItemStack(material, 1));
        return true;
    }

    /** Aplica 1 ponto de dano (2 para espadas), respeitando Inquebravel. Devolve true se a ferramenta partiu. */
    private boolean damageTool(Player player, ItemStack tool) {
        short max = tool.getType().getMaxDurability();
        if (max <= 0) {
            return false;
        }
        int damage = ToolType.SWORD.matches(tool.getType()) ? 2 : 1;
        int unbreaking = tool.getEnchantmentLevel(Enchantment.DURABILITY);
        int applied = 0;
        for (int i = 0; i < damage; i++) {
            if (unbreaking <= 0 || random.nextInt(unbreaking + 1) == 0) {
                applied++;
            }
        }
        int newDurability = tool.getDurability() + applied;
        if (newDurability > max) {
            player.setItemInHand(null);
            player.playSound(player.getLocation(), Sound.ITEM_BREAK, 1f, 1f);
            return true;
        }
        tool.setDurability((short) newDurability);
        return false;
    }

    static void give(CustomEnchantsPlugin plugin, Player player, List<ItemStack> drops) {
        boolean overflow = false;
        for (ItemStack stack : BlockUtil.normalize(drops)) {
            Map<Integer, ItemStack> left = player.getInventory().addItem(stack);
            for (ItemStack rest : left.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), rest);
                overflow = true;
            }
        }
        if (overflow) {
            player.sendMessage(plugin.msg("inventory-full"));
        }
    }

    private void give(Player player, List<ItemStack> drops) {
        give(plugin, player, drops);
    }
}
