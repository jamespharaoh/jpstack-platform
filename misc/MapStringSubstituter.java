package wbs.platform.misc;

import java.util.Map;

import wbs.utils.string.AbstractStringSubstituter;

public
class MapStringSubstituter
	extends AbstractStringSubstituter {

	private final
	Map<String,String> substitutions;

	private final
	boolean ignoreMissing;

	public
	MapStringSubstituter (
			Map <String, String> substitutions,
			boolean ignoreMissing) {

		this.substitutions =
			substitutions;

		this.ignoreMissing =
			ignoreMissing;

	}

	@Override
	protected
	String getSubstitute (
			String name) {

		String value = substitutions.get (
			name);

		if (value != null) {
			return value;
		} else if (ignoreMissing) {
			return "{" + name + "}";
		} else {
			return null;
		}

	}

	public static
	String substitute (
			String input,
			Map<String,String> map) {

		return new MapStringSubstituter (
				map,
				false)

			.substitute (
				input);

	}

	public static
	String substituteIgnoreMissing (
			String input,
			Map<String,String> map) {

		return new MapStringSubstituter (
				map,
				true)

			.substitute (
				input);

	}

}
