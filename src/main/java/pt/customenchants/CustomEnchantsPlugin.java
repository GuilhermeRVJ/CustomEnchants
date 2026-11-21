package pt.customenchants;

import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomEnchantsPlugin extends JavaPlugin {

    private EnchantManager enchantManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        enchantManager = new EnchantManager();

        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new BookListener(this), this);

        CeCommand command = new CeCommand(this);
        PluginCommand ce = getCommand("ce");
        ce.setExecutor(command);
        ce.setTabCompleter(command);

        // Pressa: renova o efeito a cada segundo enquanto o jogador segura a ferramenta
        new HasteTask(this).runTaskTimer(this, 20L, 20L);

        getLogger().info("CustomEnchants ativado com " + CustomEnchant.values().length + " encantamentos.");
    }

    public EnchantManager getEnchantManager() {
        return enchantManager;
    }

    /** Mensagem do config com prefixo, cores e placeholders ({chave}, valor, {chave}, valor...). */
    public String msg(String key, String... replacements) {
        String text = getConfig().getString("messages." + key, key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix", "") + text);
    }
}
