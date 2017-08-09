package wbs.platform.php;

import static wbs.utils.etc.NumberUtils.toJavaIntegerRequired;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

class PhpInteger
	extends AbstractPhpEntity {

	private
	Long value;

	public
	PhpInteger (
			Long newValue) {

		super (
			PhpType.pInteger);

		value =
			newValue;

	}

	@Override
	public
	Boolean asBoolean () {

		return value != 0;

	}

	@Override
	public
	Long asInteger () {

		return value;

	}

	@Override
	public
	Long asLong () {

		return value;

	}

	@Override
	public
	Double asDouble () {

		return (double) value;

	}

	@Override
	public
	String asString () {

		return Long.toString (
			value);

	}

	@Override
	public
	String asString (
			String encoding) {

		return Long.toString (
			value);

	}

	@Override
	public
	byte[] asByteArray () {

		try {

			return Long.toString (
				value
			).getBytes (
				"iso-8859-1");

		} catch (UnsupportedEncodingException exception) {

			throw new RuntimeException (
				exception);

		}

	}

	@Override
	public
	Map<Object,PhpEntity> asMap () {

		return ImmutableMap.<Object,PhpEntity>of (
			0,
			this);

	}

	@Override
	public
	List<PhpEntity> asList () {

		return ImmutableList.<PhpEntity>of (
			this);

	}

	@Override
	public
	Map<Object,PhpEntity> asObjectMap () {

		return ImmutableMap.<Object,PhpEntity>of (
			"scalar",
			this);

	}

	@Override
	public
	Object asObject () {

		return value;

	}

	@Override
	public
	int hashCode () {

		return toJavaIntegerRequired (
			value);

	}

	@Override
	public
	boolean equals (
			Object object) {

		if (! (object instanceof PhpInteger)) {
			return false;
		}

		PhpInteger other =
			(PhpInteger) object;

		return value == other.value;

	}

	public final static
	PhpInteger zero =
		new PhpInteger (
			0l);

	public final static
	PhpInteger one =
		new PhpInteger (
			1l);

}
