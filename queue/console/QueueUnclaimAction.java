package wbs.platform.queue.console;

import static wbs.utils.etc.LogicUtils.referenceNotEqualWithClass;

import lombok.NonNull;

import wbs.console.action.ConsoleAction;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.NamedDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.Database;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.queue.model.QueueItemRec;
import wbs.platform.user.console.UserConsoleLogic;
import wbs.platform.user.model.UserRec;

import wbs.web.responder.WebResponder;

@PrototypeComponent ("queueUnclaimAction")
public
class QueueUnclaimAction
	extends ConsoleAction {

	// dependencies

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	QueueConsoleLogic queueConsoleLogic;

	@SingletonDependency
	QueueItemConsoleHelper queueItemHelper;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	// prototype dependencies

	@PrototypeDependency
	@NamedDependency ("queueHomeResponder")
	ComponentProvider <WebResponder> queueHomeResponderProvider;

	// details

	@Override
	public
	WebResponder backupResponder (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"backupResponder");

		) {

			return queueHomeResponderProvider.provide (
				taskLogger);

		}

	}

	// implementation

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

			long queueItemId =
				requestContext.parameterIntegerRequired (
					"queueItemId");

			QueueItemRec queueItem =
				queueItemHelper.findRequired (
					transaction,
					queueItemId);

			if (
				referenceNotEqualWithClass (
					UserRec.class,
					queueItem.getQueueItemClaim ().getUser (),
					userConsoleLogic.userRequired (
						transaction))
			) {

				requestContext.addError (
					"Queue item is not claimed by you");

				return null;

			}

			queueConsoleLogic.unclaimQueueItem (
				transaction,
				queueItem,
				userConsoleLogic.userRequired (
					transaction));

			transaction.commit ();

			requestContext.addNotice (
				"Queue item returned to queue");

			return null;

		}

	}

}