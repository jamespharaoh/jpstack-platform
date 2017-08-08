package wbs.platform.media.console;

import static wbs.utils.etc.IoUtils.writeBytes;
import static wbs.utils.string.StringUtils.stringEqualSafe;

import java.io.OutputStream;

import lombok.NonNull;

import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.TaskLogger;

import wbs.platform.media.logic.MediaLogic;
import wbs.platform.media.logic.RawMediaLogic;
import wbs.platform.media.model.MediaObjectHelper;
import wbs.platform.media.model.MediaRec;

import wbs.web.responder.BufferedResponder;

public abstract
class AbstractMediaImageResponder
	extends BufferedResponder {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	MediaObjectHelper mediaHelper;

	@SingletonDependency
	MediaLogic mediaLogic;

	@SingletonDependency
	RawMediaLogic rawMediaLogic;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	// state

	MediaRec media;

	byte[] data;

	// hooks

	protected abstract
	byte[] getData (
			TaskLogger parentTaskLogger,
			MediaRec media);

	protected abstract
	String getMimeType (
			MediaRec media);

	// implementation

	@Override
	protected
	void prepare (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"prepare");

		) {

			media =
				mediaHelper.findRequired (
					transaction,
					requestContext.stuffIntegerRequired (
						"mediaId"));

			transform (
				transaction);

		}

	}

	protected
	void transform (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"transform");

		) {

			String rotate =
				requestContext.parameterOrEmptyString (
					"rotate");

			if (
				stringEqualSafe (
					rotate,
					"90")
			) {

				data =
					rawMediaLogic.writeImage (
						rawMediaLogic.rotateImage90 (
							rawMediaLogic.readImageRequired (
								transaction,
								getData (
									transaction,
									media),
								getMimeType (
									media))),
						getMimeType (media));

			} else if (
				stringEqualSafe (
					rotate,
					"180")
			) {

				data =
					rawMediaLogic.writeImage (
						rawMediaLogic.rotateImage180 (
							rawMediaLogic.readImageRequired (
								transaction,
								getData (
									transaction,
									media),
								getMimeType (
									media))),
						getMimeType (
							media));

			} else if (
				stringEqualSafe (
					rotate,
					"270")
			) {

				data =
					rawMediaLogic.writeImage (
						rawMediaLogic.rotateImage270 (
							rawMediaLogic.readImageRequired (
								transaction,
								getData (
									transaction,
									media),
								getMimeType (
									media))),
						getMimeType (
							media));

			} else {

				data =
					getData (
						transaction,
						media);

			}

		}

	}

	@Override
	protected
	void headers (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"headers");

		) {

			requestContext.contentType (
				getMimeType (
					media));

		}

	}

	@Override
	protected
	void render (
			@NonNull Transaction parentTransaction,
			@NonNull OutputStream outputStream) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"render");

		) {

			writeBytes (
				outputStream,
				data);

		}

	}

}
