package com.wormzjl.icheme.runtime.instrument;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import com.wormzjl.icheme.world.item.GaugeSettings;
import java.util.List;

/** Chunk-synchronized static attachment descriptions; the registry is the server's authority. */
public final class InstrumentMetadata {
    private List<Instrument.Installed> installed=List.of();
    private long revision;
    private boolean recoverOnBreak=true;
    public boolean recoverOnBreak(){return recoverOnBreak;}
    public void recoverOnBreak(boolean value){recoverOnBreak=value;}
    public List<Instrument.Installed> installed(){return installed;}
    public long revision(){return revision;}
    public void set(List<Instrument.Installed> value,long revision){installed=List.copyOf(value);this.revision=revision;}
    public void write(CompoundTag tag){tag.putLong("AccessoryRevision",revision);var list=new ListTag();for(var i:installed)list.add(encode(i));tag.put("Instruments",list);}
    public void read(CompoundTag tag){revision=tag.getLong("AccessoryRevision");if(revision<0)throw new IllegalArgumentException("Invalid accessory revision");var list=tag.getList("Instruments",10);if(list.size()>6)throw new IllegalArgumentException("Too many host instruments");var result=new java.util.ArrayList<Instrument.Installed>();for(var t:list)result.add(decode((CompoundTag)t));installed=List.copyOf(result);}
    public static CompoundTag encode(Instrument.Installed i){var t=new CompoundTag();t.putLong("host",i.key().host());t.putString("mount",i.key().mount());t.putUUID("instance",i.instance());t.putLong("revision",i.revision());t.put("settings",GaugeSettings.CODEC.encodeStart(NbtOps.INSTANCE,i.settings()).getOrThrow());return t;}
    public static Instrument.Installed decode(CompoundTag t){return new Instrument.Installed(new Instrument.Key(t.getLong("host"),t.getString("mount")),t.getUUID("instance"),t.getLong("revision"),GaugeSettings.CODEC.parse(NbtOps.INSTANCE,t.get("settings")).getOrThrow());}
}
