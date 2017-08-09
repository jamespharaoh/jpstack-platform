package wbs.platform.rpc.core;

import static wbs.utils.etc.TypeUtils.classNameSimple;
import static wbs.utils.string.StringUtils.stringFormat;

import org.joda.time.LocalDate;

public
class RpcPrimitive
	extends RpcElem {

	private final
	Object value;

	public
	RpcPrimitive (
			String name,
			Object newValue) {

		super (
			name,
			typeOfPrimitive (
				newValue));

		value =
			newValue;

	}

	private static
	RpcType typeOfPrimitive (
			Object primitive) {

		if (primitive instanceof Long)
			return RpcType.rInteger;

		if (primitive instanceof String)
			return RpcType.rString;

		if (primitive instanceof Boolean)
			return RpcType.rBoolean;

		if (primitive instanceof Double)
			return RpcType.rFloat;

		if (primitive instanceof byte[])
			return RpcType.rBinary;

		if (primitive instanceof LocalDate)
			return RpcType.rDate;

		throw new RuntimeException (
			stringFormat (
				"Don't know what to do with %s",
				classNameSimple (
					primitive.getClass ())));

	}

	@Override
	public
	Object getValue () {
		return value;
	}

	@Override
	public
	Object getNative () {
		return value;
	}

}
