package wbs.platform.media.logic;

import static wbs.utils.etc.Misc.contains;
import static wbs.utils.etc.NumberUtils.fromJavaInteger;
import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.etc.OptionalUtils.optionalIsNotPresent;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.string.StringUtils.stringToBytes;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.GlobalId;
import wbs.framework.logging.LogContext;

import wbs.platform.media.model.ContentObjectHelper;
import wbs.platform.media.model.ContentRec;
import wbs.platform.media.model.MediaObjectHelper;
import wbs.platform.media.model.MediaRec;
import wbs.platform.media.model.MediaTypeObjectHelper;
import wbs.platform.media.model.MediaTypeRec;

@SingletonComponent ("mediaLogic")
public
class MediaLogicImplementation
	implements MediaLogic {

	// singleton dependencies

	@SingletonDependency
	ContentObjectHelper contentHelper;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	MediaObjectHelper mediaHelper;

	@SingletonDependency
	MediaTypeObjectHelper mediaTypeHelper;

	@SingletonDependency
	RawMediaLogic rawMediaLogic;

	// implementation

	@Override
	public
	ContentRec findOrCreateContent (
			@NonNull Transaction parentTransaction,
			@NonNull byte[] data) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findOrCreateContent");

		) {

			// work out hash code

			Long shortHash =
				fromJavaInteger (
					Arrays.hashCode (
						data));

			// look for existing content

			List <ContentRec> list =
				contentHelper.findByShortHash (
					transaction,
					shortHash);

			long index = 0;

			for (
				ContentRec content
					: list) {

				if (
					Arrays.equals (
						content.getData (),
						data)
				) {
					return content;
				}

				if (content.getI () >= index) {

					index =
						content.getI () + 1;

				}

			}

			// create a new content object

			ContentRec content =
				contentHelper.insert (
					transaction,
					contentHelper.createInstance ()

				.setData (
					data)

				.setHash (
					shortHash)

				.setI (
					index)

			);

			// return

			return content;

		}

	}

	@Override
	public
	Optional <MediaRec> createMedia (
			@NonNull Transaction parentTransaction,
			@NonNull byte[] data,
			@NonNull String mimeType,
			@NonNull String filename,
			@NonNull Optional <String> encoding) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createMedia");

		) {

			if (
				contains (
					imageTypes,
					mimeType)
			) {

				return createMediaFromImage (
					transaction,
					data,
					mimeType,
					filename);

			} else if (
				contains (
					textualTypes,
					mimeType)
			) {

				if (
					optionalIsNotPresent (
						encoding)
				) {

					throw new IllegalArgumentException (
						"Encoding must be specified for textual content");

				}

				return createTextualMedia (
					transaction,
					data,
					mimeType,
					filename,
					encoding.get ());

			} else if (
				contains (
					videoTypes,
					mimeType)
			) {

				return createMediaFromVideo (
					transaction,
					data,
					mimeType,
					filename);

			} else {

				throw new RuntimeException (
					stringFormat (
						"Unknown media type \"%s\"",
						mimeType));

			}

		}

	}

	private final static
	String defaultMimeType = "image/jpeg";

	@Override
	public
	MediaRec createMediaFromImage (
			@NonNull Transaction parentTransaction,
			@NonNull BufferedImage image,
			@NonNull String mimeType,
			@NonNull String filename) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createMediaFromImage");

		) {

			// encode the image

			byte[] data =
				rawMediaLogic.writeImage (
					image,
					defaultMimeType);

			return createMediaWithThumbnail (
				transaction,
				data,
				image,
				mimeType,
				filename,
				fromJavaInteger (
					image.getWidth ()),
				fromJavaInteger (
					image.getHeight ()));

		}

	}

	@Override
	public
	Optional <MediaRec> createMediaFromImage (
			@NonNull Transaction parentTransaction,
			@NonNull byte[] data,
			@NonNull String mimeType,
			@NonNull String filename) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createMediaFromImage");

		) {

			Optional <BufferedImage> imageOptional =
				rawMediaLogic.readImage (
					transaction,
					data,
					mimeType);

			if (
				optionalIsNotPresent (
					imageOptional)
			) {
				return optionalAbsent ();
			}

			BufferedImage image =
				imageOptional.get ();

			return optionalOf (
				createMediaWithThumbnail (
					transaction,
					data,
					image,
					mimeType,
					filename,
					fromJavaInteger (
						image.getWidth ()),
					fromJavaInteger (
						image.getHeight ())));

		}

	}

	@Override
	public
	MediaRec createMediaWithThumbnail (
			@NonNull Transaction parentTransaction,
			@NonNull byte[] data,
			@NonNull BufferedImage thumbnailImage,
			@NonNull String mimeType,
			@NonNull String filename,
			@NonNull Long width,
			@NonNull Long height) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createMediaWithThumbnail");

		) {

			// create the 100x100 thumbnail

			BufferedImage image100 =
				rawMediaLogic.resampleImageToFit (
					thumbnailImage,
					100l,
					100l);

			byte[] data100 =
				rawMediaLogic.writeImage (
					image100,
					defaultMimeType);

			// create the 32x32 thumbnail

			BufferedImage image32 =
				rawMediaLogic.resampleImageToFit (
					thumbnailImage,
					32l,
					32l);

			byte[] data32 =
				rawMediaLogic.writeImage (
					image32,
					defaultMimeType);

			return createMediaWithThumbnail (
				transaction,
				data,
				data100,
				data32,
				mimeType,
				filename,
				width,
				height);

		}

	}

	@Override
	public
	MediaRec createMediaWithThumbnail (
			@NonNull Transaction parentTransaction,
			@NonNull byte[] data,
			@NonNull byte[] thumb100,
			@NonNull byte[] thumb32,
			@NonNull String mimeType,
			@NonNull String filename,
			@NonNull Long width,
			@NonNull Long height) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createMediaWithThumbnail");

		) {

			MediaTypeRec mediaType =
				findMediaTypeRequired (
					transaction,
					mimeType);

			return mediaHelper.insert (
				transaction,
				mediaHelper.createInstance ()

				.setFilename (
					filename)

				.setContent (
					findOrCreateContent (
						transaction,
						data))

				.setThumb100Content (
					findOrCreateContent (
						transaction,
						thumb100))

				.setThumb32Content (
					findOrCreateContent (
						transaction,
						thumb32))

				.setMediaType (
					mediaType)

				.setThumbMediaType (
					mediaType)

				.setWidth (
					width)

				.setHeight (
					height)

			);

		}

	}

	@Override
	public
	Optional <MediaRec> createMediaFromVideo (
			@NonNull Transaction parentTransaction,
			@NonNull byte[] data,
			@NonNull String mimeType,
			@NonNull String filename) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createMediaFromVideo");

		) {

			Optional <BufferedImage> videoFrameImageOptional =
				rawMediaLogic.videoFrame (
					transaction,
					data);

			if (
				optionalIsNotPresent (
					videoFrameImageOptional)
			) {
				return optionalAbsent ();
			}

			BufferedImage videoFrameImage =
				videoFrameImageOptional.get ();

			return optionalOf (
				createMediaWithThumbnail (
					transaction,
					data,
					videoFrameImage,
					mimeType,
					filename,
					fromJavaInteger (
						videoFrameImage.getWidth ()),
					fromJavaInteger (
						videoFrameImage.getHeight ())));

		}

	}

	@Override
	public
	MediaRec createMediaFromAudio (
			@NonNull Transaction parentTransaction,
			@NonNull byte[] data,
			@NonNull String mimeType,
			@NonNull String filename) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createMediaFromAudio");

		) {

			MediaTypeRec mediaType =
				findMediaTypeRequired (
					transaction,
					mimeType);

			return mediaHelper.insert (
				transaction,
				mediaHelper.createInstance ()

				.setFilename (
					filename)

				.setContent (
					findOrCreateContent (
						transaction,
						data))

				.setMediaType (
					mediaType)

			);

		}

	}

	@Override
	public
	MediaTypeRec findMediaTypeRequired (
			@NonNull Transaction parentTransaction,
			@NonNull String mimeType) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findMediaTypeRequired");

		) {

			return mediaTypeHelper.findByCodeRequired (
				transaction,
				GlobalId.root,
				mimeType);

		}

	}

	@Override
	public
	Optional <MediaRec> createTextualMedia (
			@NonNull Transaction parentTransaction,
			@NonNull byte[] data,
			@NonNull String mimeType,
			@NonNull String filename,
			@NonNull String encoding) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createTextualMedia");

		) {

			return optionalOf (
				mediaHelper.insert (
					transaction,
					mediaHelper.createInstance ()

				.setMediaType (
					findMediaTypeRequired (
						transaction,
						mimeType))

				.setContent (
					findOrCreateContent (
						transaction,
						data))

				.setFilename (
					filename)

				.setEncoding (
					encoding)

			));

		}

	}

	@Override
	public
	MediaRec createTextMedia (
			@NonNull Transaction parentTransaction,
			@NonNull String text,
			@NonNull String mimeType,
			@NonNull String filename) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createTextMedia");

		) {

			return mediaHelper.insert (
				transaction,
				mediaHelper.createInstance ()

				.setMediaType (
					findMediaTypeRequired (
						transaction,
						mimeType))

				.setContent (
					findOrCreateContent (
						transaction,
						stringToBytes (
							text,
							"utf-8")))

				.setFilename (
					filename)

				.setEncoding (
					"utf-8")

			);

		}

	}

	@Override
	public
	Optional <BufferedImage> getImage (
			@NonNull Transaction parentTransaction,
			@NonNull MediaRec media) {

		return rawMediaLogic.readImage (
			parentTransaction,
			media.getContent ().getData (),
			media.getMediaType ().getMimeType ());

	}

	@Override
	public
	boolean isApplication (
			@NonNull String mimeType) {

		return applicationTypes.contains (
			mimeType);

	}

	@Override
	public
	boolean isApplication (
			@NonNull MediaTypeRec mediaType) {

		return isApplication (
			mediaType.getMimeType ());

	}

	@Override
	public
	boolean isApplication (
			@NonNull MediaRec media) {

		return isApplication (
			media.getMediaType ());

	}

	@Override
	public
	boolean isAudio (
			@NonNull String mimeType) {

		return audioTypes.contains (
			mimeType);

	}

	@Override
	public
	boolean isAudio (
			@NonNull MediaTypeRec mediaType) {

		return isAudio (
			mediaType.getMimeType ());

	}

	@Override
	public
	boolean isAudio (
			@NonNull MediaRec media) {

		return isAudio (
			media.getMediaType ());

	}

	@Override
	public
	boolean isImage (
			@NonNull String mimeType) {

		return imageTypes.contains (
			mimeType);

	}

	@Override
	public
	boolean isImage (
			@NonNull MediaTypeRec mediaType) {

		return isImage (
			mediaType.getMimeType ());

	}

	@Override
	public
	boolean isImage (
			@NonNull MediaRec media) {

		return isImage (
			media.getMediaType ());

	}

	@Override
	public
	boolean isText (
			@NonNull String mimeType) {

		return textTypes.contains (
			mimeType);

	}

	@Override
	public
	boolean isText (
			@NonNull MediaTypeRec mediaType) {

		return isText (
			mediaType.getMimeType ());

	}

	@Override
	public
	boolean isText (
			@NonNull MediaRec media) {

		return isText (
			media.getMediaType ());

	}

	@Override
	public
	boolean isTextual (
			@NonNull String mimeType) {

		return textualTypes.contains (
			mimeType);

	}

	@Override
	public
	boolean isTextual (
			@NonNull MediaTypeRec mediaType) {

		return isTextual (
			mediaType.getMimeType ());

	}

	@Override
	public
	boolean isTextual (
			@NonNull MediaRec media) {

		return isTextual (
			media.getMediaType ());

	}

	@Override
	public
	boolean isVideo (
			@NonNull String mimeType) {

		return videoTypes.contains (
			mimeType);

	}

	@Override
	public
	boolean isVideo (
			@NonNull MediaTypeRec mediaType) {

		return isVideo (
			mediaType.getMimeType ());

	}

	@Override
	public
	boolean isVideo (
			@NonNull MediaRec media) {

		return isVideo (
			media.getMediaType ());

	}

	@Override
	public
	Set <String> videoProfileNames () {

		return FfmpegProfileData.profiles.keySet ();

	}

	// data

	public final static
	Set <String> imageTypes =
		ImmutableSet.of (
			"image/gif",
			"image/jpeg",
			"image/jpg",
			"image/png");

	public final static
	Set <String> textTypes =
		ImmutableSet.of (
			"text/plain");

	public final static
	Set <String> applicationTypes =
		ImmutableSet.of (
			"application/smil");

	public final static
	Set <String> videoTypes =
		ImmutableSet.of (
			"video/3gpp",
			"video/mpeg",
			"video/mp4");

	public final static
	Set <String> audioTypes =
		ImmutableSet.of (
			"audio/mpeg");

	public final static
	Set <String> textualTypes =
		ImmutableSet.of (
			"text/plain",
			"application/smil");

}
