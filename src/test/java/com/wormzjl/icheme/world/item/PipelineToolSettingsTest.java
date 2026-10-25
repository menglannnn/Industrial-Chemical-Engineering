package com.wormzjl.icheme.world.item;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PipelineToolSettingsTest {
    @Test
    void componentRoundTripsExactDiametersAndRejectsInvalidStorageAndWireValues() {
        for (double diameter : new double[]{.001, PipelineToolItem.Settings.DEFAULT.diameter(), .123456789012345, PipelineToolItem.Settings.MAX_DIAMETER}) {
            var settings = new PipelineToolItem.Settings(diameter);
            var saved = PipelineToolItem.Settings.CODEC.encodeStart(NbtOps.INSTANCE, settings).getOrThrow();
            assertEquals(settings, PipelineToolItem.Settings.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow());
            var buffer = Unpooled.buffer();
            try {
                PipelineToolItem.Settings.STREAM_CODEC.encode(buffer, settings);
                assertEquals(settings, PipelineToolItem.Settings.STREAM_CODEC.decode(buffer));
                assertFalse(buffer.isReadable());
            } finally {
                buffer.release();
            }
        }
        for (double diameter : new double[]{Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0, -.1, .0009, .8001, 1}) {
            assertThrows(IllegalArgumentException.class, () -> new PipelineToolItem.Settings(diameter));
            assertTrue(PipelineToolItem.Settings.CODEC.parse(NbtOps.INSTANCE, DoubleTag.valueOf(diameter)).error().isPresent());
            var buffer = Unpooled.buffer();
            try {
                buffer.writeDouble(diameter);
                assertThrows(IllegalArgumentException.class, () -> PipelineToolItem.Settings.STREAM_CODEC.decode(buffer));
            } finally {
                buffer.release();
            }
        }
        assertTrue(PipelineToolItem.Settings.CODEC.parse(JsonOps.INSTANCE, new com.google.gson.JsonPrimitive("invalid")).error().isPresent());
    }
}
