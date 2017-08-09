package wbs.platform.core.console;

import static wbs.utils.etc.Misc.doNothing;
import static wbs.web.utils.HtmlScriptUtils.htmlScriptBlockWrite;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.utils.string.FormatWriter;

import wbs.web.responder.BufferedTextResponder;

@PrototypeComponent ("coreLogoffResponder")
public
class CoreLogoffResponder
	extends BufferedTextResponder {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// implementation

	@Override
	protected
	void prepare (
			@NonNull Transaction parentTransaction) {

		doNothing ();

	}

	@Override
	protected
	void headers (
			@NonNull Transaction parentTransaction) {

		doNothing ();

	}

	@Override
	protected
	void render (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"render");

		) {

			htmlScriptBlockWrite (
				formatWriter,
				"window.top.location = '/';");

		}

	}

}
