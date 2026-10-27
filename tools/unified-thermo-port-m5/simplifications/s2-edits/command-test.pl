# FluidIslandCommandTest for S2: the soft-timeout test is restated to the full solve that now runs past what was the
# soft budget; the anchor-renewal test goes. CRLF file.
undef $/; my $s=<>;
my $new=join "\r\n",
'    /**',
'     * A job that runs past what was the soft budget is the full solve, on the whole wall budget: the clock advances 1 ns',
'     * per read, so the old 1 ns soft budget cut this job at its first checkpoint and started the approximate fallback.',
'     * Owner decision D37 (S2) removed that mode, so the job is the same computation as a solve with no clock at all.',
'     */',
'    @Test void aJobPastTheOldSoftBudgetIsTheFullSolveOnTheWholeWallBudget() {',
'        var model=FluidTestSupport.networkModel();',
'        var graph=new PassiveNetwork(List.of(new PassiveNetwork.Reservoir(1,0,model.initialNitrogenCharge(1,350,150000,()->{}),PassiveNetwork.NodeKind.GENERATOR),',
'                new PassiveNetwork.Reservoir(2,0,model.initialNitrogenCharge(1,350,149000,()->{}))),List.of(new PassiveNetwork.Pipe(1,0,1,new PipeResistance.Geometry(100,.01,.000045,0))));',
'        graph=new PassiveIntervalSolver(model).solve(graph,.05,PassiveIntervalSolver.Settings.defaults(),()->{}).graph();',
'        var command=new ProcessSolveServices.FluidIslandCommand(model,graph,5,PassiveIntervalSolver.Settings.defaults(),1_000_000);',
'        var clock=new AtomicLong();var result=assertInstanceOf(ProcessSolveServices.FluidIslandSolveResult.class,command.solve(token(false),clock::getAndIncrement));',
'        assertEquals("FULL",result.detail());assertTrue(clock.get()>1,"the job read the clock past the old soft budget");',
'        var reference=new PassiveIntervalSolver(model).solve(graph,5,PassiveIntervalSolver.Settings.defaults(),()->{});',
'        assertTrue(reference.averageMassFlows()[0]>0);',
'        assertArrayEquals(reference.averageMassFlows(),result.candidate().orElseThrow().averageMassFlows());',
'        assertEquals(reference.graph().reservoirs().get(1).inventory(),result.candidate().orElseThrow().graph().reservoirs().get(1).inventory());',
'    }',
'';
$s=~s/    \@Test void aSoftTimeoutCanProduceAUsefulConservativeApproximationAndThenRecover\(\) \{\r\n.*?\r\n    \}\r\n/$new/s or die "1";
$s=~s/    \@Test void oldHydraulicAnchorCannotTriggerSoftFallbackAndIsRenewedOnlyByAFullSolve\(\) \{\r\n.*?\r\n    \}\r\n//s or die "2";
print $s;
