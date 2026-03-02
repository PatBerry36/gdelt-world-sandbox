package org.example.gdelt;

public record GdeltEventMarker(
        String globalEventId,
        String timestampUtc,
        String actor1Name,
        String actor2Name,
        String eventCode,
        String avgTone,
        int numMentions,
        int numSources,
        double latitude,
        double longitude,
        String sourceUrl
) {
}
