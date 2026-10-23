package com.wormzjl.icheme.client.gui.common;

/** Presentation time basis only. Network controls and process rates always use seconds. */
public enum FlowRateUnit {
    PER_SECOND(1,"/s"),PER_HOUR(3600,"/h");

    private final double seconds;
    private final String suffix;
    FlowRateUnit(double seconds,String suffix){this.seconds=seconds;this.suffix=suffix;}
    public double display(double perSecond){return perSecond*seconds;}
    public double perSecond(double displayed){return displayed/seconds;}
    public String unit(String quantity){return quantity+suffix;}
    public String suffix(){return suffix;}
    public FlowRateUnit next(){return this==PER_SECOND?PER_HOUR:PER_SECOND;}
}
