package com.example.demo.service;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SnowflakeShortCodeGenerator implements ShortCodeGenerator {
    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final long NODE_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_NODE_ID = (1L << NODE_ID_BITS) - 1;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;
    private static final long NODE_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = NODE_ID_BITS + SEQUENCE_BITS;

    private final Clock clock;
    private final long customEpochMillis;
    private final long nodeId;

    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public SnowflakeShortCodeGenerator(
        Clock clock,
        @Value("${teenyurl.short-code.snowflake.node-id:0}") long nodeId,
        @Value("${teenyurl.short-code.snowflake.epoch-millis:1735689600000}") long customEpochMillis
    ) {
        if (nodeId < 0 || nodeId > MAX_NODE_ID) {
            throw new IllegalArgumentException("snowflake node-id must be between 0 and " + MAX_NODE_ID);
        }

        this.clock = clock;
        this.customEpochMillis = customEpochMillis;
        this.nodeId = nodeId;
    }

    @Override
    public synchronized String nextShortCode() {
        long timestamp = currentTimestamp();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards while generating a short code");
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                timestamp = waitForNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }

        lastTimestamp = timestamp;

        long snowflakeId = ((timestamp - customEpochMillis) << TIMESTAMP_SHIFT)
            | (nodeId << NODE_ID_SHIFT)
            | sequence;

        return encodeBase62(snowflakeId);
    }

    private long currentTimestamp() {
        return clock.millis();
    }

    private long waitForNextMillis(long previousTimestamp) {
        long timestamp = currentTimestamp();
        while (timestamp <= previousTimestamp) {
            timestamp = currentTimestamp();
        }
        return timestamp;
    }

    private String encodeBase62(long value) {
        if (value == 0) {
            return String.valueOf(BASE62.charAt(0));
        }

        StringBuilder encoded = new StringBuilder();
        long remaining = value;
        while (remaining > 0) {
            int index = (int) (remaining % BASE62.length());
            encoded.append(BASE62.charAt(index));
            remaining = remaining / BASE62.length();
        }
        return encoded.reverse().toString();
    }
}
