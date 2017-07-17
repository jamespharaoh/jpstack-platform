package wbs.platform.queue.console;

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
import wbs.platform.queue.model.QueueRec;
import wbs.platform.user.console.UserConsoleLogic;

import wbs.web.responder.WebResponder;

@PrototypeComponent ("queueClaimAction")
public
class QueueClaimAction
	extends ConsoleAction {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	QueueConsoleLogic queueConsoleLogic;

	@SingletonDependency
	QueueConsoleHelper queueHelper;

	@SingletonDependency
	QueueManager queuePageFactoryManager;

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

			Long queueId =
				Long.parseLong (
					requestContext.parameterRequired (
						"queue_id"));

			QueueRec queue =
				queueHelper.findRequired (
					transaction,
					queueId);

			QueueItemRec queueItem =
				queueConsoleLogic.claimQueueItem (
					transaction,
					queue,
					userConsoleLogic.userRequired (
						transaction));

			if (queueItem == null) {

				requestContext.addError (
					"No more items to claim in this queue");

				return null;

			}

			WebResponder responder =
				queuePageFactoryManager.getItemResponder (
					transaction,
					requestContext,
					queueItem);

			transaction.commit ();

			return responder;

		}

	}

}