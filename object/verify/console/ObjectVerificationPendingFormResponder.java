package wbs.platform.object.verify.console;

import static wbs.web.utils.HtmlBlockUtils.htmlParagraphWrite;

import lombok.NonNull;

import wbs.console.responder.ConsoleHtmlResponder;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.utils.string.FormatWriter;

@PrototypeComponent ("objectVerificationPendingFormResponder")
public
class ObjectVerificationPendingFormResponder
	extends ConsoleHtmlResponder {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

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

			// TODO

		}

	}

	@Override
	protected
	void renderHtmlBodyContents (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderHtmlBodyContents");

		) {

			htmlParagraphWrite (
				formatWriter,
				"TODO");

		}

	}

}
