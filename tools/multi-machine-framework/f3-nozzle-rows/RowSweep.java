package com.wormzjl.icheme.science.equipment;

import java.util.*;

/**
 * How far ColumnStructure's row search moves each side nozzle from the row its stage wants, over a grid of column designs:
 * the trays each move crosses, and the moves that put a nozzle on the wrong side of its stage (sump nozzles above the
 * bottom tray, reflux below the top tray, a pumparound return below its draw).
 */
public final class RowSweep {
    record Want(EquipmentShape.Role role, double y, int stage) {}

    public static void main(String[] args) {
        int designs = 0, refused = 0, crossing = 0, wrongSide = 0;
        var crossByRole = new TreeMap<String, Integer>();
        var wrongByRole = new TreeMap<String, Integer>();
        var examples = new ArrayList<String>();
        for (double d : new double[]{1, 1.5, 2, 3, 4, 6, 8})
            for (int n : new int[]{5, 10, 15, 20, 30, 40})
                for (String config : new String[]{"kettle", "kettle+steam+draw+PA", "steam+draw"}) {
                    var t = trays(n, config);
                    var s = EquipmentShape.column(d, t, EquipmentShape.Heading.NORTH, EquipmentShape.minimumSkirt(d));
                    designs++;
                    if (ColumnDesign.misfit(s).isPresent()) { refused++; continue; }
                    double yB = s.bottomTangent(), sump = ColumnTrays.bottomSpace(d), bottomTray = yB + t.trayHeight(d, n), topTray = yB + t.trayHeight(d, 1);
                    var wants = wants(t, d, yB, sump);
                    var used = new HashMap<EquipmentShape.Role, Integer>();
                    boolean anyCross = false, anyWrong = false;
                    double paDraw = Double.NaN;
                    var line = new StringBuilder();
                    for (var nozzle : s.nozzles()) {
                        var list = wants.get(nozzle.role());
                        if (list == null) continue;
                        var want = list.get(used.merge(nozzle.role(), 1, Integer::sum) - 1);
                        double placed = nozzle.base()[1], wanted = Math.floor(want.y()) + .5;
                        if (nozzle.role() == EquipmentShape.Role.PUMPAROUND_DRAW) paDraw = placed;
                        int trays = 0;
                        for (int k = 1; k <= n; k++) {
                            double h = yB + t.trayHeight(d, k);
                            if (Math.min(want.y(), placed) < h && h < Math.max(want.y(), placed)) trays++;
                        }
                        boolean sumpNozzle = nozzle.role() == EquipmentShape.Role.VAPOUR_RETURN || want.stage() > n;
                        boolean wrong = sumpNozzle && placed > bottomTray || nozzle.role() == EquipmentShape.Role.REFLUX && placed < topTray - .1
                                || nozzle.role() == EquipmentShape.Role.RETURN && placed < paDraw;
                        if (trays >= 2) { anyCross = true; crossByRole.merge(nozzle.role().name(), 1, Integer::sum); }
                        if (wrong) { anyWrong = true; wrongByRole.merge(nozzle.role().name(), 1, Integer::sum); }
                        if (placed != wanted) line.append(String.format(" %s %.1f->%.1f(%d trays)", nozzle.role(), wanted, placed, trays));
                    }
                    if (anyCross) crossing++;
                    if (anyWrong) wrongSide++;
                    if ((anyCross || anyWrong) && examples.size() < 4) examples.add(String.format("d %.1f n %d %s:%s", d, n, config, line));
                }
        System.out.printf("designs %d, refused at Save design %d, with a nozzle moved past two trays or more %d, with a nozzle on the wrong side of its stage %d%n",
                designs, refused, crossing, wrongSide);
        System.out.println("moved past two trays or more, by role: " + crossByRole);
        System.out.println("wrong side, by role: " + wrongByRole);
        examples.forEach(System.out::println);
    }

    static ColumnTrays trays(int n, String config) {
        boolean kettle = config.startsWith("kettle"), steam = config.contains("steam"), draw = config.contains("draw"), pa = config.contains("PA");
        var pumparounds = pa ? List.of(new ColumnTrays.Pumparound(Math.max(2, n / 3 + 1), Math.max(1, n / 3 - 1))) : List.<ColumnTrays.Pumparound>of();
        var c = new ColumnComponents(ColumnComponents.Condenser.AIR_COOLER, ColumnComponents.Pump.STANDARD,
                pa ? List.of(ColumnComponents.Pump.STANDARD) : List.of(), false, kettle);
        return new ColumnTrays(n, n / 2 + 2, draw ? List.of(Math.max(1, n / 2 - 1)) : List.of(), steam ? List.of(n + 1) : List.of(), pumparounds, c);
    }

    /** ColumnStructure.place's wanted heights, by role, in placing order. */
    static Map<EquipmentShape.Role, List<Want>> wants(ColumnTrays t, double d, double yB, double sump) {
        var out = new EnumMap<EquipmentShape.Role, List<Want>>(EquipmentShape.Role.class);
        var R = EquipmentShape.Role.class;
        add(out, new Want(EquipmentShape.Role.REFLUX, yB + t.trayHeight(d, 1) + .45, 1));
        add(out, new Want(EquipmentShape.Role.FEED, yB + (t.feed() > t.trays() ? sump - .3 : t.trayHeight(d, t.feed()) + .3), t.feed()));
        for (var p : t.pumparounds()) {
            add(out, new Want(EquipmentShape.Role.PUMPAROUND_DRAW, yB + t.trayHeight(d, p.draw()) - .2, p.draw()));
            add(out, new Want(EquipmentShape.Role.RETURN, yB + t.trayHeight(d, p.ret()) + .3, p.ret()));
        }
        if (ColumnStructure.reboiled(t)) add(out, new Want(EquipmentShape.Role.VAPOUR_RETURN, yB + .75 * sump, t.trays() + 1));
        for (int draw : t.draws()) add(out, new Want(EquipmentShape.Role.DRAW, yB + t.trayHeight(d, draw) - .2, draw));
        for (int stage : t.steam()) add(out, new Want(EquipmentShape.Role.STEAM, yB + (stage > t.trays() ? sump : t.trayHeight(d, stage)) - .3, stage));
        return out;
    }

    static void add(Map<EquipmentShape.Role, List<Want>> out, Want want) {
        out.computeIfAbsent(want.role(), r -> new ArrayList<>()).add(want);
    }
}
