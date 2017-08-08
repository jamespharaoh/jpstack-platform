package wbs.platform.media.console;

import java.awt.image.BufferedImage;

import lombok.NonNull;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.media.logic.MediaLogic;
import wbs.platform.media.model.ContentRec;
import wbs.platform.media.model.MediaRec;
import wbs.platform.media.model.MediaTypeRec;

@PrototypeComponent ("mediaImageScaleResponder")
public
class MediaImageScaleResponder
	extends AbstractMediaImageResponder {

	// singleton dependencies

	@SingletonDependency
	MediaLogic mediaLogic;

	// implementation

	@Override
	protected
	byte[] getData (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull MediaRec media) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"getData");

		) {

			Long maxWidth =
				requestContext.parameterIntegerRequired (
					"width");

			Long maxHeight =
				requestContext.parameterIntegerRequired (
					"height");

			MediaTypeRec mediaType =
				media.getMediaType ();

			ContentRec content =
				media.getContent ();

			BufferedImage fullImage =
				rawMediaLogic.readImageRequired (
					taskLogger,
					content.getData (),
					mediaType.getMimeType ());

			BufferedImage scaledImage =
				rawMediaLogic.resampleImageToFit (
					fullImage,
					maxWidth,
					maxHeight);

			byte[] scaledImageData =
				rawMediaLogic.writeImage (
					scaledImage,
					mediaType.getMimeType ());

			return scaledImageData;

		}

	}

	@Override
	protected
	String getMimeType (
			MediaRec media) {

		MediaTypeRec mediaType =
			media.getMediaType ();

		return mediaType.getMimeType ();

	}

}
