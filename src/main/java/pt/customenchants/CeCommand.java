package pt.customenchants;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CeCommand implements CommandExecutor, TabCompleter {

    private static final String ADMIN = "customenchants.admin";
    private static final List<String> SUBCOMMANDS = Arrays.asList("list", "enchant", "remove", "book", "reload");

    private final CustomEnchantsPlugin plugin;
    private final EnchantManager manager;

    public CeCommand(CustomEnchantsPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getEnchantManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }
        String sub = args[0].toLowerCase();

        if (sub.equals("list")) {
            sender.sendMessage(ChatColor.AQUA + "=== Encantamentos personalizados ===");
            for (CustomEnchant enchant : CustomEnchant.values()) {
                sender.sendMessage(ChatColor.WHITE + enchant.getDisplayName() + ChatColor.DARK_GRAY + " ("
                        + enchant.getKey() + ", max " + EnchantManager.toRoman(enchant.getMaxLevel()) + ")");
                sender.sendMessage(ChatColor.GRAY + "  " + enchant.getDescription()
                        + ChatColor.DARK_GRAY + " [" + enchant.getToolsDisplay() + "]");
            }
            return true;
        }

        if (!sender.hasPermission(ADMIN)) {
            sender.sendMessage(plugin.msg("no-permission"));
            return true;
        }

        if (sub.equals("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(plugin.msg("reloaded"));
            return true;
        }

        if (sub.equals("book")) {
            return handleBook(sender, label, args);
        }

        if (sub.equals("enchant") || sub.equals("remove")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.msg("players-only"));
                return true;
            }
            Player player = (Player) sender;
            ItemStack item = player.getItemInHand();
            if (item == null || item.getType() == Material.AIR) {
                player.sendMessage(plugin.msg("hold-item"));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Uso: /" + label + " " + sub + " <encantamento>"
                        + (sub.equals("enchant") ? " [nivel]" : ""));
                return true;
            }
            CustomEnchant enchant = CustomEnchant.byKey(args[1]);
            if (enchant == null) {
                player.sendMessage(plugin.msg("unknown-enchant"));
                return true;
            }

            if (sub.equals("remove")) {
                if (manager.remove(item, enchant)) {
                    player.setItemInHand(item);
                    player.sendMessage(plugin.msg("removed", "enchant", enchant.getDisplayName()));
                } else {
                    player.sendMessage(plugin.msg("not-present", "enchant", enchant.getDisplayName()));
                }
                return true;
            }

            if (!enchant.canApply(item)) {
                player.sendMessage(plugin.msg("not-applicable", "enchant", enchant.getDisplayName()));
                return true;
            }
            int level = parseLevel(args, 2, enchant);
            manager.set(item, enchant, level);
            player.setItemInHand(item);
            player.sendMessage(plugin.msg("applied", "enchant", enchant.getDisplayName(),
                    "level", EnchantManager.toRoman(Math.min(level, enchant.getMaxLevel()))));
            return true;
        }

        sendHelp(sender, label);
        return true;
    }

    private boolean handleBook(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /" + label + " book <encantamento> [nivel] [jogador]");
            return true;
        }
        CustomEnchant enchant = CustomEnchant.byKey(args[1]);
        if (enchant == null) {
            sender.sendMessage(plugin.msg("unknown-enchant"));
            return true;
        }
        int level = parseLevel(args, 2, enchant);
        Player target;
        if (args.length >= 4) {
            target = Bukkit.getPlayerExact(args[3]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Jogador nao encontrado.");
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(plugin.msg("players-only"));
            return true;
        }
        int shown = Math.min(level, enchant.getMaxLevel());
        java.util.Map<Integer, ItemStack> left = target.getInventory().addItem(manager.createBook(enchant, level));
        for (ItemStack rest : left.values()) {
            target.getWorld().dropItemNaturally(target.getLocation(), rest);
        }
        target.sendMessage(plugin.msg("book-given", "enchant", enchant.getDisplayName(),
                "level", EnchantManager.toRoman(shown)));
        return true;
    }

    private int parseLevel(String[] args, int index, CustomEnchant enchant) {
        if (args.length > index) {
            try {
                return Math.max(1, Integer.parseInt(args[index]));
            } catch (NumberFormatException ignored) {
                // usa o nivel maximo por omissao
            }
        }
        return enchant.getMaxLevel();
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.AQUA + "=== CustomEnchants ===");
        sender.sendMessage(ChatColor.WHITE + "/" + label + " list" + ChatColor.GRAY + " - lista os encantamentos");
        if (sender.hasPermission(ADMIN)) {
            sender.sendMessage(ChatColor.WHITE + "/" + label + " enchant <nome> [nivel]" + ChatColor.GRAY
                    + " - encanta o item na mao");
            sender.sendMessage(ChatColor.WHITE + "/" + label + " remove <nome>" + ChatColor.GRAY
                    + " - remove do item na mao");
            sender.sendMessage(ChatColor.WHITE + "/" + label + " book <nome> [nivel] [jogador]" + ChatColor.GRAY
                    + " - da um livro de encantamento");
            sender.sendMessage(ChatColor.WHITE + "/" + label + " reload" + ChatColor.GRAY + " - recarrega o config");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<String>();
        if (args.length == 1) {
            options.addAll(SUBCOMMANDS);
        } else if (args.length == 2 && Arrays.asList("enchant", "remove", "book").contains(args[0].toLowerCase())) {
            for (CustomEnchant enchant : CustomEnchant.values()) {
                options.add(enchant.getKey());
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("book")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                options.add(player.getName());
            }
        }
        String prefix = args[args.length - 1].toLowerCase();
        List<String> filtered = new ArrayList<String>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(prefix)) {
                filtered.add(option);
            }
        }
        return filtered;
    }
}
