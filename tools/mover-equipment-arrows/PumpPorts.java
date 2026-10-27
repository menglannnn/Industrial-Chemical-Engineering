package com.wormzjl.icheme.science.equipment;

/** Offline before/after connection fingerprint for the pump model refinement. */
public final class PumpPorts {
    public static void main(String[] args) {
        var design=UnitDesign.of(EquipmentShape.Kind.PUMP);
        for(int k=0;;k++) {
            double size=design.minSize()+k*design.sizeStep();
            if(size>design.maxSize()+1e-9)break;
            for(var heading:EquipmentShape.Heading.values()) {
                int count=design.minLength(size),least=design.minSupport(size,count);
                for(int support:new int[]{least,design.maxSupport()}) {
                    var shape=EquipmentShape.unit(EquipmentShape.Kind.PUMP,size,count,heading,support);
                    System.out.print(k+" "+heading+" "+support+" bounds="+shape.sizeX()+","+shape.sizeY()+","+shape.sizeZ());
                    for(var nozzle:shape.nozzles()) {
                        var face=shape.transform().point(nozzle.face());
                        System.out.print(" "+nozzle.role()+"="+Math.round(face[0]*1e9)+","+Math.round(face[1]*1e9)+","+Math.round(face[2]*1e9));
                    }
                    System.out.println(" ports="+ShapeRasterizer.ports(shape)+" fluid="+ShapeRasterizer.fluidCell(shape));
                }
            }
        }
    }
}
