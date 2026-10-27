package com.wormzjl.icheme.world.level.block.entity;

import com.wormzjl.icheme.runtime.fluid.PhysicalFluidTopology;
import com.wormzjl.icheme.science.equipment.Paint;
import com.wormzjl.icheme.science.equipment.RackGeometry;
import com.wormzjl.icheme.world.level.block.entity.PipeRackBlockEntity.Lane;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A pipe block's line facts (AGENTS.md, "Saves and data formats"; batch 2026-10-26-pipe-laying-system): its joints, its
 * paint and its secondary paint, read by name and validated. A pipe saved before they existed reads every face, bare,
 * and its flanges in its paint, as it was; a damaged value is repaired to that; every corpus image under {@code compat/pipes/} still reads to what it was saved with.
 *
 * <p>The pipe rack's saved lane copies (plan 8 and RACK_LAYERS_PLAN RD9): read by name, validated, a damaged or
 * repeated lane dropped with the others kept (the ledger's registration, the fact, restores it at the next load), and
 * every corpus image under {@code compat/racks/} still reads to what it was saved with.</p>
 */
class PipeLineCompatTest {
    private static final Path PIPES = Path.of("src/test/resources/compat/pipes"), RACKS = Path.of("src/test/resources/compat/racks");
    private static final int DOWN = 0, UP = 1, NORTH = 2, SOUTH = 3, WEST = 4, EAST = 5;

    static String describe(CompoundTag tag) {
        int paint = FluidDeviceBlockEntity.readPaint(tag), secondary = FluidDeviceBlockEntity.readSecondary(tag, paint);
        // A secondary paint equal to the paint is described as before it existed, so a pipe-1 image still reads to what it recorded.
        return "joints " + FluidDeviceBlockEntity.readJoints(tag) + " paint " + name(paint) + (secondary != paint ? " secondary " + name(secondary) : "");
    }

    private static String name(int paint) {
        return paint == Paint.BARE ? "bare" : Paint.hex(paint);
    }

    @Test
    void everyPipeCorpusImageReadsToWhatItWasSavedWith() throws IOException, com.mojang.brigadier.exceptions.CommandSyntaxException {
        int images = 0;
        try (var files = Files.list(PIPES)) {
            for (var file : files.sorted().toList())
                for (var line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    if (line.isBlank()) continue;
                    var entry = TagParser.parseTag(line);
                    var saved = entry.getCompound("Saved");
                    assertEquals(entry.getString("Expected"), describe(saved), file.toString());
                    int paint = FluidDeviceBlockEntity.readPaint(saved);
                    var again = FluidDeviceBlockEntity.writeLine(FluidDeviceBlockEntity.readJoints(saved), paint, FluidDeviceBlockEntity.readSecondary(saved, paint), new CompoundTag());
                    assertEquals(entry.getString("Expected"), describe(again), file + " saved again");
                    images++;
                }
        }
        assertTrue(images >= 6, "the corpus holds an old pipe, a laid one, a painted one and three with a secondary paint");
    }

    @Test
    void aDamagedValueReadsAsBeforeItExisted() {
        for (var damage : List.<java.util.function.Consumer<CompoundTag>>of(t -> t.putString("Paint", "blue"), t -> t.putInt("Paint", 0x1000000), t -> t.putInt("Paint", -2))) {
            var tag = FluidDeviceBlockEntity.writeLine(1 << 4 | 1 << 5, 0x2271B3, 0x2271B3, new CompoundTag());
            damage.accept(tag);
            assertEquals(Paint.BARE, FluidDeviceBlockEntity.readPaint(tag), "paint " + tag.get("Paint"));
            assertEquals(1 << 4 | 1 << 5, FluidDeviceBlockEntity.readJoints(tag), "the joints are kept");
        }
        var joints = new CompoundTag();
        joints.putByte("Joints", (byte) 64);
        assertEquals(PhysicalFluidTopology.ALL_FACES, FluidDeviceBlockEntity.readJoints(joints), "a value past the six faces is no joint set");
        for (var damage : List.<java.util.function.Consumer<CompoundTag>>of(t -> t.putString("Secondary", "red"), t -> t.putInt("Secondary", 0x1000000), t -> t.putInt("Secondary", -2))) {
            var tag = FluidDeviceBlockEntity.writeLine(PhysicalFluidTopology.ALL_FACES, 0x2271B3, 0xA72920, new CompoundTag());
            damage.accept(tag);
            assertEquals(0x2271B3, FluidDeviceBlockEntity.readSecondary(tag, FluidDeviceBlockEntity.readPaint(tag)), "secondary " + tag.get("Secondary") + " reads as the paint");
        }
        assertTrue(FluidDeviceBlockEntity.writeLine(PhysicalFluidTopology.ALL_FACES, Paint.BARE, Paint.BARE, new CompoundTag()).isEmpty(), "a hand-placed bare pipe saves as before");
        assertFalse(FluidDeviceBlockEntity.writeLine(PhysicalFluidTopology.ALL_FACES, 0x2271B3, 0x2271B3, new CompoundTag()).contains("Secondary"), "flanges in the paint save no secondary");
    }

    // ---- rack lanes ----------------------------------------------------------------------------------------------------

    /**
     * A straight lane on position 0, a 10" riser on positions 1 and 2 painted RAL 5015 with RAL 3000 flanges (rack-3), and
     * an open crossing above the tier.
     */
    static List<Lane> corpusLanes() {
        return List.of(new Lane(41, RackGeometry.option(0, RackGeometry.TIER), 1, 1 << WEST | 1 << EAST, 1 << WEST | 1 << EAST, .1),
                new Lane(42, RackGeometry.option(1, RackGeometry.TIER), 2, 1 << WEST | 1 << UP, 1 << WEST, .25, 0x2271B3, 0xA72920),
                new Lane(43, RackGeometry.crossing(RackGeometry.ABOVE), 1, 1 << NORTH | 1 << SOUTH, 0, .05));
    }

    static String describe(List<Lane> lanes) {
        var out = new StringBuilder();
        for (var lane : lanes) {
            out.append("lane ").append(lane.identity()).append(" option ").append(lane.option()).append('+').append(lane.width()).append(" arms ").append(lane.arms())
                    .append(" joints ").append(lane.joints()).append(" bore ").append(lane.bore());
            if (lane.paint() != Paint.BARE) out.append(" paint ").append(Paint.hex(lane.paint()));
            if (lane.secondary() != lane.paint()) out.append(" secondary ").append(lane.secondary() == Paint.BARE ? "bare" : Paint.hex(lane.secondary()));
            out.append("; ");
        }
        return out.toString();
    }

    @Test
    void aRackRoundTripsItsLanesExactly() {
        var saved = PipeRackBlockEntity.write(corpusLanes(), new CompoundTag());
        assertEquals(corpusLanes(), PipeRackBlockEntity.read(saved));
        assertEquals(saved, PipeRackBlockEntity.write(PipeRackBlockEntity.read(saved), new CompoundTag()));
        assertEquals(List.of(), PipeRackBlockEntity.read(new CompoundTag()), "a rack saved without lanes, or before they existed");
    }

    @Test
    void aDamagedOrRepeatedLaneIsDroppedAndTheOthersKept() {
        var lanes = corpusLanes();
        for (var damage : List.<java.util.function.Consumer<CompoundTag>>of(e -> e.remove("Bore"), e -> e.putInt("Option", RackGeometry.OPTIONS), e -> e.putInt("Width", 0),
                e -> e.putInt("Width", 5), e -> { e.putInt("Option", RackGeometry.option(3, RackGeometry.ABOVE)); e.putInt("Width", 2); }, e -> { e.putInt("Option", RackGeometry.crossing(RackGeometry.ABOVE)); e.putInt("Width", 2); },
                e -> e.putInt("Arms", 1 << NORTH | 1 << SOUTH | 1 << EAST), e -> e.putInt("Joints", 1 << UP), e -> e.putDouble("Bore", -1),
                e -> e.putString("Identity", "41"))) {
            var saved = PipeRackBlockEntity.write(lanes, new CompoundTag());
            damage.accept(saved.getList("Lanes", 10).getCompound(0));
            assertEquals(lanes.subList(1, 3), PipeRackBlockEntity.read(saved), "damaged " + saved.getList("Lanes", 10).getCompound(0));
        }
        var repeated = PipeRackBlockEntity.write(List.of(lanes.get(1), new Lane(44, lanes.get(1).option(), 1, 1 << DOWN | 1 << UP, 0, .2), lanes.get(0)), new CompoundTag());
        assertEquals(List.of(lanes.get(0), lanes.get(1)), PipeRackBlockEntity.read(repeated), "the lane read second on a used option is dropped; the rest in option order");
    }

    @Test
    void aLaneSavedBareOrWithADamagedPaintReadsBareAndIsKept() {
        var painted = corpusLanes().get(1);
        assertEquals(0x2271B3, PipeRackBlockEntity.read(PipeRackBlockEntity.write(List.of(painted), new CompoundTag())).getFirst().paint());
        for (var damage : List.<java.util.function.Consumer<CompoundTag>>of(e -> e.remove("Paint"), e -> e.putString("Paint", "blue"), e -> e.putInt("Paint", 0x1000000), e -> e.putInt("Paint", -2))) {
            var saved = PipeRackBlockEntity.write(List.of(painted), new CompoundTag());
            damage.accept(saved.getList("Lanes", 10).getCompound(0));
            assertEquals(List.of(painted.painted(Paint.BARE, painted.secondary())), PipeRackBlockEntity.read(saved), "paint " + saved.getList("Lanes", 10).getCompound(0).get("Paint"));
        }
        assertFalse(PipeRackBlockEntity.write(List.of(painted.painted(Paint.BARE)), new CompoundTag()).getList("Lanes", 10).getCompound(0).contains("Paint"), "a bare lane saves no paint");
    }

    @Test
    void aSecondaryPaintAbsentOrDamagedReadsThePaint() {
        var painted = corpusLanes().get(1);
        assertEquals(0xA72920, PipeRackBlockEntity.read(PipeRackBlockEntity.write(List.of(painted), new CompoundTag())).getFirst().secondary());
        for (var damage : List.<java.util.function.Consumer<CompoundTag>>of(e -> e.remove("Secondary"), e -> e.putString("Secondary", "red"), e -> e.putInt("Secondary", 0x1000000), e -> e.putInt("Secondary", -2))) {
            var saved = PipeRackBlockEntity.write(List.of(painted), new CompoundTag());
            damage.accept(saved.getList("Lanes", 10).getCompound(0));
            assertEquals(List.of(painted.painted(painted.paint())), PipeRackBlockEntity.read(saved), "secondary " + saved.getList("Lanes", 10).getCompound(0).get("Secondary"));
        }
        var bareFlanges = painted.painted(painted.paint(), Paint.BARE);
        assertEquals(List.of(bareFlanges), PipeRackBlockEntity.read(PipeRackBlockEntity.write(List.of(bareFlanges), new CompoundTag())), "bare flanges on a painted pipe");
        assertFalse(PipeRackBlockEntity.write(List.of(painted.painted(painted.paint())), new CompoundTag()).getList("Lanes", 10).getCompound(0).contains("Secondary"), "flanges as the pipe save no secondary");
    }

    @Test
    void aLaneFacesItsOpenArmElseItsLowestJoint() {
        var lanes = corpusLanes();
        assertEquals(WEST, lanes.get(0).facing(), "both arms jointed: the lowest");
        assertEquals(UP, lanes.get(1).facing(), "open upwards");
        assertEquals(NORTH, lanes.get(2).facing(), "open at both arms: the lowest");
        for (var lane : lanes) assertEquals(lane.arms(), Lane.arms(lane.joints(), lane.facing()), "the registration's joints and facing give back the arms: " + lane);
    }

    /**
     * Every rack corpus image reads to the lanes recorded beside it, and saves back to lanes that read the same. The image
     * of this build's format must exist; when it is missing the test writes it under {@code build/compat/racks/} to be
     * copied into the corpus.
     */
    @Test
    void everyRackCorpusImageReadsToWhatItWasSavedWith() throws IOException, com.mojang.brigadier.exceptions.CommandSyntaxException {
        var current = RACKS.resolve(PipeRackBlockEntity.FORMAT + ".snbt");
        if (!Files.exists(current)) {
            var written = Path.of("build/compat/racks/" + PipeRackBlockEntity.FORMAT + ".snbt");
            Files.createDirectories(written.getParent());
            var entry = new CompoundTag();
            entry.put("Saved", PipeRackBlockEntity.write(corpusLanes(), new CompoundTag()));
            entry.putString("Expected", describe(corpusLanes()));
            Files.write(written, List.of(entry.toString()), StandardCharsets.UTF_8);
            fail("No corpus image of rack format " + PipeRackBlockEntity.FORMAT + ": copy " + written.toAbsolutePath() + " to " + current);
        }
        try (var files = Files.list(RACKS)) {
            for (var file : files.sorted().toList())
                for (var line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    if (line.isBlank()) continue;
                    var entry = TagParser.parseTag(line);
                    var loaded = PipeRackBlockEntity.read(entry.getCompound("Saved"));
                    assertEquals(entry.getString("Expected"), describe(loaded), file.toString());
                    assertEquals(entry.getString("Expected"), describe(PipeRackBlockEntity.read(PipeRackBlockEntity.write(loaded, new CompoundTag()))), file + " saved again");
                }
        }
    }
}
