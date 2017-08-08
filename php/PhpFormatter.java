package wbs.platform.php;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

public
class PhpFormatter {

	private final
	String indentString;

	public
	PhpFormatter (
			String indentString) {

		this.indentString =
			indentString;

	}

	public
	void format (
			Writer out,
			PhpEntity entity,
			int indentCount)
		throws IOException {

		switch (entity.getType ()) {

		case pArray:

			out.write ("array (\n");

			for (Map.Entry<Object,PhpEntity> ent : entity.asMap ().entrySet ()) {
				for (int j = 0; j < indentCount + 1; j++) out.write (indentString);
				Object key = ent.getKey ();
				if (key instanceof String) out.write ('"');
				out.write (key.toString ());
				if (key instanceof String) out.write ('"');
				out.write (" => ");
				format (out, ent.getValue (), indentCount + 1);
				out.write ("\n");
			}

			for (int j = 0; j < indentCount + 1; j++) {

				out.write (
					indentString);

			}

			break;

		case pBoolean:
		case pFloat:
		case pInteger:
			out.write (entity.asBoolean () ? "true" : "false");
			break;

		case pNull:
			out.write ("null");
			break;

		case pObject:
			out.write ("(object)");
			break;

		case pString:
			out.write ('"');
			out.write (entity.asString ());
			out.write ('"');
			break;

		}

	}

	public
	String format (
			PhpEntity entity) {

		try {

			StringWriter out =
				new StringWriter ();

			format (
				out,
				entity,
				0);

			return out.toString ();

		} catch (IOException exception) {

			throw new RuntimeException (
				exception);

		}

	}

	public final static
	PhpFormatter DEFAULT =
		new PhpFormatter ("  ");

}
