package com.wormzjl.createcheme.science.thermo.reference;

import com.google.gson.JsonObject;

/**
 * CoolProp saturation ancillary (the {@code ANCILLARIES.pS/rhoL/rhoV} entries of a fluid file), used only as a
 * starting point for the density and saturation iterations. With theta = 1 - T / T_r and S = sum n_i theta^t_i:
 * the exponential types ({@code pL}, {@code pV}, {@code rhoL}, {@code rhoV}) give reducing_value exp(T_r / T S) when
 * {@code using_tau_r} is true and reducing_value exp(S) otherwise; the {@code *noexp} types give
 * reducing_value (1 + S).
 */
final class SaturationAncillary {
    private final boolean exponential;
    private final boolean usingTauR;
    private final double reducingTemperature;
    private final double reducingValue;
    private final double minimumTemperature;
    private final double maximumTemperature;
    private final double[] n;
    private final double[] t;

    private SaturationAncillary(boolean exponential, boolean usingTauR, double reducingTemperature,
                                double reducingValue, double minimumTemperature, double maximumTemperature,
                                double[] n, double[] t) {
        this.exponential = exponential;
        this.usingTauR = usingTauR;
        this.reducingTemperature = reducingTemperature;
        this.reducingValue = reducingValue;
        this.minimumTemperature = minimumTemperature;
        this.maximumTemperature = maximumTemperature;
        this.n = n;
        this.t = t;
    }

    static SaturationAncillary parse(JsonObject object) {
        String type = object.get("type").getAsString();
        boolean exponential = switch (type) {
            case "pL", "pV", "rhoL", "rhoV" -> true;
            case "rhoLnoexp", "rhoVnoexp" -> false;
            default -> throw new IllegalArgumentException("unsupported ancillary type " + type);
        };
        double[] n = JsonFluidFiles.doubles(object, "n");
        double[] t = JsonFluidFiles.doubles(object, "t");
        if (n.length != t.length) {
            throw new IllegalArgumentException("ancillary n and t differ in length");
        }
        boolean usingTauR = object.has("using_tau_r") && object.get("using_tau_r").getAsBoolean();
        return new SaturationAncillary(exponential, usingTauR, JsonFluidFiles.number(object, "T_r"),
                JsonFluidFiles.number(object, "reducing_value"), JsonFluidFiles.number(object, "Tmin"),
                JsonFluidFiles.number(object, "Tmax"), n, t);
    }

    /** The ancillary value at {@code temperature}; NaN at or above the ancillary's reducing temperature. */
    double evaluate(double temperature) {
        double theta = 1.0 - temperature / reducingTemperature;
        if (!(theta > 0.0)) {
            return Double.NaN;
        }
        double sum = 0.0;
        for (int i = 0; i < n.length; i++) {
            sum += n[i] * Math.pow(theta, t[i]);
        }
        if (!exponential) {
            return reducingValue * (1.0 + sum);
        }
        return reducingValue * Math.exp(usingTauR ? reducingTemperature / temperature * sum : sum);
    }

}
