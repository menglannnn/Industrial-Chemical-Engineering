package com.wormzjl.icheme.science.equipment;

import java.util.Objects;

/** Constructed-machine sound gain only; dimensions impose no hydraulic limits. */
public final class MoverSoundVolume {
    private MoverSoundVolume(){}
    public static float of(EquipmentShape shape){
        Objects.requireNonNull(shape);
        if(!shape.kind().mover())return 0;
        var design=UnitDesign.of(shape.kind());double size=(shape.diameter()-design.minSize())/(design.maxSize()-design.minSize());
        int least=design.minLength(shape.diameter()),most=design.maxLength(shape.diameter());
        if(most>least)size=(size+(shape.tangentLength()-least)/(most-least))/2;
        return (float)(.10+.50*Math.clamp(size,0,1));
    }
}
