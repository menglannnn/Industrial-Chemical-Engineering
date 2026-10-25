package com.wormzjl.icheme.runtime.instrument;

import com.wormzjl.icheme.world.item.GaugeSettings;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Format 1: static instruments, slot tombstones and accepted, uncommitted player operations. */
public final class AccessoryStore extends SavedData {
    public static final int FORMAT=1;
    public enum Type { PRESSURE, TEMPERATURE, SIGHT_GLASS, SHUTOFF, CHECK, PRESSURE_VALVE, FILTER, PUMP, COMPRESSOR }
    public record Legacy(Instrument.Key key,UUID instance,long revision,Type type) {
        public Legacy{Objects.requireNonNull(key);Objects.requireNonNull(instance);Objects.requireNonNull(type);if(!key.mount().equals("pipe_accessory")||revision<=0||type==Type.PRESSURE||type==Type.TEMPERATURE)throw new IllegalArgumentException("Invalid pipe accessory");}
    }
    public record Pending(UUID request,UUID player,Instrument.Key key,UUID instance,long expectedRevision,
                          boolean install,Type type,GaugeSettings settings,String dimension,long position,int slot,boolean offhand) {
        public Pending{Objects.requireNonNull(request);Objects.requireNonNull(player);Objects.requireNonNull(key);Objects.requireNonNull(instance);Objects.requireNonNull(type);Objects.requireNonNull(dimension);if(expectedRevision<0||slot<0||slot>8||net.minecraft.resources.ResourceLocation.tryParse(dimension)==null||install&&settings==null)throw new IllegalArgumentException("Invalid accessory operation");if(install&&type!=Type.PRESSURE&&type!=Type.TEMPERATURE||settings!=null&&(type!=Type.PRESSURE&&type!=Type.TEMPERATURE||(type==Type.PRESSURE)!=(settings.kind()==GaugeSettings.Kind.PRESSURE)))throw new IllegalArgumentException("Accessory settings mismatch");}
    }
    private long nextDue;
    public long nextDue(){return nextDue;}
    public void nextDue(long tick){if(tick<0)throw new IllegalArgumentException("Invalid accessory deadline");nextDue=tick;setDirty();}
    private final Map<Instrument.Key,Instrument.Installed> installed=new LinkedHashMap<>();
    private final Map<Long,Legacy> legacy=new LinkedHashMap<>();
    public Legacy legacy(long host){return legacy.get(host);}
    public Legacy installLegacy(long host,Type type){var old=legacy.get(host);if(old!=null){if(old.type()!=type)throw new IllegalStateException("Occupied accessory slot");return old;}if(!host(host).isEmpty())throw new IllegalStateException("Occupied pipe");var value=new Legacy(new Instrument.Key(host,"pipe_accessory"),UUID.randomUUID(),Math.addExact(revision(host),1),type);revisions.put(host,value.revision());legacy.put(host,value);setDirty();return value;}
    public Legacy removeLegacy(long host,UUID instance){var old=legacy.get(host);if(old==null||!old.instance().equals(instance))return null;advance(host);legacy.remove(host);setDirty();return old;}
    private final Map<Long,Long> revisions=new LinkedHashMap<>();
    private final Map<UUID,Pending> pending=new LinkedHashMap<>();
    public Collection<Instrument.Installed> installed(){return List.copyOf(installed.values());}
    public Instrument.Installed get(Instrument.Key key){return installed.get(key);}
    public List<Instrument.Installed> host(long host){return installed.values().stream().filter(i->i.key().host()==host).toList();}
    public long revision(long host){return revisions.getOrDefault(host,0L);}
    public List<Pending> pending(){return List.copyOf(pending.values());}
    public boolean queue(Pending p){if(pending.containsKey(p.request)||p.expectedRevision!=revision(p.key.host())||pending.size()>=4096||pending.values().stream().anyMatch(v->v.key.equals(p.key)))return false;pending.put(p.request,p);setDirty();return true;}
    public void finish(UUID request){if(pending.remove(request)!=null)setDirty();}
    public Instrument.Installed install(Pending p){if(!p.equals(pending.get(p.request))||!p.install||revision(p.key.host())!=p.expectedRevision||installed.containsKey(p.key)||legacy.containsKey(p.key.host())||p.key.mount().startsWith("pipe_")&&!host(p.key.host()).isEmpty())return null;long rev=Math.addExact(revision(p.key.host()),1);var i=new Instrument.Installed(p.key,p.instance,rev,p.settings);revisions.put(p.key.host(),rev);installed.put(p.key,i);setDirty();return i;}
    public Instrument.Installed remove(Instrument.Key key,UUID instance){var old=installed.get(key);if(old==null||!old.instance().equals(instance))return null;advance(key.host());installed.remove(key);setDirty();return old;}
    public void changed(long host){advance(host);setDirty();}
    private long advance(long host){long next=Math.addExact(revision(host),1);revisions.put(host,next);return next;}
    @Override public CompoundTag save(CompoundTag t,HolderLookup.Provider provider){return save(t);}
    public CompoundTag save(CompoundTag t){t.putInt("format",FORMAT);t.putLong("next_due",nextDue);var list=new ListTag();installed.values().forEach(i->list.add(InstrumentMetadata.encode(i)));t.put("installed",list);var fittings=new ListTag();for(var f:legacy.values()){var e=new CompoundTag();e.putLong("host",f.key().host());e.putUUID("instance",f.instance());e.putLong("revision",f.revision());e.putString("type",f.type().name());fittings.add(e);}t.put("fittings",fittings);var revs=new ListTag();revisions.forEach((id,rev)->{var r=new CompoundTag();r.putLong("host",id);r.putLong("revision",rev);revs.add(r);});t.put("revisions",revs);var events=new ListTag();for(var p:pending.values()){var e=new CompoundTag();e.putUUID("request",p.request);e.putUUID("player",p.player);e.putLong("host",p.key.host());e.putString("mount",p.key.mount());e.putUUID("instance",p.instance);e.putLong("revision",p.expectedRevision);e.putBoolean("install",p.install);e.putString("type",p.type.name());if(p.settings!=null)e.put("settings",GaugeSettings.CODEC.encodeStart(NbtOps.INSTANCE,p.settings).getOrThrow());e.putString("dimension",p.dimension);e.putLong("position",p.position);e.putInt("slot",p.slot);e.putBoolean("offhand",p.offhand);events.add(e);}t.put("pending",events);return t;}
    private static ListTag list(CompoundTag t,String key){var value=t.get(key);if(!(value instanceof ListTag list)||!list.isEmpty()&&list.getElementType()!=10)throw new IllegalArgumentException("Invalid accessory list "+key);return list;}
    public static AccessoryStore load(CompoundTag t,HolderLookup.Provider ignored){return load(t);}
    public static AccessoryStore load(CompoundTag t){if(t.getInt("format")!=FORMAT||!t.contains("next_due",4)||!t.contains("installed",9)||!t.contains("revisions",9)||!t.contains("pending",9)||!t.contains("fittings",9))throw new IllegalArgumentException("Unsupported accessory storage format");var s=new AccessoryStore();s.nextDue(t.getLong("next_due"));var revs=list(t,"revisions");for(var v:revs){var r=(CompoundTag)v;long host=r.getLong("host"),rev=r.getLong("revision");if(host<=0||rev<=0||s.revisions.put(host,rev)!=null)throw new IllegalArgumentException("Invalid accessory revision");}for(var v:list(t,"installed")){var i=InstrumentMetadata.decode((CompoundTag)v);if(i.revision()>s.revision(i.key().host())||i.key().mount().startsWith("pipe_")&&!s.host(i.key().host()).isEmpty()||s.host(i.key().host()).stream().anyMatch(existing->existing.key().mount().startsWith("pipe_"))||s.installed.put(i.key(),i)!=null)throw new IllegalArgumentException("Invalid installed instrument");}for(var v:list(t,"fittings")){var e=(CompoundTag)v;var f=new Legacy(new Instrument.Key(e.getLong("host"),"pipe_accessory"),e.getUUID("instance"),e.getLong("revision"),Type.valueOf(e.getString("type")));if(f.revision()>s.revision(f.key().host())||!s.host(f.key().host()).isEmpty()||s.legacy.put(f.key().host(),f)!=null)throw new IllegalArgumentException("Invalid pipe fitting");}var events=list(t,"pending");if(events.size()>4096)throw new IllegalArgumentException("Too many accessory operations");for(var v:events){var e=(CompoundTag)v;for(String field:List.of("revision","position"))if(!e.contains(field,4))throw new IllegalArgumentException("Missing accessory operation field");for(String field:List.of("install","offhand"))if(!e.contains(field,1))throw new IllegalArgumentException("Missing accessory operation flag");if(!e.contains("slot",3))throw new IllegalArgumentException("Missing accessory slot");var p=new Pending(e.getUUID("request"),e.getUUID("player"),new Instrument.Key(e.getLong("host"),e.getString("mount")),e.getUUID("instance"),e.getLong("revision"),e.getBoolean("install"),Type.valueOf(e.getString("type")),e.contains("settings")?GaugeSettings.CODEC.parse(NbtOps.INSTANCE,e.get("settings")).getOrThrow():null,e.getString("dimension"),e.getLong("position"),e.getInt("slot"),e.getBoolean("offhand"));if(p.expectedRevision()>s.revision(p.key().host())||s.pending.values().stream().anyMatch(existing->existing.key().equals(p.key()))||s.pending.put(p.request,p)!=null)throw new IllegalArgumentException("Duplicate accessory operation");}return s;}
}
