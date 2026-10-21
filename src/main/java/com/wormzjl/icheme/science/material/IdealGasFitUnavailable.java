package com.wormzjl.icheme.science.material;

import java.util.Objects;

/**
 * A consumer that reads the {@code shifted_polynomial_5} ideal-gas fits ({@link MaterialCatalog.Property#cp()}) was
 * given a package whose ideal gas is a reference spine (P3 WP5, decision D6: one ideal-gas source per species, so a
 * spine package's property records carry no fit). The V3 column is such a consumer until P8 moves it to the spine; it
 * refuses the package with this type instead of failing on an empty coefficient list.
 *
 * <p>It is an {@link IllegalArgumentException} (an unusable package, like any other refused package id); its own type
 * lets a caller tell "this package is for another consumer" from malformed data.</p>
 */
public final class IdealGasFitUnavailable extends IllegalArgumentException {
    private final String packageId;
    private final String consumer;

    public IdealGasFitUnavailable(String packageId, String consumer) {
        super("Package " + Objects.requireNonNull(packageId, "packageId") + " takes its ideal gas from a reference spine and "
                + "carries no shifted_polynomial_5 fits; " + Objects.requireNonNull(consumer, "consumer")
                + " reads the fits and cannot use it");
        this.packageId = packageId;
        this.consumer = consumer;
    }

    public String packageId() { return packageId; }
    /** What refused the package, for example "the V3 column". */
    public String consumer() { return consumer; }
}
