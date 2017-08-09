package wbs.platform.media.logic;

import static wbs.utils.etc.NumberUtils.toJavaIntegerRequired;

import javax.imageio.stream.ImageInputStreamImpl;

import lombok.NonNull;

public
class ByteArrayImageInputStream
	extends ImageInputStreamImpl {

	byte[] data;

	ByteArrayImageInputStream (
			@NonNull byte[] newData) {

		data = newData;

	}

	@Override
	public
	int read () {

		if (streamPos < data.length) {

			return Byte.toUnsignedInt (
				data [
					toJavaIntegerRequired (
						streamPos ++)]);

		} else {

			return -1;

		}

	}

	@Override
	public
	int read (
			@NonNull byte[] bytes,
			int offset,
			int length) {

		if (streamPos + length <= data.length) {

			System.arraycopy (
				data,
				toJavaIntegerRequired (
					streamPos),
				bytes,
				offset,
				length);

			streamPos +=
				length;

			return length;

		} else if (streamPos < data.length) {

			int numread =
				toJavaIntegerRequired (
					+ data.length
					- streamPos);

			System.arraycopy (
				data,
				toJavaIntegerRequired (
					streamPos),
				bytes,
				offset,
				numread);

			streamPos =
				data.length;

			return numread;

		}

		return -1;

	}

}

