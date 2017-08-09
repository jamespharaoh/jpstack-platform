package wbs.platform.php;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

public
interface PhpEntity {

	PhpType getType ();

	boolean isBoolean ();

	boolean isInteger ();

	boolean isFloat ();

	boolean isString ();

	boolean isArray ();

	boolean isObject ();

	boolean isNormalArray ();

	boolean isNull ();

	boolean isNumber ();

	boolean isScalar ();

	Boolean asBoolean ();

	Long asInteger ();

	Long asLong ();

	Double asDouble ();

	String asString ();

	String asString (
			String encoding)
		throws UnsupportedEncodingException;

	String asStringUtf8 ();

	byte[] asByteArray ();

	Map<Object,PhpEntity> asMap ();

	List<PhpEntity> asList ();

	Object[] asArray ();

	Map<Object,PhpEntity> asObjectMap ();

	Object asObject ();

	Number asNumber ();

	PhpEntity getAt (
			Object key);

}
