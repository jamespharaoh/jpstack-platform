package wbs.platform.queue.console;

import static wbs.utils.etc.LogicUtils.ifNotNullThenElse;
import static wbs.utils.time.TimeUtils.toInterval;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableDetailsRowWriteRaw;
import static wbs.web.utils.HtmlTableUtils.htmlTableHeaderRowWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenDetails;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenList;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowOpen;

import java.util.Set;
import java.util.TreeSet;

import lombok.NonNull;

import org.joda.time.Interval;

import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.part.AbstractPagePart;
import wbs.console.priv.UserPrivChecker;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;

import wbs.platform.queue.model.QueueItemProcessedTimeComparator;
import wbs.platform.queue.model.QueueItemRec;
import wbs.platform.queue.model.QueueRec;
import wbs.platform.user.console.UserConsoleHelper;
import wbs.platform.user.console.UserConsoleLogic;
import wbs.platform.user.model.UserRec;

import wbs.utils.string.FormatWriter;
import wbs.utils.time.interval.TextualInterval;

@PrototypeComponent ("queueSupervisorItemsPart")
public
class QueueSupervisorItemsPart
	extends AbstractPagePart {

	// dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	QueueItemConsoleHelper queueItemHelper;

	@SingletonDependency
	UserPrivChecker privChecker;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	@SingletonDependency
	UserConsoleHelper userHelper;

	// state

	Set <QueueItemRec> queueItems;

	UserRec user;

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

			Interval interval =
				toInterval (
					TextualInterval.parseRequired (
						userConsoleLogic.timezone (
							transaction),
						requestContext.parameterRequired (
							"interval"),
						userConsoleLogic.hourOffset (
							transaction)));

			user =
				userHelper.findRequired (
					transaction,
					requestContext.parameterIntegerRequired (
						"userId"));

			queueItems =
				new TreeSet<> (
					QueueItemProcessedTimeComparator.instance);

			queueItems.addAll (
				queueItemHelper.findByProcessedTime (
					transaction,
					user,
					interval));

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

			renderParameterTable (
				transaction,
				formatWriter);

			renderContentTable (
				transaction,
				formatWriter);

		}

	}

	private
	void renderParameterTable (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderParameterTable");

		) {

			// open table

			htmlTableOpenDetails (
				formatWriter);

			// write user table row

			htmlTableDetailsRowWriteRaw (
				formatWriter,
				"User",
				() ->
					objectManager.writeTdForObjectMiniLink (
						transaction,
						formatWriter,
						privChecker,
						user));

			// close table

			htmlTableClose (
				formatWriter);

		}

	}

	private
	void renderContentTable (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderContentTable");

		) {

			// open table

			htmlTableOpenList (
				formatWriter);

			// write table header

			htmlTableHeaderRowWrite (
				formatWriter,
				"Object",
				"Queue",
				"Item",
				"Created",
				"Pending",
				"Processed");

			// write table contents

			for (
				QueueItemRec queueItem
					: queueItems
			) {

				QueueRec queue =
					ifNotNullThenElse (
						queueItem.getQueueSubject (),
						() -> queueItem.getQueueSubject ().getQueue (),
						() -> queueItem.getQueue ());

				Record <?> parent =
					objectManager.getParentRequired (
						transaction,
						queue);

				// open table row

				htmlTableRowOpen (
					formatWriter);

				objectManager.writeTdForObjectLink (
					transaction,
					formatWriter,
					privChecker,
					parent);

				objectManager.writeTdForObjectMiniLink (
					transaction,
					formatWriter,
					privChecker,
					queue,
					parent);

				objectManager.writeTdForObjectMiniLink (
					transaction,
					formatWriter,
					privChecker,
					queueItem,
					queue);

				htmlTableCellWrite (
					formatWriter,
					userConsoleLogic.timestampWithTimezoneString (
						transaction,
						queueItem.getCreatedTime ()));

				htmlTableCellWrite (
					formatWriter,
					userConsoleLogic.timestampWithTimezoneString (
						transaction,
						queueItem.getPendingTime ()));

				htmlTableCellWrite (
					formatWriter,
					userConsoleLogic.timestampWithTimezoneString (
						transaction,
						queueItem.getProcessedTime ()));

				htmlTableRowClose (
					formatWriter);

			}

			htmlTableClose (
				formatWriter);

		}

	}

}
