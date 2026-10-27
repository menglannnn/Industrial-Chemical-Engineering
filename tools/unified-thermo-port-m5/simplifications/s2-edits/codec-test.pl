# FluidCheckpointCodecTest for S2: no fallback allowance or approximation anchor in the island unit. LF file.
undef $/; my $s=<>;
$s=~s/new IslandClock\.Snapshot\(350,200,400,100\),new FallbackAllowance\(2,100,100\),Optional\.of\(ApproximationAnchor\.fromFull\(model,full\)\),Optional\.of\(full\),"APPROXIMATE",Map\.of\(UUID\.randomUUID\(\),300L\)\);/new IslandClock.Snapshot(350,200,400,100),Optional.of(full),"FULL",Map.of(UUID.randomUUID(),300L));/ or die "1";
$s=~s/    \@Test void roundTripRetainsStockDebtFencesAndFallback\(\) \{/    \@Test void roundTripRetainsStockDebtAndFences() {/ or die "2";
$s=~s/assertEquals\(a\.fences\(\),b\.fences\(\)\);assertEquals\(a\.allowance\(\),b\.allowance\(\)\);\n        assertEquals\(a\.anchor\(\)\.orElseThrow\(\)\.propertyRevision\(\),b\.anchor\(\)\.orElseThrow\(\)\.propertyRevision\(\)\);assertEquals\(a\.revision\(\),b\.revision\(\)\);assertEquals\(a\.status\(\),b\.status\(\)\);\n        assertSame\(b\.graph\(\),b\.anchor\(\)\.orElseThrow\(\)\.graph\(\),"the anchor is stored as the island's own graph"\);\n/assertEquals(a.fences(),b.fences());\n        assertEquals(a.revision(),b.revision());assertEquals(a.status(),b.status());\n/ or die "3";
my $velocity=join "\n",
'    /** A saved island stays readable under another velocity limit: the limit is in the model\'s revision (a certificate',
'     * issued under the old one is not kept, FluidCheckpointFormatTest) but not in its thermodynamic one, which the',
'     * package table holds. Restated without the approximation anchor (owner decision D37, S2). */',
'    @Test void changingVelocityLimitPreservesSavedStockAndClocks() {',
'        var before=fixture();var image=FluidCheckpointCodec.encode(before,key->model);',
'        var loaded=FluidCheckpointCodec.decode(image,key->FluidThermodynamics.forNetwork(MaterialCatalog.bundled(),PACKAGE,10, FluidThermodynamics.DEFAULT_TRACE_CUTOFF_MOLE_FRACTION));',
'        var a=before.islands().getFirst().snapshot();var b=loaded.islands().getFirst().snapshot();',
'        assertEquals(a.graph().reservoirs().getFirst().inventory(),b.graph().reservoirs().getFirst().inventory());assertEquals(a.clock(),b.clock());assertEquals(a.fences(),b.fences());',
'    }',
'';
$s=~s/    \@Test void changingVelocityLimitPreservesSavedStockClocksAndAllowanceButInvalidatesTheOldAnchor\(\) \{\n.*?\n    \}\n/$velocity/s or die "4";
$s=~s/     \* endpoint modes and the anchor come back exactly, and the reloaded checkpoint re-encodes to the same bytes\./     * endpoint modes come back exactly, and the reloaded checkpoint re-encodes to the same bytes./ or die "5";
$s=~s/new IslandClock\.Snapshot\(350,200,400,100\),new FallbackAllowance\(2,100,100\),Optional\.of\(ApproximationAnchor\.fromFull\(model,full\)\),Optional\.of\(full\),"FULL"\);/new IslandClock.Snapshot(350,200,400,100),Optional.of(full),"FULL");/ or die "6";
$s=~s/\n        assertEquals\(full\.graph\(\)\.pipes\(\),after\.anchor\(\)\.orElseThrow\(\)\.graph\(\)\.pipes\(\)\);\n/\n/ or die "7";
print $s;
