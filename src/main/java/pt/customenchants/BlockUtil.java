package pt.customenchants;

import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Jukebox;
import org.bukkit.block.Skull;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Utilitarios de blocos para 1.8.8.
 * Nota: Block#getDrops(ItemStack) na 1.8 ignora a ferramenta (fortuna, silk touch, nivel de ferramenta)
 * e nao inclui as sementes extra das culturas, por isso tudo isso e tratado aqui.
 */
public final class BlockUtil {

    private static final Random RNG = new Random();

    private static final Set<Material> ORES = EnumSet.of(
            Material.COAL_ORE, Material.IRON_ORE, Material.GOLD_ORE, Material.DIAMOND_ORE,
            Material.EMERALD_ORE, Material.LAPIS_ORE, Material.REDSTONE_ORE,
            Material.GLOWING_REDSTONE_ORE, Material.QUARTZ_ORE);

    private static final Set<Material> FORTUNE_MULTIPLY = EnumSet.of(
            Material.COAL_ORE, Material.DIAMOND_ORE, Material.EMERALD_ORE,
            Material.LAPIS_ORE, Material.QUARTZ_ORE);

    private static final Set<Material> INSTANT_BREAK = EnumSet.of(
            Material.CROPS, Material.CARROT, Material.POTATO, Material.NETHER_WARTS,
            Material.LONG_GRASS, Material.DEAD_BUSH, Material.YELLOW_FLOWER, Material.RED_ROSE,
            Material.SAPLING, Material.TORCH, Material.SUGAR_CANE_BLOCK, Material.DOUBLE_PLANT,
            Material.REDSTONE_TORCH_ON, Material.REDSTONE_TORCH_OFF);

    /** Blocos que deixamos o servidor partir normalmente (estado interno ou varias partes). */
    private static final Set<Material> UNHANDLED = EnumSet.of(
            Material.BED_BLOCK, Material.FLOWER_POT, Material.DOUBLE_PLANT, Material.PISTON_EXTENSION,
            Material.PISTON_MOVING_PIECE, Material.CAKE_BLOCK);

    private static final Set<Material> SILK_TOUCHABLE = EnumSet.of(
            Material.STONE, Material.COAL_ORE, Material.IRON_ORE, Material.GOLD_ORE, Material.DIAMOND_ORE,
            Material.EMERALD_ORE, Material.LAPIS_ORE, Material.REDSTONE_ORE, Material.GLOWING_REDSTONE_ORE,
            Material.QUARTZ_ORE, Material.GLASS, Material.STAINED_GLASS, Material.THIN_GLASS,
            Material.STAINED_GLASS_PANE, Material.ICE, Material.PACKED_ICE, Material.GRASS, Material.MYCEL,
            Material.GLOWSTONE, Material.BOOKSHELF, Material.CLAY, Material.SNOW_BLOCK,
            Material.MELON_BLOCK, Material.LEAVES, Material.LEAVES_2);

    // tier da picareta necessaria: 0 = madeira/ouro, 1 = pedra, 2 = ferro, 3 = diamante
    private static final Map<Material, Integer> PICKAXE_TIER = new HashMap<Material, Integer>();

    static {
        Material[] tier0 = {Material.STONE, Material.COBBLESTONE, Material.MOSSY_COBBLESTONE, Material.COAL_ORE,
                Material.SANDSTONE, Material.RED_SANDSTONE, Material.NETHERRACK, Material.QUARTZ_ORE,
                Material.BRICK, Material.NETHER_BRICK, Material.HARD_CLAY, Material.STAINED_CLAY,
                Material.COBBLE_WALL, Material.ENDER_STONE, Material.PRISMARINE, Material.COAL_BLOCK,
                Material.SMOOTH_BRICK};
        Material[] tier1 = {Material.IRON_ORE, Material.LAPIS_ORE, Material.IRON_BLOCK, Material.LAPIS_BLOCK};
        Material[] tier2 = {Material.GOLD_ORE, Material.DIAMOND_ORE, Material.EMERALD_ORE, Material.REDSTONE_ORE,
                Material.GLOWING_REDSTONE_ORE, Material.GOLD_BLOCK, Material.DIAMOND_BLOCK,
                Material.EMERALD_BLOCK, Material.REDSTONE_BLOCK};
        for (Material m : tier0) PICKAXE_TIER.put(m, 0);
        for (Material m : tier1) PICKAXE_TIER.put(m, 1);
        for (Material m : tier2) PICKAXE_TIER.put(m, 2);
        PICKAXE_TIER.put(Material.OBSIDIAN, 3);
    }

    private static Map<Material, List<Object[]>> smeltCache;

    private BlockUtil() {
    }

    // ------------------------------------------------------------------ classificacao

    public static boolean isLog(Material m) {
        return m == Material.LOG || m == Material.LOG_2;
    }

