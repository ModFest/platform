package net.modfest.platform.pojo;

import java.time.Instant;

public record HealthData(String health, Instant runningSince) {
}
