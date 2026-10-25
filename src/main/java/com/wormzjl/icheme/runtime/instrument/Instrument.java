package com.wormzjl.icheme.runtime.instrument;

import com.wormzjl.icheme.world.item.GaugeSettings;
import java.util.Objects;
import java.util.UUID;

/** Immutable persistent attachments and ephemeral published samples. */
public final class Instrument {
    private Instrument(){}
    public record Key(long host,String mount) {
        public Key{if(host<=0||mount==null||!java.util.Set.of("pipe_north","pipe_south","pipe_east","pipe_west","pipe_accessory","pressure_top","pressure_bottom","temperature_top","temperature_bottom").contains(mount))throw new IllegalArgumentException("Invalid instrument key");}
    }
    public record Installed(Key key,UUID instance,long revision,GaugeSettings settings) {
        public Installed{Objects.requireNonNull(key);Objects.requireNonNull(instance);Objects.requireNonNull(settings);if(revision<=0||key.mount().equals("pipe_accessory")||key.mount().startsWith("pressure_")&&settings.kind()!=GaugeSettings.Kind.PRESSURE||key.mount().startsWith("temperature_")&&settings.kind()!=GaugeSettings.Kind.TEMPERATURE)throw new IllegalArgumentException("Invalid instrument revision or mount kind");}
    }
    public enum Availability { AVAILABLE, NO_FLOW, UNAVAILABLE; public int code(){return switch(this){case AVAILABLE->0;case NO_FLOW->1;case UNAVAILABLE->2;};} public static Availability fromCode(int code){return switch(code){case 0->AVAILABLE;case 1->NO_FLOW;case 2->UNAVAILABLE;default->throw new IllegalArgumentException("Invalid instrument availability");};} }
    public record Sample(double value,long tick,Availability availability) {
        public Sample{Objects.requireNonNull(availability);if(tick<0||!Double.isFinite(value))throw new IllegalArgumentException("Invalid instrument sample");}
        public static Sample unavailable(long tick){return new Sample(0,tick,Availability.UNAVAILABLE);}
        public static Sample temperature(Double flowing,Sample previous,long tick){return flowing!=null&&Double.isFinite(flowing)?new Sample(flowing,tick,Availability.AVAILABLE):previous!=null&&previous.availability!=Availability.UNAVAILABLE?new Sample(previous.value,previous.tick,Availability.NO_FLOW):unavailable(tick);}
    }
}
