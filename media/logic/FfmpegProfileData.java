package wbs.platform.media.logic;

import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.etc.OptionalUtils.optionalOf;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

public
class FfmpegProfileData {

	public final static
	Map <String, FfmpegProfile> profiles =
		ImmutableMap.<String, FfmpegProfile> builder ()

		.put (
			"3gpp",
			new FfmpegVideoProfile (
				true,
				"3gp",
				optionalOf ("h263"),
				optionalOf ("128x96"),
				optionalOf ("100k"),
				optionalOf ("00:00:15")))

		.put (
			"flv",
			new FfmpegVideoProfile (
				false,
				"flv",
				optionalOf ("flv"),
				optionalAbsent (),
				optionalAbsent (),
				optionalAbsent ()))

		.put (
			"mp4",
			new FfmpegVideoProfile (
				false,
				"mp4",
				optionalOf ("h263"),
				optionalAbsent (),
				optionalAbsent (),
				optionalAbsent ()))

		.put (
			"mp3",
			new FfmpegAudioProfile (
				"mp3"))

		.build ();

}
