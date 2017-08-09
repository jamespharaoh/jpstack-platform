package wbs.platform.queue.console;

import static wbs.utils.etc.LogicUtils.referenceEqualWithClass;
import static wbs.utils.etc.NullUtils.isNotNull;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphClose;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphOpen;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphWriteFormat;
import static wbs.web.utils.HtmlFormUtils.htmlFormClose;
import static wbs.web.utils.HtmlFormUtils.htmlFormOpenPostAction;

import lombok.NonNull;

import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.part.AbstractPagePart;
import wbs.console.priv.UserPrivChecker;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.platform.object.core.console.ObjectTypeConsoleHelper;
import wbs.platform.queue.model.QueueItemRec;
import wbs.platform.user.console.UserConsoleLogic;
import wbs.platform.user.model.UserRec;

import wbs.utils.string.FormatWriter;

@PrototypeComponent ("queueItemActionsPart")
public
class QueueItemActionsPart
	extends AbstractPagePart {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	ObjectTypeConsoleHelper objectTypeHelper;

	@SingletonDependency
	UserPrivChecker privChecker;

	@SingletonDependency
	QueueConsoleLogic queueConsoleLogic;

	@SingletonDependency
	QueueItemConsoleHelper queueItemHelper;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	// state

	QueueItemRec queueItem;

	boolean canSupervise;

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

			queueItem =
				queueItemHelper.findFromContextRequired (
					transaction);

			canSupervise =
				queueConsoleLogic.canSupervise (
					transaction,
					queueItem.getQueue ());

		}

	}

	@Override
	public
	void renderHtmlBodyContent (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderHtmlBodyContent");

		) {

			if (! canSupervise) {

				htmlParagraphWriteFormat (
					formatWriter,
					"You do not have permission to perform actions on this queue ",
					"item.");

				return;

			}

			htmlFormOpenPostAction (
				formatWriter,
				requestContext.resolveLocalUrl (
					"/queueItem.actions"));

			if (
				isNotNull (
					queueItem.getQueueItemClaim ())
			) {

				if (
					referenceEqualWithClass (
						UserRec.class,
						queueItem.getQueueItemClaim ().getUser (),
						userConsoleLogic.userRequired (
							transaction))
				) {

					htmlParagraphWriteFormat (
						formatWriter,
						"This queue item is claimed by you. You may return it ",
						"to the queue.");

					htmlParagraphOpen (
						formatWriter);

					formatWriter.writeLineFormat (
						"<input",
						" type=\"submit\"",
						" name=\"unclaim\"",
						" value=\"unclaim\"",
						">");

					htmlParagraphClose (
						formatWriter);

				} else {

					htmlParagraphWriteFormat (
						formatWriter,
						"This queue item is claimed by \"%h\". ",
						objectManager.objectPathMini (
							transaction,
							queueItem.getQueueItemClaim ().getUser ()),
						"You may return it to the queue or reclaim it ",
						"yourself.");

					htmlParagraphOpen (
						formatWriter);

					formatWriter.writeFormat (
						"<input",
						" type=\"submit\"",
						" name=\"unclaim\"",
						" value=\"unclaim\"",
						">");

					formatWriter.writeFormat (
						"<input",
						" type=\"submit\"",
						" name=\"reclaim\"",
						" value=\"reclaim\"",
						">");

					htmlParagraphClose (
						formatWriter);

				}

			} else {

				htmlParagraphWriteFormat (
					formatWriter,
					"There are no actions that you can perform on this queue ",
					"item at this time.");

			}

			htmlFormClose (
				formatWriter);

		}

	}

}
