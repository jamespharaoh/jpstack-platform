package wbs.platform.php;

import static wbs.utils.etc.NumberUtils.parseIntegerRequired;
import static wbs.utils.string.StringUtils.bytesToString;
import static wbs.utils.string.StringUtils.stringToBytes;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public
class PhpString
	extends AbstractPhpEntity {

	private final
	byte[] data;

	public
	PhpString (
			byte[] newData) {

		super (
			PhpType.pString);

		data =
			newData;

	}

	@Override
	public
	Boolean asBoolean () {

		if (data.length == 0)
			return false;

		if (
			data.length == 1 &&
			data [0] == (byte) '0'
		) {
			return false;
		}

		return true;

	}

	/** This implementation is inconsistent with PHP's behaviour. */
	@Override
	public
	Long asInteger () {

		return parseIntegerRequired (
			bytesToString (
				data,
				"iso-8859-1"));

	}

	@Override
	public
	Double asDouble () {

		return Double.parseDouble (
			bytesToString (
				data,
				"iso-8859-1"));

	}

	@Override
	public
	String asString () {

		return bytesToString (
			data,
			"iso-8859-1");

	}

	@Override
	public
	String asString (
			String charset)
		throws UnsupportedEncodingException {

		return bytesToString (
			data,
			charset);

	}

	@Override
	public
	byte[] asByteArray () {

		return data;

	}

	@Override
	public
	PhpEntity[] asArray () {

		return new PhpEntity [] {
			this
		};

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

		return data;

	}

	@Override
	public
	int hashCode () {

		return Arrays.hashCode (
			data);

	}

	@Override
	public
	boolean equals (
			Object object) {

		if (! (object instanceof PhpString))
			return false;

		PhpString other =
			(PhpString)
			object;

		return Arrays.equals (
			data,
			other.data);

	}

	public final static
	PhpString scalar =
		new PhpString (
			stringToBytes (
				"scalar",
				"iso-8859-1"));

}
