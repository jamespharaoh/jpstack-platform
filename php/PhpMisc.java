package wbs.platform.php;

import static wbs.utils.etc.TypeUtils.classNameSimple;
import static wbs.utils.string.StringUtils.stringFormat;

import java.util.regex.Pattern;

public
class PhpMisc {

	private final static
	Pattern integerPattern =
		Pattern.compile (
			"-?\\d+");

	public static
	Object normaliseKey (
			String key) {

		return integerPattern.matcher (key).matches ()
			? Integer.parseInt (key)
			: key;

	}

	public static
	Object normaliseKey (
			Object key) {

		if (key == null)
			throw new NullPointerException();

		if (key instanceof Integer)
			return key;

		if (key instanceof String)
			return normaliseKey((String) key);

		throw new ClassCastException (
			key.getClass ().getName ());

	}

	public static
	PhpEntity asEntity (
			Object object) {

		if (object == null) {
			return PhpNull.instance;
		}

		if (object instanceof Object[]) {

		}

		throw new RuntimeException (
			stringFormat (
				"Don't know what to do with %s",
				classNameSimple (
					object.getClass ())));

	}

}
