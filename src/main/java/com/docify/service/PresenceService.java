package com.docify.service;

import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {
    // userId -> last heartbeat timestamp
    private final Map<Long, Instant> heartbeats = new ConcurrentHashMap<>();

    public void heartbeat(Long userId) {
        heartbeats.put(userId, Instant.now());
    }

    /** Online = heartbeat within last 5 minutes */
    public boolean isOnline(Long userId) {
        Instant last = heartbeats.get(userId);
        return last != null && Instant.now().minusSeconds(300).isBefore(last);
    }

    /** Returns seconds since last seen, or null if never seen */
    public Long secondsSince(Long userId) {
        Instant last = heartbeats.get(userId);
        if (last == null) return null;
        return Instant.now().getEpochSecond() - last.getEpochSecond();
    }
}
