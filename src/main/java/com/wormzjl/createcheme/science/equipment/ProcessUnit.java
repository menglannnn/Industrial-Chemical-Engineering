package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Nozzle;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Orientation;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Role;
import com.wormzjl.createcheme.science.math.DeterministicMath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.wormzjl.createcheme.science.equipment.Mesh.*;

/**
 * A standalone process unit (owner request and decisions 2026-10-16, batch 2026-10-15-custom-equipment-rendering,
 * {@code STANDALONE_UNITS_PLAN.md}): a shell-and-tube heat exchanger, a kettle reboiler, an air cooler bay or a steam
 * ejector, drawn only. The player places it with the constructor and runs the pipes; its nozzles take the pipe's size.
 *
 * <p>Sizes, from the physics of the plan. An exchanger (TEMA AES) of shell D and tubes L holds about 1,260 D² tubes of
 * 3/4" on a 1" pitch, about 75 D² L m² of surface; its tubes are at least 3 D long. A kettle's bundle of D_b lies in a
 * shell of 1.6 D_b with vapour space over the pool. An air cooler is a bank of single-pass bays in parallel and in series
 * ({@link Bank}), each 1.5 to 3 m wide and 2 to 6 m long where its one to three fans cover at least 40 % of its bundle
 * face (API 661; owner requests 2026-10-16: smaller bays, banks), its fans, rings and motors Blender parts ({@link Part}).
 * An ejector's proportions follow its suction s (8.3 s long).
 *
 * <p>Built in the unit's frame, which stands as a vertical vessel's ({@link EquipmentShape}): y up from the ground, the
 * front (frame -z) towards the constructor, a lying unit's axis along z with its channel (the air cooler's inlet header)
 * at the front. Every nozzle's centre line runs through cell centres and its flange face lies on a block face; the block
 * beyond each face is left free for a pipe. {@code diameter}, {@code tangentLength} and {@code support} of the shape are
 * the unit's own sizes: an exchanger's shell, tube length and the height of its shell's bottom; a kettle's bundle, tube
 * length and the height of its kettle's bottom; an air cooler's bay width, bay length and the height of its bundle; an
 * ejector's suction, 1 (unused) and the height of its discharge flange.
 */
public final class ProcessUnit {
    private static final int CACHE = 64;
    private static final Map<EquipmentShape, ProcessUnit> BUILT = new ConcurrentHashMap<>();

    /**
     * Steps of an exchanger's shell and a kettle's bundle, and of an ejector's suction, m; the largest suction of an
     * ejector on a single post (owner request 2026-10-16: "allow smaller ejectors").
     */
    public static final double SHELL_STEP = .25, SUCTION_STEP = .05, SMALL_EJECTOR = .2;
    /**
     * An air cooler bay's lengths, m (header to header, whole metres; owner request 2026-10-16: "allow smaller ejectors and
     * ACs"); its width runs from 1.5 to 3 m in 0.5 m steps. A bay is built only where its fans cover {@link #FAN_COVERAGE}
     * of its bundle face (API 661), which a bay under 1.5 m wide never does.
     */
    public static final int MIN_BAY_LENGTH = 2, MAX_BAY_LENGTH = 6;
    public static final double FAN_COVERAGE = .4;
    /**
     * Air cooler parts, m: the largest header depth along the tubes, a header's height, the bundle's depth, and the
     * deepest a fan ring and guard hang under the plenum (the Blender ring is 0.35 of its fan deep).
     */
    static final double HEADER = .6, HEADER_HEIGHT = .6, BUNDLE = .5, RING_DEPTH = .9;
    /**
     * A fan ring's widest parts, its top flange and its bellmouth rim, in rotor diameters (the Blender part
     * {@code ac_fan_ring}); fans are sized so that rings side by side keep apart (owner report 2026-10-16).
     */
    static final double RING_OD = 1.12;

    /**
     * An air cooler bank (owner request 2026-10-16: "AC should be able to be built in series & parallel", decided: one bank
     * unit with its manifolds and crossovers): {@code parallel} trains of bays side by side across the frame's x, each
     * {@code series} bays back to back along z. Single-pass bays: the process enters a train at its first bay's front
     * header and crosses from each back header to the next front header; the bank has one inlet and one outlet.
     */
    public record Bank(int parallel, int series) {
        public static final Bank ONE = new Bank(1, 1);
        public static final int MAX_PARALLEL = 6, MAX_SERIES = 4;

        public Bank {
            if (parallel < 1 || parallel > MAX_PARALLEL || series < 1 || series > MAX_SERIES)
                throw new IllegalArgumentException("A bank is 1 to " + MAX_PARALLEL + " bays in parallel by 1 to " + MAX_SERIES + " in series, not " + parallel + " × " + series);
        }

        public int bays() {
            return parallel * series;
        }

        public int[] encode() {
            return new int[]{parallel, series};
        }

        /** @throws IllegalArgumentException unless {@code values} holds the bays in parallel and in series, in range */
        public static Bank decode(int[] values) {
            if (values.length != 2) throw new IllegalArgumentException("A bank holds its bays in parallel and in series");
            return new Bank(values[0], values[1]);
        }
    }

    /** A bay's fans: how many, along its tubes, and their diameter, m. */
    public record Fans(int count, double diameter) {
    }

    /**
     * Heights under an air cooler's bundle for its fans, m above grade: the plenum's floor 0.2 + 0.4 D under the bundle,
     * the ring and guard hanging {@code up} (at most {@link #RING_DEPTH}, drawn at 0.35 of it) down to the ring's
     * {@code inlet}, the {@code rotor} turning a sixth of the way down, and the motor (scaled {@code motor}, smaller for
     * a small fan) under the guard on its shaft.
     */
    public record FanStack(double floor, double up, double rotor, double inlet, double motor, double motorTop, double motorBottom) {
        public static FanStack of(double bundle, double fan) {
            double up = Math.min(fan, RING_DEPTH), floor = bundle - (.2 + .4 * fan), motor = Math.max(.45, Math.min(1, fan / 1.9)), motorTop = floor - .35 * up - .03;
            return new FanStack(floor, up, floor - .15 * up, floor - .35 * up, motor, motorTop, motorTop - .58 * motor);
        }
    }

    /** Steelwork, m: a post's section, a beam's depth and flange. */
    static final double POST = .2, BEAM = .2, FLANGE_BEAM = .15;

    // ---- Sizes ------------------------------------------------------------------------------------------------------

    public static double minDiameter(Kind kind) {
        return switch (kind) {
            case EXCHANGER, KETTLE -> .5;
            case AIR_COOLER -> 1.5;
            case EJECTOR, PUMP -> SUCTION_STEP;
            case COMPRESSOR -> .15;
            case VERTICAL_HEATER -> 2.5;
            case CABIN_HEATER -> 4;
            default -> throw notUnit(kind);
        };
    }

    public static double maxDiameter(Kind kind) {
        return switch (kind) {
            case EXCHANGER -> 2.5;
            case KETTLE -> 2;
            case AIR_COOLER -> 3;
            case EJECTOR -> 1;
            case PUMP -> .75;
            case COMPRESSOR -> 1.2;
            case VERTICAL_HEATER -> 12;
            case CABIN_HEATER -> 12;
            default -> throw notUnit(kind);
        };
    }

    public static double diameterStep(Kind kind) {
        return switch (kind) {
            case EXCHANGER, KETTLE -> SHELL_STEP;
            case AIR_COOLER, VERTICAL_HEATER -> .5;
            case EJECTOR, PUMP, COMPRESSOR -> SUCTION_STEP;
            case CABIN_HEATER -> 1;
            default -> throw notUnit(kind);
        };
    }

    /**
     * The shortest tubes, whole metres: three bundle diameters; an air cooler bay of width {@code diameter}, the shortest
     * whose fans cover {@link #FAN_COVERAGE} of its bundle face; an ejector's and a pump's 1, unused; a compressor's
     * least throws; a vertical heater's radiant section 1.5 diameters high, a cabin's 1.2 widths long.
     */
    public static int minLength(Kind kind, double diameter) {
        return switch (kind) {
            case EXCHANGER, KETTLE -> Math.max(2, (int) Math.ceil(3 * diameter - 1e-9));
            case AIR_COOLER -> {
                int length = MIN_BAY_LENGTH;
                while (length < MAX_BAY_LENGTH && coverage(diameter, length) < FAN_COVERAGE) length++;
                yield length;
            }
            case EJECTOR, PUMP -> 1;
            case COMPRESSOR -> 2;
            case VERTICAL_HEATER -> Math.max(4, (int) Math.ceil(1.5 * diameter - 1e-9));
            case CABIN_HEATER -> Math.max(6, (int) Math.ceil(1.2 * diameter - 1e-9));
            default -> throw notUnit(kind);
        };
    }

    /**
     * The longest tubes, whole metres; a compressor's most throws; a vertical heater's radiant section at most 2.75
     * diameters high (API 560's slenderness) and 20 m (vertical radiant tubes reach about 18.3 m,
     * {@code MACHINES_SCALE_RESEARCH.md}), a cabin's 48 m: above {@link #MAX_CABIN_CELL} a twin cell of two half-length
     * cells.
     */
    public static int maxLength(Kind kind, double diameter) {
        return switch (kind) {
            case EXCHANGER -> 12;
            case KETTLE -> 10;
            case AIR_COOLER -> MAX_BAY_LENGTH;
            case EJECTOR, PUMP -> 1;
            case COMPRESSOR -> MAX_THROWS;
            case VERTICAL_HEATER -> Math.min(MAX_RADIANT_HEIGHT, (int) Math.floor(2.75 * diameter + 1e-9));
            case CABIN_HEATER -> 2 * MAX_CABIN_CELL;
            default -> throw notUnit(kind);
        };
    }

    /**
     * The lowest support, m: a lying unit's bottom 2 m up leaves a block under its bottom nozzles for their pipes; an air
     * cooler's bundle 3 m up (owner request 2026-10-16) keeps its fans' ring inlets at least 0.6 fan diameters above
     * grade, more than the half diameter of free air a forced-draft fan usually takes; an ejector's discharge 1 m up; a
     * pump stands on its plinth at grade; a compressor's foundation high enough that its discharge bottles clear grade by
     * 0.3 m and its motor by 0.1 m; a heater's floor 3 m up, room to stand under its burners.
     */
    public static int minSupport(Kind kind, double diameter, double length) {
        return switch (kind) {
            case EXCHANGER, KETTLE -> 2;
            case AIR_COOLER -> 3;
            case EJECTOR -> 1;
            case PUMP -> 0;
            case COMPRESSOR -> {
                var c = Compressor.of(diameter, (int) length);
                int h = 1;
                while (h < EquipmentShape.MAX_SUPPORT && !c.clears(h)) h++;
                yield h;
            }
            case VERTICAL_HEATER, CABIN_HEATER -> 3;
            default -> throw notUnit(kind);
        };
    }

    /** The highest support, m: a pump's plinth is at grade (the constructor's Up lifts it onto a deck). */
    public static int maxSupport(Kind kind) {
        return kind == Kind.PUMP ? 0 : EquipmentShape.MAX_SUPPORT;
    }

    /** A new unit's support, m: an air cooler's bundle 4 m up (owner request 2026-10-16), 1.68 m or more under its motor beams; the least for the rest. */
    public static int defaultSupport(Kind kind, double diameter, double length) {
        return switch (kind) {
            case AIR_COOLER -> 4;
            case EXCHANGER, KETTLE, EJECTOR -> 2;
            default -> minSupport(kind, diameter, length);
        };
    }

    /** An ejector and a vertical heater stand; the others lie. */
    public static Orientation orientation(Kind kind) {
        return kind == Kind.EJECTOR || kind == Kind.VERTICAL_HEATER ? Orientation.VERTICAL : Orientation.HORIZONTAL;
    }

    /** A diameter on its kind's grid, cleared of rounding (0.30000000000000004 becomes 0.3). */
    public static double snap(Kind kind, double diameter) {
        double step = diameterStep(kind), min = minDiameter(kind);
        double k = Math.max(0, Math.min(Math.round((maxDiameter(kind) - min) / step), Math.round((diameter - min) / step)));
        return Math.round((min + k * step) * 1000) / 1000.0;
    }

    static void validate(Kind kind, double diameter, double length, Orientation orientation, int support, Bank bank) {
        String name = kind.name().toLowerCase(Locale.ROOT);
        if (kind != Kind.AIR_COOLER && !bank.equals(Bank.ONE)) throw new IllegalArgumentException("Only air coolers are built in banks");
        if (!(diameter >= minDiameter(kind) - 1e-9 && diameter <= maxDiameter(kind) + 1e-9) || Math.abs(snap(kind, diameter) - diameter) > 1e-9)
            throw new IllegalArgumentException("A " + name + "'s size must be " + minDiameter(kind) + " to " + maxDiameter(kind) + " m in " + diameterStep(kind)
                    + " m steps, not " + diameter);
        if (length != Math.rint(length) || length < minLength(kind, diameter) || length > maxLength(kind, diameter))
            throw new IllegalArgumentException("A " + name + "'s length must be " + minLength(kind, diameter) + " to " + maxLength(kind, diameter) + " whole metres, not " + length);
        if (kind == Kind.COMPRESSOR && length % 2 != 0) throw new IllegalArgumentException("A balanced-opposed compressor has an even number of throws, not " + length);
        if (orientation != orientation(kind)) throw new IllegalArgumentException("A " + name + " " + (orientation(kind).horizontal() ? "lies" : "stands"));
        int lowest = minSupport(kind, diameter, length);
        if (support < lowest || support > maxSupport(kind))
            throw new IllegalArgumentException("A " + name + "'s support must be " + lowest + " to " + maxSupport(kind) + " m, not " + support);
        if (kind == Kind.AIR_COOLER && coverage(diameter, length) < FAN_COVERAGE)
            throw new IllegalArgumentException("An air cooler bay of " + diameter + " × " + length + " m has no room for fans covering " + FAN_COVERAGE + " of its bundle");
    }

    private static IllegalArgumentException notUnit(Kind kind) {
        return new IllegalArgumentException(kind + " is not a unit");
    }

    /** Tube surface of an exchanger or a kettle's bundle of shell {@code shell} and tubes {@code tubes}, m². */
    public static double tubeSurface(double shell, double tubes) {
        return 75 * shell * shell * tubes;
    }

    /** A kettle's shell over its bundle: 1.6 times, to 0.25 m. */
    public static double kettleShell(double bundle) {
        return Math.round(1.6 * bundle * 4) / 4.0;
    }

    /** An air cooler bay's header depth along its tubes, m: 0.15 of its length, at most {@link #HEADER}. */
    static double header(double length) {
        return Math.min(HEADER, .15 * length);
    }

    /** An air cooler bay's bundle face between its headers (centred 0.5 m in from each end), m². */
    public static double bundleFace(double width, double length) {
        return (width - .22) * (length - 1 - header(length));
    }

    /**
     * An air cooler bay's fans: one to three along its tubes, as many as cover its bundle face best, each the largest whose
     * ring fits its share of the plenum ({@link #fanDiameter}).
     */
    public static Fans fans(double width, double length) {
        Fans best = null;
        double covered = -1;
        for (int k = 1; k <= 3; k++) {
            double d = fanDiameter(width, length, k), c = d > 0 ? k * Math.PI * d * d / 4 : 0;
            if (c > covered + 1e-9) {
                covered = c;
                best = new Fans(k, d);
            }
        }
        return best;
    }

    /**
     * The largest of {@code k} fans whose rings fit a bay, m, on a 0.05 m grid: each ring ({@link #RING_OD} rotor
     * diameters) inside its share of the bundle along the tubes with 0.02 m to spare, and 0.1 m inside the plenum's
     * width (the side beams' 0.22 m off the bay width).
     */
    static double fanDiameter(double width, double length, int k) {
        double ring = Math.min(width - .32, (length - 1.1 - header(length)) / k - .02);
        return Math.floor(ring / RING_OD * 20 + 1e-9) / 20;
    }

    /** Every air cooler bay a player may build, {width, length}, the narrowest and shortest first. */
    public static List<double[]> bays() {
        var out = new ArrayList<double[]>();
        for (double w = minDiameter(Kind.AIR_COOLER); w <= maxDiameter(Kind.AIR_COOLER) + 1e-9; w += diameterStep(Kind.AIR_COOLER))
            for (int l = minLength(Kind.AIR_COOLER, w); l <= MAX_BAY_LENGTH; l++) if (coverage(w, l) >= FAN_COVERAGE) out.add(new double[]{snap(Kind.AIR_COOLER, w), l});
        return out;
    }

    /** The share of an air cooler bay's bundle face its fans cover (API 661 asks 0.4 at least). */
    public static double coverage(double width, double length) {
        var fans = fans(width, length);
        return fans.count() * Math.PI * fans.diameter() * fans.diameter() / 4 / bundleFace(width, length);
    }

    /** An ejector's length from its steam chest to its diffuser's exit, m: 8.3 suction diameters. */
    public static double ejectorLength(double suction) {
        return 8.3 * suction;
    }

    // ---- Machines and heaters (owner request 2026-10-17, MACHINES_HEATERS_PLAN.md) ----------------------------------

    /** A pump's design head, m of liquid, and its efficiency, for the datasheet's motor size. */
    static final double PUMP_HEAD = 100, PUMP_EFFICIENCY = .7;
    /**
     * The largest overhung (API 610 OH2) pump's discharge, m: larger pumps are axially split (BB1), up to the 0.75 m whose
     * 0.95 m suction is the largest nozzle the mod's pipes (1 m outside) connect ({@code MACHINES_SCALE_RESEARCH.md}: OH2
     * stops at 0.4 m, BB1 reaches 1.2 m).
     */
    public static final double OH2_MAX = .4;
    /**
     * A balanced-opposed compressor's most throws, and its mean piston speed, m/s: process frames reach 1.25 m bore and 10
     * throws and run at 3 to 4.5 m/s ({@code MACHINES_SCALE_RESEARCH.md}: Siemens HHE-VL, Baker Hughes HE/HF).
     */
    public static final int MAX_THROWS = 10;
    static final double PISTON_SPEED = 4;
    /** A vertical heater's tallest radiant section, m. */
    static final int MAX_RADIANT_HEIGHT = 20;
    /** A fired heater's mean radiant flux, kW/m² of tube, and the radiant section's share of its duty (API 560 practice). */
    static final double RADIANT_FLUX = 35, RADIANT_SHARE = .7;

    /** A centrifugal pump's suction for discharge {@code d}, m: one size up, about 1.25 d, on a 0.05 m grid. */
    public static double pumpSuction(double d) {
        return Math.round((1.25 * d + .025) * 20) / 20.0;
    }

    /** A centrifugal pump's flow, m³/h: liquid at 3 m/s in its discharge {@code d}. */
    public static double pumpFlow(double d) {
        return 3 * Math.PI / 4 * d * d * 3600;
    }

    /** The shaft power of a pump of discharge {@code d} at {@link #PUMP_HEAD} of water, kW. */
    public static double pumpPower(double d) {
        return 1000 * 9.81 * pumpFlow(d) / 3600 * PUMP_HEAD / PUMP_EFFICIENCY / 1000;
    }

    /** A TEFC motor's body across, m, for {@code kw}: about 0.19 P^0.25 (0.32 m at 8 kW, 0.91 m at 530 kW). */
    static double motorDiameter(double kw) {
        return .19 * Math.sqrt(Math.sqrt(kw));
    }

    /** A compressor's stroke for bore {@code b}, m. */
    public static double stroke(double b) {
        return .1 + .25 * b;
    }

    /** A compressor's speed, rev/min: its mean piston speed over twice its stroke (larger frames, longer strokes, run slower). */
    public static double compressorSpeed(double b) {
        return 60 * PISTON_SPEED / (2 * stroke(b));
    }

    /** A compressor's shaft power, MW: about 0.05 + 1.7 b² per throw (a fit to Siemens HHE-VL and the Thai Oil machines, {@code MACHINES_SCALE_RESEARCH.md}). */
    public static double compressorPower(double b, int throwCount) {
        return throwCount * (.05 + 1.7 * b * b);
    }

    /** A compressor's displacement, m³/h: double-acting cylinders of bore {@code b}, {@code throwCount} of them. */
    public static double displacement(double b, int throwCount) {
        return throwCount * 2 * Math.PI / 4 * b * b * stroke(b) * compressorSpeed(b) * 60;
    }

    /**
     * A fired heater's duty, MW, from its radiant tubes: 6" tubes at two diameters' pitch (π²/2 m² of tube per metre of
     * wall run) at {@link #RADIANT_FLUX}, the radiant section taking {@link #RADIANT_SHARE}. A vertical heater's tubes stand
     * round a circle 0.6 m inside its casing, 0.5 m shorter than its radiant height; a cabin's lie along both side walls of
     * each of its {@link #cabinCells} (1 m short each way) and a third as much again on its roof.
     */
    public static double heaterDuty(Kind kind, double diameter, double length) {
        int cells = kind == Kind.CABIN_HEATER ? cabinCells(length) : 1;
        double tubes = kind == Kind.VERTICAL_HEATER ? Math.PI * Math.PI / 2 * (diameter - .6) * (length - .5)
                : 2 * cells * Math.PI / 2 * (cabinWall(diameter) - 1) * (length / cells - 1) * 1.3;
        return RADIANT_FLUX * tubes / RADIANT_SHARE / 1000;
    }

    /** A cabin heater's wall height for width {@code w}, m. */
    static double cabinWall(double w) {
        return w + 1;
    }

