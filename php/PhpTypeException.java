package wbs.platform.php;

import static wbs.utils.string.StringUtils.stringFormat;

import lombok.Getter;

public
class PhpTypeException
	extends UnsupportedOperationException {

	private static final
	long serialVersionUID =
		-8118217792629090077L;

	@Getter
	private final
	PhpType type;

	@Getter
	private final
	String conversion;

	public
	PhpTypeException (
			PhpType newType,
			String newConversion) {

		super (
			stringFormat (
				"PHP type %s does not support %s",
				newType.toString (),
				newConversion));

		type =
			newType;

		conversion =
			newConversion;

	}

}
