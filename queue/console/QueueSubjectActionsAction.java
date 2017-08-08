package wbs.platform.queue.console;

import lombok.NonNull;

import wbs.console.action.ConsoleAction;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.NamedDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.Database;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.web.responder.WebResponder;

@PrototypeComponent ("queueSubjectActionsAction")
public
class QueueSubjectActionsAction
	extends ConsoleAction {

	// dependencies

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	// prototype dependencies

	@PrototypeDependency
	@NamedDependency ("queueSubjectActionsResponder")
	ComponentProvider <WebResponder> actionsResponderProvider;

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

		/*

		// begin transaction

		OwnedTransaction transaction =
			database.beginReadWrite (
				"QueueSubjectActionsAction.goReal ()",
				this);

		// load data

		myUser =
			userHelper.find (
				requestContext.userId ());

		message =
			messageHelper.find (
				requestContext.stuffInt (
					"messageId"));

		// hand off to appropriate method

		if (
			isNotNull (
				requestContext.parameter (
					"manuallyUndeliver"))
		) {

			return manuallyUndeliver (
				transaction);

		} else if (
			isNotNull (
				requestContext.parameter (
					"manuallyDeliver"))
		) {

			return manuallyDeliver (
				transaction);

		} else if (
			isNotNull (
				requestContext.parameter (
					"manuallyUnhold"))
		) {

			return manuallyUnhold (
				transaction);

		} else if (
			isNotNull (
				requestContext.parameter (
					"manuallyRetry"))
		) {

			return manuallyRetry (
				transaction);

		} else {

			throw new RuntimeException ();

		}

	}

	private
	Responder manuallyUndeliver (
			@NonNull OwnedTransaction transaction) {

		if (
			notEqual (
				message.getDirection (),
				MessageDirection.out)
		) {
			throw new RuntimeException ();
		}

		if (
			notIn (
				message.getStatus (),
				MessageStatus.sent,
				MessageStatus.submitted,
				MessageStatus.delivered)
		) {

			requestContext.addError (
				"Message in invalid state for this operation");

			return null;

		}

		messageLogic.messageStatus (
			message,
			MessageStatus.manuallyUndelivered);

		eventLogic.createEvent (
			"message_manually_undelivered",
			myUser,
			message);

		transaction.commit ();

		requestContext.addNotice (
			"Message manually undelivered");

		return null;

	}

	private
	Responder manuallyDeliver (
			@NonNull OwnedTransaction transaction) {

		if (
			notEqual (
				message.getDirection (),
				MessageDirection.out)
		) {
			throw new RuntimeException ();
		}

		if (
			notIn (
				message.getStatus (),
				MessageStatus.undelivered,
				MessageStatus.reportTimedOut,
				MessageStatus.manuallyUndelivered)
		) {

			requestContext.addError (
				"Message in invalid state for this operation");

			return null;

		}

		messageLogic.messageStatus (
			message,
			MessageStatus.manuallyDelivered);

		eventLogic.createEvent (
			"message_manually_delivered",
			myUser,
			message);

		transaction.commit ();

		requestContext.addNotice (
			"Message manually undelivered");

		return null;

	}

	private
	Responder manuallyUnhold (
			@NonNull OwnedTransaction transaction) {

		if (
			notEqual (
				message.getStatus (),
				MessageStatus.held)
		) {

			requestContext.addError (
				"Message in invalid state for this operation");

			return null;

		}

		outboxLogic.unholdMessage (
			message);

		eventLogic.createEvent (
			"message_manually_unheld",
			myUser,
			message);

		transaction.commit ();

		requestContext.addNotice (
			"Message manually unheld");

		return null;

	}

	private
	Responder manuallyRetry (
			@NonNull OwnedTransaction transaction) {

		if (
			notIn (
				message.getStatus (),
				MessageStatus.failed,
				MessageStatus.cancelled)
		) {

			requestContext.addError (
				"Message in invalid state for this operation");

			return null;

		}

		outboxLogic.retryMessage (
			message);

		eventLogic.createEvent (
			"message_manually_retried",
			myUser,
			message);

		transaction.commit ();

		requestContext.addNotice (
			"Message manually retried");

		*/

		return null;

	}

}
