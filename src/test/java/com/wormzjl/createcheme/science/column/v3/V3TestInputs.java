package com.wormzjl.createcheme.science.column.v3;

import java.util.List;

/** Shared fixtures of the V3 column tests. */
public final class V3TestInputs {
    /** The learned route at the shipped walls (16 iterations, two seconds) and the qualified progress rule. */
    static final V3InitializationOptions LEARNED = new V3InitializationOptions(V3InitializationOptions.Mode.LNN_FIRST,
            V3InitializationOptions.WetStart.AUTO, 16, 2_000, V3InitializationOptions.Correction.PROGRESS);

    private V3TestInputs() {}

    /** An accepted certificate without a state: no streams, duty ledger, tray hydraulics or inspection. */
    static V3ColumnResult stateless(V3ColumnProblem problem, V3InputDigest digest, V3AcceptanceAudit audit,
            V3ConvergenceEvidence evidence) {
        return new V3ColumnResult(problem, digest, audit, evidence, List.of(),
                V3ColumnCalculator.formulationRevision(problem.input(), problem.truncationSupport().cutoffMoleFraction()),
                null, null, null);
    }
}