    /**
     * A balanced-opposed compressor's sizes, m, for bore {@code b} and {@code throwCount} ({@code MACHINES_HEATERS_SURVEY.md}):
     * its stroke; a cylinder's outside across (water jacket and valve pockets, 1.45 bores and 0.15 m: 2.5 bores small, 1.6
     * large) and length (the stroke and 0.5 to 0.9 m); a crosshead guide's half section and its length (3.5 strokes, 0.7 m
     * at least), a distance piece's half section; the frame's half width, the crankshaft's height over the foundation and
     * the frame's top (the crankshaft about half way up); the crank pitch; the cylinders' centres out from the crankshaft
     * (whole metres: their bottles' nozzles run through cell centres; the distance piece takes the rest, 0.5 + 0.4 b at
     * least); the suction and discharge bottles across; the synchronous motor's frame across, 0.3 + 1.1 (kW / rpm)^⅓
     * (wider than long: 0.45 of that and 0.4 m), from {@link #compressorPower} ({@code MACHINES_SCALE_RESEARCH.md}).
     */
    record Compressor(double bore, int throwCount, double stroke, double cylinder, double cylinderLength, double guide, double guideLength, double piece, double half,
                      double crank, double frameTop, double pitch, double out, double suction, double discharge, double motor, double motorLength) {
        static Compressor of(double b, int throwCount) {
            double s = ProcessUnit.stroke(b), c = 1.45 * b + .15, lc = 2.2 * s + .3 + .3 * b, half = .45 + .3 * b, crank = .5 + .35 * b, guide = .25 + .2 * b;
            double lg = Math.max(.7, 3.5 * s), out = Math.ceil(half + lg + .5 + .4 * b + lc / 2 - 1e-9), ds = 1.1 * b + .3;
            double dm = .3 + 1.1 * DeterministicMath.cbrt(1000 * compressorPower(b, throwCount) / compressorSpeed(b));
            return new Compressor(b, throwCount, s, c, lc, guide, lg, .8 * guide, half, crank, crank + .45 + .5 * b, Math.max(.55, (c + .3) / 2), out, ds, .85 * ds, dm,
                    .45 * dm + .4);
        }

        /** The distance piece's length from the guide to the cylinder. */
        double pieceLength() {
            return out - half - guideLength - cylinderLength / 2;
        }

        /**
         * The suction header across: every cylinder's flow at about 12 m/s, 0.5 b √n; the discharge header 0.7 of it (a
         * pressure ratio of about 2); at most 1 m ({@code MACHINES_SCALE_RESEARCH.md}).
         */
        double suctionHeader() {
            return Math.min(PipeSizes.MAX_OUTSIDE, .5 * bore * Math.sqrt(throwCount) + .05);
        }

        double dischargeHeader() {
            return Math.min(PipeSizes.MAX_OUTSIDE, .7 * suctionHeader() + .02);
        }

        /** The depth of the steel table's rails: about W24 (0.6 m) at 2 MW to W36 (0.9 m) at 20 MW, 0.4 m on the smallest. */
        double rail() {
            return Math.min(.9, .4 + .1 * Math.sqrt(compressorPower(bore, throwCount)));
        }

        /** The gap between a cylinder and its bottles (survey: 0.2 to 0.5 m). */
        double gap() {
            return .2 + .15 * bore;
        }

        /** The discharge bottles' axis for a foundation {@code h} high: the cell centre under the cylinders at least {@link #gap} below them. */
        double dischargeAxis(int h) {
            return Math.floor(h + crank - cylinder / 2 - gap() - discharge / 2 - .5) + .5;
        }

        /** Whether a foundation {@code h} high keeps the discharge bottles 0.3 m and the motor 0.1 m over grade. */
        boolean clears(int h) {
            return dischargeAxis(h) - discharge / 2 >= .3 && h + crank - motor / 2 >= .1;
        }
    }

    /**
     * An air cooler's fan rotors, {x, y, z, diameter} in box coordinates at each hub's centre, for the client that turns
     * them (owner request 2026-10-16); none for another unit.
     */
    public static List<double[]> rotors(EquipmentShape shape) {
        if (shape.kind() != Kind.AIR_COOLER) return List.of();
        var out = new ArrayList<double[]>();
        for (var r : of(shape).rotors) {
            var p = shape.transform().point(new double[]{r[0], r[1], r[2]});
            out.add(new double[]{p[0], p[1], p[2], r[3]});
        }
        return out;
    }

    /**
     * A fan's rotor alone ({@link Material#AC_FAN}), its hub's centre at the origin, as polygons in metres cut where the
     * static model cuts it, so the turning rotor shows the texels of the still one.
     */
    public static List<Mesh.Polygon> rotor(double diameter) {
        int reach = (int) Math.ceil(diameter / 2) + 1;
        var mesh = new Mesh();
        Part.of("ac_fan").place(mesh, new double[]{reach, reach, reach}, diameter, diameter, 0, Material.AC_FAN);
        var out = new ArrayList<Mesh.Polygon>();
        for (int x = 0; x < 2 * reach; x++)
            for (int y = 0; y < 2 * reach; y++)
                for (int z = 0; z < 2 * reach; z++)
                    for (var piece : mesh.cell(x, y, z)) {
                        var p = piece.positions().clone();
                        for (int i = 0; i < p.length; i += 3) {
                            p[i] += x - reach;
                            p[i + 1] += y - reach;
                            p[i + 2] += z - reach;
                        }
                        out.add(new Mesh.Polygon(p, piece.uvs(), piece.normals(), piece.material()));
                    }
        return out;
    }

    static ProcessUnit of(EquipmentShape shape) {
        var built = BUILT.get(shape);
        if (built != null) return built;
        if (BUILT.size() >= CACHE) BUILT.clear();
        return BUILT.computeIfAbsent(shape, s -> new ProcessUnit(s, false));
    }

    // ---- Collision parts --------------------------------------------------------------------------------------------

