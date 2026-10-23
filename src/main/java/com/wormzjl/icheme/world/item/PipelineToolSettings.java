package com.wormzjl.icheme.world.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** The tool's internal diameter in metres; the same bounds govern storage, wire and editor input. */
public record PipelineToolSettings(double diameter) {
    public static final double MIN_DIAMETER = .001;
    public static final double MAX_DIAMETER = 1;
    public static final PipelineToolSettings DEFAULT = new PipelineToolSettings(.05);
    public static final Codec<PipelineToolSettings> CODEC = Codec.DOUBLE.comapFlatMap(
            value -> valid(value) ? DataResult.success(new PipelineToolSettings(value))
                    : DataResult.error(() -> "Pipeline diameter must be finite and between 0.001 and 1 m"),
            PipelineToolSettings::diameter);
    public static final StreamCodec<ByteBuf, PipelineToolSettings> STREAM_CODEC = ByteBufCodecs.DOUBLE.map(
            PipelineToolSettings::new, PipelineToolSettings::diameter);

    public PipelineToolSettings {
        if (!valid(diameter)) throw new IllegalArgumentException("Pipeline diameter must be finite and between 0.001 and 1 m");
    }

    public static boolean valid(double diameter) {
        return Double.isFinite(diameter) && diameter >= MIN_DIAMETER && diameter <= MAX_DIAMETER;
    }
}
