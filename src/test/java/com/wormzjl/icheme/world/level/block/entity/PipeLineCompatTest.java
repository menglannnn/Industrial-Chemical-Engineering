package com.wormzjl.icheme.world.level.block.entity;

import com.wormzjl.icheme.runtime.fluid.PhysicalFluidTopology;
import com.wormzjl.icheme.science.equipment.Paint;
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
 * A pipe block's line facts (AGENTS.md, "Saves and data formats"; batch 2026-10-26-pipe-laying-system): its joints and
 * its paint, read by name and validated. A pipe saved before either existed reads every face and bare, as it was; a
 * damaged value is repaired to that; every corpus image under {@code compat/pipes/} still reads to what it was saved with.
 */
class PipeLineCompatTest {
    private static final Path CORPUS = Path.of("src/test/resources/compat/pipes");

    static String describe(CompoundTag tag) {
        int paint = FluidDeviceBlockEntity.readPaint(tag);
        return "joints " + FluidDeviceBlockEntity.readJoints(tag) + " paint " + (paint == Paint.BARE ? "bare" : Paint.hex(paint));
    }

    @Test
    void everyCorpusImageReadsToWhatItWasSavedWith() throws IOException, com.mojang.brigadier.exceptions.CommandSyntaxException {
        int images = 0;
        try (var files = Files.list(CORPUS)) {
            for (var file : files.sorted().toList())
                for (var line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    if (line.isBlank()) continue;
                    var entry = TagParser.parseTag(line);
                    var saved = entry.getCompound("Saved");
                    assertEquals(entry.getString("Expected"), describe(saved), file.toString());
                    var again = FluidDeviceBlockEntity.writeLine(FluidDeviceBlockEntity.readJoints(saved), FluidDeviceBlockEntity.readPaint(saved), new CompoundTag());
                    assertEquals(entry.getString("Expected"), describe(again), file + " saved again");
                    images++;
                }
        }
        assertTrue(images >= 3, "the corpus holds an old pipe, a laid one and a painted one");
    }

    @Test
    void aDamagedValueReadsAsBeforeItExisted() {
        for (var damage : List.<java.util.function.Consumer<CompoundTag>>of(t -> t.putString("Paint", "blue"), t -> t.putInt("Paint", 0x1000000), t -> t.putInt("Paint", -2))) {
            var tag = FluidDeviceBlockEntity.writeLine(1 << 4 | 1 << 5, 0x2271B3, new CompoundTag());
            damage.accept(tag);
            assertEquals(Paint.BARE, FluidDeviceBlockEntity.readPaint(tag), "paint " + tag.get("Paint"));
            assertEquals(1 << 4 | 1 << 5, FluidDeviceBlockEntity.readJoints(tag), "the joints are kept");
        }
        var joints = new CompoundTag();
        joints.putByte("Joints", (byte) 64);
        assertEquals(PhysicalFluidTopology.ALL_FACES, FluidDeviceBlockEntity.readJoints(joints), "a value past the six faces is no joint set");
        assertTrue(FluidDeviceBlockEntity.writeLine(PhysicalFluidTopology.ALL_FACES, Paint.BARE, new CompoundTag()).isEmpty(), "a hand-placed bare pipe saves as before");
    }
}
