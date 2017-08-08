package wbs.platform.php;

public
class PhpBoolean
	extends AbstractPhpEntity {

	private final
	boolean value;

	private
	PhpBoolean (
			boolean newValue) {

		super (
			PhpType.pBoolean);

		value =
			newValue;

	}

	@Override
	public
	Boolean asBoolean () {

		return value;

	}

	@Override
	public
	Long asInteger () {

		return value
			? 1l
			: 0l;

	}

	@Override
	public
	Double asDouble () {

		return value
			? 1.0
			: 0.0;

	}

	@Override
	public
	String asString () {

		return value
			? "true"
			: "false";

	}

	@Override
	public
	String asString (
			String charset) {

		return value
			? "true"
			: "false";

	}

	@Override
	public
	Object asObject () {

		return value;

	}

	public final static
	PhpBoolean pTrue =
		new PhpBoolean (true);

	public final static
	PhpBoolean pFalse =
		new PhpBoolean (false);

	public static
	PhpBoolean valueOf (
			boolean bool) {

		return bool
			? pTrue
			: pFalse;

	}

}
