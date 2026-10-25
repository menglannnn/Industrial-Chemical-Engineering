package com.wormzjl.icheme.science.equipment;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MoverSoundVolumeTest {
    private static final EquipmentShape.Kind[] KINDS={EquipmentShape.Kind.PUMP,EquipmentShape.Kind.COMPRESSOR,EquipmentShape.Kind.CENTRIFUGAL_COMPRESSOR};
    @Test void gainIsBoundedAndIncreasesWithConstructorDiameterAndStageOrThrowCount(){
        for(var kind:KINDS){
            var design=UnitDesign.of(kind);float previous=0;
            int steps=(int)Math.round((design.maxSize()-design.minSize())/design.sizeStep());
            for(int i=0;i<=steps;i++){
                double size=Math.round((design.minSize()+i*design.sizeStep())*1e9)/1e9;
                float gain=UnitDesign.soundGain(shape(kind,size,design.minLength(size),EquipmentShape.Heading.NORTH));
                assertTrue(gain>=.10F&&gain<=.60F);assertTrue(gain>previous);previous=gain;
            }
            assertEquals(.10F,UnitDesign.soundGain(shape(kind,design.minSize(),design.minLength(design.minSize()),EquipmentShape.Heading.NORTH)),1e-6);
            assertEquals(.60F,UnitDesign.soundGain(shape(kind,design.maxSize(),design.maxLength(design.maxSize()),EquipmentShape.Heading.NORTH)),1e-6);
            double middle=Math.round((design.minSize()+design.maxSize())/2/design.sizeStep())*design.sizeStep();previous=0;
            for(int count=design.minLength(middle);count<=design.maxLength(middle);count+=design.lengthStep()){
                float gain=UnitDesign.soundGain(shape(kind,middle,count,EquipmentShape.Heading.NORTH));assertTrue(gain>previous);previous=gain;
            }
        }
    }
    @Test void foundationAndHeadingDoNotMakeTheMachineLouder(){
        for(var kind:KINDS){
            var design=UnitDesign.of(kind);double size=design.minSize();int count=design.minLength(size);
            var original=shape(kind,size,count,EquipmentShape.Heading.NORTH);
            var raised=EquipmentShape.unit(kind,size,count,EquipmentShape.Heading.SOUTH,design.maxSupport());
            assertEquals(UnitDesign.soundGain(original),UnitDesign.soundGain(raised));
        }
        assertEquals(0,UnitDesign.soundGain(EquipmentShape.drum(2,4,EquipmentShape.Orientation.VERTICAL,EquipmentShape.Heading.NORTH,4)));
        // The pump range ends at 0.6 m (its suction a pipe's largest, 0.8 m): a 0.4 m pump sits 0.35 / 0.55 along it.
        assertEquals(.10+.50*.35/.55,UnitDesign.soundGain(shape(EquipmentShape.Kind.PUMP,.4,1,EquipmentShape.Heading.NORTH)),1e-6);
    }
    private static EquipmentShape shape(EquipmentShape.Kind kind,double size,int count,EquipmentShape.Heading front){return EquipmentShape.unit(kind,size,count,front,UnitDesign.of(kind).defaultSupport(size,count));}
}