    /**
     * A solid part in the frame: an axis-aligned box ({@code radius} 0), or a cylinder along frame axis {@code axis}
     * between {@code lo} and {@code hi} on it, its radius going from {@code radius} at {@code lo} to {@code radiusHi} at
     * {@code hi} round the line through {@code centre} (a cone where they differ), with a 2:1 head of depth {@code headHi}
     * beyond its high end (0: flat); a cylinder may be hollow inside {@code inner} and cut to the angles [from, to]
     * (counter-clockwise from +x towards +z about a vertical axis; from = to: whole), a deck's ring.
     */
    private record Solid(double[] min, double[] max, int axis, double[] centre, double radius, double radiusHi, double lo, double hi, double headHi, double inner,
                         double from, double to) {
        static Solid box(double[] min, double[] max) {
            return new Solid(min, max, -1, null, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        /** A ring about the vertical line through (x, z), from {@code inner} to {@code outer} out and {@code lo} to {@code hi} up, over the angles [from, to]. */
        static Solid ring(double x, double z, double lo, double hi, double inner, double outer, double from, double to) {
            var whole = cone(new double[]{x, 0, z}, 1, lo, hi, outer, outer, 0);
            return new Solid(whole.min, whole.max, 1, whole.centre, outer, outer, lo, hi, 0, inner, from, to);
        }

        static Solid rod(double[] centre, int axis, double lo, double hi, double radius, double headHi) {
            return cone(centre, axis, lo, hi, radius, radius, headHi);
        }

        static Solid cone(double[] centre, int axis, double lo, double hi, double radius, double radiusHi, double headHi) {
            double[] min = centre.clone(), max = centre.clone();
            double widest = Math.max(radius, radiusHi);
            for (int a = 0; a < 3; a++) {
                min[a] -= widest;
                max[a] += widest;
            }
            min[axis] = lo;
            max[axis] = hi + headHi;
            return new Solid(min, max, axis, centre, radius, radiusHi, lo, hi, headHi, 0, 0, 0);
        }

        boolean contains(double[] p) {
            for (int a = 0; a < 3; a++) if (p[a] < min[a] || p[a] > max[a]) return false;
            if (axis < 0) return true;
            double rho2 = 0;
            for (int a = 0; a < 3; a++) if (a != axis) rho2 += (p[a] - centre[a]) * (p[a] - centre[a]);
            if (rho2 < inner * inner) return false;
            if (to > from) {
                double dx = p[0] - centre[0], dz = p[2] - centre[2];
                double c0 = DeterministicMath.cos(from), s0 = DeterministicMath.sin(from), c1 = DeterministicMath.cos(to), s1 = DeterministicMath.sin(to);
                boolean afterFrom = c0 * dz - s0 * dx >= 0, beforeTo = dx * s1 - dz * c1 >= 0;
                if (!(to - from <= Math.PI ? afterFrom && beforeTo : afterFrom || beforeTo)) return false;
            }
            double s = p[axis];
            if (s <= hi) {
                double r = hi > lo ? radius + (radiusHi - radius) * (s - lo) / (hi - lo) : radius;
                return rho2 <= r * r;
            }
            double out = (s - hi) / headHi;
            return headHi > 0 && out * out + rho2 / (radiusHi * radiusHi) <= 1;
        }
    }

    // ---- The unit ---------------------------------------------------------------------------------------------------

    private final EquipmentShape shape;
    /** The mesh being drawn: the cached unit drops it once built; {@link #mesh()} draws the facets again. */
    private Mesh mesh = new Mesh();
    private final List<Solid> solids = new ArrayList<>(), volumes = new ArrayList<>();
    /** An air cooler's fan rotors, {x, y, z, diameter} in the frame at each hub's centre. */
    private final List<double[]> rotors = new ArrayList<>();
    /** A heater's ladders. */
    private final List<Access.Climb> ladders = new ArrayList<>();

    /** A heater's deck: its run along its wall, the height under its walking surface, its width, whether a rail closes each end. */
    record Deck(Access.Run run, double level, double width, boolean closedStart, boolean closedEnd) {
    }

    private final List<Deck> decks = new ArrayList<>();
    private final Map<Long, List<Solid>> solidCells = new HashMap<>(), volumeCells = new HashMap<>();
    private final List<Nozzle> nozzles = new ArrayList<>();
    /** Fixed outline edges, {x0, y0, z0, x1, y1, z1}; round bodies outlined against the eye, {x, y, z on the axis, axis, radius, lo, hi}. */
    private final List<double[]> edges = new ArrayList<>(), rounds = new ArrayList<>();
    private final double[] box;

    private ProcessUnit(EquipmentShape s, boolean drawing) {
        shape = s;
        switch (s.kind()) {
            case EXCHANGER -> exchanger();
            case KETTLE -> kettle();
            case AIR_COOLER -> airCooler();
            case EJECTOR -> ejector();
            case PUMP -> pump();
            case COMPRESSOR -> compressor();
            case VERTICAL_HEATER -> verticalHeater();
            case CABIN_HEATER -> cabinHeater();
            default -> throw notUnit(s.kind());
        }
        box = box(mesh.bounds());
        if (drawing) return;
        mesh = null;
        index(solids, solidCells);
        index(volumes, volumeCells);
    }

    /** The parts' facets in the frame, drawn afresh: the layout is deterministic. */
    Mesh mesh() {
        return new ProcessUnit(shape, true).mesh;
    }

    List<Nozzle> nozzles() {
        return List.copyOf(nozzles);
    }

    /** A heater's decks and ladders, for the access gates. */
    List<Deck> decks() {
        return List.copyOf(decks);
    }

    List<Access.Climb> ladders() {
        return List.copyOf(ladders);
    }

    /** The box in the frame, {x0, y0, z0, x1, y1, z1}: the parts and every nozzle's flange face, in whole cells. */
    double[] box() {
        return box.clone();
    }

    /** Length along the frame's z (a lying unit's axis), m. */
    double length() {
        return box[5] - box[2];
    }

    /** Height of the unit's top (its parts, not its nozzles) above the ground, m. */
    double height() {
        return top;
    }

    private double top;

    private double[] box(double[] bounds) {
        double[] out = {Math.floor(bounds[0] + 1e-9), 0, Math.floor(bounds[2] + 1e-9), Math.ceil(bounds[3] - 1e-9), Math.ceil(bounds[4] - 1e-9), Math.ceil(bounds[5] - 1e-9)};
        top = bounds[4];
        for (var n : nozzles) {
            double face = n.face()[n.axis()];
            if (n.sign() > 0) out[n.axis() + 3] = Math.max(out[n.axis() + 3], face);
            else out[n.axis()] = Math.min(out[n.axis()], face);
        }
        return out;
    }

    /** Whether a frame point lies in a part a player collides with: the parts, and a ladder's rung plane (a player climbs by pressing against it). */
    boolean inside(double[] p) {
        if (within(solidCells, p)) return true;
        for (var climb : ladders) if (climb.rungs(p)) return true;
        return false;
    }

    /** Whether a frame point lies where a ladder is climbed: in front of its rungs, inside its cage, from its foot to its top. */
    boolean climbable(double[] p) {
        for (var climb : ladders) if (climb.climbable(p)) return true;
        return false;
    }

    /** Whether a frame point lies inside a closed body (a shell, a plenum): no cell there is open. */
    boolean filled(double[] p) {
        return within(volumeCells, p);
    }

    /** Outline edges in the frame: the round bodies' rims and the air cooler's bundle. */
    List<double[]> edges() {
        return edges;
    }

    /** The round bodies' outlines seen from frame point {@code eye}: the two lines along each body where it turns away. */
    void silhouette(double[] eye, List<double[]> out) {
        for (var r : rounds) {
            int ax = (int) r[3], u = (ax + 1) % 3, v = (ax + 2) % 3;
            double radius = r[4], du = eye[u] - r[u], dv = eye[v] - r[v], d = Math.sqrt(du * du + dv * dv);
            if (d <= radius) continue;
            double towards = DeterministicMath.atan2(dv, du), spread = DeterministicMath.acos(radius / d);
            for (double angle : new double[]{towards - spread, towards + spread}) {
                double[] p = {r[0], r[1], r[2]}, q = {r[0], r[1], r[2]};
                p[u] += radius * DeterministicMath.cos(angle);
                p[v] += radius * DeterministicMath.sin(angle);
                q[u] = p[u];
                q[v] = p[v];
                p[ax] = r[5];
                q[ax] = r[6];
                out.add(new double[]{p[0], p[1], p[2], q[0], q[1], q[2]});
            }
        }
    }

    private static boolean within(Map<Long, List<Solid>> cells, double[] p) {
        var list = cells.get(key(Mesh.floor(p[0]), Mesh.floor(p[1]), Mesh.floor(p[2])));
        if (list == null) return false;
        for (var s : list) if (s.contains(p)) return true;
        return false;
    }

    private static void index(List<Solid> parts, Map<Long, List<Solid>> cells) {
        for (var s : parts)
            for (int x = Mesh.floor(s.min()[0]); x <= Mesh.floor(s.max()[0]); x++)
                for (int y = Mesh.floor(s.min()[1]); y <= Mesh.floor(s.max()[1]); y++)
                    for (int z = Mesh.floor(s.min()[2]); z <= Mesh.floor(s.max()[2]); z++) cells.computeIfAbsent(key(x, y, z), k -> new ArrayList<>()).add(s);
    }

    private static long key(int x, int y, int z) {
        return (long) (x + 1024) << 42 | (long) (y + 1024) << 21 | (z + 1024);
    }

    // ---- Shell-and-tube exchanger -----------------------------------------------------------------------------------

    /**
     * A TEMA AES exchanger lying along z on two saddles, its bottom {@code support} up: from the front, the channel's
     * cover, the channel (tube side in at its bottom, out at its top), the tubesheet's flange, the shell (shell side in
     * on top near the back, out at the bottom near the front), the floating head's flange and its cover.
     */
    private void exchanger() {
        double d = shape.diameter(), r = d / 2, y = shape.support() + r, a = centre(1.1 * r);
        int n = sides(d);
        double channel = .5 * d + .2, zn = Math.ceil(.08 + channel / 2 - .5) + .5;
        double c0 = zn - channel / 2 - .08, ch1 = zn + channel / 2, s0 = ch1 + .1, s1 = s0 + .95 * shape.tangentLength(), end = s1 + .1;
        var f = new Frame(new double[]{a, y, 0}, new double[]{0, 0, 1}, new double[]{0, 1, 0});
        mesh.ring(f, 0, 1.1 * r, c0, c0 + .08, n, 3, Material.FLANGE);
        mesh.bolts(f, d, r, 1.1 * r, c0, -1, Material.BOLT);
        mesh.prism(f, r, c0 + .08, ch1, n, Material.SHELL);
        mesh.ring(f, r, 1.1 * r, ch1, s0, n, 3, Material.FLANGE);
        mesh.bolts(f, d, r, 1.1 * r, ch1, -1, Material.BOLT);
        mesh.bolts(f, d, r, 1.1 * r, s0, 1, Material.BOLT);
        mesh.prism(f, r, s0, s1, n, Material.SHELL);
        mesh.ring(f, r, 1.08 * r, s1, end, n, 3, Material.FLANGE);
        mesh.bolts(f, d, r, 1.08 * r, s1, -1, Material.BOLT);
        mesh.bolts(f, d, r, 1.08 * r, end, 1, Material.BOLT);
        mesh.head(f, r, end, d / 4, n, Material.HEAD);
        solids.add(Solid.rod(new double[]{a, y, 0}, 2, c0, end, 1.05 * r, d / 4));
        volumes.add(Solid.rod(new double[]{a, y, 0}, 2, c0 + .1, end, .94 * r, .9 * d / 4));
        round(a, y, c0, end, r);

        double tube = Math.min(1, .4 * d), shellOd = Math.min(1, d / 2), flange = PipeSizes.flange(shellOd);
        nozzles.add(radial(Role.TUBE_INLET, a, y, r, n, zn, -1, tube, ColumnStructure.size(.25 * d, tube)));
        nozzles.add(radial(Role.TUBE_OUTLET, a, y, r, n, zn, 1, tube, ColumnStructure.size(.25 * d, tube)));
        // The shell nozzles near the tubesheets, two rows at least behind the channel's (one free block between pipes).
        double zIn = Math.max(Math.floor(s1 - flange / 2 - .1 - .5) + .5, zn + 2), zOut = Math.max(Math.ceil(s0 + flange / 2 + .1 - .5) + .5, zn + 2);
        nozzles.add(radial(Role.SHELL_INLET, a, y, r, n, zIn, 1, shellOd, ColumnStructure.size(.3 * d, shellOd)));
        nozzles.add(radial(Role.SHELL_OUTLET, a, y, r, n, zOut, -1, shellOd, ColumnStructure.size(.3 * d, shellOd)));
        // The saddles a fifth of the shell in from its ends, off the pipe blocks under the tube inlet and the shell outlet.
        double w = saddleWidth(r), length = s1 - s0;
        saddle(saddleAt(s0 + .2 * length, w, s0, s1, zn, zOut), a, y, r, w);
        saddle(saddleAt(s1 - .2 * length, w, s0, s1, zn, zOut), a, y, r, w);
    }

    // ---- Kettle reboiler --------------------------------------------------------------------------------------------

    /**
     * A kettle reboiler (TEMA AKT) lying along z, its kettle's bottom {@code support} up: the channel at the front (steam
     * in at its top, condensate out at its bottom), a cone up to the kettle's shell (1.6 times the bundle), the shell
     * (liquid fed low at its front, vapour out on top) and its back head, where the product leaves low behind the weir.
     */
    private void kettle() {
        double db = shape.diameter(), rb = db / 2, dk = kettleShell(db), rk = dk / 2, y = shape.support() + rk, a = centre(rk);
        int n = sides(dk), nb = sides(db);
        double channel = .5 * db + .2, zn = Math.ceil(.08 + channel / 2 - .5) + .5;
        double c0 = zn - channel / 2 - .08, ch1 = zn + channel / 2, cone = Math.max(.3, .6 * (dk - db)), s0 = ch1 + .1 + cone, s1 = s0 + .9 * shape.tangentLength() + .3;
        var f = new Frame(new double[]{a, y, 0}, new double[]{0, 0, 1}, new double[]{0, 1, 0});
        mesh.ring(f, 0, 1.1 * rb, c0, c0 + .08, nb, 3, Material.FLANGE);
        mesh.bolts(f, db, rb, 1.1 * rb, c0, -1, Material.BOLT);
        mesh.prism(f, rb, c0 + .08, ch1, nb, Material.SHELL);
        mesh.ring(f, rb, 1.12 * rb, ch1, ch1 + .1, nb, 3, Material.FLANGE);
        mesh.bolts(f, db, rb, 1.12 * rb, ch1, -1, Material.BOLT);
        mesh.frustum(f, 1.05 * rb, rk, ch1 + .1, s0, n, Material.SHELL);
        mesh.prism(f, rk, s0, s1, n, Material.SHELL);
        mesh.head(f, rk, s1, dk / 4, n, Material.HEAD);
        // Channel, cone and kettle collide as drawn and enclose their insides.
        var axis = new double[]{a, y, 0};
        solids.add(Solid.rod(axis, 2, c0, ch1 + .1, 1.12 * rb, 0));
        solids.add(Solid.cone(axis, 2, ch1 + .1, s0, 1.05 * rb, rk, 0));
        solids.add(Solid.rod(axis, 2, s0, s1, rk, dk / 4));
        volumes.add(Solid.rod(axis, 2, c0 + .1, ch1 + .1, .94 * rb, 0));
        volumes.add(Solid.cone(axis, 2, ch1 + .1, s0 + .02, .94 * 1.05 * rb, .94 * rk, 0));
        volumes.add(Solid.rod(axis, 2, s0 + .02, s1, .94 * rk, .9 * dk / 4));
        round(a, y, c0, ch1 + .1, rb);
        round(a, y, s0, s1, rk);

        double tube = Math.min(1, .4 * db), liquid = Math.min(1, db / 2), vapour = Math.min(1, dk / 2), flange = PipeSizes.flange(liquid);
        nozzles.add(radial(Role.TUBE_INLET, a, y, rb, nb, zn, 1, tube, ColumnStructure.size(.25 * db, tube)));
        nozzles.add(radial(Role.TUBE_OUTLET, a, y, rb, nb, zn, -1, tube, ColumnStructure.size(.15 * db, tube)));
        // The feed near the cone, two rows at least behind the channel's outlet (one free block between pipes).
        double zFeed = Math.max(Math.ceil(s0 + flange / 2 + .1 - .5) + .5, zn + 2);
        nozzles.add(radial(Role.FEED, a, y, rk, n, zFeed, -1, liquid, ColumnStructure.size(.15 * db, liquid)));
        nozzles.add(radial(Role.VAPOUR_OUTLET, a, y, rk, n, Math.floor((s0 + s1) / 2) + .5, 1, vapour, ColumnStructure.size(.3 * dk, vapour)));
        // The product: low on the back head, at the lowest cell centre 0.3 m over the shell's bottom.
        double yP = Math.ceil(shape.support() + .3 - .5) + .5, e = y - yP, depth = dk / 4;
        double od = Math.min(liquid, 2 * (.85 * rk - Math.abs(e)));
        double edge = (Math.abs(e) + od / 2) / rk, under = depth * Math.sqrt(Math.max(0, 1 - edge * edge)), start = s1 + under - .03;
        double nearest = Math.max(0, Math.abs(e) - od / 2) / rk, bulge = depth * Math.sqrt(Math.max(0, 1 - nearest * nearest));
        nozzles.add(new Nozzle(Role.LIQUID_OUTLET, new double[]{a, yP, start}, 2, 1, Math.ceil(s1 + depth + EquipmentShape.STAND_OUT) - start, od,
                ColumnStructure.size(.15 * db, od), 0, bulge - under + .03));
        double w = saddleWidth(rk), length = s1 - s0;
        saddle(saddleAt(s0 + .2 * length, w, s0, s1, zFeed), a, y, rk, w);
        saddle(saddleAt(s1 - .2 * length, w, s0, s1, zFeed), a, y, rk, w);
    }

    // ---- Air cooler -------------------------------------------------------------------------------------------------

    /**
     * An air cooler bank of forced-draft single-pass bays ({@link Bank}), their tubes along z. Per bay: the inlet header at
     * its front and the outlet header at its back (each centred half a metre in from its end), the finned bundle between
     * them {@code support} up, a plenum under it tapering to its fans' floor, each fan (a Blender rotor in its ring and
     * guard) with its motor under it on a beam, and legs under the headers' ends (one shared where trains meet). Bays in
     * series join back header to front header by crossover spools. Trains in parallel take the process from an inlet
     * manifold over their first front headers, the bank's inlet on its top, and give it to an outlet manifold behind their
     * last back headers, the outlet under it where the condensate drains; a single train's inlet is on its first header and
     * its outlet under a stub behind its last.
     */
    private void airCooler() {
        var bank = shape.bank();
        int trains = bank.parallel(), rows = bank.series();
        double w = shape.diameter(), length = shape.tangentLength(), hb = shape.support(), h = header(length);
        double low = hb - .05, high = low + HEADER_HEIGHT, a = centre(trains * w / 2), x0 = a - trains * w / 2, x1 = x0 + trains * w, end = rows * length;
        var fans = fans(w, length);
        var stack = FanStack.of(hb, fans.diameter());
        double d = fans.diameter(), up = stack.up(), bottom = stack.floor(), rotor = stack.rotor();
        double motor = stack.motor(), motorTop = stack.motorTop(), motorBottom = stack.motorBottom();
        // A bay's nozzles and crossovers by its width; a manifold carries the trains' flow at the same speed.
        double stub = .1 + .04 * w, manifold = Math.min(HEADER_HEIGHT - .1, stub * Math.sqrt(trains));
        var fanZ = new ArrayList<Double>();
        for (int k = 0; k < rows; k++) {
            double front = k * length + .5, back = (k + 1) * length - .5, front1 = front + h / 2, back0 = back - h / 2;
            for (int i = 0; i < trains; i++) {
                double bx0 = x0 + i * w, bx1 = bx0 + w, mid = bx0 + w / 2, ix0 = bx0 + .11, ix1 = bx1 - .11;
                for (double zh : new double[]{front, back})
                    box(new double[]{bx0 + .05, low, zh - h / 2}, new double[]{bx1 - .05, high, zh + h / 2}, Material.AC_HEADER, true);
                box(new double[]{ix0, hb, front1}, new double[]{ix1, hb + BUNDLE, back0}, Material.AC_FINS, true);
                for (double x : new double[]{bx0 + .05, ix1})
                    box(new double[]{x, hb - .02, front1}, new double[]{x + .06, hb + BUNDLE + .02, back0}, Material.STEELWORK, false);
                double share = (back0 - front1 - .1) / fans.count();
                var centres = new double[fans.count()];
                for (int f = 0; f < centres.length; f++) centres[f] = front1 + .05 + (f + .5) * share;
                plenum(ix0, ix1, front1, back0, hb, bottom, centres, RING_OD / 2 * d);
                for (int f = 0; f < fans.count(); f++) {
                    double zc = front1 + .05 + (f + .5) * share;
                    if (i == 0 && k == 0) fanZ.add(zc);
                    Part.of("ac_fan_ring").place(mesh, new double[]{mid, bottom, zc}, d, up, 0, Material.AC_RING);
                    Part.of("ac_fan").place(mesh, new double[]{mid, rotor, zc}, d, d, 0, Material.AC_FAN);
                    rotors.add(new double[]{mid, rotor, zc, d});
                    // Terminal boxes face out of the bank.
                    Part.of("ac_motor").place(mesh, new double[]{mid, motorTop, zc}, motor, motor, 2 * i < trains - 1 ? 2 : 0, Material.AC_MOTOR);
                    mesh.cylinder(Frame.of(new double[]{mid, 0, zc}, 1, 1), .025 * motor, motorTop, rotor - .06 * d, 8, Material.FLANGE);
                    solids.add(Solid.rod(new double[]{mid, 0, zc}, 1, bottom - .35 * up, bottom, .535 * d, 0));
                    solids.add(Solid.rod(new double[]{mid, 0, zc}, 1, motorBottom, motorTop, .2 * motor, 0));
                }
                if (k + 1 < rows) spool(mid, (low + high) / 2, back + h / 2, back + 1 - h / 2, stub);
            }
        }
        // Legs under the headers' ends; beams under the headers across the bank and along each leg line; at the motors'
        // level beams along the leg lines and across the bank under each row of motors.
        var legX = new ArrayList<Double>();
        legX.add(x0 + .15);
        for (int i = 1; i < trains; i++) legX.add(x0 + i * w);
        legX.add(x1 - .15);
        var legZ = new ArrayList<Double>();
        for (int k = 0; k < rows; k++) {
            legZ.add(k * length + .5);
            legZ.add((k + 1) * length - .5);
        }
        for (double x : legX)
            for (double z : legZ) box(new double[]{x - POST / 2, 0, z - POST / 2}, new double[]{x + POST / 2, low, z + POST / 2}, Material.STEELWORK, true);
        for (double z : legZ) box(new double[]{x0 + .05, low - BEAM, z - FLANGE_BEAM / 2}, new double[]{x1 - .05, low, z + FLANGE_BEAM / 2}, Material.STEELWORK, true);
        for (double x : legX)
            for (double y : new double[]{low, motorBottom})
                box(new double[]{x - FLANGE_BEAM / 2, y - BEAM, legZ.getFirst()}, new double[]{x + FLANGE_BEAM / 2, y, legZ.getLast()}, Material.STEELWORK, true);
        for (int k = 0; k < rows; k++)
            for (double zc : fanZ)
                box(new double[]{x0 + .15, motorBottom - BEAM, k * length + zc - FLANGE_BEAM / 2}, new double[]{x1 - .15, motorBottom, k * length + zc + FLANGE_BEAM / 2},
                        Material.STEELWORK, true);
        // The process in at the front, on top; out at the back, underneath.
        double zIn = .5, zOut = end + .5;
        if (trains == 1) nozzles.add(new Nozzle(Role.PROCESS_INLET, new double[]{a, high - .03, zIn}, 1, 1, Math.ceil(high + EquipmentShape.STAND_OUT) - high + .03,
                stub, stub, 0, .03));
        else {
            double ym = high + .1 + manifold / 2;
            manifold(x0 + w / 2, x1 - w / 2, ym, zIn, manifold);
            for (int i = 0; i < trains; i++) {
                var f = Frame.of(new double[]{x0 + (i + .5) * w, 0, zIn}, 1, 1);
                mesh.prism(f, stub / 2, high, ym, sides(stub), Material.PIPE);
                mesh.ring(f, stub / 2, PipeSizes.flange(stub) / 2, high, high + .03, sides(stub), 3, Material.FLANGE);
                mesh.bolts(f, stub, stub / 2, PipeSizes.flange(stub) / 2, high + .03, 1, Material.BOLT);
            }
            nozzles.add(new Nozzle(Role.PROCESS_INLET, new double[]{a, ym + manifold / 2 - .03, zIn}, 1, 1,
                    Math.ceil(ym + manifold / 2 + EquipmentShape.STAND_OUT) - ym - manifold / 2 + .03, manifold, manifold, 0, .03));
        }
        double out = trains == 1 ? stub : manifold, yo = low + .05 + stub / 2;
        for (int i = 0; i < trains; i++) {
            double mid = x0 + (i + .5) * w, z0 = end - .5 + h / 2;
            var f = Frame.of(new double[]{mid, yo, 0}, 2, 1);
            mesh.prism(f, stub / 2, z0, trains == 1 ? zOut + stub / 2 : zOut, sides(stub), Material.PIPE);
            mesh.ring(f, stub / 2, PipeSizes.flange(stub) / 2, z0, z0 + .03, sides(stub), 3, Material.FLANGE);
            mesh.bolts(f, stub, stub / 2, PipeSizes.flange(stub) / 2, z0 + .03, 1, Material.BOLT);
            if (trains == 1) mesh.annulus(f, 0, stub / 2, zOut + stub / 2, sides(stub), 1, Material.PIPE);
            solids.add(Solid.rod(new double[]{mid, yo, 0}, 2, z0, zOut, stub / 2, 0));
        }
        if (trains > 1) manifold(x0 + w / 2, x1 - w / 2, yo, zOut, manifold);
        double outBase = yo - out / 2 + .03;
        nozzles.add(new Nozzle(Role.PROCESS_OUTLET, new double[]{a, outBase, zOut}, 1, -1, outBase - Math.floor(yo - out / 2 - EquipmentShape.STAND_OUT), out, out, 0, .03));
        var outline = new double[][]{{x0 + .05, high, .5 - h / 2}, {x1 - .05, high, .5 - h / 2}, {x1 - .05, high, end - .5 + h / 2}, {x0 + .05, high, end - .5 + h / 2}};
        for (int k = 0; k < 4; k++) segment(outline[k], outline[(k + 1) % 4]);
    }

    /** Sides of the Blender fan ring's polygon ({@code ac_fan_ring}), a multiple of 4 for the hopper's quadrants. */
    static final int RING_SIDES = 24;

    /**
     * A bay's plenum (owner report 2026-10-16 with a photo: "the metal sheets lack a bottom casing"): a sheet-metal hopper
     * per fan, from the bundle's frame (x0 to x1, the fan's share of z0 to z1, at {@code top}) down to its ring's top
     * flange (radius {@code ring} round each of {@code centres}) at {@code bottom}. Each hopper is a square-to-round
     * transition: a triangle from each side of the frame to the circle's point facing it, a fan of triangles from each
     * corner to the quarter of the circle between, at the ring's {@link #RING_SIDES} corners. Sheets are drawn on both
     * faces, so the casing also shows from inside a ring; looking up through a ring one sees the bundle's fins.
     */
    private void plenum(double x0, double x1, double z0, double z1, double top, double bottom, double[] centres, double ring) {
        for (int f = 0; f < centres.length; f++) {
            double za = f == 0 ? z0 : (centres[f - 1] + centres[f]) / 2, zb = f + 1 == centres.length ? z1 : (centres[f] + centres[f + 1]) / 2;
            // The ring part's top flange is a 24-gon at these angles (ac_parts.py, lathe n = 24): the hopper meets its corners.
            transition(x0, x1, za, zb, top, (x0 + x1) / 2, centres[f], ring, bottom, RING_SIDES, Material.AC_PLENUM, true);
        }
        // Collides as its frame's box; encloses its inside.
        solids.add(Solid.box(new double[]{x0, bottom, z0}, new double[]{x1, top, z1}));
        volumes.add(Solid.box(new double[]{x0 + .02, bottom + .02, z0 + .02}, new double[]{x1 - .02, top - .02, z1 - .02}));
    }

    /**
     * A square-to-round transition of sheet metal: from the rectangle x0 to x1 by z0 to z1 at height {@code at} to the
     * circle of {@code radius} round (cx, cz) at height {@code to} (an air cooler's plenum hopper down to its fan ring, a
     * heater's breeching up to its stack). A triangle runs from each side of the rectangle to the circle's point facing it,
     * a fan of triangles from each corner to the quarter of the circle between, at the circle's {@code n} corners (a
     * multiple of 4, angle 0 towards +x, whose corners a fan ring's or a stack's prism about y shares). Drawn facing out, and also in where
     * {@code inside} (a plenum seen up through its fan ring).
     */
    private void transition(double x0, double x1, double z0, double z1, double at, double cx, double cz, double radius, double to, int n, Material material, boolean inside) {
        int quarter = n / 4;
        // The rectangle's corners in the circle's quadrants, counter-clockwise from +x +z.
        double[][] corners = {{x1, at, z1}, {x0, at, z1}, {x0, at, z0}, {x1, at, z0}};
        var circle = new double[n + 1][];
        for (int i = 0; i <= n; i++) {
            double angle = 2 * Math.PI * (i % n) / n;
            circle[i] = new double[]{cx + radius * DeterministicMath.cos(angle), to, cz + radius * DeterministicMath.sin(angle)};
        }
        var axis = new double[]{cx, (at + to) / 2, cz};
        for (int q = 0; q < 4; q++) {
            sheet(List.of(corners[(q + 3) % 4], corners[q], circle[q * quarter]), axis, material, inside);
            for (int i = q * quarter; i < (q + 1) * quarter; i++) sheet(List.of(corners[q], circle[i], circle[i + 1]), axis, material, inside);
        }
    }

    /** One transition sheet, facing away from its {@code axis} point and, where {@code inside}, also towards it. */
    private void sheet(List<double[]> triangle, double[] axis, Material material, boolean inside) {
        mesh.add(triangle, axis, FLAT, material, null, null);
        if (!inside) return;
        var centroid = new double[3];
        for (var p : triangle)
            for (int a = 0; a < 3; a++) centroid[a] += p[a] / 3;
        mesh.add(triangle, new double[]{2 * centroid[0] - axis[0], 2 * centroid[1] - axis[1], 2 * centroid[2] - axis[2]}, FLAT, material, null, null);
    }

    /** A crossover spool along z at (x, y) from a back header's face {@code z0} to the next front header's {@code z1}, flanged at both. */
    private void spool(double x, double y, double z0, double z1, double od) {
        var f = Frame.of(new double[]{x, y, 0}, 2, 1);
        int n = sides(od);
        double r = PipeSizes.flange(od) / 2;
        mesh.prism(f, od / 2, z0, z1, n, Material.PIPE);
        mesh.ring(f, od / 2, r, z0, z0 + .03, n, 3, Material.FLANGE);
        mesh.bolts(f, od, od / 2, r, z0 + .03, 1, Material.BOLT);
        mesh.ring(f, od / 2, r, z1 - .03, z1, n, 3, Material.FLANGE);
        mesh.bolts(f, od, od / 2, r, z1 - .03, -1, Material.BOLT);
        solids.add(Solid.rod(new double[]{x, y, 0}, 2, z0, z1, od / 2, 0));
    }

    /** A manifold along x from {@code from} to {@code to} (capped half its size beyond each) at height {@code y}, station {@code z}. */
    private void manifold(double from, double to, double y, double z, double od) {
        var f = Frame.of(new double[]{0, y, z}, 0, 1);
        mesh.cylinder(f, od / 2, from - od / 2, to + od / 2, sides(od), Material.PIPE);
        solids.add(Solid.rod(new double[]{0, y, z}, 0, from - od / 2, to + od / 2, od / 2, 0));
    }

    // ---- Steam ejector ----------------------------------------------------------------------------------------------

    /**
     * A steam ejector standing on its own frame, sized by its suction s: from the top, the steam chest (motive steam in on
     * top), the suction chamber (suction from the front), the inlet cone, the throat and the diffuser down to its discharge
     * flange, {@code support} up. Four posts round it from the ground to over the chest, tied by square frames every
     * 2.5 m, open at the front in the suction's row; a small one ({@link #SMALL_EJECTOR}), which a plant hangs on its
     * piping, on one post in the cell behind it. Lengths in suction diameters: chest 0.8, chamber 1.5 (1.2 wide), inlet
     * 2.2, throat 0.8 (0.35 wide), diffuser 3.0 (to 0.75 wide).
     */
    private void ejector() {
        // Posts at ±e round the axis, their base plates outside the pipe blocks of the suction and the discharge (half a
        // block each side of the axis), and clear of the chamber; a small ejector's axis on its cell's centre.
        double s = shape.diameter(), e = Math.max(.5 + POST, .6 * s + .3), h = shape.support();
        boolean small = s <= SMALL_EJECTOR + 1e-9;
        double a = small ? .5 : centre(e + POST);
        // The suction's centre on a cell centre: the diffuser's exit at least 0.2 m over the discharge flange.
        int row = (int) Math.ceil(h + .2 + 6.75 * s - .5);
        double exit = row + .5 - 6.75 * s, chamber = exit + 6 * s, chest = exit + 7.5 * s, top = exit + 8.3 * s;
        var f = Frame.of(new double[]{a, exit, a}, 1, 1);
        int n = sides(1.2 * s);
        double rc = .6 * s, throat = .175 * s, outlet = .375 * s;
        mesh.ring(f, 0, outlet + .05 + .1 * s, -.03, 0, n, 1, Material.FLANGE);
        mesh.bolts(f, 2 * outlet, outlet, outlet + .05 + .1 * s, 0, 1, Material.BOLT);
        mesh.frustum(f, outlet, throat, 0, 3 * s, n, Material.MACHINE);
        mesh.prism(f, throat, 3 * s, 3.8 * s, n, Material.MACHINE);
        mesh.frustum(f, throat, rc, 3.8 * s, 6 * s, n, Material.MACHINE);
        mesh.prism(f, rc, 6 * s, 7.5 * s, n, Material.MACHINE);
        mesh.annulus(f, .225 * s, rc, 7.5 * s, n, 1, Material.MACHINE);
        mesh.ring(f, .225 * s, .3 * s, 7.5 * s, 7.58 * s, n, 3, Material.FLANGE);
        mesh.bolts(f, .45 * s, .225 * s, .3 * s, 7.58 * s, 1, Material.BOLT);
        mesh.cylinder(f, .225 * s, 7.5 * s, 8.3 * s, n, Material.MACHINE);
        solids.add(Solid.rod(new double[]{a, 0, a}, 1, exit, chest, rc, 0));
        solids.add(Solid.rod(new double[]{a, 0, a}, 1, chest, top, .225 * s, 0));
        round(a, a, chamber, chest, rc, 1);
        nozzles.add(radialSide(Role.SUCTION, a, row + .5, a, rc, n, s, s));
        // The motive inlet's flange on the block face over the steam chest, and at least two rows over the suction's (one
        // free block between their pipes): a small ejector's stub runs up to it.
        double motive = Math.max(PipeSizes.MIN_OUTSIDE, .45 * s), motiveFace = Math.max(Math.ceil(top + EquipmentShape.STAND_OUT), row + 2);
        nozzles.add(new Nozzle(Role.MOTIVE, new double[]{a, top - .03, a}, 1, 1, motiveFace - top + .03, motive,
                ColumnStructure.size(.3 * s, motive), 0, .03));
        double discharge = Math.max(PipeSizes.MIN_OUTSIDE, 2 * outlet);
        nozzles.add(new Nozzle(Role.DISCHARGE, new double[]{a, exit + .03, a}, 1, -1, exit + .03 - h, discharge, discharge, 0, .03));
        double postTop = top + .3, bar = .14, clamp = exit + 3.4 * s;
        if (small) {
            // One post in the cell behind (the suction's pipe leaves in front, the discharge's below, the motive's above),
            // braced to the throat and the chamber.
            double z = a + 1;
            box(new double[]{a - POST / 2, 0, z - POST / 2}, new double[]{a + POST / 2, postTop, z + POST / 2}, Material.STEELWORK, true);
            box(new double[]{a - POST, 0, z - POST}, new double[]{a + POST, .04, z + POST}, Material.SUPPORT, false);
            mesh.bar(new double[]{a, clamp, z}, new double[]{a, clamp, a + throat}, bar * .6, new double[]{0, 1, 0}, Material.STEELWORK);
            mesh.bar(new double[]{a, (chamber + chest) / 2, z}, new double[]{a, (chamber + chest) / 2, a + rc}, bar * .6, new double[]{0, 1, 0}, Material.STEELWORK);
            return;
        }
        // The frame: posts beside the suction's pipe block, square frames open at the front in its row, a clamp at the
        // throat.
        double[][] corners = {{a - e, a - e}, {a + e, a - e}, {a + e, a + e}, {a - e, a + e}};
        for (var c : corners) {
            box(new double[]{c[0] - POST / 2, 0, c[1] - POST / 2}, new double[]{c[0] + POST / 2, postTop, c[1] + POST / 2}, Material.STEELWORK, true);
            box(new double[]{c[0] - POST, 0, c[1] - POST}, new double[]{c[0] + POST, .04, c[1] + POST}, Material.SUPPORT, false);
        }
        var levels = new ArrayList<Double>();
        for (double y = 2.5; y < postTop - .3; y += 2.5) levels.add(y);
        levels.add(clamp);
        levels.add(postTop - .1);
        for (double y : levels) {
            // In the suction's row its pipe leaves between the front posts: the front bar and brackets give way there.
            boolean suction = y + bar / 2 > row - .05 && y - bar / 2 < row + 1.05;
            for (int k = suction ? 1 : 0; k < 4; k++) {
                var p = corners[k];
                var q = corners[(k + 1) % 4];
                mesh.bar(new double[]{p[0], y, p[1]}, new double[]{q[0], y, q[1]}, bar * .8, new double[]{0, 1, 0}, Material.STEELWORK);
            }
            // At the clamp, brackets from the corners to the throat.
            if (y == clamp)
                for (var c : corners)
                    if (!suction || c[1] > a)
                        mesh.bar(new double[]{c[0], y, c[1]}, new double[]{a + (c[0] - a) * throat / e, y, a + (c[1] - a) * throat / e}, bar * .6,
                                new double[]{0, 1, 0}, Material.STEELWORK);
        }
    }

    // ---- Centrifugal pump -------------------------------------------------------------------------------------------

    /**
     * An API 610 OH2 pump (owner decision M2 2026-10-17; {@code MACHINES_HEATERS_SURVEY.md}) on its baseplate, its shaft
     * along z through a cell centre at height y. From the front: the pump's own Class 300 suction flange on a short cast
     * bell; the volute, its scroll growing from 0.55 to 0.88 impeller diameters round a hub, its throat turning up into
     * the cast discharge neck and flange on the casing's centre line; the cover's joint ring of studs; the bearing bracket
     * with a window each side showing the seal chamber and gland; the bearing housing (oil sump in front with its
     * constant-level oiler, cooling fins behind, a vent on top); the coupling's half-round guard; the TEFC motor (fins,
     * feet, fan cowl and grille, terminal box, lifting eyes). The casing stands on its centreline feet on pedestals (API
     * 610 allows no foot under the bearing housing). From 0.1 m a seal pot (API 682 plans 52/53) stands on its post beside
     * the bracket, from 0.15 m a plan 11 flush line runs from the discharge neck to the gland. The baseplate's drain rim
     * stands on a grout line on a chamfered plinth; between the pump's flanges and the block faces the nozzles run as
     * spools of the connected pipe. Sizes follow the discharge d: suction {@link #pumpSuction}, impeller 0.18 + 1.2 d, the
     * motor {@link #motorDiameter} of {@link #pumpPower} across.
     */
    private void pump() {
        if (shape.diameter() > OH2_MAX + 1e-9) {
            splitPump();
            return;
        }
        double d = shape.diameter(), s = pumpSuction(d), di = .18 + 1.2 * d;
        double rh = .45 * di, rv0 = .55 * di, rv1 = .88 * di, width = .4 * di + .06, joint = .82 * di, tj = .04 + .04 * di;
        double plate = .15 + .1 * di, rm = motorDiameter(pumpPower(d)) / 2, lm = 3.2 * rm + .1, fin = .025 + .06 * rm;
        double rb = .08 + .22 * di, lb = .3 + .6 * di, lk = .2 + .3 * di, guard = .12 + .2 * di, spacer = .15 + .25 * di, shaft = .03 + .05 * di;
        // The pump's own Class 300 flanges: about 15 % wider and 30 % thicker than the line's Class 150.
        double fs = 1.15 * PipeSizes.flange(s) / 2, ts = 1.3 * PipeSizes.flangeThickness(s), fd = 1.15 * PipeSizes.flange(d) / 2, td = 1.3 * PipeSizes.flangeThickness(d);
        double bell = .08 + .7 * s, pot = d >= .1 ? .07 + .1 * d : 0;
        double half = Math.max(Math.max(rv1, joint) + .14, Math.max(Math.max(rm + fin, guard) + .08, pot > 0 ? .55 * di + 2 * pot + .17 : 0));
        double a = centre(half + .12);
        // Heights: a plinth of 0.15 to 0.45 m, a grout line, the baseplate; the shaft on the lowest cell centre whose casing
        // clears the baseplate by 0.06 m, the pedestals taking what the plinth does not.
        double y = Math.max(.5, Math.ceil(.15 + .04 + plate + .06 + rv1 - .5) + .5);
        double plinth = Math.max(.15, Math.min(.45, y - rv1 - .3 - plate - .04)), top = plinth + .04 + plate;
        // Along z: the line's spool, the suction flange and bell, the casing (its middle on a cell centre, the discharge's
        // line), the cover, the bracket, the bearing housing, the spacer under its guard, the motor.
        double zc = Math.ceil(.25 + ts + bell + .3 * width - .5) + .5, z0 = zc - width / 2, z1 = zc + width / 2, zh = zc - .3 * width, zf = zh - bell - ts;
        double zk = z1 + tj, zb0 = zk + lk, zb1 = zb0 + lb, zm0 = zb1 + .08 + spacer, zm1 = zm0 + lm, cowl = zm1 - .18 * lm, seal = zk + .45 * lk;
        var f = Frame.of(new double[]{a, y, 0}, 2, 1);
        int n = sides(2 * rv1), nm = sides(2 * rm), nb = sides(2 * rb);
        // The suction flange and bell, the volute and its hub, the cover with its joint ring of studs.
        mesh.cylinder(f, fs, zf, zf + ts, n, Material.FLANGE);
        mesh.bolts(f, s, .5 * s + .03, fs, zf, -1, Material.BOLT);
        mesh.frustum(f, .5 * s + .04, .43 * s + .04, zf + ts, zh, n, Material.MACHINE);
        scroll(a, y, z0, z1, zh, rh, rv0, rv1);
        mesh.prism(f, rh, zh, z1, n, Material.MACHINE);
        mesh.annulus(f, .43 * s + .04, rh, zh, n, -1, Material.MACHINE);
        mesh.cylinder(f, joint, z1, zk, n, Material.MACHINE);
        mesh.bolts(f, Math.min(PipeSizes.MAX_OUTSIDE, 1.6 * joint), joint - .12, joint, zk, 1, Material.BOLT);
        // The bracket, windowed each side over the seal chamber, its gland and the shaft.
        bracket(f, .62 * di, rb + .03, zk, zb0, n);
        mesh.cylinder(f, .27 * di, zk, seal, n, Material.MACHINE);
        mesh.cylinder(f, .2 * di, seal, seal + .05, n, Material.FLANGE);
        mesh.prism(f, shaft, seal + .05, zb0, 8, Material.PIPE);
        // The bearing housing: its oil sump, the finned round body, the end cover; the oiler on the sump, the vent on top.
        double sump = zb0 + .45 * lb;
        box(new double[]{a - .9 * rb, y - 1.2 * rb, zb0}, new double[]{a + .9 * rb, y + .55 * rb, sump}, Material.MACHINE, false);
        mesh.prism(f, rb, zb0, zb1, nb, Material.MACHINE);
        mesh.annulus(f, rb, rb + .03, zb0, nb, 1, Material.MACHINE);
        mesh.annulus(f, 0, rb, zb1, nb, 1, Material.MACHINE);
        for (int k = 1; k <= 5; k++) {
            double z = sump + (zb1 - sump) * k / 6;
            mesh.ring(f, rb, rb + .025, z - .01, z + .01, nb, 3, Material.MACHINE);
        }
        mesh.cylinder(f, .55 * rb, zb1, zb1 + .03, nb, Material.MACHINE);
        double bulb = .035 + .02 * di, oiler = y - .8 * rb, xo = a + .9 * rb + .06 + bulb;
        box(new double[]{a + .9 * rb, oiler - .015, zb0 + .2 * lb - .015}, new double[]{xo, oiler + .015, zb0 + .2 * lb + .015}, Material.PIPE, false);
        mesh.cylinder(Frame.of(new double[]{xo, 0, zb0 + .2 * lb}, 1, 1), bulb, oiler, oiler + .13 + .05 * di, 8, Material.OPENING);
        mesh.cylinder(Frame.of(new double[]{a, 0, sump + .6 * (zb1 - sump)}, 1, 1), .025 + .01 * di, y + rb - .01, y + rb + .05, 8, Material.PIPE);
        // The coupling guard, a half-round hood over the spacer.
        var hood = new ArrayList<double[]>();
        hood.add(new double[]{a - guard, top});
        hood.add(new double[]{a + guard, top});
        for (int k = 0; k <= 6; k++)
            hood.add(new double[]{a + guard * DeterministicMath.cos(Math.PI * k / 6), y + guard * DeterministicMath.sin(Math.PI * k / 6)});
        extrude(hood, zb1 + .04, zm0 - .03, Material.GUARD);
        mesh.prism(f, shaft, zb1 + .03, zm0, 8, Material.PIPE);
        solids.add(Solid.box(new double[]{a - guard, top, zb1 + .04}, new double[]{a + guard, y + guard, zm0 - .03}));
        motor(f, a, y, rm, fin, zm0, cowl, zm1, nm, shaft, top);
        // The casing's centreline feet near its cover on their pedestals.
        for (int side = -1; side <= 1; side += 2) {
            double inner = a + side * rh, outer = a + side * (rv1 + .1), px = a + side * (rv1 + .03);
            box(new double[]{Math.min(inner, outer), y - .06, zc}, new double[]{Math.max(inner, outer), y + .02, zk}, Material.MACHINE, false);
            box(new double[]{px - .07, top, zc + .02}, new double[]{px + .07, y - .06, zk - .02}, Material.STEELWORK, false);
        }
        // The cast discharge: a throat of about 1.2 DN square turning round into the flange, 0.12 + 0.2 d over the crown.
        double hx = .6 * d + .03, hz = Math.min(.6 * d + .03, .45 * width), neck = y + rv1 + .12 + .2 * d;
        box(new double[]{a - hx, y + rh, zc - hz}, new double[]{a + hx, y + .98 * rv0, zc + hz}, Material.MACHINE, false);
        transition(a - hx, a + hx, zc - hz, zc + hz, y + .98 * rv0, a, zc, .5 * d + .025, neck, 4 * ((sides(d) + 3) / 4), Material.MACHINE, false);
        var up = Frame.of(new double[]{a, 0, zc}, 1, 1);
        mesh.cylinder(up, fd, neck, neck + td, n, Material.FLANGE);
        mesh.bolts(up, d, .5 * d + .03, fd, neck + td, 1, Material.BOLT);
        // The baseplate on its grout line and chamfered plinth: its drain rim and drain.
        double zp0 = z0 - .1, zp1 = zm1 + .05;
        box(new double[]{a - half, plinth + .04, zp0}, new double[]{a + half, top, zp1}, Material.STEELWORK, true);
        for (int side = -1; side <= 1; side += 2) {
            box(new double[]{side < 0 ? a - half : a + half - .03, top, zp0}, new double[]{side < 0 ? a - half + .03 : a + half, top + .05, zp1}, Material.STEELWORK, false);
            double ze = side < 0 ? zp0 : zp1 - .03;
            box(new double[]{a - half + .03, top, ze}, new double[]{a + half - .03, top + .05, ze + .03}, Material.STEELWORK, false);
        }
        mesh.cylinder(Frame.of(new double[]{a + half - .15, plinth + .04 + plate / 2, 0}, 2, 1), .03, zp0 - .06, zp0, 8, Material.PIPE);
        box(new double[]{a - half + .02, plinth, zp0 + .02}, new double[]{a + half - .02, plinth + .04, zp1 - .02}, Material.CONCRETE, false);
        double base = half + .12, chamfer = .04;
        extrude(List.of(new double[]{a - base, 0}, new double[]{a + base, 0}, new double[]{a + base, plinth - chamfer}, new double[]{a + base - chamfer, plinth},
                new double[]{a - base + chamfer, plinth}, new double[]{a - base, plinth - chamfer}), zp0 - .12, zp1 + .12, Material.CONCRETE);
        solids.add(Solid.box(new double[]{a - base, 0, zp0 - .12}, new double[]{a + base, plinth + .04, zp1 + .12}));
        // The seal's pot on its post beside the bracket and its two lines into the gland; the flush line from the neck.
        if (pot > 0) {
            double xp = a - (.55 * di + pot + .12), bottom = y - .05, height = .55 + 1.2 * d;
            var p = Frame.of(new double[]{xp, 0, seal}, 1, 1);
            mesh.prism(p, pot, bottom, bottom + height, 12, Material.MACHINE);
            mesh.head(p, pot, bottom + height, pot / 2, 12, Material.MACHINE);
            mesh.annulus(p, 0, pot, bottom, 12, -1, Material.MACHINE);
            box(new double[]{xp - .04, top, seal - .04}, new double[]{xp + .04, bottom, seal + .04}, Material.STEELWORK, false);
            for (double yl : new double[]{y - .02, y + .1 * di + .04})
                mesh.cylinder(Frame.of(new double[]{0, yl, seal + .025}, 0, 1), .012, xp + pot - .01, a - .2 * di + .01, 6, Material.PIPE);
            solids.add(Solid.rod(new double[]{xp, 0, seal}, 1, bottom, bottom + height, pot, pot / 2));
        }
        if (d >= .15) {
            double x1 = a + Math.max(rv1, joint) + .06, tap = (y + rv0 + neck) / 2, low = y + .1 * di;
            line(.015, Material.PIPE, new double[]{a + .5 * d + .02, tap, zc}, new double[]{x1, tap, zc}, new double[]{x1, low, zc}, new double[]{x1, low, seal + .025},
                    new double[]{a + .2 * di - .01, low, seal + .025});
        }
        solids.add(Solid.rod(new double[]{a, y, 0}, 2, zf, zk, Math.max(rv1, joint), 0));
        solids.add(Solid.rod(new double[]{a, y, 0}, 2, zk, zb1 + .03, Math.max(rb + .03, .62 * di), 0));
        round(a, y, z0, z1, rv1);
        // The suction's line from its flange to the box's front face; the discharge's up from the neck's flange.
        nozzles.add(new Nozzle(Role.SUCTION, new double[]{a, y, zf + .02}, 2, -1, zf + .02, s, s, 0, .03));
        double base0 = neck + td - .01, face = Math.ceil(neck + td + EquipmentShape.STAND_OUT);
        nozzles.add(new Nozzle(Role.DISCHARGE, new double[]{a, base0, zc}, 1, 1, face - base0, d, d, 0, .03));
    }

    /**
     * An API 610 BB1 pump (owner decision 2026-10-17, "add an axially split pump as a second pump type": ok;
     * {@code MACHINES_SCALE_RESEARCH.md}): between bearings, single stage, double suction, its casing split along the
     * shaft's centre line. The lower half, deep enough for both nozzles, carries them side by side across the shaft (the
     * suction on the right seen from the constructor, the discharge on the left, both horizontal, half a metre under the
     * shaft) and stands on four feet; the upper half, the volute's round cover over the suction chambers either side of
     * it, is bolted on the split flange running the casing's length, with two lifting lugs. Seal chambers in windowed
     * brackets and finned bearing housings at both ends; the coupling guard and a large motor with its top cooler behind,
     * on one baseplate, grout line and plinth. Sizes follow the discharge d: suction {@link #pumpSuction}, impeller
     * 0.35 + 0.8 d, the motor about 1.15 {@link #motorDiameter} of {@link #pumpPower} across.
     */
    private void splitPump() {
        double d = shape.diameter(), s = pumpSuction(d), di = .35 + .8 * d;
        double rv = .9 * di, rc = .72 * di, wv = .5 * di, lc = 1.5 * di, split = .03 + .02 * di;
        double fs = 1.15 * PipeSizes.flange(s) / 2, ts = 1.3 * PipeSizes.flangeThickness(s), fd = 1.15 * PipeSizes.flange(d) / 2, td = 1.3 * PipeSizes.flangeThickness(d);
        double rb = .1 + .2 * di, lb = .25 + .35 * di, lk = .2 + .25 * di, guard = .15 + .2 * di, spacer = .2 + .2 * di, shaft = .04 + .05 * di;
        double across = 1.15 * motorDiameter(pumpPower(d)), lm = 1.7 * across + .2, plate = .2 + .1 * di;
        // Across x: each nozzle's branch reaches a spool past the casing; the box holds both flanges and their block faces.
        double reachS = rv + .25 * s + .15, reachD = rv + .25 * d + .15, deck = Math.max(rv + .2, across / 2 + .3);
        double a = centre(Math.max(Math.max(reachS + ts, reachD + td) + EquipmentShape.STAND_OUT + .05, deck + .12));
        // Heights: a 0.3 m plinth, a grout line, the baseplate; the nozzles on the lowest cell centre whose larger flange
        // clears the baseplate by 0.06 m, the shaft half a metre over them.
        double plinth = .3, top = plinth + .04 + plate, yn = Math.ceil(top + Math.max(fs, fd) + .06 - .5) + .5, y = yn + .5, low = yn - .5 * s - .05;
        // Along z: the free end's housing and bracket, the casing (its middle, the nozzles' line, on a cell centre), the
        // drive end's bracket and housing, the coupling under its guard, the motor.
        double zc = Math.ceil(.15 + lb + lk + lc / 2 - .5) + .5, z0 = zc - lc / 2, z1 = zc + lc / 2;
        double zf1 = z0 - lk, zf0 = zf1 - lb, zb0 = z1 + lk, zb1 = zb0 + lb, zm0 = zb1 + .08 + spacer, zm1 = zm0 + lm;
        var f = Frame.of(new double[]{a, y, 0}, 2, 1);
        int n = sides(2 * rv), nb = sides(2 * rb);
        // The upper casing: the suction chambers, the volute between them, the end walls.
        mesh.annulus(f, 0, rc, z0, n, -1, Material.MACHINE);
        mesh.prism(f, rc, z0, zc - wv / 2, n, Material.MACHINE);
        mesh.annulus(f, rc, rv, zc - wv / 2, n, -1, Material.MACHINE);
        mesh.prism(f, rv, zc - wv / 2, zc + wv / 2, n, Material.MACHINE);
        mesh.annulus(f, rc, rv, zc + wv / 2, n, 1, Material.MACHINE);
        mesh.prism(f, rc, zc + wv / 2, z1, n, Material.MACHINE);
        mesh.annulus(f, 0, rc, z1, n, 1, Material.MACHINE);
        // The lower casing down to the nozzles, its four feet; the split flange along the casing with its studs; the lugs.
        double lx = .8 * rv;
        box(new double[]{a - lx, low, z0}, new double[]{a + lx, y, z1}, Material.MACHINE, true);
        for (int side = -1; side <= 1; side += 2)
            for (double z : new double[]{z0 + .05, z1 - .3}) {
                double outer = a + side * lx, inner = outer - side * .22;
                box(new double[]{Math.min(inner, outer), top, z}, new double[]{Math.max(inner, outer), low, z + .25}, Material.MACHINE, false);
            }
        box(new double[]{a - rv - .08, y - split, z0 - .04}, new double[]{a + rv + .08, y + split, z1 + .04}, Material.MACHINE, false);
        int studs = Math.max(3, (int) Math.round(lc / .22));
        for (int side = -1; side <= 1; side += 2)
            for (int k = 0; k < studs; k++) {
                double x = a + side * (rv + .03), z = z0 + lc * (k + .5) / studs;
                box(new double[]{x - .025, y + split, z - .025}, new double[]{x + .025, y + split + .06, z + .025}, Material.BOLT, false);
            }
        for (double z : new double[]{zc - .3 * lc, zc + .3 * lc}) box(new double[]{a - .03, y + rc - .05, z - .08}, new double[]{a + .03, y + rc + .12, z + .08}, Material.MACHINE, false);
        // The suction branch to the constructor's right (-x), the discharge branch to its left, each with its Class 300 flange.
        for (int side = -1; side <= 1; side += 2) {
            double bore = side < 0 ? s : d, reach = side < 0 ? reachS : reachD, fr = side < 0 ? fs : fd, ft = side < 0 ? ts : td;
            var branch = Frame.of(new double[]{a, yn, zc}, 0, side);
            int nn = sides(bore + .1);
            mesh.frustum(branch, .6 * bore + .06, .5 * bore + .04, .5 * lx, reach, nn, Material.MACHINE);
            mesh.cylinder(branch, fr, reach, reach + ft, nn, Material.FLANGE);
            mesh.bolts(branch, bore, .5 * bore + .03, fr, reach + ft, 1, Material.BOLT);
            double inner = a + side * .5 * lx, outer = a + side * (reach + ft);
            solids.add(Solid.rod(new double[]{0, yn, zc}, 0, Math.min(inner, outer), Math.max(inner, outer), Math.max(fr, .6 * bore + .06), 0));
        }
        // The brackets, seal chambers and finned bearing housings at both ends; the shaft through them.
        bracket(f, rb + .03, .45 * di, zf1, z0, n);
        bracket(f, .45 * di, rb + .03, z1, zb0, n);
        mesh.cylinder(f, .22 * di, z0 - .55 * lk, z0, n, Material.MACHINE);
        mesh.cylinder(f, .22 * di, z1, z1 + .55 * lk, n, Material.MACHINE);
        mesh.prism(f, shaft, zf1, z0 - .55 * lk, 8, Material.PIPE);
        mesh.prism(f, shaft, z1 + .55 * lk, zb0, 8, Material.PIPE);
        for (int e = 0; e < 2; e++) {
            double h0 = e == 0 ? zf0 : zb0, h1 = e == 0 ? zf1 : zb1;
            mesh.prism(f, rb, h0, h1, nb, Material.MACHINE);
            mesh.annulus(f, 0, rb, e == 0 ? zf0 : zb1, nb, e == 0 ? -1 : 1, Material.MACHINE);
            for (int k = 1; k <= 3; k++) {
                double z = h0 + (h1 - h0) * k / 4;
                mesh.ring(f, rb, rb + .025, z - .01, z + .01, nb, 3, Material.MACHINE);
            }
            mesh.cylinder(Frame.of(new double[]{a, 0, (h0 + h1) / 2}, 1, 1), .025 + .01 * di, y + rb - .01, y + rb + .05, 8, Material.PIPE);
        }
        // The coupling guard, a half-round hood over the spacer, and the motor.
        var hood = new ArrayList<double[]>();
        hood.add(new double[]{a - guard, top});
        hood.add(new double[]{a + guard, top});
        for (int k = 0; k <= 6; k++)
            hood.add(new double[]{a + guard * DeterministicMath.cos(Math.PI * k / 6), y + guard * DeterministicMath.sin(Math.PI * k / 6)});
        extrude(hood, zb1 + .04, zm0 - .03, Material.GUARD);
        mesh.prism(f, shaft, zb1 + .03, zm0, 8, Material.PIPE);
        solids.add(Solid.box(new double[]{a - guard, top, zb1 + .04}, new double[]{a + guard, y + guard, zm0 - .03}));
        largeMotor(a, y, zm0, across, lm, top, false);
        // The baseplate on its grout line and chamfered plinth.
        double zp0 = zf0 - .15, zp1 = zm1 + .1, base = deck + .12, chamfer = .04;
        box(new double[]{a - deck, plinth + .04, zp0}, new double[]{a + deck, top, zp1}, Material.STEELWORK, true);
        box(new double[]{a - deck + .02, plinth, zp0 + .02}, new double[]{a + deck - .02, plinth + .04, zp1 - .02}, Material.CONCRETE, false);
        extrude(List.of(new double[]{a - base, 0}, new double[]{a + base, 0}, new double[]{a + base, plinth - chamfer}, new double[]{a + base - chamfer, plinth},
                new double[]{a - base + chamfer, plinth}, new double[]{a - base, plinth - chamfer}), zp0 - .12, zp1 + .12, Material.CONCRETE);
        solids.add(Solid.box(new double[]{a - base, 0, zp0 - .12}, new double[]{a + base, plinth + .04, zp1 + .12}));
        solids.add(Solid.rod(new double[]{a, y, 0}, 2, z0, z1, rv, 0));
        solids.add(Solid.rod(new double[]{a, y, 0}, 2, zf0, zb1, Math.max(rb + .03, .45 * di), 0));
        volumes.add(Solid.rod(new double[]{a, y, 0}, 2, z0 + .05, z1 - .05, rc - .05, 0));
        volumes.add(Solid.box(new double[]{a - lx + .05, low + .05, z0 + .05}, new double[]{a + lx - .05, y, z1 - .05}));
        round(a, y, zc - wv / 2, zc + wv / 2, rv);
        // The nozzles: from each flange's face to the block face beyond it.
        double bs = a - reachS - ts + .02, bd = a + reachD + td - .02;
        nozzles.add(new Nozzle(Role.SUCTION, new double[]{bs, yn, zc}, 0, -1, bs - Math.floor(bs - .03 - EquipmentShape.STAND_OUT), s, s, 0, .03));
        nozzles.add(new Nozzle(Role.DISCHARGE, new double[]{bd, yn, zc}, 0, 1, Math.ceil(bd + .03 + EquipmentShape.STAND_OUT) - bd, d, d, 0, .03));
    }

    /**
     * A pump's volute round its shaft (along z at (x, y)) from {@code z0} to {@code z1}: a scroll from just past the top
     * (the cutwater) round to the throat at the top, its outer wall growing from {@code r0} to {@code r1} over a hub of
     * radius {@code rh} whose front is recessed to {@code zh}; its section a rectangle with chamfered outer corners. The
     * outer wall is lit as round, the rest flat; a chamfer, twisted between stations, is drawn as two triangles.
     */
    private void scroll(double x, double y, double z0, double z1, double zh, double rh, double r0, double r1) {
        int steps = 20;
        double t0 = .45, t1 = 2 * Math.PI - .1, c = .3 * (r0 - rh), zm = (z0 + z1) / 2;
        var round = new Radial(new double[]{x, y, 0}, new double[]{0, 0, 1});
        double[][][] sections = new double[steps + 1][][];
        for (int i = 0; i <= steps; i++) {
            double t = t0 + (t1 - t0) * i / steps, r = r0 + (r1 - r0) * i / steps, sx = DeterministicMath.sin(t), cy = DeterministicMath.cos(t);
            double[][] rz = {{rh, z0}, {r - c, z0}, {r, z0 + c}, {r, z1 - c}, {r - c, z1}, {rh, z1}, {rh, zh}};
            sections[i] = new double[rz.length][];
            for (int k = 0; k < rz.length; k++) sections[i][k] = new double[]{x + rz[k][0] * sx, y + rz[k][0] * cy, rz[k][1]};
        }
        for (int i = 0; i < steps; i++) {
            double[][] p = sections[i], q = sections[i + 1];
            double t = t0 + (t1 - t0) * (i + .5) / steps, r = r0 + (r1 - r0) * (i + .5) / steps, mid = (rh + r) / 2;
            var inside = new double[]{x + mid * DeterministicMath.sin(t), y + mid * DeterministicMath.cos(t), zm};
            mesh.flat(List.of(p[0], p[1], q[1], q[0]), new double[]{0, 0, -1}, Material.MACHINE);
            mesh.flat(List.of(p[4], p[5], q[5], q[4]), new double[]{0, 0, 1}, Material.MACHINE);
            mesh.add(List.of(p[1], p[2], q[2]), inside, FLAT, Material.MACHINE, null, null);
            mesh.add(List.of(p[1], q[2], q[1]), inside, FLAT, Material.MACHINE, null, null);
            mesh.add(List.of(p[2], p[3], q[3], q[2]), inside, round, Material.MACHINE, null, null);
            mesh.add(List.of(p[3], p[4], q[4]), inside, FLAT, Material.MACHINE, null, null);
            mesh.add(List.of(p[3], q[4], q[3]), inside, FLAT, Material.MACHINE, null, null);
            // The inner wall shows only in front of the hub.
            mesh.add(List.of(p[0], p[6], q[6], q[0]), new double[]{x + (rh + .1) * DeterministicMath.sin(t), y + (rh + .1) * DeterministicMath.cos(t), (z0 + zh) / 2}, FLAT,
                    Material.MACHINE, null, null);
        }
        for (int end = 0; end < 2; end++) {
            double t = end == 0 ? t0 : t1;
            var cap = new ArrayList<double[]>();
            for (int k = 0; k < 6; k++) cap.add(sections[end == 0 ? 0 : steps][k]);
            double sign = end == 0 ? -1 : 1;
            mesh.flat(cap, new double[]{sign * DeterministicMath.cos(t), -sign * DeterministicMath.sin(t), 0}, Material.MACHINE);
        }
    }

    /**
     * A pump's bearing bracket on frame {@code f} (its shaft) from {@code z0} to {@code z1}, narrowing from {@code r0} to
     * {@code r1}: a cast shell 0.025 m thick, open in a window each side (frame ±x) over the middle 60 % of its length and
     * 100° of its round, drawn inside and out so the seal shows through.
     */
    private void bracket(Frame f, double r0, double r1, double z0, double z1, int n) {
        double t = .025, w0 = z0 + .2 * (z1 - z0), w1 = z0 + .8 * (z1 - z0), window = Math.toRadians(50);
        double[] stations = {z0, w0, w1, z1};
        for (int band = 0; band < 3; band++) {
            double za = stations[band], zb = stations[band + 1], ra = r0 + (r1 - r0) * (za - z0) / (z1 - z0), rb = r0 + (r1 - r0) * (zb - z0) / (z1 - z0);
            for (int i = 0; i < n; i++) {
                double a0 = 2 * Math.PI * i / n, a1 = 2 * Math.PI * (i + 1) / n, mid = (a0 + a1) / 2;
                boolean open = band == 1 && (apart(mid, 0) < window || apart(mid, Math.PI) < window);
                if (open) continue;
                var axis = f.along((za + zb) / 2);
                mesh.add(List.of(f.at(a0, ra, za), f.at(a1, ra, za), f.at(a1, rb, zb), f.at(a0, rb, zb)), axis, FLAT, Material.MACHINE, null, null);
                mesh.add(List.of(f.at(a0, ra - t, za), f.at(a1, ra - t, za), f.at(a1, rb - t, zb), f.at(a0, rb - t, zb)), f.at(mid, 2 * ra, (za + zb) / 2), FLAT, Material.MACHINE,
                        null, null);
                if (band == 1) {
                    // A window's jambs at its edges.
                    for (double edge : new double[]{a0, a1}) {
                        boolean beside = apart(edge == a0 ? a0 - Math.PI / n : a1 + Math.PI / n, 0) < window || apart(edge == a0 ? a0 - Math.PI / n : a1 + Math.PI / n, Math.PI) < window;
                        if (beside)
                            mesh.add(List.of(f.at(edge, ra - t, za), f.at(edge, ra, za), f.at(edge, rb, zb), f.at(edge, rb - t, zb)), f.at(mid, (ra + rb) / 2 - t / 2, (za + zb) / 2),
                                    FLAT, Material.MACHINE, null, null);
                    }
                }
            }
            if (band == 1)
                for (int i = 0; i < n; i++) {
                    double a0 = 2 * Math.PI * i / n, a1 = 2 * Math.PI * (i + 1) / n, mid = (a0 + a1) / 2;
                    if (!(apart(mid, 0) < window || apart(mid, Math.PI) < window)) continue;
                    // The window's sills: the ends of the bands either side.
                    mesh.flat(List.of(f.at(a0, ra - t, za), f.at(a1, ra - t, za), f.at(a1, ra, za), f.at(a0, ra, za)), f.axis(), Material.MACHINE);
                    mesh.flat(List.of(f.at(a0, rb - t, zb), f.at(a1, rb - t, zb), f.at(a1, rb, zb), f.at(a0, rb, zb)), neg(f.axis()), Material.MACHINE);
                }
        }
        mesh.annulus(f, r0 - t, r0, z0, n, -1, Material.MACHINE);
    }

    /** The smaller angle between two directions, radians. */
    private static double apart(double a, double b) {
        double d = Math.abs(a - b) % (2 * Math.PI);
        return Math.min(d, 2 * Math.PI - d);
    }

    /**
     * A TEFC motor on frame {@code f} (its shaft, at (x, y)) of radius {@code r}: its body from {@code z0} with axial fins
     * {@code fin} high round all but its underside, its fan cowl from {@code cowl} to {@code z1} with the grille, the
     * drive end's bearing cap and shaft, its terminal box and lifting eyes on top, four feet on a pedestal down to the
     * baseplate at {@code top}.
     */
    private void motor(Frame f, double x, double y, double r, double fin, double z0, double cowl, double z1, int n, double shaft, double top) {
        mesh.prism(f, r, z0, cowl, n, Material.MOTOR);
        mesh.annulus(f, 0, r, z0, n, -1, Material.MOTOR);
        mesh.cylinder(f, .35 * r, z0 - .04, z0, n, Material.MOTOR);
        mesh.prism(f, shaft, z0 - .1, z0 - .04, 8, Material.PIPE);
        int fins = Math.max(16, Math.min(32, (int) Math.round(2 * Math.PI * r / .08)));
        for (int k = 0; k < fins; k++) {
            double angle = Math.PI / 2 + 2 * Math.PI * k / fins, below = apart(angle, -Math.PI / 2);
            if (below < Math.toRadians(50)) continue;
            double[] radial = {DeterministicMath.cos(angle), DeterministicMath.sin(angle), 0}, tangent = {-radial[1], radial[0], 0};
            mesh.block(f.at(angle, r + fin / 2 - .01, (z0 + .03 + cowl - .02) / 2), scale(radial, fin / 2), new double[]{0, 0, (cowl - .05 - z0) / 2}, scale(tangent, .01), Material.MOTOR);
        }
        double rc = r + fin;
        mesh.annulus(f, r, rc, cowl, n, -1, Material.MOTOR);
        mesh.frustum(f, rc, .92 * rc, cowl, z1, n, Material.MOTOR);
        mesh.annulus(f, .8 * rc, .92 * rc, z1, n, 1, Material.MOTOR);
        mesh.annulus(f, 0, .8 * rc, z1, n, 1, Material.OPENING);
        double terminal = z0 + .3 * (z1 - z0);
        box(new double[]{x - .35 * r, y + r - .02, terminal}, new double[]{x + .35 * r, y + 1.45 * r, terminal + .55 * r}, Material.MOTOR, false);
        mesh.cylinder(Frame.of(new double[]{0, y + 1.2 * r, terminal + .27 * r}, 0, 1), .06 * r + .01, x + .35 * r, x + .35 * r + .05, 8, Material.MOTOR);
        for (double z : new double[]{z0 + .12 * (z1 - z0), cowl - .1 * (z1 - z0)}) {
            var eye = Frame.of(new double[]{x, y + rc + .02 + .02 * r, 0}, 2, 1);
            mesh.ring(eye, .02 + .02 * r, .04 + .03 * r, z - .01, z + .01, 8, 3, Material.MOTOR);
        }
        double feet = y - r - .02;
        for (int side = -1; side <= 1; side += 2)
            for (double z : new double[]{z0 + .08, cowl - .25}) {
                double inner = x + side * .5 * r, outer = x + side * .9 * r;
                box(new double[]{Math.min(inner, outer), feet, z}, new double[]{Math.max(inner, outer), y - .55 * r, z + .17}, Material.MOTOR, false);
            }
        if (feet > top + .01) box(new double[]{x - .95 * r, top, z0 + .02}, new double[]{x + .95 * r, feet, cowl - .02}, Material.STEELWORK, true);
        solids.add(Solid.rod(new double[]{x, y, 0}, 2, z0, z1, rc, 0));
        round(x, y, z0, z1, r);
    }

    /** A line of radius {@code r} through {@code points}, a ball at each bend (a seal's flush tube, a heater's insulated crossover); legs of no length are skipped. */
    private void line(double r, Material material, double[]... points) {
        int sides = r < .05 ? 6 : 12;
        for (int k = 0; k + 1 < points.length; k++) {
            var along = sub(points[k + 1], points[k]);
            double length = Math.sqrt(dot(along, along));
            if (length < 1e-6) continue;
            var dir = unit(along);
            var ref = Math.abs(dir[1]) < .9 ? new double[]{0, 1, 0} : new double[]{1, 0, 0};
            mesh.prism(new Frame(points[k], dir, unit(cross(dir, ref))), r, 0, length, sides, material);
            if (k > 0) mesh.sphere(points[k], r * 1.05, sides, material);
        }
    }

    /** A prism along z from {@code z0} to {@code z1} whose section is the convex polygon {@code outline} of (x, y) points, both ends drawn. */
    private void extrude(List<double[]> outline, double z0, double z1, Material material) {
        int m = outline.size();
        double cx = 0, cy = 0;
        for (var p : outline) {
            cx += p[0] / m;
            cy += p[1] / m;
        }
        var front = new ArrayList<double[]>();
        var back = new ArrayList<double[]>();
        for (int i = 0; i < m; i++) {
            double[] p = outline.get(i), q = outline.get((i + 1) % m);
            mesh.add(List.of(new double[]{p[0], p[1], z0}, new double[]{q[0], q[1], z0}, new double[]{q[0], q[1], z1}, new double[]{p[0], p[1], z1}),
                    new double[]{cx, cy, (z0 + z1) / 2}, FLAT, material, null, null);
            front.add(new double[]{p[0], p[1], z0});
            back.add(new double[]{p[0], p[1], z1});
        }
        mesh.flat(front, new double[]{0, 0, -1}, material);
        mesh.flat(back, new double[]{0, 0, 1}, material);
    }

    // ---- Reciprocating compressor -----------------------------------------------------------------------------------

    /**
     * An API 618 balanced-opposed compressor (owner decision M4 2026-10-17; {@code MACHINES_HEATERS_SURVEY.md}), its
     * crankshaft along z on a steel table {@code support} high (owner request 2026-10-17: steel beams like the other
     * units). The frame, ribbed, a bolted cover over each throw, a breather, the oil pump's cover at its free end and the
     * lubricator on its side; on alternate sides of it, each throw's crosshead guide (a bolted door on top), its distance
     * piece (flanged at both ends, a window cover each side of each compartment, two compartments when long) and its
     * double-acting cylinder: a heavy flange at its crank end, valve caps at both ends on top (suction) and underneath
     * (discharge), one at each end of a small cylinder and two of a large (0.45 m bore up) beside its nozzles, a bolted head
     * and from 0.3 m bore a clearance pocket with its handwheel, an outboard support under the head. On each side a
     * suction bottle lies over the cylinders and a discharge bottle under them on saddled posts, both as long as the longer
     * side's, each joined to every cylinder by a flanged neck. One suction header crosses the machine over its outboard
     * (front) end and drops into both suction bottles, the inlet on top of its middle; one discharge header joins both discharge bottles'
     * front heads in front of the frame, the outlet at the front of its middle (owner request 2026-10-17: one inlet, one
     * outlet). The synchronous motor drives the crankshaft's back end through a guarded coupling ({@link #syncMotor}).
     * Sizes from {@link Compressor}.
     */
    private void compressor() {
        var c = Compressor.of(shape.diameter(), (int) shape.tangentLength());
        int throwCount = c.throwCount(), h = shape.support();
        double b = c.bore(), yc = h + c.crank(), ds = c.suction(), dd = c.discharge(), cyl = c.cylinder(), lc = c.cylinderLength(), gh = c.guide(), piece = c.piece();
        double ysb = yc + cyl / 2 + c.gap() + ds / 2, ydb = c.dischargeAxis(h), pocket = b >= .3 ? .3 + .3 * b : 0;
        double hs = c.suctionHeader(), hd = c.dischargeHeader();
        double a = centre(c.out() + Math.max(lc / 2 + .12 + pocket + (pocket > 0 ? .12 : 0), Math.max(ds, dd) / 2) + .05);
        // Along z from the frame's front: an auxiliary end of 0.6 m, the throws at the crank pitch, 0.6 m to the drive end.
        // The bottles span the cylinders of either side and two diameters at least.
        var zs = new double[throwCount];
        for (int k = 0; k < throwCount; k++) zs[k] = .6 + (k + .5) * c.pitch();
        double end = throwCount * c.pitch() + 1.2;
        double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
        for (int side = 0; side < 2; side++) {
            lo = Math.min(lo, zs[side] - cyl / 2 - .1);
            hi = Math.max(hi, zs[throwCount - 2 + side] + cyl / 2 + .1);
        }
        double grow = Math.max(0, 2 * ds - (hi - lo)) / 2;
        lo -= grow;
        hi += grow;
        // The discharge header on a cell centre in front of the bottles' heads and the table; everything shifted by whole
        // metres (cell centres stay cell centres) so the outlet's flange starts at z 0.
        double depth = dd / 4, zd = Math.floor(Math.min(lo - depth - .1, -.25) - hd / 2 - .5) + .5;
        double shift = -Math.floor(zd - hd / 2 - EquipmentShape.STAND_OUT);
        for (int k = 0; k < throwCount; k++) zs[k] += shift;
        lo += shift;
        hi += shift;
        zd += shift;
        double front = shift;
        end += shift;
        // The steel table: posts at the frame's corners and between the throws, beams along its sides and across at each
        // post line; the frame on it, its ribs between the throws, a cover over each throw, the breather, the oil pump's
        // cover and the pump at the free end, the lubricator on its side.
        double half = c.half(), top = h + c.frameTop();
        var lines = new ArrayList<Double>();
        lines.add(front + .1);
        for (int k = 1; k < throwCount; k += 2) lines.add((zs[k] + (k + 1 < throwCount ? zs[k + 1] : end - .1)) / 2);
        if (lines.getLast() < end - .4) lines.add(end - .1);
        table(a, half, front - .1, end + .1, h, lines, c.rail());
        box(new double[]{a - half, h, front}, new double[]{a + half, top, end}, Material.MACHINE, true);
        volumes.add(Solid.box(new double[]{a - half + .05, h + .05, front + .05}, new double[]{a + half - .05, top - .05, end - .05}));
        for (int k = 0; k < throwCount; k++) {
            box(new double[]{a - half + .08, top, zs[k] - .38 * c.pitch()}, new double[]{a + half - .08, top + .03, zs[k] + .38 * c.pitch()}, Material.MACHINE, false);
            mesh.bar(new double[]{a - .15, top + .05, zs[k]}, new double[]{a + .15, top + .05, zs[k]}, .03, new double[]{0, 1, 0}, Material.PIPE);
            // A rib on the side where this throw has no guide.
            double rx = a + (k % 2 == 0 ? 1 : -1) * half;
            box(new double[]{rx - .04, h + .05, zs[k] - .05}, new double[]{rx + .04, top - .05, zs[k] + .05}, Material.MACHINE, false);
        }
        mesh.cylinder(Frame.of(new double[]{a + .5 * half, 0, front + .3}, 1, 1), .06 + .06 * b, top, top + .15 + .1 * b, 10, Material.MACHINE);
        mesh.cylinder(Frame.of(new double[]{a, yc, 0}, 2, 1), .25 + .15 * b, front - .06, front, sides(.5 + .3 * b), Material.MACHINE);
        box(new double[]{a - .2, h + .15, front - .25}, new double[]{a + .2, h + .45, front}, Material.MACHINE, false);
        box(new double[]{a + half, yc - .1, front + .05}, new double[]{a + half + .3, yc + .3, front + .45}, Material.MACHINE, false);
        int n = sides(cyl), nb = sides(ds), nd = sides(dd), caps = b >= .45 ? 2 : 1;
        double neck = .25 * b + .04, cap = Math.min(.225, .1 + .2 * b), capHeight = .08 + .08 * b;
        for (int k = 0; k < throwCount; k++) {
            int sx = k % 2 == 0 ? -1 : 1;
            double z = zs[k], xb = a + sx * c.out(), g0 = a + sx * half, g1 = a + sx * (half + c.guideLength()), p1 = xb - sx * (lc / 2 + .06);
            // The crosshead guide and its door; the distance piece between its flanges, a window cover each side of each compartment.
            box(new double[]{Math.min(g0, g1), yc - gh, z - gh}, new double[]{Math.max(g0, g1), yc + gh, z + gh}, Material.MACHINE, true);
            double d0 = g0 + sx * .15, d1 = g1 - sx * .15;
            box(new double[]{Math.min(d0, d1), yc + gh, z - .7 * gh}, new double[]{Math.max(d0, d1), yc + gh + .025, z + .7 * gh}, Material.MACHINE, false);
            mesh.bar(new double[]{(d0 + d1) / 2 - .1, yc + gh + .06, z}, new double[]{(d0 + d1) / 2 + .1, yc + gh + .06, z}, .03, new double[]{0, 1, 0}, Material.PIPE);
            plate(g1 - sx * .05, g1, yc, z, piece + .06);
            box(new double[]{Math.min(g1, p1), yc - piece, z - piece}, new double[]{Math.max(g1, p1), yc + piece, z + piece}, Material.MACHINE, true);
            int compartments = c.pieceLength() > 1.1 ? 2 : 1;
            double length = Math.abs(p1 - g1) / compartments;
            for (int j = 0; j < compartments; j++) {
                double mid = g1 + sx * length * (j + .5);
                if (j > 0) plate(g1 + sx * (length * j - .025), g1 + sx * (length * j + .025), yc, z, piece + .04);
                for (int face = -1; face <= 1; face += 2) {
                    double zf = z + face * piece, zo = zf + face * .02;
                    box(new double[]{mid - .3 * length, yc - .55 * piece, Math.min(zf, zo)}, new double[]{mid + .3 * length, yc + .55 * piece, Math.max(zf, zo)}, Material.OPENING, false);
                }
            }
            // The cylinder: crank-end flange, body, head flange with its studs, head cover; the clearance pocket and its wheel.
            var fx = Frame.of(new double[]{xb, yc, z}, 0, sx);
            mesh.cylinder(fx, cyl / 2 + .05, -lc / 2 - .06, -lc / 2 + .02, n, Material.FLANGE);
            mesh.prism(fx, cyl / 2, -lc / 2 + .02, lc / 2, n, Material.MACHINE);
            mesh.cylinder(fx, cyl / 2 + .03, lc / 2, lc / 2 + .07, n, Material.FLANGE);
            mesh.bolts(fx, Math.min(cyl, PipeSizes.MAX_OUTSIDE), .35 * cyl, cyl / 2 + .03, lc / 2 + .07, 1, Material.BOLT);
            mesh.cylinder(fx, .42 * cyl, lc / 2 + .07, lc / 2 + .12, n, Material.MACHINE);
            if (pocket > 0) {
                double wheel = .12 + .1 * b, at = lc / 2 + .12 + pocket;
                mesh.cylinder(fx, .25 * cyl, lc / 2 + .12, at, n, Material.MACHINE);
                mesh.prism(fx, .02, at, at + .1, 6, Material.PIPE);
                mesh.ring(fx, wheel - .025, wheel, at + .07, at + .1, 12, 3, Material.ACTUATOR);
                mesh.bar(fx.at(0, wheel - .02, at + .085), fx.at(Math.PI, wheel - .02, at + .085), .02, new double[]{0, 0, 1}, Material.ACTUATOR);
            }
            solids.add(Solid.rod(new double[]{0, yc, z}, 0, Math.min(xb - sx * (lc / 2 + .06), xb + sx * (lc / 2 + .12 + pocket)),
                    Math.max(xb - sx * (lc / 2 + .06), xb + sx * (lc / 2 + .12 + pocket)), cyl / 2 + .05, 0));
            round(new double[]{0, yc, z}, 0, xb - lc / 2, xb + lc / 2, cyl / 2);
            volumes.add(Solid.rod(new double[]{0, yc, z}, 0, xb - lc / 2 + .05, xb + lc / 2 - .05, cyl / 2 - .05, 0));
            // Valve caps at both ends, on top (suction) and underneath (discharge); their nuts are too small to show.
            for (double t : new double[]{-(lc / 2 - cap - .08), lc / 2 - cap - .08})
                for (int below = 0; below < 2; below++)
                    for (int j = 0; j < caps; j++) {
                        double phi = (below == 0 ? 0 : Math.PI) + (caps == 1 ? 0 : (j == 0 ? -1 : 1) * Math.toRadians(35));
                        double[] radial = {0, DeterministicMath.cos(phi), DeterministicMath.sin(phi)};
                        var cf = new Frame(new double[]{xb + sx * t, yc, z}, radial, new double[]{1, 0, 0});
                        mesh.cylinder(cf, cap, cyl / 2 - .04, cyl / 2 + capHeight, 12, Material.FLANGE);
                    }
            // The necks to the bottles; the outboard support under the head.
            var up = Frame.of(new double[]{xb, 0, z}, 1, 1);
            mesh.prism(up, neck, yc + cyl / 2 - .05, ysb - .45 * ds, 12, Material.PIPE);
            mesh.ring(up, neck, 1.6 * neck, yc + cyl / 2 + .05, yc + cyl / 2 + .09, 12, 3, Material.FLANGE);
            mesh.prism(up, neck, ydb + .45 * dd, yc - cyl / 2 + .05, 12, Material.PIPE);
            mesh.ring(up, neck, 1.6 * neck, yc - cyl / 2 - .09, yc - cyl / 2 - .05, 12, 3, Material.FLANGE);
            double xs = xb + sx * (lc / 2 + .04);
            box(new double[]{xs - .04, .03, z - .2 * cyl}, new double[]{xs + .04, yc - .4 * cyl, z + .2 * cyl}, Material.STEELWORK, true);
            box(new double[]{xs - .12, 0, z - .2 * cyl - .06}, new double[]{xs + .12, .03, z + .2 * cyl + .06}, Material.STEELWORK, false);
            // A beam under the guide from the table's rail out to a post at the guide's end, a saddle up to the guide.
            double rail = Math.min(c.rail(), h), gx = g1 - sx * .2;
            box(new double[]{Math.min(g0, gx) - .05, h - rail, z - FLANGE_BEAM}, new double[]{Math.max(g0, gx) + .05, h, z + FLANGE_BEAM}, Material.STEELWORK, true);
            if (h - rail > .05) box(new double[]{gx - POST / 2, .03, z - POST / 2}, new double[]{gx + POST / 2, h - rail, z + POST / 2}, Material.STEELWORK, true);
            box(new double[]{Math.min(g0, gx) + .1, h, z - .6 * gh}, new double[]{Math.max(g0, gx) - .1, yc - gh, z + .6 * gh}, Material.STEELWORK, true);
        }
        for (int side = 0; side < 2; side++) {
            int sx = side == 0 ? -1 : 1;
            double xb = a + sx * c.out();
            bottle(xb, ysb, lo, hi, ds, nb);
            bottle(xb, ydb, lo, hi, dd, nd);
            // Posts with saddles under the discharge bottle, a quarter in from each end.
            for (double zp : new double[]{lo + .25 * (hi - lo), hi - .25 * (hi - lo)}) {
                box(new double[]{xb - .1, 0, zp - .1}, new double[]{xb + .1, ydb - .45 * dd, zp + .1}, Material.STEELWORK, true);
                box(new double[]{xb - .4 * dd, ydb - .45 * dd - .04, zp - .13}, new double[]{xb + .4 * dd, ydb - .3 * dd, zp + .13}, Material.STEELWORK, false);
            }
        }
        // The suction header across the outboard (front) end on a cell centre, over the frame, its branches down into both
        // suction bottles, the inlet on top of its middle; the discharge header from both discharge bottles' front heads,
        // the outlet at the front of its middle.
        double xl = a - c.out(), xr = a + c.out(), zh = Math.ceil(lo + .1 + hs / 2 - .5) + .5, yh = Math.ceil(ysb + ds / 2 + .2 + hs / 2 - .5) + .5, into = ysb + .3 * ds;
        header(hs, new double[]{xl, into, zh}, new double[]{xl, yh, zh}, new double[]{xr, yh, zh}, new double[]{xr, into, zh});
        nozzles.add(radial(Role.SUCTION, a, yh, hs / 2, sides(hs), zh, 1, hs, ColumnStructure.size(hs, hs)));
        double head = lo - .7 * depth;
        header(hd, new double[]{xl, ydb, head}, new double[]{xl, ydb, zd}, new double[]{xr, ydb, zd}, new double[]{xr, ydb, head});
        double start = zd + .03, face = Math.floor(zd - hd / 2 - EquipmentShape.STAND_OUT);
        nozzles.add(new Nozzle(Role.DISCHARGE, new double[]{a, ydb, start}, 2, -1, start - face, hd, ColumnStructure.size(hd, hd), 0, hd / 2 + .03));
        // The guarded coupling and the synchronous motor.
        double mz0 = end + .35, g = .15 + .15 * b;
        mesh.cylinder(Frame.of(new double[]{a, yc, 0}, 2, 1), .1 + .1 * b, end, mz0, 12, Material.MACHINE);
        box(new double[]{a - g, Math.max(h, yc - g), end + .03}, new double[]{a + g, yc + 1.2 * g, mz0 - .03}, Material.GUARD, true);
        largeMotor(a, yc, mz0, c.motor(), c.motorLength(), 0, true);
    }

    /**
     * A process header of {@code od} through {@code points} (a bend at each), with a flange pair where it leaves the first
     * and enters the last; it collides as drawn.
     */
    private void header(double od, double[]... points) {
        double r = od / 2;
        line(r, Material.PIPE, points);
        for (int k = 0; k + 1 < points.length; k++) {
            double[] p = points[k], q = points[k + 1];
            int axis = p[0] != q[0] ? 0 : p[1] != q[1] ? 1 : 2;
            double[] origin = p.clone();
            origin[axis] = 0;
            solids.add(Solid.rod(origin, axis, Math.min(p[axis], q[axis]) - r, Math.max(p[axis], q[axis]) + r, r, 0));
        }
        for (int end = 0; end < 2; end++) {
            double[] p = end == 0 ? points[0] : points[points.length - 1], q = end == 0 ? points[1] : points[points.length - 2];
            var dir = unit(sub(q, p));
            var ref = Math.abs(dir[1]) < .9 ? new double[]{0, 1, 0} : new double[]{1, 0, 0};
            double at = Math.min(.35 + r, .45 * Math.sqrt(dot(sub(q, p), sub(q, p))));
            var f = new Frame(p, dir, unit(cross(dir, ref)));
            mesh.cylinder(f, r + .04 + .1 * r, at - .03, at + .03, sides(od), Material.FLANGE);
        }
    }

    /**
     * A compressor's steel table (owner request 2026-10-17; {@code MACHINES_SCALE_RESEARCH.md}): two rails {@code rail} deep
     * under the frame's sides (half width {@code half} about x {@code a}) from {@code z0} to {@code z1}, cross beams as deep
     * at each of the {@code lines} along z, posts under them on base plates, their tops at {@code top}. A slow-speed machine
     * of the largest frames stands on a concrete block in real plants; steel here keeps to the other units' look.
     */
    private void table(double a, double half, double z0, double z1, double top, List<Double> lines, double rail) {
        double beam = Math.min(rail, top), post = POST + .1, x0 = a - half + post / 2, x1 = a + half - post / 2;
        for (double x : new double[]{x0, x1}) {
            box(new double[]{x - FLANGE_BEAM, top - beam, z0}, new double[]{x + FLANGE_BEAM, top, z1}, Material.STEELWORK, true);
            for (double z : lines) {
                if (top - beam > .05) box(new double[]{x - post / 2, .03, z - post / 2}, new double[]{x + post / 2, top - beam, z + post / 2}, Material.STEELWORK, true);
                box(new double[]{x - post / 2 - .06, 0, z - post / 2 - .06}, new double[]{x + post / 2 + .06, .03, z + post / 2 + .06}, Material.STEELWORK, false);
            }
        }
        for (double z : lines)
            box(new double[]{x0 + FLANGE_BEAM, top - beam, z - FLANGE_BEAM / 2 - .025}, new double[]{x1 - FLANGE_BEAM, top, z + FLANGE_BEAM / 2 + .025}, Material.STEELWORK, true);
    }

    /**
     * A large motor from {@code z0} along z, its shaft at (x, y) ({@code MACHINES_HEATERS_SURVEY.md}): a fabricated
     * octagonal stator frame {@code across} wide and {@code length} long with banded ends, round end shields and the
     * drive-end bearing; louvres on its sides; feet on a steel base frame on posts down to {@code floor}; a top-hat
     * air-to-air cooler with its fan cowl; the terminal box on its side with its conduit down. A compressor's
     * {@code synchronous} motor adds the outboard pedestal bearing with its sight glass and the brushless exciter beyond
     * it, its rotor the flywheel (owner request 2026-10-17: a better motor than a box); a large pump's induction motor
     * ends at its fan cowl.
     */
    private void largeMotor(double x, double y, double z0, double across, double length, double floor, boolean synchronous) {
        double r = across / 2, z1 = z0 + length, corner = r * (Math.sqrt(2) - 1); // tan(pi/8): half a side of the octagon of apothem r
        var octagon = new ArrayList<double[]>();
        for (int k = 0; k < 8; k++) {
            double t = Math.PI / 4 * k, c = DeterministicMath.cos(t), s = DeterministicMath.sin(t), u = -s, v = c;
            octagon.add(new double[]{x + r * c - corner * u, y + r * s - corner * v});
        }
        extrude(octagon, z0 + .1, z1 - .1, Material.MOTOR);
        for (double zb : new double[]{z0 + .1, z1 - .22}) {
            var band = new ArrayList<double[]>();
            for (var p : octagon) band.add(new double[]{x + (p[0] - x) * (r + .04) / r, y + (p[1] - y) * (r + .04) / r});
            extrude(band, zb, zb + .12, Material.MOTOR);
        }
        var axis = Frame.of(new double[]{x, y, 0}, 2, 1);
        int n = sides(across);
        mesh.cylinder(axis, .82 * r, z0 + .02, z0 + .1, n, Material.MOTOR);
        mesh.cylinder(axis, .82 * r, z1 - .1, z1 - .02, n, Material.MOTOR);
        mesh.cylinder(axis, .22 * r + .06, z0 - .02, z0 + .02, 12, Material.MOTOR);
        solids.add(Solid.rod(new double[]{x, y, 0}, 2, z0, z1, 1.04 * r, 0));
        // Enclosed: the octagon's inscribed cylinder (a point of the octagon outside it lies within 0.09 r of a face, so a
        // cell whose centre is there holds part of that face).
        volumes.add(Solid.rod(new double[]{x, y, 0}, 2, z0 + .15, z1 - .15, r - .02, 0));
        for (int side = -1; side <= 1; side += 2) {
            double xf = x + side * r, xo = xf + side * .01;
            for (double zl : new double[]{z0 + .3 * length, z1 - .3 * length})
                box(new double[]{Math.min(xf, xo), y - .25 * r, zl - .12 * length}, new double[]{Math.max(xf, xo), y + .3 * r, zl + .12 * length}, Material.OPENING, false);
        }
        // Feet on the base frame; the base frame's beams along z under the feet, on posts when the motor stands high.
        double base = y - r, beam = Math.min(BEAM, base - floor), pedestal = z1 + .05, exciter = pedestal + .4, outer = Math.min(.9, .25 + .12 * across);
        double zEnd = synchronous ? exciter + .2 + .4 * outer : z1 + .05;
        double stand = Math.min(1, .3 + .15 * across) / 2, terminal = Math.min(1.2, .3 + .2 * across);
        for (int side = -1; side <= 1; side += 2) {
            double xf = x + side * .78 * r;
            box(new double[]{xf - .16 * r, base, z0 + .15}, new double[]{xf + .16 * r, y - .55 * r, z1 - .15}, Material.MOTOR, false);
            if (beam > .02) box(new double[]{xf - FLANGE_BEAM, base - beam, z0 - .05}, new double[]{xf + FLANGE_BEAM, base, zEnd + .05}, Material.STEELWORK, true);
            if (base - beam - floor > .05)
                for (double zp : new double[]{z0 + .1, (z0 + zEnd) / 2, zEnd - .1})
                    box(new double[]{xf - POST / 2, floor, zp - POST / 2}, new double[]{xf + POST / 2, base - beam, zp + POST / 2}, Material.STEELWORK, true);
        }
        // The top-hat cooler: its box with louvred sides and tube-plate ends, the fan cowl at its back end.
        double cool = .6 * r, c0 = z0 + .12 * length, c1 = z1 - .12 * length;
        box(new double[]{x - .75 * r, y + r - .02, c0}, new double[]{x + .75 * r, y + r + cool, c1}, Material.MOTOR, true);
        volumes.add(Solid.box(new double[]{x - .75 * r + .05, y + r, c0 + .05}, new double[]{x + .75 * r - .05, y + r + cool - .05, c1 - .05}));
        for (int side = -1; side <= 1; side += 2) {
            double xf = x + side * .75 * r, xo = xf + side * .01;
            box(new double[]{Math.min(xf, xo), y + r + .2 * cool, c0 + .1}, new double[]{Math.max(xf, xo), y + r + .8 * cool, c1 - .1}, Material.OPENING, false);
        }
        mesh.cylinder(Frame.of(new double[]{x, y + r + .55 * cool, 0}, 2, 1), .35 * cool + .05, c1, Math.min(z1 + .1, c1 + .3), 12, Material.MOTOR);
        // The terminal box on the right side, its conduit to the ground.
        double zt = (z0 + z1) / 2;
        box(new double[]{x + r, y - .35 * r, zt - terminal / 2}, new double[]{x + r + .4 * terminal, y - .35 * r + .7 * terminal, zt + terminal / 2}, Material.MOTOR, false);
        mesh.bar(new double[]{x + r + .2 * terminal, floor + .02, zt}, new double[]{x + r + .2 * terminal, y - .35 * r, zt}, .08, new double[]{1, 0, 0}, Material.PIPE);
        if (!synchronous) return;
        // The outboard pedestal bearing and its sight glass; the exciter.
        if (beam > .02) box(new double[]{x - .78 * r + FLANGE_BEAM, base - beam, pedestal}, new double[]{x + .78 * r - FLANGE_BEAM, base, exciter}, Material.STEELWORK, true);
        box(new double[]{x - stand, beam > .02 ? base : floor, pedestal}, new double[]{x + stand, y - .1, exciter}, Material.MOTOR, true);
        mesh.cylinder(axis, .12 + .06 * across, pedestal - .02, exciter + .02, 12, Material.MOTOR);
        box(new double[]{x + stand, y - .3, (pedestal + exciter) / 2 - .05}, new double[]{x + stand + .02, y - .2, (pedestal + exciter) / 2 + .05}, Material.OPENING, false);
        mesh.cylinder(axis, .05 + .02 * across, z1 - .02, pedestal, 8, Material.MACHINE);
        mesh.cylinder(axis, outer / 2, exciter + .02, zEnd - .05, sides(outer), Material.MOTOR);
        mesh.cylinder(axis, .4 * outer, zEnd - .05, zEnd, sides(.8 * outer), Material.MOTOR);
    }

    /** A square flange plate across a throw's line from {@code x0} to {@code x1}, {@code half} out from its axis at (y, z). */
    private void plate(double x0, double x1, double y, double z, double half) {
        box(new double[]{Math.min(x0, x1), y - half, z - half}, new double[]{Math.max(x0, x1), y + half, z + half}, Material.MACHINE, false);
    }

    /** A pulsation bottle lying along z at (x, y), its tangent lines at {@code lo} and {@code hi}, with 2:1 heads; it collides and encloses as drawn. */
    private void bottle(double x, double y, double lo, double hi, double od, int n) {
        double r = od / 2, depth = od / 4;
        mesh.prism(Frame.of(new double[]{x, y, 0}, 2, 1), r, lo, hi, n, Material.SHELL);
        mesh.head(Frame.of(new double[]{x, y, 0}, 2, 1), r, hi, depth, n, Material.HEAD);
        mesh.head(Frame.of(new double[]{x, y, 0}, 2, -1), r, -lo, depth, n, Material.HEAD);
        var axis = new double[]{x, y, 0};
        solids.add(Solid.rod(axis, 2, lo - depth, hi, r, depth));
        volumes.add(Solid.rod(axis, 2, lo, hi, .94 * r, .9 * depth));
        volumes.add(Solid.rod(axis, 2, lo - .6 * depth, lo, .65 * r, 0));
        round(x, y, lo, hi, r);
    }

    // ---- Fired heaters ----------------------------------------------------------------------------------------------

    /**
     * A vertical cylindrical heater (owner decision M3 2026-10-17; API 560; {@code MACHINES_HEATERS_SURVEY.md}): a round
     * radiant section of diameter D and height {@code tangentLength} on 6 to 14 H-section legs about 2.3 m apart on concrete
     * pedestals, a ring girder under its floor from leg to leg, X-braced in alternate bays and knee-braced in the others
     * (the front's left open), its floor {@code support} up with the burners (muffler drums under their registers) firing
     * up through it from a circle of 0.4 D; casing columns over the legs, girth bands every 3 m; on its roof the convection
     * section with its panelled header boxes and soot blowers, the breeching and the stack with its damper. Platforms (API 560 12.4.1): the
     * hearth platform round the casing at the floor, a 270° one for each further row of peep doors when the radiant
     * section is over 8 m, peep doors 1.4 m over each and an access door over the hearth at the back; a handrail round the
     * roof's edge; caged ladders at the side legs ({@link Access}), alternating, the last walked through onto the roof.
     * Five nozzles at the front (owner request 2026-10-17): the convection coil in near the top of its front header box and
     * out near its bottom (it flows down against the flue gas), the radiant coil in just under the roof and out just over
     * the floor (the crossover between them is the player's pipe), fuel gas to the burners' ring header.
     */
    private void verticalHeater() {
        double dia = shape.diameter(), r = dia / 2, height = shape.tangentLength(), y0 = shape.support() - .3, y1 = shape.support() + height;
        int floor = shape.support();
        double hx = Math.min(2.25, Math.max(.75, .225 * dia)), hz = Math.min(.425 * dia, Math.sqrt(r * r - hx * hx) - .1), process = Math.min(.6, hx);
        // The convection section high enough for its outlet's row and its inlet's two rows over it (one free block between
        // their pipes, owner rule 2026-10-17).
        double yConvection = Math.ceil(y1 + .2 + process / 2 - .5) + .5, hc = Math.max(Math.min(6, Math.max(2, .4 * dia + 1)), yConvection + 2.2 - y1);
        double wall = r + .12, width = ColumnStructure.MIN_DECK, edge = wall + width;
        double a = centre(Math.max(edge + .05, hz + HEADER_BOX + .2));
        int n = 4 * (int) Math.ceil(.75 * Math.PI * dia), legs = Math.max(6, 2 * (int) Math.round(Math.PI * dia / 4.6));
        double step = 2 * Math.PI / legs;
        var up = Frame.of(new double[]{a, 0, a}, 1, 1);
        mesh.prism(up, r, y0, y1, n, Material.HEATER);
        mesh.annulus(up, 0, r, y0, n, -1, Material.HEATER);
        mesh.annulus(up, 0, r, y1, n, 1, Material.HEATER);
        solids.add(Solid.rod(new double[]{a, 0, a}, 1, y0, y1, r, 0));
        volumes.add(Solid.rod(new double[]{a, 0, a}, 1, y0 + .1, y1 - .1, .94 * r, 0));
        round(a, a, y0, y1, r, 1);
        for (double y = floor + 3; y < y1 - 1; y += 3) band(up, r, y, n);
        // Legs under the casing columns, half a pitch off the front, their webs radial; a ring girder under the floor from
        // leg to leg; X-bracing in alternate bays from the front's right, knee braces in the others but the front's.
        double cased = casing(legDepth(dia)), girder = Math.min(.6, .3 + .025 * dia), low = y0 - girder;
        var legAt = new double[legs][];
        for (int k = 0; k < legs; k++) {
            double t = -Math.PI / 2 + (k + .5) * step;
            double[] radial = {DeterministicMath.cos(t), 0, DeterministicMath.sin(t)}, tangent = {-radial[2], 0, radial[0]};
            legAt[k] = new double[]{a + (r - .12) * radial[0], a + (r - .12) * radial[2]};
            leg(legAt[k][0], legAt[k][1], y0, cased, radial);
            mesh.block(new double[]{a + (r + .06) * radial[0], (y0 + y1) / 2, a + (r + .06) * radial[2]}, scale(radial, .06), new double[]{0, (y1 - y0) / 2, 0}, scale(tangent, .1),
                    Material.HEATER);
        }
        for (int k = 0; k < legs; k++) girder(legAt[k], legAt[(k + 1) % legs], low, y0, cased);
        solids.add(Solid.ring(a, a, low, y0, r - .12 - GIRDER_HALF, r - .12 + GIRDER_HALF, 0, 2 * Math.PI));
        for (int k = 0; k + 1 < legs; k++)
            if (k % 2 == 1 && k + 1 < legs - 1) brace(legAt[k], legAt[k + 1], .6, low);
            else knees(legAt[k], legAt[k + 1], low, cased);
        // The platforms and their ladders, alternating between the side legs.
        int extra = height > 8 ? (int) Math.ceil(height / 8) - 1 : 0;
        var levels = new ArrayList<Integer>();
        levels.add(floor);
        for (int k = 1; k <= extra; k++) levels.add(floor + (int) Math.round(height * k / (extra + 1)));
        int side = (int) Math.ceil(legs / 4.0 - .5);
        double right = -Math.PI / 2 + (side + .5) * step, left = Math.PI - right, gap = DeterministicMath.asin(Math.min(.95, .6 / (wall + Access.GAP)));
        double reach = wall + Access.STANDOFF - .05, cage = Access.CAGE_HALF + .2, clear = DeterministicMath.acos(reach / Math.sqrt(reach * reach + cage * cage));
        var arcs = new ArrayList<double[]>();
        for (int k = 0; k < levels.size(); k++) {
            int level = levels.get(k);
            double in = k % 2 == 0 ? right : left;
            verticalLadder(a, wall, in, k == 0 ? 0 : levels.get(k - 1) + Access.DECK, level + Access.DECK + Access.LANDING, false);
            arcs.clear();
            if (in == right) {
                arcs.add(new double[]{right + clear, 1.5 * Math.PI - gap, 0, 1});
                if (k == 0) arcs.add(new double[]{-Math.PI / 2 + gap, right - clear, 1, 0});
            } else {
                arcs.add(new double[]{-Math.PI / 2 + gap, left - clear, 1, 0});
                if (k == 0) arcs.add(new double[]{left + clear, 1.5 * Math.PI - gap, 0, 1});
            }
            for (var arc : arcs) platform(new Access.Arc(a, a, wall, arc[0], arc[1], edge, Access.Arc.step(edge)), level, width, arc[2] > 0, arc[3] > 0);
            // Peep doors between the columns over the decks, 1.4 m up; the access door over the hearth at the back.
            for (int j = 0; j + 1 < legs; j++) {
                double t = -Math.PI / 2 + (j + 1) * step;
                boolean decked = false;
                for (var arc : arcs) decked |= t > arc[0] + .05 && t < arc[1] - .05;
                if (decked) door(a, a, r, t, level + Access.DECK + 1.4, .125, .15);
            }
            if (k == 0) door(a, a, r, Math.PI / 2, level + Access.DECK + .4, .3, .3);
        }
        double roofIn = levels.size() % 2 == 0 ? right : left;
        verticalLadder(a, wall, roofIn, levels.getLast() + Access.DECK, y1 + Access.LANDING, true);
        // The roof's handrail on its edge, open at the ladder, the front's nozzle and header boxes that overhang it.
        double rail = r - .03, front = DeterministicMath.asin(Math.min(.95, (Math.max(.6, hx) + .1) / rail)), ladderGap = DeterministicMath.asin(Math.min(.95, (Access.LADDER_HALF + .2) / rail));
        var gaps = new ArrayList<double[]>();
        gaps.add(new double[]{roofIn - ladderGap, roofIn + ladderGap});
        if (hz + HEADER_BOX > Math.sqrt(Math.max(0, rail * rail - hx * hx)) - .1) {
            double back = DeterministicMath.asin(Math.min(.95, (hx + .05) / rail));
            gaps.add(new double[]{Math.PI / 2 - back, Math.PI / 2 + back});
        }
        gaps.sort((p, q) -> Double.compare(p[0], q[0]));
        double from = -Math.PI / 2 + front;
        for (var g : gaps) {
            if (g[0] > from + .05) railing(new Access.Arc(a, a, r - 1, from, g[0], r, Access.Arc.step(r)), y1 - Access.DECK, 1);
            from = Math.max(from, g[1]);
        }
        if (1.5 * Math.PI - front > from + .05) railing(new Access.Arc(a, a, r - 1, from, 1.5 * Math.PI - front, r, Access.Arc.step(r)), y1 - Access.DECK, 1);
        // Burners on a circle under the floor, their ring header and its risers.
        double duty = heaterDuty(Kind.VERTICAL_HEATER, dia, height), circle = .2 * dia, register = .25 + .02 * dia;
        int burners = Math.max(3, Math.min(16, Math.min((int) Math.round(duty / 2.5), (int) Math.floor(2 * Math.PI * circle / (2.6 * register + .1)))));
        var at = new double[burners][];
        for (int i = 0; i < burners; i++) {
            double t = 2 * Math.PI * (i + .5) / burners;
            at[i] = new double[]{a + circle * DeterministicMath.sin(t), a + circle * DeterministicMath.cos(t)};
        }
        double yFuel = Math.floor(y0 - 1.2 - .5) + .5, pipe = .04 + .004 * dia;
        burners(at, register, y0, yFuel, pipe);
        for (int i = 0; i < burners; i++) {
            double[] p = {at[i][0], yFuel, at[i][1]}, q = {at[(i + 1) % burners][0], yFuel, at[(i + 1) % burners][1]};
            mesh.cylinder(new Frame(p, unit(sub(q, p)), new double[]{0, 1, 0}), pipe, 0, Math.sqrt(dot(sub(q, p), sub(q, p))), 8, Material.PIPE);
        }
        // The convection section with its header boxes, the stack.
        double y2 = y1 + hc;
        convection(a - hx, a + hx, a - hz, a + hz, y1, y2, y1 + 1);
        stack(a, a, a - hx, a + hx, a - hz, a + hz, y2, .25 * dia + .3, y1);
        // The convection coil in near the top of its front header box and out near its bottom; the radiant coil in at the
        // front under the roof, two rows under the convection outlet, and out just over the floor; fuel gas to the ring
        // header's front from the front. Every pipe block keeps one free block from the next.
        double feed = ColumnStructure.size(.1 + .02 * dia, process), zIn = a - hz - HEADER_BOX + .03;
        nozzles.add(front(Role.CONVECTION_INLET, a, Math.floor(y2 - .7) + .5, zIn, process, feed));
        nozzles.add(front(Role.CONVECTION_OUTLET, a, yConvection, zIn, process, feed));
        nozzles.add(radialSide(Role.RADIANT_INLET, a, Math.min(Math.floor(y1 - .7 - process / 2) + .5, yConvection - 2), a, r, n, process, feed));
        nozzles.add(radialSide(Role.RADIANT_OUTLET, a, floor + .5, a, r, n, process, feed));
        // The ring's front: a burner's riser where there is an odd number, else the chord between two.
        double zFuel = a - circle * (burners % 2 == 1 ? 1 : DeterministicMath.cos(Math.PI / burners)) + .03, fuel = .3;
        nozzles.add(new Nozzle(Role.FUEL, new double[]{a, yFuel, zFuel}, 2, -1, zFuel - Math.floor(a - r - EquipmentShape.STAND_OUT), fuel,
                ColumnStructure.size(.05 + .003 * duty, fuel), 0, .03 + pipe));
    }

    /** A vertical heater's caged ladder at angle {@code t} round its casing (the casing columns' face {@code wall} from its axis), bracketed to the leg and column behind it. */
    private void verticalLadder(double a, double wall, double t, double base, double top, boolean walkThrough) {
        double rung = wall + Access.STANDOFF;
        ladder(a + rung * DeterministicMath.cos(t), a + rung * DeterministicMath.sin(t), new double[]{DeterministicMath.cos(t), 0, DeterministicMath.sin(t)}, Access.STANDOFF + .12,
                base, top, walkThrough);
    }

    /**
     * A cabin heater (owner decision M3 2026-10-17; API 560; {@code MACHINES_HEATERS_SURVEY.md}): a box radiant section
     * of width W and length {@code tangentLength} along z on H-section legs every 3 m or so on concrete pedestals, a girder
     * under the floor round its edge, X-braced along both sides in the end bays and every third and knee-braced in the
     * others (the burner aisles open), its walls W + 1 high, a hipped roof rising to the convection section
     * along its whole length, its floor {@code support} up with one or two rows of burners (muffler drums under their
     * registers) firing up through it. Casing columns over the legs with an eave beam, peep doors 1.4 m over each walkway
     * in every bay and an access door over the hearth walkway; panelled header boxes on both end walls for the side walls'
     * tube bends and for the convection section's, soot blowers along its right side, an insulated crossover between the
     * back radiant header boxes (the coil runs back along the right wall and returns along the left); one stack per 15 m
     * with its damper. Walkways on both sides (API 560
     * 12.4.1, {@link Access}): the hearth walkway the full length, a second one under the eaves when the walls are 6 m or
     * more, the convection walkway over the roof's slope; a ladder walked through from grade onto the hearth walkway (the
     * left's at the back, the right's at the front), side-step ladders against the wall between the side walkways and one
     * walked through onto the convection walkway. Five nozzles at the front (owner request 2026-10-17): the convection
     * coil in near the top of its front header box and out near its bottom, the radiant coil into the left front radiant
     * header box (+x, seen from the constructor) near its top and out of the right one just over the floor (the crossover between the coils is the
     * player's pipe), fuel gas to the burner rows' headers.
     */
    private void cabinHeater() {
        double w = shape.diameter(), total = shape.tangentLength(), y0 = shape.support() - .3, y1 = shape.support() + cabinWall(w);
        int floor = shape.support(), cells = cabinCells(total);
        double length = total / cells, wc = Math.min(4, Math.max(1.5, .4 * w)), yr = y1 + .6 * (w - wc) / 2, walk = 1.2, process = Math.min(.6, wc / 2);
        // The convection section high enough for its outlet's row and its inlet's two rows over it (one free block between
        // their pipes, owner rule 2026-10-17).
        double yConvection = Math.ceil(yr + .2 + process / 2 - .5) + .5, hc = Math.max(Math.min(5, Math.max(2, .25 * w + 1)), yConvection + 2.2 - yr);
        // One cell in the middle, or two either side of an aisle; the convection section over the middle, over a twin's
        // aisle and half its throat into each cell. A wall outside the convection section is an eave, its roof slope rising
        // to the section's side; a twin's inner wall stands under the section, up to it.
        double gap = cells == 2 ? AISLE : 0, half = (cells * w + gap) / 2;
        double a = centre(half + .12 + walk + .05 + 2 * Access.CAGE_HALF + .1), zs = 1, ze = zs + length, c0 = a - (wc + gap) / 2, c1 = a + (wc + gap) / 2;
        double[] cellX = cells == 1 ? new double[]{a} : new double[]{a - (gap + w) / 2, a + (gap + w) / 2};
        int bays = Math.max(2, (int) Math.round(length / 3)), out = (int) Math.floor(w / 2 - .5);
        double cased = casing(legDepth(w)), low = y0 - legDepth(w), rise = yr - y1;
        for (double ac : cellX) {
            double x0 = ac - w / 2, x1 = ac + w / 2;
            boolean eave0 = x0 < c0 - 1e-9, eave1 = x1 > c1 + 1e-9;
            double top0 = eave0 ? y1 : yr, top1 = eave1 ? y1 : yr, run0 = c0 - x0, run1 = x1 - c1;
            // The walls, the end walls up to the roof, the floor's underside and the roof's slopes.
            mesh.flat(List.of(new double[]{x0, y0, zs}, new double[]{x0, top0, zs}, new double[]{x0, top0, ze}, new double[]{x0, y0, ze}), new double[]{-1, 0, 0}, Material.HEATER);
            mesh.flat(List.of(new double[]{x1, y0, zs}, new double[]{x1, y0, ze}, new double[]{x1, top1, ze}, new double[]{x1, top1, zs}), new double[]{1, 0, 0}, Material.HEATER);
            mesh.flat(List.of(new double[]{x0, y0, zs}, new double[]{x0, y0, ze}, new double[]{x1, y0, ze}, new double[]{x1, y0, zs}), new double[]{0, -1, 0}, Material.HEATER);
            for (double z : new double[]{zs, ze}) {
                var outline = new ArrayList<double[]>(List.of(new double[]{x0, y0, z}, new double[]{x1, y0, z}, new double[]{x1, top1, z}));
                if (eave1) outline.add(new double[]{c1, yr, z});
                if (eave0) outline.add(new double[]{c0, yr, z});
                outline.add(new double[]{x0, top0, z});
                mesh.flat(outline, new double[]{0, 0, z == zs ? -1 : 1}, Material.HEATER);
            }
            if (eave0) mesh.flat(List.of(new double[]{x0, y1, zs}, new double[]{c0, yr, zs}, new double[]{c0, yr, ze}, new double[]{x0, y1, ze}), new double[]{-rise, run0, 0}, Material.HEATER);
            if (eave1) mesh.flat(List.of(new double[]{x1, y1, zs}, new double[]{c1, yr, zs}, new double[]{c1, yr, ze}, new double[]{x1, y1, ze}), new double[]{rise, run1, 0}, Material.HEATER);
            solids.add(Solid.box(new double[]{x0, y0, zs}, new double[]{x1, y1, ze}));
            volumes.add(Solid.box(new double[]{x0 + .1, y0 + .1, zs + .1}, new double[]{x1 - .1, y1, ze - .1}));
            // Under the roof in three steps, each inside the slopes at its top.
            for (int k = 0; k < 3; k++) {
                double b0 = y1 + k * rise / 3, b1 = b0 + rise / 3, lo = eave0 ? x0 + (k + 1) * run0 / 3 + .05 : x0, hi = eave1 ? x1 - (k + 1) * run1 / 3 - .05 : x1;
                solids.add(Solid.box(new double[]{lo, b0, zs}, new double[]{hi, b1, ze}));
                volumes.add(Solid.box(new double[]{lo, b0, zs + .1}, new double[]{hi, b1, ze - .1}));
            }
            for (double[] p : new double[][]{{x0, zs, x1, zs}, {x1, zs, x1, ze}, {x1, ze, x0, ze}, {x0, ze, x0, zs}}) segment(new double[]{p[0], y1, p[1]}, new double[]{p[2], y1, p[3]});
            // Fireproofed legs and casing columns every 3 m or so; a girder under the floor round its edge; X-bracing in the
            // end bays and every third, knee braces in the others and across both ends; an eave beam along each eave wall.
            for (int k = 0; k <= bays; k++) {
                double z = zs + length * k / bays;
                for (int side = -1; side <= 1; side += 2) {
                    double wall = ac + side * w / 2, leg = ac + side * (w / 2 - .15);
                    leg(leg, z, y0, cased, new double[]{1, 0, 0});
                    box(new double[]{Math.min(wall, wall + side * .12), y0, z - .1}, new double[]{Math.max(wall, wall + side * .12), side < 0 ? top0 : top1, z + .1}, Material.HEATER, false);
                    if (k == bays) continue;
                    double[] p = {leg, z}, q = {leg, zs + length * (k + 1) / bays};
                    if (k == 0 || k == bays - 1 || k % 3 == 1) brace(p, q, .6, low);
                    else knees(p, q, low, cased);
                }
            }
            for (int side = -1; side <= 1; side += 2) {
                double leg = ac + side * (w / 2 - .15);
                box(new double[]{leg - GIRDER_HALF, low, zs - cased / 2}, new double[]{leg + GIRDER_HALF, y0, ze + cased / 2}, Material.STEELWORK, true);
            }
            for (double z : new double[]{zs, ze}) {
                double left = ac - (w / 2 - .15), right = ac + (w / 2 - .15);
                box(new double[]{left + cased / 2, low, z - GIRDER_HALF}, new double[]{right - cased / 2, y0, z + GIRDER_HALF}, Material.STEELWORK, true);
                knees(new double[]{left, z}, new double[]{right, z}, low, cased);
            }
            for (int side = -1; side <= 1; side += 2) {
                if (!(side < 0 ? eave0 : eave1)) continue;
                double wall = ac + side * w / 2, outer = wall + side * .15;
                box(new double[]{Math.min(wall, outer), y1 - .25, zs - .1}, new double[]{Math.max(wall, outer), y1, ze + .1}, Material.HEATER, false);
            }
            // Header boxes on both end walls for the side walls' tube bends, panelled; the crossover between the back ones.
            for (int side = -1; side <= 1; side += 2) {
                double x = ac + side * out;
                for (int end = 0; end < 2; end++) {
                    double[] lo = {x - .4, floor, end == 0 ? zs - HEADER_BOX : ze}, hi = {x + .4, y1 - .4, end == 0 ? zs : ze + HEADER_BOX};
                    mesh.box(lo, hi, ALL_FACES & ~(end == 0 ? 32 : 16), Material.HEATER);
                    solids.add(Solid.box(lo, hi));
                    panels(lo, hi, 2, end == 0 ? -1 : 1, 1, Math.max(2, (int) Math.round((hi[1] - lo[1]) / 1.3)));
                }
            }
            double cross = .06 + .008 * w, yCross = Math.floor(y1 - 1.2) + .5, zCross = ze + HEADER_BOX + .35;
            line(cross, Material.CLADDING, new double[]{ac + out, yCross, ze + HEADER_BOX - .05}, new double[]{ac + out, yCross, zCross}, new double[]{ac - out, yCross, zCross},
                    new double[]{ac - out, yCross, ze + HEADER_BOX - .05});
        }
        if (cells == 2) mesh.flat(List.of(new double[]{a - gap / 2, yr, zs}, new double[]{a + gap / 2, yr, zs}, new double[]{a + gap / 2, yr, ze}, new double[]{a - gap / 2, yr, ze}),
                new double[]{0, -1, 0}, Material.HEATER);
        // Burners in one row of each cell (two from 7 m wide) under the floor, a header along each row and one across the front.
        double duty = heaterDuty(Kind.CABIN_HEATER, w, total), register = .25 + .015 * w, yFuel = Math.floor(y0 - 1.2 - .5) + .5, pipe = .05 + .004 * w * Math.sqrt(cells);
        var rows = new ArrayList<Double>();
        for (double ac : cellX) {
            if (w <= 6) rows.add(ac);
            else {
                rows.add(ac - w / 4);
                rows.add(ac + w / 4);
            }
        }
        int each = Math.max(2, Math.min((int) Math.round(duty / 4 / rows.size()), (int) Math.floor((length - 1) / (2.6 * register + .2))));
        var at = new double[rows.size() * each][];
        for (int row = 0; row < rows.size(); row++)
            for (int i = 0; i < each; i++) at[row * each + i] = new double[]{rows.get(row), zs + length * (i + .5) / each};
        burners(at, register, y0, yFuel, pipe);
        double zHeader = zs + .3;
        for (double x : rows) mesh.cylinder(Frame.of(new double[]{x, yFuel, 0}, 2, 1), pipe, zHeader, zs + length * (each - .5) / each, 8, Material.PIPE);
        if (rows.size() > 1) mesh.cylinder(Frame.of(new double[]{0, yFuel, zHeader}, 0, 1), pipe, rows.getFirst(), rows.getLast(), 8, Material.PIPE);
        // The convection section along the whole roof, a stack over each of its parts (a twin's flue gas through stacks
        // √2 as wide).
        double y2 = yr + hc, stack = Math.min(3.5, (.25 * w + .3) * Math.sqrt(cells));
        int convection = (int) Math.ceil(yr + .05);
        convection(c0, c1, zs, ze, yr, y2, convection + 1);
        int stacks = Math.max(1, (int) Math.round(length / 15));
        for (int j = 0; j < stacks; j++) {
            double zj = zs + length * (j + .5) / stacks, room = Math.min(length / stacks / 2 - .2, Math.max((c1 - c0) / 2, stack / 2 + .3));
            stack(a, zj, c0, c1, zj - room, zj + room, y2, stack, y1);
        }
        // The walkways and ladders of each eave wall; peep doors over each walkway in every bay but where a ladder stands,
        // an access door over the hearth walkway in the middle bay.
        boolean second = y1 - 2.4 - floor >= 3;
        int middle = second ? (int) (y1 - 2.4) : -1;
        for (int side = -1; side <= 1; side += 2) {
            double wall = side < 0 ? cellX[0] - w / 2 : cellX[cellX.length - 1] + w / 2, wallX = wall + side * .12, edge = side < 0 ? c0 : c1;
            double[] outward = {side, 0, 0}, along = {0, 0, 1};
            boolean back = side < 0;
            double zGrade = back ? ze - .6 : zs + .6, zMid = back ? zs + .5 : ze - .5, zConv = back ? ze - 1.6 : zs + 1.6, start = zs - .3;
            platform(new Access.Line(new double[]{wallX, 0, start}, along, outward, length + .6), floor, walk, true, true, zGrade - .3 - start, zGrade + .3 - start);
            ladder(wallX + side * (walk + .05), zGrade, outward, .05, 0, floor + Access.DECK + Access.LANDING, true);
            int top = floor;
            if (second) {
                double from = back ? zMid + .55 : start, to = back ? ze + .3 : zMid - .55;
                platform(new Access.Line(new double[]{wallX, 0, from}, along, outward, to - from), middle, walk, !back, back);
                ladder(wallX + side * Access.STANDOFF, zMid, outward, Access.STANDOFF, floor + Access.DECK, middle + Access.DECK + Access.LANDING, false);
                top = middle;
            }
            platform(new Access.Line(new double[]{edge, 0, zs}, along, outward, length), convection, Math.abs(wall - edge) + .12, true, true, zConv - .3 - zs, zConv + .3 - zs);
            ladder(wallX + side * .05, zConv, outward, .05, top + Access.DECK, convection + Access.DECK + Access.LANDING, true);
            for (int k = 0; k < bays; k++) {
                double z = zs + length * (k + .5) / bays;
                for (int level : second ? new int[]{floor, middle} : new int[]{floor}) {
                    boolean ladderThere = Math.abs(z - zConv) < .7 && level == top || Math.abs(z - zMid) < .7 && second;
                    if (!ladderThere) door(wall, z, side, level + Access.DECK + 1.4, .125, .15);
                }
                if (k == bays / 2 && Math.abs(z - zConv) > 1 && Math.abs(z - zMid) > 1) door(wall, z + .7, side, floor + Access.DECK + .4, .3, .3);
            }
        }
        // Nozzles at the front: the convection coil's at its header box; one cell's radiant coil in at the left header box
        // (+x, seen from the constructor) and out of the right one; a twin's coils fed from an inlet manifold between their inner header boxes and joined in
        // an outlet manifold between their outer ones, both across the front of the aisle.
        double feed = ColumnStructure.size(.1 + .02 * w * Math.sqrt(cells), process), zIn = zs - HEADER_BOX + .03, yIn = Math.floor(y1 - 1.2) + .5, yOut = floor + .5;
        nozzles.add(front(Role.CONVECTION_INLET, a, Math.floor(y2 - .7) + .5, zIn, process, feed));
        nozzles.add(front(Role.CONVECTION_OUTLET, a, yConvection, zIn, process, feed));
        if (cells == 1) {
            nozzles.add(front(Role.RADIANT_INLET, a + out, yIn, zIn, process, feed));
            nozzles.add(front(Role.RADIANT_OUTLET, a - out, yOut, zIn, process, feed));
        } else {
            double manifold = feed / 2 + .05, zm = zs - HEADER_BOX - manifold - .1, face = zs - HEADER_BOX;
            for (double[] ends : new double[][]{{cellX[0] + out, cellX[1] - out, yIn}, {cellX[0] - out, cellX[1] + out, yOut}})
                line(manifold, Material.PIPE, new double[]{ends[0], ends[2], face}, new double[]{ends[0], ends[2], zm}, new double[]{ends[1], ends[2], zm}, new double[]{ends[1], ends[2], face});
            nozzles.add(front(Role.RADIANT_INLET, a, yIn, zm, process, feed));
            nozzles.add(front(Role.RADIANT_OUTLET, a, yOut, zm, process, feed));
        }
        double zFuel = zHeader + .03, fuel = .3;
        nozzles.add(new Nozzle(Role.FUEL, new double[]{a, yFuel, zFuel}, 2, -1, zFuel - Math.floor(zs - EquipmentShape.STAND_OUT), fuel,
                ColumnStructure.size(.05 + .003 * duty, fuel), 0, .03 + pipe));
    }

    /** A twin cabin's aisle between its cells' inner walls, m: room for a player between the legs' pedestals. */
    static final double AISLE = 1.5;

    /** A cabin's longest radiant cell, m: horizontal radiant tubes reach about 24 m ({@code MACHINES_SCALE_RESEARCH.md}). */
    static final int MAX_CABIN_CELL = 24;

    /** A cabin's radiant cells: a longer cabin is a twin cell, two cells of half its length side by side (owner decision 2026-10-17). */
    public static int cabinCells(double length) {
        return length > MAX_CABIN_CELL ? 2 : 1;
    }

    /** A heater's header box depth beyond the casing it stands on, m (its tube bends and their covers). */
    static final double HEADER_BOX = .6;

    /** A girth band round a vertical heater's casing or its stack (radius {@code r}, {@code n} sides about frame {@code up}) at height {@code y}. */
    private void band(Frame up, double r, double y, int n) {
        mesh.prism(up, r + .05, y, y + .12, n, Material.HEATER);
        mesh.annulus(up, r, r + .05, y, n, -1, Material.HEATER);
        mesh.annulus(up, r, r + .05, y + .12, n, 1, Material.HEATER);
    }

    /** X-bracing between two legs at (x, z) from {@code low} to {@code high}: angles 0.15 m across. */
    private void brace(double[] p, double[] q, double low, double high) {
        mesh.bar(new double[]{p[0], low, p[1]}, new double[]{q[0], high, q[1]}, .15, new double[]{0, 1, 0}, Material.STEELWORK);
        mesh.bar(new double[]{p[0], high, p[1]}, new double[]{q[0], low, q[1]}, .15, new double[]{0, 1, 0}, Material.STEELWORK);
    }

    /** A girder's half width, m. */
    private static final double GIRDER_HALF = .125;

    /**
     * A heater leg's H-section (inside its concrete {@link #casing}), its depth and flange width, and the depth of the cabin's
     * girder under the floor, m: about W12 on
     * the smallest heater to W18 on the largest (owner request 2026-10-17, "make furnace support looks more rigid"; the
     * girder at most 0.45 m, over the burners' muffler drums).
     */
    static double legDepth(double size) {
        return Math.min(.45, .28 + .015 * size);
    }

    /**
     * A fireproofed leg's concrete casing across, m: the H-section with 50 mm of cover, 0.45 m at least (refinery practice
     * over the fire zone, {@code MACHINES_SCALE_RESEARCH.md}; owner decision 2026-10-17, "add concrete fireproofing up to
     * the floor": yes).
     */
    static double casing(double depth) {
        return Math.max(.45, depth + .1);
    }

    /**
     * A heater's leg at (x, z): an H-section in a square concrete casing {@code cased} across (its faces square to
     * {@code web}, horizontal) from its pedestal 0.3 m over grade up to the floor at {@code top}.
     */
    private void leg(double x, double z, double top, double cased, double[] web) {
        double[] flange = {-web[2], 0, web[0]};
        double half = cased / 2, pad = half + .1;
        mesh.block(new double[]{x, .15, z}, scale(web, pad), new double[]{0, .15, 0}, scale(flange, pad), Material.CONCRETE);
        mesh.block(new double[]{x, (.3 + top) / 2, z}, scale(web, half), new double[]{0, (top - .3) / 2, 0}, scale(flange, half), Material.CONCRETE);
        solids.add(Solid.box(new double[]{x - pad, 0, z - pad}, new double[]{x + pad, .3, z + pad}));
        solids.add(Solid.box(new double[]{x - half, .3, z - half}, new double[]{x + half, top, z + half}));
    }

    /** A girder from leg p to leg q, {x, z} each, from {@code low} to {@code high}, between the legs' casings ({@code depth} across). */
    private void girder(double[] p, double[] q, double low, double high, double depth) {
        double dx = q[0] - p[0], dz = q[1] - p[1], length = Math.sqrt(dx * dx + dz * dz);
        double[] along = {dx / length, 0, dz / length}, across = {-along[2], 0, along[0]};
        mesh.block(new double[]{(p[0] + q[0]) / 2, (low + high) / 2, (p[1] + q[1]) / 2}, scale(along, length / 2 - depth / 2), new double[]{0, (high - low) / 2, 0},
                scale(across, GIRDER_HALF), Material.STEELWORK);
    }

    /** Knee braces from the casings ({@code depth} across) of legs p and q ({x, z}) up to the girder's underside at {@code low}, a third of the bay or 0.8 m along it. */
    private void knees(double[] p, double[] q, double low, double depth) {
        double dx = q[0] - p[0], dz = q[1] - p[1], length = Math.sqrt(dx * dx + dz * dz), reach = Math.min(.8, length / 3) + depth / 2;
        double ux = dx / length, uz = dz / length;
        for (int end = 0; end < 2; end++) {
            double[] from = end == 0 ? p : q;
            double s = end == 0 ? 1 : -1, x = from[0] + s * ux * depth / 2, z = from[1] + s * uz * depth / 2;
            mesh.bar(new double[]{x, low - reach + depth / 2, z}, new double[]{from[0] + s * ux * reach, low, from[1] + s * uz * reach}, .15, new double[]{0, 1, 0}, Material.STEELWORK);
        }
    }

    /** A nozzle out of a box's front face (frame -z) at (x, y), its neck starting at z: its flange face on the next block face {@link EquipmentShape#STAND_OUT} beyond. */
    private static Nozzle front(Role role, double x, double y, double z, double maxOd, double defaultOd) {
        return new Nozzle(role, new double[]{x, y, z}, 2, -1, z - Math.floor(z - .03 - EquipmentShape.STAND_OUT), maxOd, defaultOd, 0, .03);
    }

    /** A door on a round casing (axis at (x, z), radius {@code r}) at angle {@code t}, centred at height {@code y}: {@code across} and {@code up} its half sizes. */
    private void door(double x, double z, double r, double t, double y, double across, double up) {
        double[] radial = {DeterministicMath.cos(t), 0, DeterministicMath.sin(t)}, tangent = {-radial[2], 0, radial[0]};
        mesh.block(new double[]{x + (r + .04) * radial[0], y, z + (r + .04) * radial[2]}, scale(radial, .04), new double[]{0, up, 0}, scale(tangent, across), Material.MACHINE);
    }

    /** A door on a flat side wall at x {@code wall} facing {@code side} (±1 along x), centred at z and height {@code y}. */
    private void door(double wall, double z, int side, double y, double across, double up) {
        double outer = wall + side * .08;
        box(new double[]{Math.min(wall, outer), y - up, z - across}, new double[]{Math.max(wall, outer), y + up, z + across}, Material.MACHINE, false);
    }

    /**
     * A heater's burners under its floor ({@code y0}) at {@code at}, {x, z} each: the air register drum ({@code register}
     * across, its louvre band and lever) under the floor, the muffler drum (1.3 times as wide, open underneath) under it,
     * and the gas riser from the fuel header at {@code yFuel} up through the muffler.
     */
    private void burners(double[][] at, double register, double y0, double yFuel, double pipe) {
        double muffler = 1.3 * register, low = y0 - 1.05;
        for (var p : at) {
            var up = Frame.of(new double[]{p[0], 0, p[1]}, 1, 1);
            mesh.cylinder(up, register, y0 - .45, y0, 12, Material.MACHINE);
            mesh.ring(up, register, register + .03, y0 - .32, y0 - .2, 12, 3, Material.FLANGE);
            mesh.bar(up.at(0, register + .02, y0 - .26), up.at(0, register + .2, y0 - .16), .03, new double[]{0, 0, 1}, Material.ACTUATOR);
            mesh.prism(up, muffler, low, y0 - .45, 12, Material.MACHINE);
            mesh.annulus(up, register, muffler, y0 - .45, 12, 1, Material.MACHINE);
            mesh.annulus(up, .55 * muffler, muffler, low, 12, -1, Material.MACHINE);
            mesh.annulus(up, 0, .55 * muffler, low + .05, 12, -1, Material.OPENING);
            mesh.prism(up, .7 * pipe, yFuel, low + .05, 8, Material.PIPE);
            solids.add(Solid.rod(new double[]{p[0], 0, p[1]}, 1, low, y0, muffler, 0));
        }
    }

    /**
     * A heater's convection section from x0 to x1 by z0 to z1, from {@code bottom} (the radiant roof) to {@code top}, its
     * top drawn (the breeching opens from it), a panelled header box on each end, soot blowers on its right side every metre
     * from {@code blowers} up and about every 4 m along; it encloses its inside.
     */
    private void convection(double x0, double x1, double z0, double z1, double bottom, double top, double blowers) {
        mesh.box(new double[]{x0, bottom, z0}, new double[]{x1, top, z1}, ALL_FACES & ~4, Material.HEATER);
        solids.add(Solid.box(new double[]{x0, bottom, z0}, new double[]{x1, top, z1}));
        volumes.add(Solid.box(new double[]{x0 + .05, bottom, z0 + .05}, new double[]{x1 - .05, top - .05, z1 - .05}));
        int count = Math.max(2, (int) Math.round((x1 - x0 - .2) / .9));
        for (int end = 0; end < 2; end++) {
            double[] lo = {x0 + .1, bottom + .2, end == 0 ? z0 - HEADER_BOX : z1}, hi = {x1 - .1, top - .2, end == 0 ? z0 : z1 + HEADER_BOX};
            mesh.box(lo, hi, ALL_FACES & ~(end == 0 ? 32 : 16), Material.HEATER);
            solids.add(Solid.box(lo, hi));
            panels(lo, hi, 2, end == 0 ? -1 : 1, 0, count);
        }
        int along = Math.max(1, (int) Math.round((z1 - z0) / 4));
        for (double y = blowers; y < top - .5; y += 1)
            for (int j = 0; j < along; j++) {
                double z = z0 + (z1 - z0) * (j + .5) / along;
                mesh.cylinder(Frame.of(new double[]{0, y, z}, 0, 1), .06, x1, x1 + .25, 8, Material.PIPE);
                box(new double[]{x1 + .25, y - .13, z - .15}, new double[]{x1 + .5, y + .13, z + .15}, Material.ACTUATOR, false);
                mesh.cylinder(Frame.of(new double[]{x1 + .375, 0, z}, 1, 1), .08, y + .13, y + .33, 8, Material.MOTOR);
            }
        for (double[] p : new double[][]{{x0, z0, x1, z0}, {x1, z0, x1, z1}, {x1, z1, x0, z1}, {x0, z1, x0, z0}}) segment(new double[]{p[0], top, p[1]}, new double[]{p[2], top, p[3]});
    }

    /**
     * Bolted panels on the face of the box (lo, hi) across frame axis {@code axis} on its {@code sign} side: {@code count}
     * of them side by side along frame axis {@code split} (the other in-face axis is their height), 0.02 m proud, 0.04 m
     * apart and 0.06 m in from the box's edges, each with a lifting handle.
     */
    private void panels(double[] lo, double[] hi, int axis, int sign, int split, int count) {
        int other = 3 - axis - split;
        double face = sign > 0 ? hi[axis] : lo[axis], from = lo[split] + .06, to = hi[split] - .06, size = (to - from - .04 * (count - 1)) / count;
        for (int k = 0; k < count; k++) {
            double[] p = new double[3], q = new double[3];
            p[axis] = Math.min(face, face + sign * .02);
            q[axis] = Math.max(face, face + sign * .02);
            p[split] = from + k * (size + .04);
            q[split] = p[split] + size;
            p[other] = lo[other] + .06;
            q[other] = hi[other] - .06;
            box(p, q, Material.HEATER, false);
            double[] h0 = new double[3], h1 = new double[3];
            h0[axis] = h1[axis] = face + sign * .06;
            h0[split] = h1[split] = (p[split] + q[split]) / 2;
            h0[other] = (p[other] + q[other]) / 2 - .1;
            h1[other] = h0[other] + .2;
            mesh.bar(h0, h1, .03, axis == 1 ? new double[]{1, 0, 0} : new double[]{0, 1, 0}, Material.PIPE);
        }
    }

    /**
     * A heater's breeching from the convection section's top (x0 to x1 by z0 to z1 at {@code base}) up to a stack of
     * {@code od} round (x, z), and the stack: its top at twice the radiant roof's height {@code roof} over grade, at least
     * 6 m and at most 30 m over the breeching; the damper's band half a metre up with its actuator and lever, a flange band
     * every 8 m and one under its top, open at the top, a lightning rod on its rim.
     */
    private void stack(double x, double z, double x0, double x1, double z0, double z1, double base, double od, double roof) {
        double r = od / 2, neck = base + Math.max(.8, .8 * (Math.max(x1 - x0, z1 - z0) / 2 - r)), top = Math.min(neck + 30, Math.max(neck + 6, 2 * roof));
        int n = Math.max(12, 4 * (int) Math.ceil(.75 * Math.PI * od));
        transition(x0, x1, z0, z1, base, x, z, r, neck, n, Material.HEATER, false);
        var up = Frame.of(new double[]{x, 0, z}, 1, 1);
        mesh.prism(up, r, neck, top, n, Material.SHELL);
        band(up, r, neck + .5, n);
        for (double y = neck + 8; y < top - 2; y += 8) band(up, r, y, n);
        band(up, r, top - .6, n);
        box(new double[]{x + r + .05, neck + .38, z - .15}, new double[]{x + r + .35, neck + .68, z + .15}, Material.ACTUATOR, false);
        mesh.bar(new double[]{x + r + .2, neck + .68, z}, new double[]{x + r + .45, neck + 1.05, z}, .04, new double[]{0, 0, 1}, Material.ACTUATOR);
        mesh.annulus(up, .92 * r, r, top, n, 1, Material.SHELL);
        mesh.bore(up, .92 * r, top - .3, top, n, Material.SHELL);
        mesh.annulus(up, 0, .92 * r, top - .3, n, 1, Material.OPENING);
        mesh.bar(new double[]{x + .96 * r, top, z}, new double[]{x + .96 * r, top + 1.2, z}, .03, new double[]{1, 0, 0}, Material.PIPE);
        solids.add(Solid.box(new double[]{x0, base, z0}, new double[]{x1, neck, z1}));
        solids.add(Solid.rod(new double[]{x, 0, z}, 1, neck, top, r, 0));
        round(x, z, neck, top, r, 1);
    }

    // ---- Parts ------------------------------------------------------------------------------------------------------

    /** The middle of a box whose width is the least odd whole number of cells holding {@code half} each side: the centre line runs through cell centres. */
    private static double centre(double half) {
        int width = (int) Math.ceil(2 * half - 1e-9);
        if (width % 2 == 0) width++;
        return width / 2.0;
    }

    /**
     * A nozzle on top ({@code sign} +1) or under (-1) a body lying along z (its axis at (x, y), radius {@code r} to its
     * {@code n} vertices) at station {@code z}: its neck starts under the surface at its largest size's edge, its flange
     * face on the next block face {@link EquipmentShape#STAND_OUT} beyond.
     */
    private static Nozzle radial(Role role, double x, double y, double r, int n, double z, int sign, double maxOd, double defaultOd) {
        double apothem = r * DeterministicMath.cos(Math.PI / n), start = Math.sqrt(Math.max(0, apothem * apothem - maxOd * maxOd / 4)) - .03;
        double surface = y + sign * r, plane = sign > 0 ? Math.ceil(surface + EquipmentShape.STAND_OUT) : Math.floor(surface - EquipmentShape.STAND_OUT);
        return new Nozzle(role, new double[]{x, y + sign * start, z}, 1, sign, Math.abs(plane - (y + sign * start)), maxOd, defaultOd, 0, r - start);
    }

    /** A nozzle on the front (frame -z) of a standing body (its axis at (x, z), radius {@code r}) at height {@code y}. */
    private static Nozzle radialSide(Role role, double x, double y, double z, double r, int n, double maxOd, double defaultOd) {
        double apothem = r * DeterministicMath.cos(Math.PI / n), start = Math.sqrt(Math.max(0, apothem * apothem - maxOd * maxOd / 4)) - .03;
        double plane = Math.floor(z - r - EquipmentShape.STAND_OUT);
        return new Nozzle(role, new double[]{x, y, z - start}, 2, -1, z - start - plane, maxOd, defaultOd, 0, r - start);
    }

    /**
     * Where a saddle {@code w} wide stands nearest {@code want} on a shell from {@code lo} to {@code hi} (along z) without
     * standing in the pipe block under a downward nozzle at any of {@code under}: half a block each side of the nozzle's
     * centre line and 0.05 m. {@code want} when no such place is on the shell.
     */
    private static double saddleAt(double want, double w, double lo, double hi, double... under) {
        double best = want, gap = Double.MAX_VALUE, keep = .55 + w / 2;
        var candidates = new ArrayList<Double>(List.of(want));
        for (double z : under) {
            candidates.add(z - keep);
            candidates.add(z + keep);
        }
        for (double c : candidates) {
            if (c < lo + w / 2 - 1e-9 || c > hi - w / 2 + 1e-9 || Math.abs(c - want) >= gap) continue;
            boolean clear = true;
            for (double z : under) if (Math.abs(c - z) < keep - 1e-9) clear = false;
            if (clear) {
                best = c;
                gap = Math.abs(c - want);
            }
        }
        return best;
    }

    private static double saddleWidth(double r) {
        return Math.max(.15, Math.min(.3, .12 * r + .1));
    }

    /** A saddle at station {@code z} under a body lying along z at (x, y) of radius {@code r}: a plate from the ground and its base plate. */
    private void saddle(double z, double x, double y, double r, double w) {
        double across = .8 * r;
        box(new double[]{x - across, 0, z - w / 2}, new double[]{x + across, y - .6 * r, z + w / 2}, Material.SUPPORT, true);
        box(new double[]{x - across - .05, 0, z - w / 2 - .05}, new double[]{x + across + .05, .04, z + w / 2 + .05}, Material.SUPPORT, false);
    }

    /** A round body lying along z at (x, y) from {@code lo} to {@code hi}: its rims are fixed edges, its sides outlined against the eye. */
    private void round(double x, double y, double lo, double hi, double r) {
        round(x, y, lo, hi, r, 2);
    }

    /** A round body along frame axis {@code axis} through the point (x, y, z) less that axis, from {@code lo} to {@code hi}. */
    private void round(double x, double y, double lo, double hi, double r, int axis) {
        round(axis == 2 ? new double[]{x, y, 0} : new double[]{x, 0, y}, axis, lo, hi, r);
    }

    /** A round body along frame axis {@code axis} through {@code centre} (its {@code axis} coordinate unused), from {@code lo} to {@code hi}. */
    private void round(double[] centre, int axis, double lo, double hi, double r) {
        rounds.add(new double[]{centre[0], centre[1], centre[2], axis, r, lo, hi});
        int u = (axis + 1) % 3, v = (axis + 2) % 3, steps = 24;
        for (double at : new double[]{lo, hi})
            for (int k = 0; k < steps; k++) {
                double a0 = 2 * Math.PI * k / steps, a1 = 2 * Math.PI * (k + 1) / steps;
                double[] p = centre.clone(), q = centre.clone();
                p[axis] = at;
                q[axis] = at;
                p[u] += r * DeterministicMath.cos(a0);
                p[v] += r * DeterministicMath.sin(a0);
                q[u] += r * DeterministicMath.cos(a1);
                q[v] += r * DeterministicMath.sin(a1);
                segment(p, q);
            }
    }

    /**
     * A platform ({@link Access}): its deck {@code width} out from the wall along {@code run}, its walking surface
     * {@link Access#DECK} over {@code level}, and its handrail ({@link #rails}), open over the {@code gaps} where a ladder
     * is walked through. A player walks the deck; its outer edge is outlined.
     */
    private void platform(Access.Run run, double level, double width, boolean closedStart, boolean closedEnd, double... gaps) {
        Access.deck(mesh, run, level, width);
        decks.add(new Deck(run, level, width, closedStart, closedEnd));
        double deck = level + Access.DECK, length = run.length();
        if (run instanceof Access.Arc arc) solids.add(Solid.ring(arc.cx(), arc.cz(), level, deck, arc.wall() + Access.GAP, arc.wall() + width, arc.from(), arc.to()));
        else solids.add(span(run.at(0, Access.GAP, level), run.at(length, width, deck)));
        rails(run, level, width, closedStart, closedEnd, gaps);
        int count = Math.max(2, (int) Math.ceil(length / .5));
        for (int j = 0; j < count; j++) segment(run.at(length * j / count, width, deck), run.at(length * (j + 1) / count, width, deck));
    }

    /** A handrail alone {@code width} out from the wall along {@code run}, standing {@link Access#DECK} over {@code level} (a roof's edge): open at both ends. */
    private void railing(Access.Run run, double level, double width) {
        rails(run, level, width, false, false);
    }

    /**
     * A deck's handrail along {@code run} but over the {@code gaps} (pairs of stations along it), each piece's posts at its
     * ends, a rail across each closed end of the run; a player meets the rails and closed ends as fences {@link Access#FENCE}
     * high and {@link Access#RAIL_BAND} thick.
     */
    private void rails(Access.Run run, double level, double width, boolean closedStart, boolean closedEnd, double... gaps) {
        double deck = level + Access.DECK, fence = deck + Access.FENCE, length = run.length();
        var pieces = new ArrayList<double[]>();
        double from = 0;
        for (int g = 0; g + 1 < gaps.length; g += 2) {
            if (gaps[g] > from + .05) pieces.add(new double[]{from, gaps[g]});
            from = Math.max(from, gaps[g + 1]);
        }
        if (length > from + .05) pieces.add(new double[]{from, length});
        for (int i = 0; i < pieces.size(); i++) {
            var piece = Access.part(run, pieces.get(i)[0], pieces.get(i)[1]);
            boolean start = closedStart && pieces.get(i)[0] == 0, end = closedEnd && pieces.get(i)[1] == length;
            Access.handrail(mesh, piece, level, width, start, end);
            if (piece instanceof Access.Arc arc) solids.add(Solid.ring(arc.cx(), arc.cz(), deck, fence, arc.wall() + width - Access.RAIL_BAND, arc.wall() + width, arc.from(), arc.to()));
            else solids.add(span(piece.at(0, width - Access.RAIL_BAND, deck), piece.at(piece.length(), width, fence)));
        }
        if (run instanceof Access.Arc arc) {
            // Inside the deck's last band, as on a straight run: the band is RAIL_BAND long at its inner edge.
            double wall = arc.wall(), band = Access.RAIL_BAND / (wall + Access.GAP);
            if (closedStart) solids.add(Solid.ring(arc.cx(), arc.cz(), deck, fence, wall + Access.GAP, wall + width, arc.from(), arc.from() + band));
            if (closedEnd) solids.add(Solid.ring(arc.cx(), arc.cz(), deck, fence, wall + Access.GAP, wall + width, arc.to() - band, arc.to()));
        } else {
            if (closedStart) solids.add(span(run.at(0, Access.GAP, deck), run.at(Access.RAIL_BAND, width, fence)));
            if (closedEnd) solids.add(span(run.at(length - Access.RAIL_BAND, Access.GAP, deck), run.at(length, width, fence)));
        }
    }

    /**
     * A caged ladder ({@link Access#ladder}) whose rung plane's middle stands at (x, z), facing {@code out} from a wall
     * {@code back} behind it, from {@code base} to {@code top}; one {@code walkThrough} is climbed facing the deck it
     * lands on and left between its rails, so its rung plane stops at the landing.
     */
    private void ladder(double x, double z, double[] out, double back, double base, double top, boolean walkThrough) {
        var climb = new Access.Climb(x, z, out[0], out[2], base, top, walkThrough ? top - Access.LANDING : top);
        Access.ladder(mesh, climb, back);
        ladders.add(climb);
    }

    /** The axis-aligned box spanned by two corners in any order, a solid. */
    private static Solid span(double[] p, double[] q) {
        return Solid.box(new double[]{Math.min(p[0], q[0]), Math.min(p[1], q[1]), Math.min(p[2], q[2])}, new double[]{Math.max(p[0], q[0]), Math.max(p[1], q[1]), Math.max(p[2], q[2])});
    }

    /** An axis-aligned box, every face drawn; {@code solid} for one a player collides with. */
    private void box(double[] lo, double[] hi, Material material, boolean solid) {
        mesh.box(lo, hi, ALL_FACES, material);
        if (solid) solids.add(Solid.box(lo.clone(), hi.clone()));
    }

    private void segment(double[] p, double[] q) {
        edges.add(new double[]{p[0], p[1], p[2], q[0], q[1], q[2]});
    }

    private static int sides(double od) {
        return Math.max(12, PipeSizes.sides(od));
    }
}
