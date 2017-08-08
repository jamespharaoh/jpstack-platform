package wbs.platform.daemon;

import static wbs.utils.time.TimeUtils.earlierThan;

import java.util.List;

import lombok.NonNull;

import org.joda.time.Duration;
import org.joda.time.Instant;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.utils.random.RandomLogic;

import info.faljse.SDNotify.SDNotify;

public
class DaemonRunner {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	RandomLogic randomLogic;

	// implementation

	public
	void runDaemon (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull List <String> arguments)
		throws InterruptedException {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"runDaemon");

		) {

			taskLogger.noticeFormat (
				"Daemon started");

			SDNotify.sendNotify ();

			try {

				Instant restartTime =
					Instant.now ().plus (
						restartFrequency.getMillis ()
						- restartFrequencyDeviation.getMillis ()
						+ randomLogic.randomInteger (
							restartFrequencyDeviation.getMillis () * 2));

				while (
					earlierThan (
						Instant.now (),
						restartTime)
				) {

					Thread.sleep (1000);

					SDNotify.sendWatchdog ();

				}

				taskLogger.noticeFormat (
					"Automatic periodic restart");

			} finally {

				taskLogger.noticeFormat (
					"Daemon shutting down");

			}

		}

	}

	// constants

	public final static
	Duration restartFrequency =
		Duration.standardHours (
			1l);

	public final static
	Duration restartFrequencyDeviation =
		Duration.standardMinutes (
			15l);

}
