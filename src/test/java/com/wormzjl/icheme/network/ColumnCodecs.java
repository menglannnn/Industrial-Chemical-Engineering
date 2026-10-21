package com.wormzjl.icheme.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.wormzjl.icheme.science.column.v3.V3ColumnDisplayResult;
import com.wormzjl.icheme.science.column.v3.V3ColumnInput;
import com.wormzjl.icheme.world.level.block.entity.ColumnCalculatorV3BlockEntity;
import com.wormzjl.icheme.world.level.block.entity.ColumnCalculatorV3BlockEntity.V3State;
import io.netty.buffer.Unpooled;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.function.Executable;

/**
 * The one reflection helper of the column codec tests.
 *
 * <p>The block-entity NBT codecs and the network wire codecs are private statics; the unit suite has no Minecraft
 * bootstrap, so the block entity cannot be instantiated and its {@code loadAdditional} is exercised through the same
 * helpers it delegates to. A decoder failure surfaces as an {@link InvocationTargetException}; {@link #failure}
 * returns its cause.</p>
 */
public final class ColumnCodecs {
    private ColumnCodecs() {}

    public static Object invoke(Class<?> owner, String name, Class<?>[] types, Object... arguments) throws Exception {
        Method method = owner.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(null, arguments);
    }

    /** The cause of the reflective failure {@code call} must raise. */
    public static Throwable failure(Executable call) {
        return assertThrows(InvocationTargetException.class, call).getCause();
    }

    public static RegistryFriendlyByteBuf buffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    }

    public static void writeInput(RegistryFriendlyByteBuf buffer, V3ColumnInput input) throws Exception {
        invoke(ColumnV3Network.class, "writeInput",
                new Class<?>[] {RegistryFriendlyByteBuf.class, V3ColumnInput.class}, buffer, input);
    }

    public static V3ColumnInput readInput(RegistryFriendlyByteBuf buffer) throws Exception {
        return (V3ColumnInput) invoke(ColumnV3Network.class, "readInput",
                new Class<?>[] {RegistryFriendlyByteBuf.class}, buffer);
    }

    public static void writeResult(RegistryFriendlyByteBuf buffer, V3ColumnDisplayResult result) throws Exception {
        invoke(ColumnV3Network.class, "writeDisplayResult",
                new Class<?>[] {RegistryFriendlyByteBuf.class, V3ColumnDisplayResult.class}, buffer, result);
    }

    public static V3ColumnDisplayResult readResult(RegistryFriendlyByteBuf buffer) throws Exception {
        return (V3ColumnDisplayResult) invoke(ColumnV3Network.class, "readDisplayResult",
                new Class<?>[] {RegistryFriendlyByteBuf.class}, buffer);
    }

    public static void writeState(RegistryFriendlyByteBuf buffer, V3State state) throws Exception {
        invoke(ColumnV3Network.class, "writeState", new Class<?>[] {RegistryFriendlyByteBuf.class, V3State.class},
                buffer, state);
    }

    public static V3State readState(RegistryFriendlyByteBuf buffer) throws Exception {
        return (V3State) invoke(ColumnV3Network.class, "readState", new Class<?>[] {RegistryFriendlyByteBuf.class}, buffer);
    }

    public static CompoundTag writeInputNbt(V3ColumnInput input) throws Exception {
        return (CompoundTag) invoke(ColumnCalculatorV3BlockEntity.class, "writeInput",
                new Class<?>[] {V3ColumnInput.class}, input);
    }

    public static V3ColumnInput readInputNbt(CompoundTag tag) throws Exception {
        return (V3ColumnInput) invoke(ColumnCalculatorV3BlockEntity.class, "readInput",
                new Class<?>[] {CompoundTag.class}, tag);
    }

    public static CompoundTag writeResultNbt(V3ColumnDisplayResult result) throws Exception {
        return (CompoundTag) invoke(ColumnCalculatorV3BlockEntity.class, "writeDisplayResult",
                new Class<?>[] {V3ColumnDisplayResult.class}, result);
    }

    public static V3ColumnDisplayResult readResultNbt(CompoundTag tag) throws Exception {
        return (V3ColumnDisplayResult) invoke(ColumnCalculatorV3BlockEntity.class, "readDisplayResult",
                new Class<?>[] {CompoundTag.class}, tag);
    }

    /** Wire round trip that also requires the decoder to consume every byte. */
    public static V3ColumnInput wireRoundTrip(V3ColumnInput input) throws Exception {
        RegistryFriendlyByteBuf buffer = buffer();
        try {
            writeInput(buffer, input);
            V3ColumnInput decoded = readInput(buffer);
            assertEquals(0, buffer.readableBytes());
            return decoded;
        } finally {
            buffer.release();
        }
    }

    /** Wire round trip that also requires the decoder to consume every byte. */
    public static V3ColumnDisplayResult wireRoundTrip(V3ColumnDisplayResult result) throws Exception {
        RegistryFriendlyByteBuf buffer = buffer();
        try {
            writeResult(buffer, result);
            V3ColumnDisplayResult decoded = readResult(buffer);
            assertEquals(0, buffer.readableBytes());
            return decoded;
        } finally {
            buffer.release();
        }
    }

    /** Wire round trip that also requires the decoder to consume every byte. */
    public static V3State wireRoundTrip(V3State state) throws Exception {
        RegistryFriendlyByteBuf buffer = buffer();
        try {
            writeState(buffer, state);
            V3State decoded = readState(buffer);
            assertEquals(0, buffer.readableBytes());
            return decoded;
        } finally {
            buffer.release();
        }
    }
}
