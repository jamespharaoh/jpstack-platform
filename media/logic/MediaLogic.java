package wbs.platform.media.logic;

import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalOrThrow;
import static wbs.utils.string.StringUtils.stringFormat;

import java.awt.image.BufferedImage;
import java.util.Set;

import com.google.common.base.Optional;

import lombok.NonNull;

import wbs.framework.database.Transaction;

import wbs.platform.media.model.ContentRec;
import wbs.platform.media.model.MediaRec;
import wbs.platform.media.model.MediaTypeRec;

public
interface MediaLogic {

	ContentRec findOrCreateContent (
			Transaction parentTransaction,
			byte[] data);

	Optional <MediaRec> createMedia (
			Transaction parentTransaction,
			byte[] data,
			String mimeType,
			String filename,
			Optional <String> encoding);

	default
	MediaRec createMediaRequired (
			@NonNull Transaction parentTransaction,
			@NonNull byte[] data,
			@NonNull String mimeType,
			@NonNull String filename,
			@NonNull Optional <String> encoding) {

		return optionalOrThrow (
			createMedia (
				parentTransaction,
				data,
				mimeType,
				filename,
				encoding),
			() -> new IllegalArgumentException (
				stringFormat (
					"Unable to decode \"%s\" of type \"%s\"",
					filename,
					mimeType)));

	}

	MediaRec createMediaFromImage (
			Transaction parentTransaction,
			BufferedImage image,
			String mimeType,
			String filename);

	Optional <MediaRec> createMediaFromImage (
			Transaction parentTransaction,
			byte[] data,
			String mimeType,
			String filename);

	default
	MediaRec createMediaFromImageRequired (
			@NonNull Transaction parentTransaction,
			@NonNull byte[] data,
			@NonNull String mimeType,
			@NonNull String filename) {

		return optionalGetRequired (
			createMediaFromImage (
				parentTransaction,
				data,
				mimeType,
				filename));

	}


	MediaRec createMediaWithThumbnail (
			Transaction parentTransaction,
			byte[] data,
			BufferedImage thumbnailImage,
			String mimeType,
			String filename,
			Long width,
			Long height);

	MediaRec createMediaWithThumbnail (
			Transaction parentTransaction,
			byte[] data,
			byte[] thumb100,
			byte[] thumb32,
			String mimeType,
			String filename,
			Long width,
			Long height);

	Optional <MediaRec> createMediaFromVideo (
			Transaction parentTransaction,
			byte[] data,
			String mimeType,
			String filename);

	default
	MediaRec createMediaFromVideoRequired (
			@NonNull Transaction parentTransaction,
			@NonNull byte[] data,
			@NonNull String mimeType,
			@NonNull String filename) {

		return optionalGetRequired (
			createMediaFromVideo (
				parentTransaction,
				data,
				mimeType,
				filename));

	}

	MediaRec createMediaFromAudio (
			Transaction parentTransaction,
			byte[] data,
			String mimeType,
			String filename);

	MediaTypeRec findMediaTypeRequired (
			Transaction parentTransaction,
			String mimeType);

	Optional <MediaRec> createTextualMedia (
			Transaction parentTransaction,
			byte[] data,
			String mimeType,
			String filename,
			String encoding);

	MediaRec createTextMedia (
			Transaction parentTransaction,
			String text,
			String mimeType,
			String filename);

	Optional <BufferedImage> getImage (
			Transaction parentTransaction,
			MediaRec media);

	default
	BufferedImage getImageRequired (
			@NonNull Transaction parentTransaction,
			@NonNull MediaRec media) {

		return optionalGetRequired (
			getImage (
				parentTransaction,
				media));

	}

	boolean isApplication (
			String mimeType);

	boolean isApplication (
			MediaTypeRec mediaType);

	boolean isApplication (
			MediaRec media);

	boolean isAudio (
			String mimeType);

	boolean isAudio (
			MediaTypeRec mediaType);

	boolean isAudio (
			MediaRec media);

	boolean isImage (
			String mimeType);

	boolean isImage (
			MediaTypeRec mediaType);

	boolean isImage (
			MediaRec media);

	boolean isText (
			String mimeType);

	boolean isText (
			MediaTypeRec mediaType);

	boolean isText (
			MediaRec media);

	boolean isTextual (
			String mimeType);

	boolean isTextual (
			MediaTypeRec mediaType);

	boolean isTextual (
			MediaRec media);

	boolean isVideo (
			String mimeType);

	boolean isVideo (
			MediaTypeRec mediaType);

	boolean isVideo (
			MediaRec media);

	Set<String> videoProfileNames ();

}
