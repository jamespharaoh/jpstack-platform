package wbs.platform.exception.console;

import static wbs.utils.thread.ConcurrentUtils.futureValue;

import java.util.concurrent.Future;

import com.google.gson.JsonObject;

import lombok.NonNull;

import wbs.console.part.PagePart;
import wbs.console.priv.UserPrivChecker;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.GlobalId;
import wbs.framework.logging.LogContext;

import wbs.platform.status.console.StatusLine;

@SingletonComponent ("exceptionStatusLine")
public
class ExceptionStatusLine
	implements StatusLine {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	NumExceptionsCache numExceptionsCache;

	@SingletonDependency
	NumFatalExceptionsCache numFatalExceptionsCache;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <ExceptionStatusLinePart> exceptionStatusLinePart;

	// details

	@Override
	public
	String typeName () {
		return "exceptions";
	}

	// implementation

	@Override
	public
	PagePart createPagePart (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createPagePart");

		) {

			return exceptionStatusLinePart.provide (
				transaction);

		}

	}

	@Override
	public
	Future <JsonObject> getUpdateData (
			@NonNull Transaction parentTransaction,
			@NonNull UserPrivChecker privChecker) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"getUpdateData");

		) {

			JsonObject updateData =
				new JsonObject ();

			// count exceptions (if visible)

			if (
				privChecker.canRecursive (
					transaction,
					GlobalId.root,
					"alert")
			) {

				updateData.addProperty (
					"exceptions",
					numExceptionsCache.get (
						transaction));

			} else {

				updateData.addProperty (
					"exceptions",
					0);

			}

			updateData.addProperty (
				"fatalExceptions",
				numFatalExceptionsCache.get (
					transaction));

			return futureValue (
				updateData);

		}

	}

}
