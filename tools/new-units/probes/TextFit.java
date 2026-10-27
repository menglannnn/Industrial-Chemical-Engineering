package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Estimates the GUI width of every unit's constructor and datasheet text with the vanilla default font's advances
 * (ascii sheet; other glyphs 6, the marker 8) and prints what runs past its place: constructor held line 180 px (panel
 * 196, text at 8), row labels 105 (steppers at 116), titles 104 (else the short name), kind buttons 110, category
 * buttons 58, datasheet lines 403 (a 427-pixel-wide GUI at scale 2).
 */
public class TextFit {
    static final Map<String, String> LANG = new HashMap<>();

    static int width(String s) {
        int w = 0;
        for (char c : s.toCharArray()) {
            w += switch (c) {
                case ' ' -> 4;
                case '!', '\'', ',', '.', ':', ';', 'i', '|' -> 2;
                case '`', 'l' -> 3;
                case '"', '(', ')', '*', 'I', '[', ']', 't', '{', '}' -> 4;
                case '<', '>', 'f', 'k' -> 5;
                case '@', '~' -> 7;
                case '▶' -> 8;
                default -> 6;
            };
        }
        return w;
    }

    static String text(String key, Object... args) {
        String v = LANG.get(key);
        if (v == null) return "MISSING " + key;
        return String.format(Locale.ROOT, v, args);
    }

    static int bad = 0;

    static void check(String what, String s, int limit) {
        int w = width(s);
        if (w > limit) {
            bad++;
            System.out.println(what + " " + w + " > " + limit + ": " + s);
        }
    }

    public static void main(String[] a) throws Exception {
        String raw = Files.readString(Path.of("src/main/resources/assets/createcheme/lang/en_us.json"));
        var m = Pattern.compile("^  \"([^\"]+)\": \"((?:[^\"\\\\]|\\\\.)*)\"", Pattern.MULTILINE).matcher(raw);
        while (m.find()) {
            String v = m.group(2).replace("\\\"", "\"").replace("\\n", "\n");
            var u = Pattern.compile("\\\\u([0-9a-fA-F]{4})").matcher(v);
            var sb = new StringBuilder();
            while (u.find()) u.appendReplacement(sb, String.valueOf((char) Integer.parseInt(u.group(1), 16)));
            u.appendTail(sb);
            LANG.put(m.group(1), sb.toString());
        }
        String C = "gui.createcheme.equipment_constructor.", V = "gui.createcheme.vessel.";
        for (String c : new String[]{"vessels", "heat", "machines", "reactors", "solid_fuel"})
            check("category", "▶ " + text(C + "category." + c), 58);
        for (var kind : Kind.values()) {
            if (!kind.unit()) continue;
            var d = UnitDesign.of(kind);
            String k = kind.name().toLowerCase(Locale.ROOT);
            String full = text(C + "equipment." + k), shorter = text(C + "equipment." + k + ".short");
            if (width(full) > 104) check(kind + " short title", shorter, 104);
            check(kind + " kind button", "▶ " + shorter, 110);
            for (String row : new String[]{d.sizeRow(), d.lengthRow(), d.supportRow()})
                if (row != null) check(kind + " row", text(C + row), 104);
            double lo = d.minSize(), hi = d.maxSize();
            for (double size : new double[]{lo, ProcessUnit.snap(kind, (lo + hi) / 2), hi})
                for (int length : new int[]{d.minLength(size), d.maxLength(size)}) {
                    EquipmentShape shape;
                    try {
                        shape = EquipmentShape.unit(kind, size, length, Heading.NORTH, d.defaultSupport(size, length));
                    } catch (IllegalArgumentException e) {
                        continue;
                    }
                    var held = d.held(shape);
                    check(kind + " held " + UnitDesign.size(size) + "x" + length, text(C + held.key(), held.args()), 184);
                    check(kind + " kind line", text(V + "kind." + k + d.variant(shape)), 403);
                    for (var line : d.datasheet(shape)) check(kind + " datasheet", text(V + line.key(), line.args()), 403);
                    check(kind + " support line", text(V + "unit_support." + k, shape.support()), 403);
                }
        }
        System.out.println(bad + " over");
    }
}
