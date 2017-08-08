package wbs.platform.php;

import static wbs.utils.string.StringUtils.stringToBytes;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public
class PhpNull
		extends AbstractPhpEntity {

	private
	PhpNull () {

		super (
			PhpType.pNull);

	}

	@Override
	public
	Boolean asBoolean () {

		return false;

	}

	@Override
	public
	Long asInteger () {

		return 0l;

	}

	@Override
	public
	Long asLong () {

		return 0L;

	}

	@Override
	public
	Double asDouble () {

		return 0.0;

	}

	@Override
	public
	String asString () {

		return "null";

	}

	@Override
	public
	String asString (
			String charset) {

		return "null";

	}

	@Override
	public
	byte[] asByteArray () {

		return stringToBytes (
			"null",
			"iso-8859-1");

	}

	private final static
	PhpEntity[] nullArray =
		new PhpEntity [0];

	@Override
	public
	PhpEntity [] asArray () {

		return nullArray;

	}

	private final static
	List<PhpEntity> nullList =
		ImmutableList.<PhpEntity>of ();

	@Override
	public
	List<PhpEntity> asList () {

		return nullList;

	}

	private final static
	Map<Object,PhpEntity> nullMap =
		ImmutableMap.<Object,PhpEntity>of ();

	@Override
	public
	Map<Object,PhpEntity> asMap () {

		return nullMap;

	}

	@Override
	public
	Map<Object,PhpEntity> asObjectMap () {

		return nullMap;

	}

	@Override
	public
	Object asObject () {

		return null;

	}

	@Override
	public
	Number asNumber () {

		return 0;

	}

	public final static
	PhpNull instance =
		new PhpNull ();

}
