package com.wormzjl.icheme.world.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.wormzjl.icheme.science.equipment.PipeSizes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** The tool's internal diameter in metres; the same bounds govern storage, wire and editor input. */
public record PipelineToolSettings(double diameter) {
    public static final double MIN_DIAMETER = .001;
    public static final double MAX_DIAMETER = PipeSizes.MAX_BORE;
    public static final PipelineToolSettings DEFAULT = new PipelineToolSettings(.05);
    public static final Codec<PipelineToolSettings> CODEC = Codec.DOUBLE.comapFlatMap(
            value -> valid(value) ? DataResult.success(new PipelineToolSettings(value))
                    : DataResult.error(() -> "Pipeline diameter must be finite and between 0.001 and " + MAX_DIAMETER + " m"),
            PipelineToolSettings::diameter);
    public static final StreamCodec<ByteBuf, PipelineToolSettings> STREAM_CODEC = ByteBufCodecs.DOUBLE.map(
            PipelineToolSettings::new, PipelineToolSettings::diameter);

    public PipelineToolSettings {
        if (!valid(diameter)) throw new IllegalArgumentException("Pipeline diameter must be finite and between 0.001 and " + MAX_DIAMETER + " m");
    }

    public static boolean valid(double diameter) {
        return Double.isFinite(diameter) && diameter >= MIN_DIAMETER && diameter <= MAX_DIAMETER;
    }
}
