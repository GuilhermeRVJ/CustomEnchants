package pt.customenchants;

import org.bukkit.Material;

/** Tipos de ferramenta/arma a que os encantamentos se podem aplicar (nomes de Material da 1.8). */
public enum ToolType {
    PICKAXE("_PICKAXE", "Picareta"),
    AXE("_AXE", "Machado"),
    SHOVEL("_SPADE", "Pa"),
    HOE("_HOE", "Enxada"),
    SWORD("_SWORD", "Espada");

    private final String suffix;
    private final String display;

    ToolType(String suffix, String display) {
        this.suffix = suffix;
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }

    public boolean matches(Material material) {
        return material != null && material.name().endsWith(suffix);
    }

    public static ToolType of(Material material) {
        for (ToolType type : values()) {
            if (type.matches(material)) {
                return type;
            }
        }
        return null;
    }
}
