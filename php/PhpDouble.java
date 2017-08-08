package wbs.platform.php;

import static wbs.utils.etc.NumberUtils.roundToIntegerRequired;
import static wbs.utils.string.StringUtils.stringToBytes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

class PhpDouble
	extends AbstractPhpEntity {

	private
	double value;

	public
	PhpDouble (
			double newValue) {

		super (
			PhpType.pFloat);

		value =
			newValue;

	}

	@Override
	public
	Boolean asBoolean () {

		return value != 0.0;

	}

	@Override
	public
	Long asInteger () {

		return roundToIntegerRequired (
			value);

	}

	@Override
	public
	Long asLong () {

		return roundToIntegerRequired (
			value);

	}

	@Override
	public
	Double asDouble () {

		return value;

	}

	@Override
	public
	String asString () {

		return Double.toString (value);

	}

	@Override
	public
	String asString (
			String encoding) {

		return Double.toString (value);

	}

	@Override
	public
	byte[] asByteArray () {

		return stringToBytes (
			Double.toString (value),
			"iso-8859-1");

	}

	@Override
	public
	Map<Object,PhpEntity> asMap () {

		return ImmutableMap.<Object,PhpEntity>builder ()
			.put (0, this)
			.build ();

	}

	@Override
	public
	List<PhpEntity> asList () {

		List<PhpEntity> ret =
			new ArrayList<PhpEntity> ();

		ret.add (this);

		return ret;

	}

	@Override
	public
	Map<Object,PhpEntity> asObjectMap () {

		return ImmutableMap.<Object,PhpEntity>builder ()
			.put ("scalar", this)
			.build ();

	}

	@Override
	public
	Object asObject () {

		return value;

	}

	@Override
	public
	int hashCode () {

		return new Double (value)
			.hashCode ();

	}

	@Override
	public
	boolean equals (
			Object object) {

		if (! (object instanceof PhpDouble))
			return false;

		PhpDouble other =
			(PhpDouble) object;

		return value == other.value;

	}

}
