package com.commafeed.backend.task;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import com.commafeed.CommaFeedConfiguration;
import com.commafeed.backend.service.DatabaseCleaningService;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
public class OldEntriesCleanupTask extends ScheduledTask {

	private final CommaFeedConfiguration config;
	private final DatabaseCleaningService cleaner;

	@Override
	public void run() {
		int maxAgeDays = config.getApplicationSettings().getMaxEntriesAgeDays();
		if (maxAgeDays > 0) {
			Instant threshold = Instant.now().minus(Duration.ofDays(maxAgeDays));
			cleaner.cleanEntriesOlderThan(threshold);
		}
	}

	@Override
	public long getInitialDelay() {
		return 5;
	}

	@Override
	public long getPeriod() {
		return 60;
	}

	@Override
	public TimeUnit getTimeUnit() {
		return TimeUnit.MINUTES;
	}

}
