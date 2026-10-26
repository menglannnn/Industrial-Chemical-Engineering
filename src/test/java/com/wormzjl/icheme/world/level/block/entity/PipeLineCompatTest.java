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
 * A pipe block's line facts (AGENTS.md, "Saves and data formats"; batch 2026-10-26-pipe-laying-system): its joints, its
 * paint and its secondary paint, read by name and validated. A pipe saved before they existed reads every face, bare,
 * and its flanges in its paint, as it was; a damaged value is repaired to that; every corpus image under {@code compat/pipes/} still reads to what it was saved with.
 */
class PipeLineCompatTest {
    private static final Path CORPUS = Path.of("src/test/resources/compat/pipes");

    static String describe(CompoundTag tag) {
        int paint = FluidDeviceBlockEntity.readPaint(tag), secondary = FluidDeviceBlockEntity.readSecondary(tag, paint);
        // A secondary paint equal to the paint is described as before it existed, so a pipe-1 image still reads to what it recorded.
        return "joints " + FluidDeviceBlockEntity.readJoints(tag) + " paint " + name(paint) + (secondary != paint ? " secondary " + name(secondary) : "");
    }

    private static String name(int paint) {
        return paint == Paint.BARE ? "bare" : Paint.hex(paint);
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
}
