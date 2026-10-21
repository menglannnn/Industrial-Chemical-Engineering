package com.wormzjl.icheme.science.column.v3;

/** Superheated steam injected into one node; {@code stageCount + 1} addresses the sump. */
public record V3SteamFeedSpec(int stageNumber, double molarFlowMolPerSecond, double temperatureKelvin) {
    public V3SteamFeedSpec {
        if (stageNumber < 1 || !Double.isFinite(molarFlowMolPerSecond) || molarFlowMolPerSecond <= 0.0
                || !Double.isFinite(temperatureKelvin) || temperatureKelvin <= 0.0) {
            throw new IllegalArgumentException("V3 steam feeds require a positive stage and finite positive rate and temperature");
        }
    }

    // Shared free-water arithmetic. Water is a known vapor profile, not a MESH state component.

    static double[] nodeFeedFlows(V3ColumnInput input, V3ColumnTopology topology) {
        double[] feeds = new double[topology.nodeCount()];
        for (V3SteamFeedSpec feed : input.steamFeeds()) {
            feeds[feed.stageNumber()] = feed.molarFlowMolPerSecond();
        }
        return feeds;
    }

    static double[] upwardVaporProfile(double[] nodeFeedFlows, V3ColumnTopology topology) {
        double[] profile = new double[nodeFeedFlows.length];
        double upward = 0.0;
        for (int node = topology.reboilerNode(); node >= 1; node--) {
            upward += nodeFeedFlows[node];
            profile[node] = upward;
        }
        return profile;
    }

    static boolean hasSumpFeed(V3ColumnInput input) {
        return input.steamFeeds().stream().anyMatch(feed -> feed.stageNumber() == input.stageCount() + 1);
    }
}
