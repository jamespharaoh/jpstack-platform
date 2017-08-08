package wbs.platform.background.logic;

import org.joda.time.Duration;

import wbs.framework.logging.TaskLogger;

public
interface BackgroundProcessHelper {

	String threadName ();

	Boolean debugEnabled ();

	Duration frequency ();

	default
	Duration frequencyVariance () {

		return frequency ()
			.multipliedBy (1)
			.dividedBy (4);

	}

	Duration calculateFirstDelay ();

	Duration calculateSubsequentDelay ();

	boolean setBackgroundProcessStart (
			TaskLogger parentTaskLogger);

	void setBackgroundProcessStop (
			TaskLogger parentTaskLogger);

}
