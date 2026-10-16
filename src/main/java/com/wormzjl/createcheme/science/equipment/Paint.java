package com.wormzjl.createcheme.science.equipment;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A vessel's paint (owner request 2026-10-16: "allow custom painting color for the metal parts while building them",
 * with a palette and a few good industrial defaults), as 0xRRGGBB: one colour for its body (shell, heads, cladding, a
 * unit's casings) and one for its structure (skirt, saddles, steelwork). Pipes, flanges, gratings, handrails, motors and
 * actuators keep their own colours ({@link Mesh.Material#channel()}). Chosen in the constructor and kept on every shell
 * cell; the client tints the light neutral sprites of the painted materials with it.
 */
public record Paint(int body, int structure) {
    public static final int BODY = 0, STRUCTURE = 1;
    /**
     * The painted sprites' mean value (tools/custom-equipment-rendering-poc/textures, {@code NEUTRAL}): a tint scales a
     * colour by 255 / this, so the sprite's mean shows the paint itself.
     */
    public static final int SPRITE_VALUE = 236;

    /** A palette colour: its RAL Classic number and a common sRGB rendering of it (approximate, as every screen's is). */
    public record Swatch(String ral, int rgb) {
        /** The swatch's name key under {@code gui.createcheme.paint.}. */
        public String key() {
            return "ral_" + ral;
        }
    }

    /**
     * Sixteen colours seen on process plants: aluminium and greys for vessels and steel, whites and ivory for tanks,
     * signal colours, blues and greens.
     */
    public static final List<Swatch> PALETTE = List.of(
            new Swatch("9006", 0xA1A1A0), new Swatch("9007", 0x878581), new Swatch("9010", 0xF1EDE1), new Swatch("1015", 0xE6D2B5),
            new Swatch("7035", 0xCBD0CC), new Swatch("7001", 0x8A9597), new Swatch("7031", 0x5B686D), new Swatch("7016", 0x383E42),
            new Swatch("1023", 0xF7B500), new Swatch("2004", 0xE25303), new Swatch("3000", 0xA72920), new Swatch("3009", 0x642424),
            new Swatch("5015", 0x2271B3), new Swatch("5010", 0x0E4C92), new Swatch("6011", 0x587246), new Swatch("6005", 0x2F4538));
    /** RAL 9006 white aluminium on RAL 7031 blue grey steel: an aluminium-painted vessel on a primed and painted skirt. */
    public static final Paint DEFAULT = new Paint(0xA1A1A0, 0x5B686D);

    public Paint {
        if ((body & ~0xFFFFFF) != 0 || (structure & ~0xFFFFFF) != 0) throw new IllegalArgumentException("A paint colour is 0xRRGGBB");
    }

    /** The colour of {@code channel} ({@link #BODY} or {@link #STRUCTURE}). */
    public int colour(int channel) {
        return channel == STRUCTURE ? structure : body;
    }

    public Paint with(int channel, int rgb) {
        return channel == STRUCTURE ? new Paint(body, rgb) : new Paint(rgb, structure);
    }

    /** The palette's swatch of this colour, or null for a custom colour. */
    public static @Nullable Swatch swatch(int rgb) {
        for (var swatch : PALETTE) if (swatch.rgb() == rgb) return swatch;
        return null;
    }

    /** {@code rgb} as #RRGGBB, as the config and the Paint page's field write it. */
    public static String hex(int rgb) {
        return String.format(java.util.Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    /** Whether {@code value} is a colour as #RRGGBB (the # optional). */
    public static boolean isHex(@Nullable Object value) {
        return value instanceof String s && s.matches("#?[0-9a-fA-F]{6}");
    }

    /** @throws IllegalArgumentException unless {@code text} is a colour as #RRGGBB (the # optional) */
    public static int parseHex(String text) {
        if (!isHex(text)) throw new IllegalArgumentException("A colour is #RRGGBB, not " + text);
        return Integer.parseInt(text.startsWith("#") ? text.substring(1) : text, 16);
    }

    /** The tint that shows {@code rgb} on a painted sprite: each channel scaled by 255 / {@link #SPRITE_VALUE}, at most 255. */
    public static int tint(int rgb) {
        int out = 0;
        for (int shift = 16; shift >= 0; shift -= 8) out = out << 8 | Math.min(255, Math.round((rgb >> shift & 0xFF) * 255F / SPRITE_VALUE));
        return out;
    }

    public int[] encode() {
        return new int[]{body, structure};
    }

    /** @throws IllegalArgumentException unless {@code values} holds a body and a structure colour */
    public static Paint decode(int[] values) {
        if (values.length != 2) throw new IllegalArgumentException("A paint holds a body and a structure colour");
        return new Paint(values[0], values[1]);
    }
}
