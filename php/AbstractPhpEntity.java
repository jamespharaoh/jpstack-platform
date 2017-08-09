package wbs.platform.php;

import static wbs.utils.etc.EnumUtils.enumInSafe;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

public abstract
class AbstractPhpEntity
	implements PhpEntity {

	protected final
	PhpType type;

	protected
	AbstractPhpEntity (
			PhpType newType) {

		if (newType == null)
			throw new NullPointerException ();

		type =
			newType;

	}

	@Override
	public
	PhpType getType () {

		return type;

	}

	@Override
	public
	boolean isBoolean () {

		return type == PhpType.pBoolean;

	}

	@Override
	public
	boolean isInteger () {

		return type == PhpType.pInteger;

	}

	@Override
	public
	boolean isFloat () {

		return type == PhpType.pFloat;

	}

	@Override
	public
	boolean isString () {

		return type == PhpType.pString;

	}

	@Override
	public
	boolean isArray () {

		return type == PhpType.pArray;

	}

	@Override
	public
	boolean isObject () {

		return type == PhpType.pObject;

	}

	@Override
	public
	boolean isNull () {

		return type == PhpType.pNull;

	}

	@Override
	public
	boolean isNumber () {

		return enumInSafe (
			type,
			PhpType.pInteger,
			PhpType.pFloat);

	}

	@Override
	public
	boolean isScalar () {

		return enumInSafe (
			type,
			PhpType.pBoolean,
			PhpType.pInteger,
			PhpType.pFloat,
			PhpType.pString);

	}

	@Override
	public
	boolean isNormalArray () {

		return false;

	}

	@Override
	public
	Boolean asBoolean () {

		throw new PhpTypeException (
			type,
			"asBoolean()");

	}

	@Override
	public
	Long asInteger () {

		throw new PhpTypeException (
			type,
			"asInteger()");

	}

	@Override
	public
	Long asLong () {

		throw new PhpTypeException (
			type,
			"asLong()");

	}

	@Override
	public
	Double asDouble () {

		throw new PhpTypeException (
			type,
			"asFloat()");

	}

	@Override
	public
	String asString () {

		throw new PhpTypeException (
			type,
			"asString()");

	}

	@Override
	public
	String asString (
			String charset)
		throws UnsupportedEncodingException {

		throw new PhpTypeException (
			type,
			"asString(charset)");

	}

	@Override
	public
	String asStringUtf8 () {

		try {

			return asString (
				"utf-8");

		} catch (UnsupportedEncodingException exception) {

			throw new RuntimeException (
				exception);

		}

	}

	@Override
	public
	byte[] asByteArray () {

		throw new PhpTypeException (
			type,
			"asByteArray()");

	}

	@Override
	public
	PhpEntity[] asArray () {

		throw new PhpTypeException (
			type,
			"asArray()");

	}

	@Override
	public
	List<PhpEntity> asList () {

		throw new PhpTypeException (
			type,
			"asList()");

	}

	@Override
	public
	Map<Object,PhpEntity> asMap () {

		throw new PhpTypeException (
			type,
			"asMap()");

	}

	@Override
	public
	Map<Object,PhpEntity> asObjectMap () {

		throw new PhpTypeException (
			type,
			"asObjectMap()");

	}

	@Override
	public
	PhpEntity getAt (
			Object key) {

		PhpEntity ret =
			asMap ().get (
				PhpMisc.normaliseKey (
					key));

		return ret != null
			? ret
			: PhpNull.instance;

	}

	@Override
	public
	Number asNumber () {

		if (isInteger ())
			return asInteger ();

		if (isFloat ())
			return asDouble ();

		throw new PhpTypeException (
			type,
			"asNumber()");

	}

}
