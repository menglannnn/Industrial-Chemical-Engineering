package com.wormzjl.icheme.science.material;

/** Test-scoped fixture context: the CDU-17 catalog with the production packages; does not modify the server/global catalog. */
public final class Cdu17FixtureExtension extends CatalogScopedExtension {
    public Cdu17FixtureExtension(){super(Cdu17TestCatalog::withProduction,Cdu17TestCatalog.PACKAGE_ID,true);}
}
