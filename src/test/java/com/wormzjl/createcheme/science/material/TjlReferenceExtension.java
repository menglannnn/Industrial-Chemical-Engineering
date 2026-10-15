package com.wormzjl.createcheme.science.material;

/** Uses unchanged Java thermodynamic reference tables only for the old numerical state fixtures (test methods only). */
public final class TjlReferenceExtension extends CatalogScopedExtension {
    public TjlReferenceExtension(){super(com.wormzjl.createcheme.science.column.v3.thermo.V3TjlReferenceCatalog::catalog,"createcheme:tjl19_dwsim",false);}
}
