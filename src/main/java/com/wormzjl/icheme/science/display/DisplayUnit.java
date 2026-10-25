package com.wormzjl.icheme.science.display;

import java.util.Locale;

/** A presentation unit. Controls cross the network in their original SI quantity. */
public interface DisplayUnit {
    double display(double si);
    double si(double displayed);

    /** Pressure units; every one preserves the existing 0.1 Pa gauge resolution. */
    enum PressureUnit implements DisplayUnit {
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
            double steps=Math.rint(si/0.1);
            return String.format(Locale.ROOT,"%."+decimals+"f",display(steps==0?0:steps*0.1));
        }
        public String format(double si){return Double.isFinite(si)?number(si)+" "+symbol:"—";}
    }

    /** Presentation time basis only. Network controls and process rates always use seconds. */
    enum FlowRateUnit implements DisplayUnit {
        PER_SECOND(1,"/s"),PER_HOUR(3600,"/h");

        private final double seconds;
        private final String suffix;
        FlowRateUnit(double seconds,String suffix){this.seconds=seconds;this.suffix=suffix;}
        public double display(double perSecond){return perSecond*seconds;}
        public double perSecond(double displayed){return displayed/seconds;}
        @Override public double si(double displayed){return perSecond(displayed);}
        public String unit(String quantity){return quantity+suffix;}
        public String suffix(){return suffix;}
        public FlowRateUnit next(){return this==PER_SECOND?PER_HOUR:PER_SECOND;}
    }
}
