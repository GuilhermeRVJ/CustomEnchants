package pt.customenchants;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Le e escreve os encantamentos personalizados no lore dos itens. */
public class EnchantManager {

    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
    public static final String BOOK_NAME_PREFIX = "Livro de Encantamento";

    public static String toRoman(int level) {
        return level >= 1 && level < ROMAN.length ? ROMAN[level] : String.valueOf(level);
    }

    public static int fromRoman(String roman) {
        for (int i = 1; i < ROMAN.length; i++) {
            if (ROMAN[i].equalsIgnoreCase(roman)) {
                return i;
            }
        }
        return -1;
    }

    public static String formatLine(CustomEnchant enchant, int level) {
        return ChatColor.GRAY + enchant.getDisplayName() + " " + toRoman(level);
    }

    /** Converte uma linha de lore em (encantamento, nivel), ou devolve null se nao for uma linha nossa. */
    private Map.Entry<CustomEnchant, Integer> parseLine(String line) {
        if (line == null) {
            return null;
        }
        String plain = ChatColor.stripColor(line).trim();
        int space = plain.lastIndexOf(' ');
        if (space <= 0) {
            return null;
        }
        CustomEnchant enchant = CustomEnchant.byDisplayName(plain.substring(0, space));
        int level = fromRoman(plain.substring(space + 1));
        if (enchant == null || level < 1) {
            return null;
        }
        return new java.util.AbstractMap.SimpleEntry<CustomEnchant, Integer>(enchant, level);
    }

    public Map<CustomEnchant, Integer> getAll(ItemStack item) {
        Map<CustomEnchant, Integer> result = new EnumMap<CustomEnchant, Integer>(CustomEnchant.class);
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return result;
        }
        List<String> lore = item.getItemMeta().getLore();
        if (lore == null) {
            return result;
        }
        for (String line : lore) {
            Map.Entry<CustomEnchant, Integer> entry = parseLine(line);
            if (entry != null) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public int getLevel(ItemStack item, CustomEnchant enchant) {
        Integer level = getAll(item).get(enchant);
        return level == null ? 0 : level;
    }

    /** Reescreve o lore: primeiro as linhas dos encantamentos, depois o resto do lore original. */
    private void write(ItemStack item, Map<CustomEnchant, Integer> enchants) {
        ItemMeta meta = item.getItemMeta();
        List<String> others = new ArrayList<String>();
        List<String> oldLore = meta.getLore();
        if (oldLore != null) {
            for (String line : oldLore) {
                if (parseLine(line) == null) {
                    others.add(line);
                }
            }
        }
        List<String> lore = new ArrayList<String>();
        for (Map.Entry<CustomEnchant, Integer> entry : enchants.entrySet()) {
            lore.add(formatLine(entry.getKey(), entry.getValue()));
        }
        lore.addAll(others);
        meta.setLore(lore.isEmpty() ? null : lore);
        item.setItemMeta(meta);
    }

    public void set(ItemStack item, CustomEnchant enchant, int level) {
        Map<CustomEnchant, Integer> all = getAll(item);
        all.put(enchant, Math.max(1, Math.min(level, enchant.getMaxLevel())));
        write(item, all);
    }

    public boolean remove(ItemStack item, CustomEnchant enchant) {
        Map<CustomEnchant, Integer> all = getAll(item);
        if (all.remove(enchant) == null) {
            return false;
        }
        write(item, all);
        return true;
    }

    public ItemStack createBook(CustomEnchant enchant, int level) {
        int lvl = Math.max(1, Math.min(level, enchant.getMaxLevel()));
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + BOOK_NAME_PREFIX + ChatColor.GRAY + " - " + ChatColor.WHITE
                + enchant.getDisplayName() + " " + toRoman(lvl));
        List<String> lore = new ArrayList<String>();
        lore.add(formatLine(enchant, lvl));
        lore.add("");
        lore.add(ChatColor.GRAY + enchant.getDescription());
        lore.add(ChatColor.DARK_GRAY + "Aplica em: " + enchant.getToolsDisplay());
        lore.add(ChatColor.YELLOW + "Arrasta o livro para cima do item no inventario.");
        meta.setLore(lore);
        // brilho de encantado sem mostrar o encantamento
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        book.setItemMeta(meta);
        return book;
    }

    public boolean isBook(ItemStack item) {
        if (item == null || item.getType() != Material.BOOK || !item.hasItemMeta()) {
            return false;
        }
        String name = item.getItemMeta().getDisplayName();
        return name != null && ChatColor.stripColor(name).startsWith(BOOK_NAME_PREFIX) && !getAll(item).isEmpty();
    }
}
