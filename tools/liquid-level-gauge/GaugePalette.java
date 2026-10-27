package com.wormzjl.icheme.runtime.fluid;

import com.wormzjl.icheme.science.material.MaterialCatalog;
import com.wormzjl.icheme.science.material.MaterialPresets;

/** Actual tank appearance reducer on a specified WTI-composition liquid, plus the catalog's water tint. No flash. */
public final class GaugePalette {
    public static int[] colours() {
        var catalog=MaterialCatalog.bundled();var p=catalog.requirePackage("icheme:wti_light_export_tjl20");
        var n=MaterialPresets.moleFractions(p,"icheme:wti_light_export");
        double[] weights=p.properties().stream().mapToDouble(MaterialCatalog.Property::molecularWeight).toArray();
        int oil=TankDisplay.mix(n,p.components(),weights,catalog,p.id());
        int water=catalog.componentAppearance(p.id(),"Water").argb();
        System.out.printf("TankDisplay.mix WTI-composition liquid=%08X, catalog water=%08X%n",oil,water);
        return new int[]{oil,water};
    }
}
