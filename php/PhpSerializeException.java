package wbs.platform.php;

import static wbs.utils.string.StringUtils.stringFormat;

public
class PhpSerializeException
	extends RuntimeException {

	private final
	Class<?> offendingClass;

	public
	PhpSerializeException (
			Class<?> newOffendingClass) {

		super (
			stringFormat (
				"Don't know how to serialize %s",
				newOffendingClass.getName ()));

		offendingClass =
			newOffendingClass;

	}

	public
	Class<?> getOffendingClass () {
		return offendingClass;
	}

}
