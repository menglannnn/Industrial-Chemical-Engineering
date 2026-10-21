package com.wormzjl.icheme.runtime.fluid;

import com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics;
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.nbt.CompoundTag;

/**
 * Test-side edits of in-memory checkpoints ({@link FluidCheckpointCodec.Image}): a test that alters one field re-seals
 * the digests, so the load reaches the validation of that field rather than the checksum.
 */
final class CheckpointImages {
    private CheckpointImages(){}
    /** Recomputes the envelope digest of a core record from its current fields. */
    static void reseal(CompoundTag core){core.putByteArray("SHA256",FluidCheckpointCodec.manifest(core));}
    /** The index position of island {@code id} in a core record. */
    static int row(CompoundTag core,long id) {
        var ids=core.getCompound("Islands").getLongArray("Id");for(int i=0;i<ids.length;i++)if(ids[i]==id)return i;throw new IllegalArgumentException("no island "+id);
    }
    /** The raw unit bytes of island {@code id}. */
    static byte[] unitBytes(FluidCheckpointCodec.Image image,long id) {
        var islands=image.core().getCompound("Islands");int i=row(image.core(),id);
        int offset=(int)islands.getLongArray("Offset")[i];return Arrays.copyOfRange(image.packs().get(islands.getLongArray("Pack")[i]),offset,offset+islands.getIntArray("Length")[i]);
    }
    /**
     * A copy of a checkpoint whose island {@code id} was decoded (as a load decodes it, before registration), edited
     * and written again under the same identity, revision and generation, in a new pack, with every digest
     * recomputed; the edit may also change the index row's clock and certificate summary through the edited snapshot.
     */
    static FluidCheckpointCodec.Image withIsland(FluidCheckpointCodec.Image image,long id,Function<FluidCheckpointCodec.PackageKey,FluidThermodynamics> models,
                                                 UnaryOperator<FluidCheckpointCodec.IslandEntry> edit) {
        var core=FluidCheckpointCodec.readCore(image.core());var strings=core.strings();
        var models2=new HashMap<FluidCheckpointCodec.PackageKey,FluidThermodynamics>();for(var p:core.packages())models2.put(p.key(),models.apply(p.key()));
        var row=core.islands().stream().filter(x->x.id()==id).findFirst().orElseThrow();
        var entry=edit.apply(FluidCheckpointCodec.island(image.packs().get(row.unit().pack()),row,core.epoch(),strings,models2));
        return withUnit(image,id,FluidCheckpointCodec.islandUnit(entry,row.generation(),strings),strings);
    }
    /** A copy of a checkpoint with island {@code id}'s unit replaced by {@code unit} in a new pack and every digest recomputed. */
    static FluidCheckpointCodec.Image withUnit(FluidCheckpointCodec.Image image,long id,byte[] unit){return withUnit(image,id,unit,null);}
    private static FluidCheckpointCodec.Image withUnit(FluidCheckpointCodec.Image image,long id,byte[] unit,FluidCheckpointCodec.Strings strings) {
        var tag=image.core().copy();if(strings!=null)tag.put("Strings",strings.tag());
        long pack=tag.getLong("NextPack");var bytes=FluidCheckpointStore.pack(pack,List.of(unit));
        var islands=tag.getCompound("Islands");int i=row(tag,id);
        islands.getLongArray("Pack")[i]=pack;islands.getLongArray("Offset")[i]=FluidCheckpointStore.PACK_HEADER;islands.getIntArray("Length")[i]=unit.length;
        System.arraycopy(FluidCheckpointCodec.sha256(unit),0,islands.getByteArray("SHA256"),32*i,32);
        var packs=new TreeMap<Long,Long>();var list=tag.getLongArray("Packs");var sizes=tag.getLongArray("PackBytes");for(int p=0;p<list.length;p++)packs.put(list[p],sizes[p]);
        packs.put(pack,(long)bytes.length);
        tag.putLongArray("Packs",packs.keySet().stream().mapToLong(Long::longValue).toArray());tag.putLongArray("PackBytes",packs.values().stream().mapToLong(Long::longValue).toArray());
        tag.putLong("NextPack",pack+1);reseal(tag);
        var all=new HashMap<>(image.packs());all.put(pack,bytes);return new FluidCheckpointCodec.Image(tag,all);
    }
}
