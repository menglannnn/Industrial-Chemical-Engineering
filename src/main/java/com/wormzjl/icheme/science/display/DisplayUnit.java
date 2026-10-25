package com.wormzjl.icheme.science.display;

/** A presentation unit. Controls cross the network in their original SI quantity. */
public interface DisplayUnit {
    double display(double si);
    double si(double displayed);
}
