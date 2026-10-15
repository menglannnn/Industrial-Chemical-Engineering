package com.wormzjl.createcheme.network;

import com.wormzjl.createcheme.science.material.*;
import com.wormzjl.createcheme.science.column.v3.*;
import com.wormzjl.createcheme.world.level.block.entity.ColumnCalculatorV3BlockEntity.*;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MaterialNamesCodecTest {
    @Test void serverNamingDescriptorsRoundTripWithoutClientCatalogLookup() throws Exception {
        var input=V3TestInputs.methaneCduInput();
        var descriptor=new MaterialName("custom_cut","custom.translation","Crude oil","petroleum_fraction",600.,650.,true);
        var state=new V3State(1,2,0,1,-1,V3Status.DIRTY,input,Optional.empty(),List.of(),Map.of("custom_cut",descriptor),V3EditorCatalog.EMPTY);
        assertEquals(state,ColumnCodecs.wireRoundTrip(state));
    }
    @Test void retiredAxesAreRejectedByTheActualNbtReader() throws Exception {
        var input=V3TestInputs.methaneCduInput();
        var tag=ColumnCodecs.writeInputNbt(input);
        var axis=tag.getList("Axis",Tag.TAG_STRING);
        for(int i=0;i<axis.size();i++) {
            String id=axis.getString(i);if(id.startsWith("crude_pc"))axis.set(i,StringTag.valueOf("tjl19_pc"+id.substring(8)));
        }
        assertInstanceOf(IllegalArgumentException.class,ColumnCodecs.failure(()->ColumnCodecs.readInputNbt(tag)));
    }
    @Test void descriptorsRejectInvalidBoundsAndOversizeLabels() {
        assertThrows(IllegalArgumentException.class,()->new MaterialName("id","key","x".repeat(129),"lump",null,null,false));
        assertThrows(IllegalArgumentException.class,()->new MaterialName("id","key","Crude oil","petroleum_fraction",600.,500.,true));
        assertThrows(IllegalArgumentException.class,()->new MaterialName("Ethane","key","Propane","chemical",null,null,false));
    }
}
