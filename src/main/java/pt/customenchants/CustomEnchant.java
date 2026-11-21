package pt.customenchants;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Lista de encantamentos personalizados. Sao guardados no lore dos itens (a 1.8 nao tem API para isto). */
public enum CustomEnchant {

    TELEPATHY("telepathy", "Telepatia", 1,
            "Drops e XP vao direto para o inventario.",
            ToolType.PICKAXE, ToolType.AXE, ToolType.SHOVEL, ToolType.HOE, ToolType.SWORD),

    AUTOPLANT("autoplant", "AutoPlant", 1,
            "Ao partires uma cultura madura, ela e replantada automaticamente.",
            ToolType.HOE),

    SMELTING("smelting", "Fundicao", 1,
            "Os drops saem ja fundidos (minerios, areia, pedra...).",
            ToolType.PICKAXE, ToolType.SHOVEL),

    TIMBER("timber", "Lenhador", 3,
            "Parte a arvore toda de uma vez.",
            ToolType.AXE),

    VEINMINER("veinminer", "Mineiro de Veios", 3,
            "Minera o veio de minerio inteiro de uma vez.",
            ToolType.PICKAXE),

    EXPERIENCE("experience", "Experiencia", 3,
            "Aumenta o XP obtido ao partir blocos e matar mobs.",
            ToolType.PICKAXE, ToolType.AXE, ToolType.SHOVEL, ToolType.SWORD),

    LIFESTEAL("lifesteal", "Vampirismo", 3,
            "Recuperas vida com o dano que causas.",
            ToolType.SWORD),

    HASTE("haste", "Pressa", 3,
            "Da Pressa (Haste) enquanto seguras a ferramenta.",
            ToolType.PICKAXE, ToolType.AXE, ToolType.SHOVEL);

    private final String key;
    private final String displayName;
    private final int maxLevel;
    private final String description;
    private final Set<ToolType> tools;

    CustomEnchant(String key, String displayName, int maxLevel, String description, ToolType... tools) {
        this.key = key;
        this.displayName = displayName;
        this.maxLevel = maxLevel;
        this.description = description;
        this.tools = EnumSet.copyOf(Arrays.asList(tools));
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public String getDescription() {
        return description;
    }

    public boolean canApply(ItemStack item) {
        if (item == null) {
            return false;
        }
        ToolType type = ToolType.of(item.getType());
        return type != null && tools.contains(type);
    }

    public String getToolsDisplay() {
        List<String> names = new ArrayList<String>();
        for (ToolType type : tools) {
            names.add(type.getDisplay());
        }
        return String.join(", ", names);
    }

    public static CustomEnchant byKey(String input) {
        if (input == null) {
            return null;
        }
        String normalized = input.toLowerCase().replace("_", "").replace("-", "");
        for (CustomEnchant enchant : values()) {
            if (enchant.key.equals(normalized)) {
                return enchant;
            }
        }
        return null;
    }

    public static CustomEnchant byDisplayName(String name) {
        if (name == null) {
            return null;
        }
        for (CustomEnchant enchant : values()) {
            if (enchant.displayName.equalsIgnoreCase(name.trim())) {
                return enchant;
            }
        }
        return null;
    }
}
