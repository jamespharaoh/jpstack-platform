package wbs.platform.php;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

public
class PhpArray
	extends AbstractPhpEntity {

	private final
	Map<Object,PhpEntity> map;

	private
	boolean triedNormal = false;

	private
	PhpEntity[] array = null;

	public
	PhpArray (
			Map<PhpEntity,PhpEntity> newMap) {

		super (
			PhpType.pArray);

		if (newMap == null)
			throw new NullPointerException ();

		map =
			new LinkedHashMap<Object,PhpEntity> ();

		for (
			Map.Entry<PhpEntity,PhpEntity> entry
				: newMap.entrySet ()
		) {

			PhpEntity key =
				entry.getKey ();

			if (key.isString ()) {

				map.put (
					key.asString (),
					entry.getValue ());

			} else if (key.isInteger ()) {

				map.put (
					key.asInteger (),
					entry.getValue ());

			} else {

				throw new RuntimeException ();

			}

		}

	}

	public @Override
	boolean isNormalArray () {

		if (triedNormal) {
			return array != null;
		}

		triedNormal = true;

		PhpEntity[] tempArray =
			new PhpEntity [
				map.size ()];

		int index = 0;

		for (
			Map.Entry<Object,PhpEntity> entry
				: map.entrySet ()
		) {

			Object key =
				entry.getKey ();

			if (! (key instanceof Integer)) {
				return false;
			}

			if ((Integer) key != index) {
				return false;
			}

			tempArray [index] =
				entry.getValue ();

			index ++;

		}

		array =
			tempArray;

		return true;

	}

	@Override
	public
	Boolean asBoolean () {

		return map.size () > 0;

	}

	@Override
	public
	Long asInteger () {

		return map.size () > 0
			? 1l
			: 0l;

	}

	@Override
	public
	Double asDouble () {

		return map.size () > 0
			? 1.0D
			: 0.0D;

	}

	@Override
	public
	String asString () {

		return "Array";

	}

	@Override
	public
	String asString (
			String charset) {

		return "Array";

	}

	@Override
	public
	byte[] asByteArray () {

		try {

			return "Array".getBytes (
				"iso-8859-1");

		} catch (UnsupportedEncodingException exception) {

			throw new RuntimeException (
				exception);

		}

	}

	@Override
	public
	PhpEntity[] asArray () {

		if (! isNormalArray ()) {

			throw new PhpTypeException (
				type,
				"asArray ()");

		}

		return array;

	}

	@Override
	public
	List<PhpEntity> asList () {

		if (! isNormalArray ()) {

			throw new PhpTypeException (
				type,
				"asList ()");

		}

		return Arrays.asList (
			array);

	}

	@Override
	public
	Map<Object,PhpEntity> asMap () {

		return ImmutableMap.copyOf (
			map);

	}

	@Override
	public
	Map<Object,PhpEntity> asObjectMap () {

		return ImmutableMap.copyOf (
			map);

	}

	@Override
	public
	Object asObject () {

		return ImmutableMap.copyOf (
			map);

	}

}
