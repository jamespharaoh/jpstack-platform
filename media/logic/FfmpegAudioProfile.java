package wbs.platform.media.logic;

import java.util.List;

import com.google.common.collect.ImmutableList;

import lombok.NonNull;

public
class FfmpegAudioProfile
		extends FfmpegProfile {

	public
	FfmpegAudioProfile (
			@NonNull String fileExtension) {

		super (
			fileExtension);

	}

	@Override
	public
	List <List <String>> toFfmpeg () {

		return ImmutableList.<List <String>> of (

			ImmutableList.<String> of (
				"ffmpeg",
				"-y",
				"-i",
				"<in>"),

			ImmutableList.<String> of (
				"<out>"));

	}

}
