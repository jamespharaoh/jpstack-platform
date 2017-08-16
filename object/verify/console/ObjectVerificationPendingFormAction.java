package wbs.platform.object.verify.console;

import static wbs.utils.etc.NullUtils.isNotNull;
import static wbs.utils.etc.OptionalUtils.optionalOf;

import lombok.NonNull;

import wbs.console.action.ConsoleAction;
import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.request.ConsoleRequestContext;
import wbs.console.responder.RedirectResponder;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.NamedDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.Database;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.entity.meta.model.ModelMetaLoader;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.object.verify.logic.ObjectVerifyLogic;
import wbs.platform.object.verify.model.ObjectVerificationRec;
import wbs.platform.queue.logic.QueueLogic;
import wbs.platform.user.console.UserConsoleLogic;

import wbs.web.responder.WebResponder;

@PrototypeComponent ("objectVerificationPendingFormAction")
public
class ObjectVerificationPendingFormAction <
	RecordType extends Record <RecordType>
>
	extends ConsoleAction {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ModelMetaLoader modelMetaLoader;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	ObjectVerifyLogic objectVerifyLogic;

	@SingletonDependency
	QueueLogic queueLogic;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	@SingletonDependency
	ObjectVerificationConsoleHelper verificationHelper;

	// prototype dependencies

	@PrototypeDependency
	@NamedDependency ("objectVerificationPendingFormResponder")
	ComponentProvider <WebResponder> pendingFormResponder;

	@PrototypeDependency
	ComponentProvider <RedirectResponder> redirectResponder;

	// implementation

	@Override
	protected
	WebResponder backupResponder (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"backupResponder");

		) {

			return pendingFormResponder.provide (
				taskLogger);

		}

	}

	@Override
	protected
	WebResponder goReal (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"goReal");

		) {

			ObjectVerificationRec verification =
				verificationHelper.findFromContextRequired (
					transaction);

			objectVerifyLogic.performVerification (
				transaction,
				verification,
				optionalOf (
					userConsoleLogic.userRequired (
						transaction)));

			if (verification.getValid ()) {

				if (
					isNotNull (
						verification.getQueueItem ())
				) {

					queueLogic.processQueueItem (
						transaction,
						verification.getQueueItem (),
						userConsoleLogic.userRequired (
							transaction));

					verification.setQueueItem (
						null);

				}

				transaction.commit ();

				requestContext.addNotice (
					"Data verification success");

				return redirectResponder.provide (
					transaction,
					redirectResponder ->
						redirectResponder

					.targetUrl (
						requestContext.resolveApplicationUrl (
							"/queues/queue.home"))

				);

			} else if (
				requestContext.parameterExists (
					"skip")
			) {

				if (
					isNotNull (
						verification.getQueueItem ())
				) {

					queueLogic.cancelQueueItem (
						transaction,
						verification.getQueueItem ());

					verification.setQueueItem (
						null);

				}

				transaction.commit ();

				requestContext.addNotice (
					"Data verification skipped");

				return redirectResponder.provide (
					transaction,
					redirectResponder ->
						redirectResponder

					.targetUrl (
						requestContext.resolveApplicationUrl (
							"/queues/queue.home"))

				);

			} else {

				transaction.commit ();

				requestContext.addError (
					"Data verifcation failed");

				return null;

			}

		}

	}

}
