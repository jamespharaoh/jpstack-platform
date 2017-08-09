package wbs.platform.php;

import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.string.StringUtils.stringFormat;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.NonNull;

import wbs.utils.io.RuntimeEofException;
import wbs.utils.io.RuntimeIoException;

public
class PhpUnserializer {

	private static
	int readInt (
			@NonNull CountingInputStream in,
			char termChar) {

		// NB this method works with negatives internally as the range is higher

		int c;

		int i = 0;

		boolean first = true;
		boolean negative = false;

		while ((c = in.read()) != termChar) {

			if (c == -1) {

				throw new PhpUnserializeException (
					"Unexpected end of input");

			}

			if (c == '-' && first) {

				negative = true;

			} else if (c >= '0' && c <= '9') {

				i = (i * 10) - (c - '0');

			} else {

				throw new PhpUnserializeException (
					stringFormat (
						"Expected digit or '%s' at %s",
						Character.toString (
							termChar),
						integerToDecimalString (
							in.getBytes ())));

			}
			first = false;
		}

		return negative ? i : -i;

	}

	private static
	void expect (
			@NonNull CountingInputStream in,
			char expectChar) {

		int i =
			in.read ();

		if (i == -1) {

			throw new PhpUnserializeException (
				"Unexpected end of input");

		}

		if (i != expectChar) {

			throw new PhpUnserializeException (
				stringFormat (
					"Expected '%s' at %s",
					Character.toString (
						(char) expectChar),
					integerToDecimalString (
						in.getBytes ())));

		}

	}

	private static
	PhpEntity unserializeS (
			@NonNull CountingInputStream inputStream) {

		expect (
			inputStream,
			':');

		int stringLength =
			readInt (
				inputStream,
				':');

		if (stringLength < 0) {

			throw new PhpUnserializeException (
				stringFormat (
					"Invalid string length %s at %s",
					integerToDecimalString (
						stringLength),
					integerToDecimalString (
						inputStream.getBytes ())));

		}

		expect (inputStream, '"');
		byte[] data = new byte [stringLength];
		int pos = 0;
		while (pos < stringLength) {
			int num = inputStream.read (data, pos, stringLength - pos);
			if (num < 0)
				throw new PhpUnserializeException ("Unexpected end of input");
			pos += num;
		}
		expect (inputStream, '"');
		expect (inputStream, ';');
		return new PhpString (data);
	}

	private static
	PhpEntity unserializeB (
			@NonNull CountingInputStream in) {

		expect (
			in,
			':');

		int i = readInt(in, ';');

		if (i < 0 || i > 1) {

			throw new PhpUnserializeException (
				stringFormat (
					"Expected 0 or 1 at %s",
					integerToDecimalString (
						in.getBytes())));

		}

		return i == 1
			? PhpBoolean.pTrue
			: PhpBoolean.pFalse;

	}

	private static
	PhpEntity unserializeI (
			@NonNull CountingInputStream in) {

		expect (in, ':');

		long i = readInt (in, ';');

		return new PhpInteger (i);

	}

	private static
	PhpEntity unserializeA (
			@NonNull CountingInputStream in) {

		expect (
			in,
			':');

		int len =
			readInt (
				in,
				':');

		expect(in, '{');
		Map<PhpEntity, PhpEntity> map = new LinkedHashMap<PhpEntity, PhpEntity>();
		while (len-- > 0) {
			map.put(unserialize(in), unserialize(in));
		}
		expect(in, '}');
		return new PhpArray(map);
	}

	private static
	PhpEntity unserializeN (
			@NonNull CountingInputStream inputStream) {

		expect (
			inputStream,
			';');

		return PhpNull.instance;

	}

	private static
	PhpEntity unserializeDouble (
			@NonNull CountingInputStream countingInputStream) {

		// read past the colon

		expect (
			countingInputStream,
			':');

		StringBuilder stringBuilder =
			new StringBuilder ();

		for (;;) {

			// read one character

			int character =
				countingInputStream.read ();

			if (character == -1) {

				throw new PhpUnserializeException (
					"Unexpected end of input");

			}

			// detect semicolon

			if (character == ';') {

				return new PhpDouble (
					Double.parseDouble (
						stringBuilder.toString ()));

			}

			stringBuilder.append (
				(char) character);

		}

	}

	private static
	PhpEntity unserialize (
			@NonNull CountingInputStream inputStream) {

		int c = inputStream.read ();

		if (c < 0) {
			throw new RuntimeEofException ();
		}

		switch (c) {

		case 'b':

			return unserializeB (
				inputStream);

		case 'i':

			return unserializeI (
				inputStream);

		case 's':

			return unserializeS (
				inputStream);

		case 'a':

			return unserializeA (
				inputStream);

		case 'N':

			return unserializeN (
				inputStream);

		case 'd':

			return unserializeDouble (inputStream);

		}

		throw new PhpUnserializeException (
			stringFormat (
				"Unknown data type: '%s' at '%s'",
				Character.toString (
					(char) c),
				integerToDecimalString (
					inputStream.getBytes ())));

	}

	public static
	PhpEntity unserialize (
			@NonNull InputStream inputStream) {

		try (

			CountingInputStream countingInputStream =
				new CountingInputStream (
					inputStream);

		) {

			return unserialize (
				countingInputStream);

		}

	}

	private static
	class CountingInputStream
		extends InputStream
		implements AutoCloseable {

		private
		InputStream delegate;

		private
		long bytes = 0;

		private
		long markBytes = 0;

		private
		CountingInputStream (
				@NonNull InputStream delegate) {

			this.delegate =
				delegate;

		}

		@Override
		public
		int read () {

			try {

				int character =
					delegate.read ();

				if (character >= 0) {
					bytes ++;
				}

				return character;

			} catch (IOException ioException ) {

				throw new RuntimeIoException (
					ioException);

			}

		}

		@Override
		public
		int read (
				byte[] b,
				int off,
				int len) {

			try {

				int ret =
					delegate.read (
						b,
						off,
						len);

				bytes +=
					ret;

				return ret;

			} catch (IOException ioException) {

				throw new RuntimeIoException (
					ioException);

			}

		}

		@Override
		public
		long skip (
				long n) {

			try {

				long ret =
					delegate.skip (n);

				bytes +=
					ret;

				return ret;

			} catch (IOException ioException) {

				throw new RuntimeIoException (
					ioException);

			}

		}

		@Override
		public synchronized
		void mark (
				int readlimit) {

			delegate.mark (
				readlimit);

			markBytes =
				bytes;

		}

		@Override
		public synchronized
		void reset () {

			try {

				delegate.reset ();

				bytes =
					markBytes;

			} catch (IOException ioException) {

				throw new RuntimeIoException (
					ioException);

			}

		}

		public
		long getBytes () {

			return bytes;

		}

		@Override
		public
		void close () {

			try {

				delegate.close ();

			} catch (IOException ioException) {

				throw new RuntimeIoException (
					ioException);

			}

		}

	}

}
