package wbs.platform.queue.console;

import lombok.NonNull;

import wbs.console.part.AbstractPagePart;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.database.Transaction;

import wbs.utils.string.FormatWriter;

@PrototypeComponent ("queueSubjectActionsPart")
public
class QueueSubjectActionsPart
	extends AbstractPagePart {

	// dependencies

	/*
	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	ObjectTypeConsoleHelper objectTypeHelper;

	@SingletonDependency
	PrivChecker privChecker;

	@SingletonDependency
	QueueConsoleLogic queueConsoleLogic;

	@SingletonDependency
	QueueItemConsoleHelper queueItemHelper;

	@SingletonDependency
	UserConsoleHelper userHelper;
	*/

	// state

	/*
	QueueTypeSpec queueTypeSpec;

	UserRec myUser;
	QueueItemRec queueItem;

	boolean canSupervise;
	*/

	// implementation

	@Override
	public
	void prepare (
			@NonNull Transaction parentTransaction) {

		/*
		myUser =
			userHelper.find (
				requestContext.userId ());

		queueItem =
			queueItemHelper.find (
				requestContext.stuffInt (
					"queueItemId"));

		queueTypeSpec =
			queueConsoleLogic.queueTypeSpec (
				queueItem.getQueue ().getQueueType ());

		String[] supervisorParts =
			queueTypeSpec.supervisorPriv ().split (":");

		Record<?> supervisorDelegate =
			(Record<?>)
			objectManager.dereference (
				queueItem.getQueue (),
				supervisorParts [0]);

		canSupervise =
			privChecker.can (
				supervisorDelegate,
				supervisorParts [1]);
		*/

	}

	@Override
	public
	void renderHtmlBodyContent (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		/*
		if (! canSupervise) {

			printFormat (
				"<p>You do not have permission to perform actions on this ",
				"queue item.</p>\n");

			return;

		}

		printFormat (
			"<form",
			" method=\"post\"",
			" action=\"%h\"",
			requestContext.resolveLocalUrl (
				"/queueItem.actions"),
			">\n");

		if (
			isNotNull (
				queueItem.getQueueItemClaim ())
		) {

			if (
				equal (
					queueItem.getQueueItemClaim ().getUser (),
					myUser)
			) {

				printFormat (
					"<p>This queue item is claimed by you.</p>\n");

			} else {

				printFormat (
					"<p>This queue item is claimed by \"%h\". You may return ",
					objectManager.objectPathMini (
						queueItem.getQueueItemClaim ().getUser ()),
					"it to the queue or reclaim it yourself.</p>\n");

			}

		} else {

			printFormat (
				"<p>There are no actions that you can perform on this queue ",
				"item at this time.</p>\n");

		}

		printFormat (
			"</form>\n");
		*/

	}

}