    public static boolean isCrop(Material m) {
        return m == Material.CROPS || m == Material.CARROT || m == Material.POTATO || m == Material.NETHER_WARTS;
    }

    public static boolean isMature(Block b) {
        return b.getType() == Material.NETHER_WARTS ? b.getData() >= 3 : b.getData() >= 7;
    }

    public static boolean isOre(Material m) {
        return ORES.contains(m);
    }

    /** Trata minerio de redstone aceso e apagado como o mesmo bloco. */
    public static Material normalizeOre(Material m) {
        return m == Material.GLOWING_REDSTONE_ORE ? Material.REDSTONE_ORE : m;
    }

    public static Material seedFor(Material crop) {
        switch (crop) {
            case CROPS:
                return Material.SEEDS;
            case CARROT:
                return Material.CARROT_ITEM;
            case POTATO:
                return Material.POTATO_ITEM;
            case NETHER_WARTS:
                return Material.NETHER_STALK;
            default:
                return null;
        }
    }

    public static boolean isInstantBreak(Material m) {
        return INSTANT_BREAK.contains(m);
    }

    /** Blocos que nao devemos partir "a mao" (baus, fornos, spawners, cabecas...), para nao perder o conteudo. */
    public static boolean isUnhandleable(Block block) {
        if (UNHANDLED.contains(block.getType())) {
            return true;
        }
        BlockState state = block.getState();
        return state instanceof InventoryHolder || state instanceof Skull || state instanceof Banner
                || state instanceof CreatureSpawner || state instanceof Jukebox;
    }

    // ------------------------------------------------------------------ colheita e drops

    public static boolean canHarvest(Material type, ItemStack tool) {
        Integer needed = PICKAXE_TIER.get(type);
        if (needed == null) {
            return true;
        }
        if (tool == null || ToolType.PICKAXE.matches(tool.getType()) == false) {
            return false;
        }
        String name = tool.getType().name();
        int tier;
        if (name.startsWith("DIAMOND")) {
            tier = 3;
        } else if (name.startsWith("IRON")) {
            tier = 2;
        } else if (name.startsWith("STONE")) {
            tier = 1;
        } else {
            tier = 0;
        }
        return tier >= needed;
    }

    public static List<ItemStack> getDrops(Block block, ItemStack tool) {
        Material type = block.getType();
        byte data = block.getData();
        int fortune = tool == null ? 0 : tool.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);
        boolean silk = tool != null && tool.containsEnchantment(Enchantment.SILK_TOUCH);
        List<ItemStack> out = new ArrayList<ItemStack>();

        if (isCrop(type)) {
            cropDrops(block, fortune, out);
            return out;
        }

        if (silk && SILK_TOUCHABLE.contains(type)) {
            Material dropType = normalizeOre(type);
            short dropData = 0;
            if (type == Material.STONE || type == Material.STAINED_GLASS || type == Material.STAINED_GLASS_PANE) {
                dropData = data;
            } else if (type == Material.LEAVES || type == Material.LEAVES_2) {
                dropData = (short) (data & 3);
            }
            out.add(new ItemStack(dropType, 1, dropData));
            return out;
        }

        // ferramenta "limpa" (sem encantamentos), porque o getDrops da 1.8 nao a usa de qualquer forma
        ItemStack plainTool = new ItemStack(tool == null ? Material.AIR : tool.getType());
        for (ItemStack drop : block.getDrops(plainTool)) {
            if (drop != null && drop.getType() != Material.AIR && drop.getAmount() > 0) {
                out.add(drop.clone());
            }
        }

