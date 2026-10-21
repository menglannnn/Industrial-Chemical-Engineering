package com.wormzjl.icheme.science.material;

/** Uses unchanged Java thermodynamic reference tables only for the old numerical state fixtures (test methods only). */
public final class TjlReferenceExtension extends CatalogScopedExtension {
    public TjlReferenceExtension(){super(com.wormzjl.icheme.science.column.v3.thermo.V3TjlReferenceCatalog::catalog,"icheme:tjl19_dwsim",false);}
}
