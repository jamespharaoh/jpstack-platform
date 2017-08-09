package wbs.platform.media.console;

import static wbs.utils.etc.IoUtils.writeBytes;
import static wbs.utils.etc.Misc.runFilter;

import java.io.OutputStream;

import lombok.NonNull;

import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.platform.media.model.MediaRec;

import wbs.web.responder.BufferedResponder;

@PrototypeComponent ("mediaVideoResponder")
public
class MediaVideoResponder
	extends BufferedResponder {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	MediaConsoleHelper mediaHelper;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	// state

	byte[] data;

	// implementation

	@Override
	public
	void prepare (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"prepare");

		) {

			MediaRec media =
				mediaHelper.findFromContextRequired (
					transaction);

			data =
				runFilter (
					transaction,
					media.getContent ().getData (),
					".3gp",
					".flv",
					"ffmpeg",
					"-y",
					"-i",
					"<in>",
					"<out>");

		} catch (InterruptedException interruptedException) {

			Thread.currentThread ().interrupt ();

			throw new RuntimeException (
				interruptedException);

		}

	}

	@Override
	public
	void headers (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"headers");

		) {

			requestContext.contentType (
				"video/x-flv");

		}

	}

	@Override
	public
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
