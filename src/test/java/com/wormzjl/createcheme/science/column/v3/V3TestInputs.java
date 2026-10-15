package com.wormzjl.createcheme.science.column.v3;

import com.wormzjl.createcheme.science.column.v3.thermo.V3PengRobinsonThermo;
import java.util.List;

/** Shared fixtures of the V3 column tests. */
public final class V3TestInputs {
    /** The learned route at the shipped walls (16 iterations, two seconds) and the qualified progress rule. */
    static final V3InitializationOptions LEARNED = new V3InitializationOptions(V3InitializationOptions.Mode.LNN_FIRST,
            V3InitializationOptions.WetStart.AUTO, 16, 2_000, V3InitializationOptions.Correction.PROGRESS);

    private V3TestInputs() {}

    /**
     * The registered cdu17 PC03/PC10 binary (50 mol/s each) at 550 K: two interior trays, feed on tray 1,
     * 250 kPa top, 750 Pa per tray, a 300 K condenser, reflux ratio 2 and no reboiler duty.
     */
    static V3ColumnInput cdu17Binary(String assayId) {
        V3PengRobinsonThermo thermo = V3PengRobinsonThermo.fromRegisteredPackage("createcheme:cdu17_tjl_acs2018");
        double[] feed = new double[thermo.componentBasis().componentCount()];
        feed[6] = 50.0; // PC03
        feed[13] = 50.0; // PC10
        return new V3ColumnInput(V3ColumnInput.SCHEMA_VERSION, thermo.packageId(), assayId, thermo.componentBasis(),
                feed, 550.0, 2, 1, 250_000.0, 750.0, List.of(
                        new V3ColumnSpecification.CondenserOutletTemperature(300.0),
                        new V3ColumnSpecification.OrganicRefluxRatio(2.0),
                        new V3ColumnSpecification.ReboilerDuty(0.0)));
    }

    /** An accepted certificate without a state: no streams, duty ledger, tray hydraulics or inspection. */
    static V3ColumnResult stateless(V3ColumnProblem problem, V3InputDigest digest, V3AcceptanceAudit audit,
            V3ConvergenceEvidence evidence) {
        return new V3ColumnResult(problem, digest, audit, evidence, List.of(),
                V3ColumnCalculator.formulationRevision(problem.input(), problem.truncationSupport().cutoffMoleFraction()),
                null, null, null);
    }
}
