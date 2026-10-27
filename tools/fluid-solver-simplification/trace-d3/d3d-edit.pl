# t42b: form D3d on a form-D3c export of FluidThermodynamics.java (prints the edited file): bulk limit min(throat share, chi * acoustic).
local $/; my $s = <>; $s =~ s/\r//g;
my $old_bulk = <<'END';
    public double velocityLimit(State state){return Math.min(throatShare(state),isothermalAcousticBound(state));}
    /** {@code V / (V_gas/v_gas + V_condensed/v_max)}: the velocity at which the phases of {@code state}, moving together, fill
     * the throat, each phase taking the area its own volume flow needs at its own limit; the gas alone {@link #vaporVelocityLimit},
     * a state holding no gas the configured maximum. */
    double throatShare(State state) {
        double gas=state.vaporVolume(),condensed=state.liquidVolume()+state.waterVolume()+state.solidMoments().volume();
        if(!(gas>0))return maximumVelocity;
        double vapor=vaporVelocityLimit(state);
        return condensed>0?(gas+condensed)/(gas/vapor+condensed/maximumVelocity):vapor;
    }
END
my $new_bulk = <<'END';
    public double velocityLimit(State state) {
        // Form D3d (t42b): the throat share V/(V_gas/v_choke + V_condensed/v_max), bounded by the homogeneous mixture's own
        // choke, the gas's choke factor sqrt(gamma) K(gamma) times the mixture's isothermal acoustic bound; a state holding gas
        // alone is its gas choke on both terms, one holding no gas min(v_max, acoustic) as before form D.
        double acoustic=isothermalAcousticBound(state),gas=state.vaporVolume();
        if(!(gas>0))return Math.min(maximumVelocity,acoustic);
        double factor=vaporChokeFactor(state),vapor=vaporAcousticBound(state)*factor;
        double condensed=state.liquidVolume()+state.waterVolume()+state.solidMoments().volume();
        double share=condensed>0?(gas+condensed)/(gas/vapor+condensed/maximumVelocity):vapor;
        return Math.min(share,factor*acoustic);
    }
END
my $old_vap = <<'END';
    public double vaporVelocityLimit(State state) {
        double stiffness=state.waterPartialPressure(),gamma=4.0/3;
        if(state.vaporProperties()!=null) {
            var gas=state.vaporProperties();stiffness+=-gas.molarVolume()/gas.volumePressureDerivative();
END
my $new_vap = <<'END';
    public double vaporVelocityLimit(State state){return vaporAcousticBound(state)*vaporChokeFactor(state);}
    /** The vapour's isothermal acoustic bound {@code V_v/sqrt(M_v V_v/stiffness)}. */
    double vaporAcousticBound(State state) {
        double stiffness=state.waterPartialPressure();
        if(state.vaporProperties()!=null){var gas=state.vaporProperties();stiffness+=-gas.molarVolume()/gas.volumePressureDerivative();}
        return acoustic(state.vaporVolume(),vaporMass(state),state.vaporVolume()/stiffness);
    }
    /** {@code sqrt(gamma) K(gamma)}, K = (2/(gamma+1))^((gamma+1)/(2(gamma-1))), gamma = cp/cv of the vapour from the equation of
     * state (the steam alone, or a refused evaluation, 4/3). */
    double vaporChokeFactor(State state) {
        double gamma=4.0/3;
        if(state.vaporProperties()!=null) {
END
my $old_ret = <<'END';
        return acoustic(state.vaporVolume(),vaporMass(state),state.vaporVolume()/stiffness)*Math.sqrt(gamma)*DeterministicMath.pow(2/(gamma+1),(gamma+1)/(2*(gamma-1)));
END
my $new_ret = <<'END';
        return Math.sqrt(gamma)*DeterministicMath.pow(2/(gamma+1),(gamma+1)/(2*(gamma-1)));
END
for ([$old_bulk,$new_bulk],[$old_vap,$new_vap],[$old_ret,$new_ret]) { my ($o,$n)=@$_; my $i=index($s,$o); die "missing block: ".substr($o,0,60)."\n" if $i<0; substr($s,$i,length $o)=$n; }
print $s;
