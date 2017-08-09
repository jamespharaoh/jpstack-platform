package wbs.platform.php;

import static wbs.utils.etc.IoUtils.writeBytes;
import static wbs.utils.etc.LogicUtils.ifThenElse;
import static wbs.utils.string.StringUtils.stringToBytes;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;

import lombok.NonNull;

import org.joda.time.LocalDate;

public
class PhpSerializer {

	private
	String charsetName = "utf8";

	public
	PhpSerializer (
			String newCharsetName) {

		charsetName =
			newCharsetName;

	}

	private
	void serializeByteArray (
			@NonNull OutputStream out,
			@NonNull byte[] string) {

		writeBytes (
			out,
			stringToBytes (
				String.format (
					"s:%d:\"",
					string.length),
				"iso-8859-1"));

		writeBytes (
			out,
			string);

		writeBytes (
			out,
			stringToBytes (
				"\";",
				"iso-8859-1"));

	}

	private
	void serializeBoolean (
			@NonNull OutputStream out,
			@NonNull Boolean value) {

		writeBytes (
			out,
			stringToBytes (
				String.format (
					"b:%d;",
					value
						? 1
						: 0),
				"iso-8859-1"));

	}

	private
	void serializeInteger (
			@NonNull OutputStream out,
			@NonNull Integer value) {

		writeBytes (
			out,
			stringToBytes (
				String.format (
					"i:%d;",
					value),
				"iso-8859-1"));

	}

	private
	void serializeLong (
			@NonNull OutputStream out,
			@NonNull Long value) {

		writeBytes (
			out,
			stringToBytes (
				String.format (
					"i:%d;",
					value),
				"iso-8859-1"));

	}

	private
	void serializeString (
			@NonNull OutputStream out,
			@NonNull String string) {

		serialize (
			out,
			stringToBytes (
				string,
				charsetName));

	}

	private
	void serializeDouble (
			@NonNull OutputStream out,
			@NonNull Double value) {

		writeBytes (
			out,
			stringToBytes (
				String.format (
					"d:%s;",
					value),
				"iso-8859-1"));

	}

	private
	void serializeFloat (
			@NonNull OutputStream out,
			@NonNull Float value) {

		writeBytes (
			out,
			stringToBytes (
				String.format (
					"d:%s;",
					value),
				"iso-8859-1"));

	}

	private
	void serializeMap (
			@NonNull OutputStream out,
			@NonNull Map <?, ?> map) {

		writeBytes (
			out,
			stringToBytes (
				String.format (
					"a:%d:{",
					map.size ()),
				"iso-8859-1"));

		for (
			Map.Entry<?,?> entry
				: map.entrySet ()
		) {

			serialize (
				out,
				PhpMisc.normaliseKey (
					entry.getKey ()));

			serialize (
				out,
				entry.getValue ());

		}

		writeBytes (
			out,
			stringToBytes (
				"}",
				"iso-8859-1"));

	}

	private
	void serializeCollection (
			@NonNull OutputStream out,
			@NonNull Collection <?> collection) {

		writeBytes (
			out,
				stringToBytes (
					String.format (
						"a:%d:{",
						collection.size ()),
					"iso-8859-1"));

		int index = 0;

		for (
			Object item
				: collection
		) {

			serialize (
				out,
				index ++);

			serialize (
				out,
				item);

		}

		writeBytes (
			out,
			stringToBytes (
				"}",
				"iso-8859-1"));

	}

	private
	void serializeArray (
			OutputStream out,
			Object[] array) {

		writeBytes (
			out,
			stringToBytes (
				String.format (
					"a:%d:{",
					array.length),
				"iso-8859-1"));

		int i = 0;

		for (
			Object obj
				: array
		) {

			serialize (
				out,
				i ++);

			serialize (
				out,
				obj);

		}

		writeBytes (
			out,
			stringToBytes (
				"}",
				"iso-8859-1"));

	}

	public
	void serialize (
			@NonNull OutputStream out,
			Object originalObject) {

		Object object =
			ifThenElse (
				originalObject instanceof PhpEntity,
				() -> ((PhpEntity) originalObject).asObject (),
				() -> originalObject);

		if (object == null) {

			writeBytes (
				out,
				stringToBytes (
					"N;",
					"iso-8859-1"));

		} else if (object instanceof byte[]) {

			serializeByteArray (
				out,
				(byte[]) object);

		} else if (object instanceof Object[]) {

			serializeArray (
				out,
				(Object[]) object);

		} else if (object instanceof Boolean) {

			serializeBoolean (
				out,
				(Boolean) object);

		} else if (object instanceof Integer) {

			serializeInteger (
				out,
				(Integer) object);

		} else if (object instanceof Long) {

			serializeLong (
				out,
				(Long) object);

		} else if (object instanceof String) {

			serializeString (
				out,
				(String) object);

		} else if (object instanceof Map) {

			serializeMap (
				out,
				(Map<?,?>) object);

		} else if (object instanceof Double) {

			serializeDouble (
				out,
				(Double) object);

		} else if (object instanceof Float) {

			serializeFloat (
				out,
				(Float) object);

		} else if (object instanceof Collection) {

			serializeCollection (
				out,
				(Collection<?>) object);

		} else if (object instanceof LocalDate) {

			serializeString (
				out,
				object.toString ());

		} else {

			throw new PhpSerializeException (
				object.getClass ());

		}

	}

	public static
	void serialize (
			OutputStream out,
			Object object,
			String charsetName) {

		PhpSerializer phpSerializer =
			new PhpSerializer (
				charsetName);

		phpSerializer.serialize (
			out,
			object);

	}

}