        if (fortune > 0) {
            if (type == Material.REDSTONE_ORE || type == Material.GLOWING_REDSTONE_ORE) {
                for (ItemStack drop : out) {
                    drop.setAmount(drop.getAmount() + RNG.nextInt(fortune + 1));
                }
            } else if (FORTUNE_MULTIPLY.contains(type)) {
                int multiplier = Math.max(0, RNG.nextInt(fortune + 2) - 1) + 1;
                for (ItemStack drop : out) {
                    drop.setAmount(drop.getAmount() * multiplier);
                }
            }
        }
        return out;
    }

    private static void cropDrops(Block block, int fortune, List<ItemStack> out) {
        Material type = block.getType();
        boolean mature = isMature(block);
        int age = block.getData();

        if (type == Material.NETHER_WARTS) {
            int amount = 1;
            if (mature) {
                amount = 2 + RNG.nextInt(3) + (fortune > 0 ? RNG.nextInt(fortune + 1) : 0);
            }
            out.add(new ItemStack(Material.NETHER_STALK, amount));
            return;
        }

        Material seed = seedFor(type);
        if (type == Material.CROPS) {
            if (mature) {
                out.add(new ItemStack(Material.WHEAT, 1));
            }
            int seeds = mature ? 0 : 1;
            if (mature) {
                for (int i = 0; i < 3 + fortune; i++) {
                    if (RNG.nextInt(15) <= age) {
                        seeds++;
                    }
                }
            }
            if (seeds > 0) {
                out.add(new ItemStack(seed, seeds));
            }
            return;
        }

        // cenouras e batatas: 1 base + tentativas extra quando maduras
        int amount = 1;
        if (mature) {
            for (int i = 0; i < 3 + fortune; i++) {
                if (RNG.nextInt(15) <= age) {
                    amount++;
                }
            }
        }
        out.add(new ItemStack(seed, amount));
        if (type == Material.POTATO && mature && RNG.nextInt(50) == 0) {
            out.add(new ItemStack(Material.POISONOUS_POTATO, 1));
        }
    }

    /** XP base que o minerio dropa (so para os blocos extra do Mineiro de Veios). */
    public static int baseExp(Material type) {
        switch (normalizeOre(type)) {
            case COAL_ORE:
                return RNG.nextInt(3);
            case DIAMOND_ORE:
            case EMERALD_ORE:
                return 3 + RNG.nextInt(5);
            case LAPIS_ORE:
            case QUARTZ_ORE:
                return 2 + RNG.nextInt(4);
            case REDSTONE_ORE:
                return 1 + RNG.nextInt(5);
            default:
                return 0;
        }
    }

    // ------------------------------------------------------------------ fundicao

    private static synchronized void buildSmeltCache() {
        if (smeltCache != null) {
            return;
        }
        smeltCache = new HashMap<Material, List<Object[]>>();
        Iterator<Recipe> it = org.bukkit.Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe recipe = it.next();
            if (recipe instanceof FurnaceRecipe) {
                FurnaceRecipe furnace = (FurnaceRecipe) recipe;
                ItemStack input = furnace.getInput();
                if (input == null) {
                    continue;
                }
                List<Object[]> list = smeltCache.get(input.getType());
                if (list == null) {
                    list = new ArrayList<Object[]>();
                    smeltCache.put(input.getType(), list);
                }
                list.add(new Object[]{(int) input.getDurability(), furnace.getResult().clone()});
            }
        }
    }

    /** Devolve o resultado fundido de um item, ou null se nao houver receita. */
    public static ItemStack smelt(ItemStack item) {
        if (isLog(item.getType())) {
            return null; // nao transformar madeira em carvao
        }
        buildSmeltCache();
        List<Object[]> options = smeltCache.get(item.getType());
        if (options == null) {
            return null;
        }
        for (Object[] option : options) {
            int data = (Integer) option[0];
            if (data == Short.MAX_VALUE || data < 0 || data == item.getDurability()) {
                ItemStack result = ((ItemStack) option[1]).clone();
                result.setAmount(result.getAmount() * item.getAmount());
                return result;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ utilitarios

    /** Pesquisa em largura de blocos ligados (26 vizinhos). O bloco inicial vem sempre em primeiro. */
    public static List<Block> floodFill(Block start, int max, Predicate<Block> accept) {
        List<Block> result = new ArrayList<Block>();
        Set<Long> visited = new HashSet<Long>();
        LinkedList<Block> queue = new LinkedList<Block>();
        queue.add(start);
        visited.add(key(start));
        while (!queue.isEmpty() && result.size() < max) {
            Block current = queue.poll();
            result.add(current);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        Block next = current.getRelative(dx, dy, dz);
                        if (visited.add(key(next)) && accept.test(next)) {
                            queue.add(next);
                        }
                    }
                }
            }
        }
        return result;
    }

    private static long key(Block b) {
        return ((long) (b.getX() & 0x3FFFFFF) << 38) | ((long) (b.getZ() & 0x3FFFFFF) << 12) | (b.getY() & 0xFFF);
    }

    /** Parte stacks acima do tamanho maximo e junta stacks iguais. */
    public static List<ItemStack> normalize(List<ItemStack> drops) {
        List<ItemStack> merged = new ArrayList<ItemStack>();
        for (ItemStack drop : drops) {
            if (drop == null || drop.getType() == Material.AIR || drop.getAmount() <= 0) {
                continue;
            }
            boolean added = false;
            for (ItemStack existing : merged) {
                if (existing.isSimilar(drop)) {
                    existing.setAmount(existing.getAmount() + drop.getAmount());
                    added = true;
                    break;
                }
            }
            if (!added) {
                merged.add(drop.clone());
            }
        }
        List<ItemStack> result = new ArrayList<ItemStack>();
        for (ItemStack stack : merged) {
            int max = Math.max(1, stack.getMaxStackSize());
            int remaining = stack.getAmount();
            while (remaining > 0) {
                ItemStack part = stack.clone();
                part.setAmount(Math.min(max, remaining));
                result.add(part);
                remaining -= part.getAmount();
            }
        }
        return result;
    }
}
