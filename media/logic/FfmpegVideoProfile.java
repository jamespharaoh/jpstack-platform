package wbs.platform.media.logic;

import static wbs.utils.etc.OptionalUtils.optionalIsPresent;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import lombok.NonNull;

public
class FfmpegVideoProfile
	extends FfmpegProfile {

	boolean twoPass;

	Optional <String> videoCodec;
	Optional <String> videoResolution;
	Optional <String> videoBitrate;
	Optional <String> videoDuration;

	public
	FfmpegVideoProfile (
			boolean twoPass,
			@NonNull String fileExtension,
			@NonNull Optional <String> videoCodec,
			@NonNull Optional <String> videoResolution,
			@NonNull Optional <String> videoBitrate,
			@NonNull Optional <String> videoDuration) {

		super (
			fileExtension);

		this.twoPass =
			twoPass;

		this.videoCodec =
			videoCodec;

		this.videoResolution =
			videoResolution;

		this.videoBitrate =
			videoBitrate;

		this.videoDuration =
			videoDuration;

	}

	@Override
	public
	List<List<String>> toFfmpeg () {

		List<String> passOne =
			new ArrayList<String> ();

		List<String> passTwo =
			new ArrayList<String> ();

		// input

		passOne.addAll (
			ImmutableList.<String>of (
				"ffmpeg",
				"-y",
				"-i",
				"<in>"));

		passTwo.addAll (
			ImmutableList.<String> of (
				"ffmpeg",
				"-y",
				"-i",
				"<in>"));

		// video

		if (
			optionalIsPresent (
				videoCodec)
		) {

			passOne.addAll (
				ImmutableList.<String> of (
					"-vcodec",
					videoCodec.get ()));

			passTwo.addAll (
				ImmutableList.<String> of (
					"-vcodec",
					videoCodec.get ()));
		}

		if (
			optionalIsPresent (
				videoResolution)
		) {

			passOne.addAll (
				ImmutableList.<String>of (
					"-s",
					videoResolution.get ()));

			passTwo.addAll (
				ImmutableList.<String>of (
					"-s",
					videoResolution.get ()));

		}

		if (
			optionalIsPresent (
				videoBitrate)
		) {

			passOne.addAll (
				ImmutableList.<String>of (
					"-b",
					videoBitrate.get ()));

			passTwo.addAll (
				ImmutableList.<String>of (
					"-b",
					videoBitrate.get ()));

		}

		// audio

		passOne.addAll (
			ImmutableList.<String>of (
				"-an"));

		passTwo.addAll (
			ImmutableList.<String>of (
				"-an"));

		// two pass

		if (twoPass) {

			passOne.addAll (
				ImmutableList.<String>of (
					"-pass",
					"1"));

			passTwo.addAll (
				ImmutableList.<String>of (
					"-pass",
					"2"));
		}

		// duration

		if (
			optionalIsPresent (
				videoDuration)
		) {

			passTwo.addAll (
				ImmutableList.<String>of (
					"-t",
					videoDuration.get ()));

		}

		// output

		passOne.addAll (
			ImmutableList.<String> of (
				"<out>"));

		passTwo.addAll (
			ImmutableList.<String> of (
				"<out>"));

		// return

		return twoPass

			? ImmutableList.<List <String>> of (
				passOne,
				passTwo)

			: ImmutableList.<List <String>> of (
				passTwo);

	}

}
