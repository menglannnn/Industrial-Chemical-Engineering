package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.regex.Pattern;

public class LangProbe {
    public static void main(String[] a) throws Exception {
        String text = Files.readString(Path.of("src/main/resources/assets/createcheme/lang/en_us.json"));
        var lang = new HashMap<String, String>();
        var m = Pattern.compile("^  \"([^\"]+)\": \"((?:[^\"\\\\]|\\\\.)*)\"", Pattern.MULTILINE).matcher(text);
        while (m.find()) lang.put(m.group(1), m.group(2));
        String C = "gui.createcheme.equipment_constructor.", V = "gui.createcheme.vessel.";
        int bad = 0, checked = 0;
        for (var kind : Kind.values()) {
            if (!kind.unit()) continue;
            var d = UnitDesign.of(kind);
            String k = kind.name().toLowerCase(Locale.ROOT);
            var keys = new LinkedHashMap<String, Integer>();
            for (String s : new String[]{"equipment." + k, "equipment." + k + ".hint", "equipment." + k + ".short"}) keys.put(C + s, -1);
            for (String row : new String[]{d.sizeRow(), d.lengthRow(), d.supportRow()})
                if (row != null) {
                    keys.put(C + row, -1);
                    keys.put(C + row + ".hint", -1);
                }
            keys.put(V + "title." + k, -1);
            keys.put(V + "unit_support." + k, 1);
            double lo = ProcessUnit.minDiameter(kind), hi = ProcessUnit.maxDiameter(kind);
            for (double size : new double[]{lo, ProcessUnit.snap(kind, (lo + hi) / 2), hi}) {
                int length = ProcessUnit.defaultLength(kind, size);
                var shape = EquipmentShape.unit(kind, size, length, Heading.NORTH, ProcessUnit.defaultSupport(kind, size, length));
                var held = d.held(shape);
                keys.put(C + held.key(), held.args().length);
                keys.put(C + held.key() + ".hint", -1);
                keys.put(V + "kind." + k + d.variant(shape), -1);
                for (var line : d.datasheet(shape)) keys.put(V + line.key(), line.args().length);
                for (var n : shape.nozzles()) keys.put(V + "role." + n.role().name().toLowerCase(Locale.ROOT), -1);
            }
            for (var e : keys.entrySet()) {
                checked++;
                String v = lang.get(e.getKey());
                if (v == null) {
                    bad++;
                    System.out.println("MISSING " + kind + " " + e.getKey());
                    continue;
                }
                int holes = 0;
                var h = Pattern.compile("%(\\d+\\$)?s").matcher(v);
                while (h.find()) holes++;
                if (e.getValue() >= 0 && holes != e.getValue()) {
                    bad++;
                    System.out.println("ARGS " + kind + " " + e.getKey() + " has " + holes + " %s for " + e.getValue() + " args: " + v);
                }
            }
        }
        for (String c : new String[]{"vessels", "heat", "machines", "reactors", "solid_fuel"})
            for (String s : new String[]{"", ".hint"})
                if (!lang.containsKey(C + "category." + c + s)) {
                    bad++;
                    System.out.println("MISSING category " + c + s);
                }
        System.out.println(checked + " keys checked, " + bad + " problems");
    }
}
