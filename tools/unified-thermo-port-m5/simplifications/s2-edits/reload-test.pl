# PropertyReloadCoordinatorTest for S2: the reload no longer has an anchor to invalidate or an allowance to keep. CRLF.
undef $/; my $s=<>;
$s=~s/new IslandClock\.Snapshot\(300,100,0,100\),new FallbackAllowance\(2,100,100\),Optional\.of\(ApproximationAnchor\.fromFull\(model,full\)\),Optional\.of\(full\),"READY"/new IslandClock.Snapshot(300,100,0,100),Optional.of(full),"READY"/ or die "1";
$s=~s/assertEquals\(original\.allowance\(\),saved\.allowance\(\)\);assertEquals\(original\.fences\(\),saved\.fences\(\)\);/assertEquals(original.fences(),saved.fences());/ or die "2";
$s=~s/\r\n            assertNotEquals\(model\.revision\(\),saved\.anchor\(\)\.orElseThrow\(\)\.propertyRevision\(\)\);\r\n/\r\n/ or die "3";
$s=~s/assertEquals\(saved\.allowance\(\),restored\.allowance\(\)\);assertEquals\(saved\.clock\(\),restored\.clock\(\)\);assertEquals\(saved\.anchor\(\)\.orElseThrow\(\)\.propertyRevision\(\),restored\.anchor\(\)\.orElseThrow\(\)\.propertyRevision\(\)\);/assertEquals(saved.clock(),restored.clock());assertEquals(saved.status(),restored.status());/ or die "4";
$s=~s/\r\n        for\(var command:commands\.values\(\)\)if\(command\.fallback\(\)\.enabled\(\)\)assertEquals\(originals\.get\(1L\)\.allowance\(\),command\.fallback\(\)\.allowance\(\)\);\r\n/\r\n/ or die "5";
$s=~s/\r\n            assertNotEquals\(model\.revision\(\),command\.fallback\(\)\.anchor\(\)\.orElseThrow\(\)\.propertyRevision\(\)\);\r\n/\r\n/ or die "6";
$s=~s/assertEquals\(200,coordinator\.snapshot\(1\)\.clock\(\)\.committedTick\(\)\);assertEquals\(FallbackAllowance\.NONE,coordinator\.snapshot\(1\)\.allowance\(\)\);/assertEquals(200,coordinator.snapshot(1).clock().committedTick());/ or die "7";
print $s;
