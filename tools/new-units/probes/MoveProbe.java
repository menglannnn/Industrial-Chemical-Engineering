package com.wormzjl.createcheme.science.equipment;

public class MoveProbe {
    public static void main(String[] args) {
        int bad = 0, n = 0;
        for (var shape : ProcessUnitTest.everySize()) {
            if (!ProcessUnit.moves(shape.kind())) continue;
            n++;
            try {
                ProcessUnitTest.movesAsStill(shape);
            } catch (Throwable t) {
                bad++;
                System.out.println(shape.kind() + " " + shape.diameter() + " x " + shape.tangentLength() + " s" + shape.support() + " " + shape.bank() + ": " + t.getMessage().substring(Math.max(0, t.getMessage().indexOf("==>"))));
            }
        }
        System.out.println(bad + " of " + n + " moving shapes fail");
    }
}
