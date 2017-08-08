package wbs.platform.graph.console;

import static wbs.utils.etc.NumberUtils.toJavaIntegerRequired;
import static wbs.utils.string.StringUtils.joinWithFullStop;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.LongStream;

public
class GraphScale {

	/** Size of each step, divided by multiplier */
	private final
	long stepSize;

	/** Number of steps */
	private final
	long numSteps;

	/** Number of places to shift scale */
	private final
	long places;

	/** Multiplier, equal to 10 ^ places */
	private final
	long multiplier;

	/** List of Steps as iteratable */
	private final
	List <Step> steps;

	/** Private constructor */
	private
	GraphScale (
			long newStepSize,
			long newNumSteps,
			long newPlaces,
			long newMultiplier) {

		stepSize = newStepSize;
		numSteps = newNumSteps;
		places = newPlaces;
		multiplier = newMultiplier;

		Step[] stepsSource =
			new Step [
				toJavaIntegerRequired (
					numSteps + 1)];

		for (
			int step = 0;
			step <= numSteps;
			step ++
		) {

			stepsSource [step] =
				new Step (step);

		}

		steps =
			Collections.unmodifiableList (
				Arrays.asList (
					stepsSource));

	}

	public
	long getStepSize () {
		return stepSize;
	}

	public
	long getNumSteps () {
		return numSteps;
	}

	public
	long getPlaces () {
		return places;
	}

	public
	long getMultiplier () {
		return multiplier;
	}

	public
	List <Step> getSteps () {
		return steps;
	}

	private static
	long iPow (
			long input,
			long power) {

		if (power < 0) {

			throw new IllegalArgumentException (
				"power must be >= 0");

		}

		if (power == 0)
			return 1;

		long value = input;

		for (power--; power > 0; power--)
			value *= input;

		return value;

	}

	/**
	 * Creates a GraphScale object to match the given range. The range is worked
	 * out as inputMax / 10 ^ inputPlaces.
	 */
	public static
	GraphScale setScale (
			long inputMax,
			long inputPlaces) {

		// if there is no maximum, just use a default scale

		if (inputMax == 0) {

			return new GraphScale (
				1,
				5,
				0,
				1);

		}

		// adjust to our working scale with a max between 10 and 100. realMax =
		// workingMax * workingMultiplier /
		// workingDivider.

		long max = inputMax;
		long multiplier = iPow(10, inputPlaces);
		long places = inputPlaces;
		long divider = 1;

		while (max <= 100) {
			max *= 10;
			multiplier *= 10;
			places++;
		}
		while (max > 1000) {
			max /= 10;
			divider *= 10;
		}

		// pick from the various pre-defined steps for the scale
		int scale, numSteps;
		if (max <= 125) {
			scale = 25;
			numSteps = 5;
		} else if (max <= 150) {
			scale = 25;
			numSteps = 6;
		} else if (max <= 200) {
			scale = 50;
			numSteps = 4;
		} else if (max <= 250) {
			scale = 50;
			numSteps = 5;
		} else if (max <= 300) {
			scale = 50;
			numSteps = 6;
		} else if (max <= 400) {
			scale = 100;
			numSteps = 4;
		} else if (max <= 500) {
			scale = 100;
			numSteps = 5;
		} else if (max <= 600) {
			scale = 100;
			numSteps = 6;
		} else if (max <= 800) {
			scale = 200;
			numSteps = 4;
		} else {
			scale = 200;
			numSteps = 5;
		}

		// create the GraphScale, factoring out a divisor if present

		return new GraphScale (
			scale * divider, // multiply up to real units
			numSteps,
			places,
			multiplier);

	}

	public
	class Step {

		private final
		int step;

		private final
		String label;

		private final
		boolean odd;

		private
		Step (
				int newStep) {

			step =
				newStep;

			label =
				shiftString (
					Long.toString (
						stepSize * step),
					places);

			odd =
				step % 2 == 1;

		}

		public
		int getStep () {
			return step;
		}

		public
		boolean isFirst () {
			return step == 0;
		}

		public
		boolean isLast () {
			return step == numSteps;
		}

		public
		boolean isEven () {
			return !odd;
		}

		public
		boolean isOdd () {
			return odd;
		}

		public
		String getLabel () {
			return label;
		}

	}

	/**
	 * Given a positive integer string, and a number of places, will shift the
	 * number to the right. Eg ("20", 2) would give "0.2".
	 */
	private static
	String shiftString (
			String source,
			long places) {

		if (source.length () <= places) {

			StringBuilder stringBuilder =
				new StringBuilder ();

			stringBuilder.append (
				"0.");

			long numZeros =
				places - source.length ();

			LongStream.range (0l, numZeros).forEach (
				index ->
					stringBuilder.append (
						'0'));

			stringBuilder.append (
				source);

			String target =
				stringBuilder.toString ();

			return target.replaceAll (
				"\\.?0*$",
				"");

		} else {

			String target =
				joinWithFullStop (
					source.substring (
						0,
						toJavaIntegerRequired (
							source.length () - places)),
					source.substring (
						toJavaIntegerRequired (
							source.length () - places)));

			return target.replaceAll (
				"\\.?0*$",
				"");

		}

	}

}
