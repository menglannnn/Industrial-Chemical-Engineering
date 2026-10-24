package com.wormzjl.icheme.client.gui.common;

/** A presentation unit. Controls cross the network in their original SI quantity. */
public interface DisplayUnit {
    double display(double si);
    double si(double displayed);
}
