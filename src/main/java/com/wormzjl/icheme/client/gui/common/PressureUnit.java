package com.wormzjl.icheme.client.gui.common;

import java.util.Locale;

/** Preserve the existing 0.1 Pa gauge resolution in every selected unit. */
public enum PressureUnit implements DisplayUnit {
    PASCAL(1,"Pa",1),KILOPASCAL(1000,"kPa",4),BAR(100000,"bar",6),MEGAPASCAL(1000000,"MPa",7);
    private final double pascals;
    private final String symbol;
    private final int decimals;
    PressureUnit(double pascals,String symbol,int decimals){this.pascals=pascals;this.symbol=symbol;this.decimals=decimals;}
    @Override public double display(double si){return si/pascals;}
    @Override public double si(double displayed){return displayed*pascals;}
    public String symbol(){return symbol;}
    public PressureUnit next(){return switch(this){case PASCAL->KILOPASCAL;case KILOPASCAL->BAR;case BAR->MEGAPASCAL;case MEGAPASCAL->PASCAL;};}
    public String number(double si){
        if(!Double.isFinite(si))return "—";
        double steps=Math.rint(si/ProcessUi.PRESSURE_RESOLUTION);
        return String.format(Locale.ROOT,"%."+decimals+"f",display(steps==0?0:steps*ProcessUi.PRESSURE_RESOLUTION));
    }
    public String format(double si){return Double.isFinite(si)?number(si)+" "+symbol:"—";}
}
