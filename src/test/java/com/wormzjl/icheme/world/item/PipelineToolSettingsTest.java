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
        for (double diameter : new double[]{.001, PipelineToolSettings.DEFAULT.diameter(), .123456789012345, PipelineToolSettings.MAX_DIAMETER}) {
            var settings = new PipelineToolSettings(diameter);
            var saved = PipelineToolSettings.CODEC.encodeStart(NbtOps.INSTANCE, settings).getOrThrow();
            assertEquals(settings, PipelineToolSettings.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow());
            var buffer = Unpooled.buffer();
            try {
                PipelineToolSettings.STREAM_CODEC.encode(buffer, settings);
                assertEquals(settings, PipelineToolSettings.STREAM_CODEC.decode(buffer));
                assertFalse(buffer.isReadable());
            } finally {
                buffer.release();
            }
        }
        for (double diameter : new double[]{Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0, -.1, .0009, .8001, 1}) {
            assertThrows(IllegalArgumentException.class, () -> new PipelineToolSettings(diameter));
            assertTrue(PipelineToolSettings.CODEC.parse(NbtOps.INSTANCE, DoubleTag.valueOf(diameter)).error().isPresent());
            var buffer = Unpooled.buffer();
            try {
                buffer.writeDouble(diameter);
                assertThrows(IllegalArgumentException.class, () -> PipelineToolSettings.STREAM_CODEC.decode(buffer));
            } finally {
                buffer.release();
            }
        }
        assertTrue(PipelineToolSettings.CODEC.parse(JsonOps.INSTANCE, new com.google.gson.JsonPrimitive("invalid")).error().isPresent());
    }
}
