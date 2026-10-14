package com.wormzjl.createcheme.science.material;

/** Liquid render tint, independent of thermodynamic and optical properties. */
public record FluidAppearance(int rgb, double transparency, boolean estimated) {
    public FluidAppearance {
        if (rgb < 0 || rgb > 0xFFFFFF) throw new IllegalArgumentException("appearance.color must be 24-bit RGB");
        if (!Double.isFinite(transparency) || transparency < 0 || transparency > 1)
            throw new IllegalArgumentException("appearance.transparency must be finite and between 0 and 1");
    }

    /** Packed AARRGGBB tint. Transparency 0 is opaque; 1 is invisible. */
    public int argb() { return ((int) Math.round((1 - transparency) * 255) << 24) | rgb; }

}
