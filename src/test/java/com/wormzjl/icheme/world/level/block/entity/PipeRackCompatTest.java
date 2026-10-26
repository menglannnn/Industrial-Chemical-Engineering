package com.wormzjl.icheme.world.level.block.entity;

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
 * The pipe rack's saved lane copies (AGENTS.md, "Saves and data formats"; batch 2026-10-26-pipe-laying-system, plan 8):
 * read by name, validated, a damaged or overlapping lane dropped with the others kept (the ledger's registration, the
 * fact, restores it at the next load), and every corpus image under {@code compat/racks/} still reads to what it was
 * saved with.
 */
class PipeRackCompatTest {
    private static final int DOWN = 0, UP = 1, NORTH = 2, SOUTH = 3, WEST = 4, EAST = 5;

    /** A straight lane, a wide elbow, and an open one-cell lane with no joints. */
    static List<Lane> corpusLanes() {
        return List.of(new Lane(41, 0, 1, 1 << WEST | 1 << EAST, EAST, .1), new Lane(42, 1, 2, 1 << NORTH | 1 << UP, NORTH, .25), new Lane(43, 4, 1, 0, SOUTH, .05));
    }

    static String describe(List<Lane> lanes) {
        var out = new StringBuilder();
        for (var lane : lanes)
            out.append("lane ").append(lane.identity()).append(" slots ").append(lane.slot()).append('+').append(lane.width()).append(" joints ").append(lane.joints())
                    .append(" facing ").append(lane.facing()).append(" bore ").append(lane.bore()).append("; ");
        return out.toString();
    }

    @Test
    void aRackRoundTripsItsLanesExactly() {
        var saved = PipeRackBlockEntity.write(corpusLanes(), new CompoundTag());
        assertEquals(corpusLanes(), PipeRackBlockEntity.read(saved));
        assertEquals(saved, PipeRackBlockEntity.write(PipeRackBlockEntity.read(saved), new CompoundTag()));
        assertEquals(List.of(), PipeRackBlockEntity.read(new CompoundTag()), "a rack saved without lanes, or before they existed");
        assertEquals("east", saved.getList("Lanes", 10).getCompound(0).getString("Facing"), "the facing is saved by name");
    }

    @Test
    void aDamagedOrOverlappingLaneIsDroppedAndTheOthersKept() {
        var lanes = corpusLanes();
        for (var damage : List.<java.util.function.Consumer<CompoundTag>>of(e -> e.remove("Bore"), e -> e.putInt("Slot", 5), e -> e.putInt("Width", 0), e -> e.putString("Facing", "sideways"),
                e -> e.putInt("Joints", 1 << NORTH | 1 << SOUTH | 1 << EAST), e -> e.putDouble("Bore", -1), e -> e.putString("Identity", "41"))) {
            var saved = PipeRackBlockEntity.write(lanes, new CompoundTag());
            damage.accept(saved.getList("Lanes", 10).getCompound(0));
            assertEquals(lanes.subList(1, 3), PipeRackBlockEntity.read(saved), "damaged " + saved.getList("Lanes", 10).getCompound(0));
        }
        var overlapping = PipeRackBlockEntity.write(List.of(lanes.get(1), new Lane(44, 2, 2, 1 << DOWN | 1 << UP, UP, .2), lanes.get(0)), new CompoundTag());
        assertEquals(List.of(lanes.get(0), lanes.get(1)), PipeRackBlockEntity.read(overlapping), "the lane read second over used slots is dropped; the rest in slot order");
    }

    private static final Path CORPUS = Path.of("src/test/resources/compat/racks");

    /**
     * Every corpus image reads to the lanes recorded beside it, and saves back to lanes that read the same. The image of
     * this build's format must exist; when it is missing the test writes it under {@code build/compat/racks/} to be
     * copied into the corpus.
     */
    @Test
    void everyCorpusImageReadsToWhatItWasSavedWith() throws IOException, com.mojang.brigadier.exceptions.CommandSyntaxException {
        var current = CORPUS.resolve(PipeRackBlockEntity.FORMAT + ".snbt");
        if (!Files.exists(current)) {
            var written = Path.of("build/compat/racks/" + PipeRackBlockEntity.FORMAT + ".snbt");
            Files.createDirectories(written.getParent());
            var entry = new CompoundTag();
            entry.put("Saved", PipeRackBlockEntity.write(corpusLanes(), new CompoundTag()));
            entry.putString("Expected", describe(corpusLanes()));
            Files.write(written, List.of(entry.toString()), StandardCharsets.UTF_8);
            fail("No corpus image of rack format " + PipeRackBlockEntity.FORMAT + ": copy " + written.toAbsolutePath() + " to " + current);
        }
        try (var files = Files.list(CORPUS)) {
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
