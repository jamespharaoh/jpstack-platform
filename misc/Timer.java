package wbs.platform.misc;

public
class Timer {

	long startTime;

	public
	Timer () {

		startTime =
			System.currentTimeMillis ();

	}

	public
	long split () {

		return
			+ System.currentTimeMillis ()
			- startTime;

	}

	public
	long lap () {

		long timeNow =
			System.currentTimeMillis ();

		long lapTime =
			+ timeNow
			- startTime;

		startTime =
			timeNow;

		return lapTime;

	}

}
