package wbs.platform.queue.console;

import static wbs.utils.etc.EnumUtils.enumNotEqualSafe;
import static wbs.utils.etc.LogicUtils.referenceEqualWithClass;
import static wbs.utils.etc.OptionalUtils.optionalIsPresent;

import lombok.NonNull;

import wbs.console.action.ConsoleAction;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.NamedDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.Database;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.queue.model.QueueItemRec;
import wbs.platform.queue.model.QueueItemState;
import wbs.platform.user.console.UserConsoleLogic;
import wbs.platform.user.model.UserRec;

import wbs.web.responder.WebResponder;

@PrototypeComponent ("queueItemActionsAction")
public
class QueueItemActionsAction
	extends ConsoleAction {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	QueueConsoleLogic queueConsoleLogic;

	@SingletonDependency
	QueueItemConsoleHelper queueItemHelper;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	// prototype dependencies

	@PrototypeDependency
	@NamedDependency ("queueItemActionsResponder")
	ComponentProvider <WebResponder> actionsResponderProvider;

	// state

	UserRec user;

	QueueItemRec queueItem;

	boolean canSupervise;

	// details

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

			return actionsResponderProvider.provide (
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

			// load data

			user =
				userConsoleLogic.userRequired (
					transaction);

			queueItem =
				queueItemHelper.findFromContextRequired (
					transaction);

			canSupervise =
				queueConsoleLogic.canSupervise (
					transaction,
					queueItem.getQueue ());

			if (! canSupervise) {

				requestContext.addError (
					"Permission denied");

				return null;

			}

			// hand off to appropriate method

			if (
				optionalIsPresent (
					requestContext.parameter (
						"unclaim"))
			) {

				return unclaimQueueItem (
					transaction);

			} else if (
				optionalIsPresent (
					requestContext.parameter (
						"reclaim"))
			) {

				return reclaimQueueItem (
					transaction);

			} else {

				throw new RuntimeException ();

			}

		}

	}

	private
	WebResponder unclaimQueueItem (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"unclaimQueueItem");

		) {

			if (

				enumNotEqualSafe (
					queueItem.getState (),
					QueueItemState.claimed)

			) {

				requestContext.addError (
					"Queue item is not claimed");

				return null;

			}

			queueConsoleLogic.unclaimQueueItem (
				transaction,
				queueItem,
				user);

			transaction.commit ();

			requestContext.addNotice (
				"Queue item returned to queue");

			return null;

		}

	}

	private
	WebResponder reclaimQueueItem (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"reclaimQueueItem");

		) {

			if (

				enumNotEqualSafe (
					queueItem.getState (),
					QueueItemState.claimed)

			) {

				requestContext.addWarning (
					"Queue item is not claimed");

				return null;

			}

			if (

				referenceEqualWithClass (
					UserRec.class,
					queueItem.getQueueItemClaim ().getUser (),
					user)

			) {

				requestContext.addWarning (
					"Queue item is already claimed by you");

				return null;

			}

			queueConsoleLogic.reclaimQueueItem (
				transaction,
				queueItem,
				queueItem.getQueueItemClaim ().getUser (),
				user);

			transaction.commit ();

			requestContext.addNotice (
				"Queue item reclaimed by you");

			return null;

		}

	}

}
