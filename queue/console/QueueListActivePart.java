package wbs.platform.queue.console;

import static wbs.utils.etc.LogicUtils.ifThenElseEmDash;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.NumberUtils.moreThanZero;
import static wbs.web.utils.HtmlAttributeUtils.htmlClassAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlDataAttribute;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableHeaderRowWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenList;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowOpen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import lombok.NonNull;

import wbs.console.context.ConsoleContext;
import wbs.console.context.ConsoleContextType;
import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.html.MagicTableScriptRef;
import wbs.console.html.ScriptRef;
import wbs.console.misc.JqueryScriptRef;
import wbs.console.module.ConsoleManager;
import wbs.console.part.AbstractPagePart;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.platform.queue.console.QueueSubjectSorter.QueueInfo;
import wbs.platform.queue.logic.DummyQueueCache;
import wbs.platform.queue.model.QueueRec;
import wbs.platform.user.console.UserConsoleLogic;

import wbs.utils.string.FormatWriter;
import wbs.utils.time.TimeFormatter;

@PrototypeComponent ("queueListActivePart")
public
class QueueListActivePart
	extends AbstractPagePart {

	// singleton dependencies

	@SingletonDependency
	ConsoleManager consoleManager;

	@SingletonDependency
	DummyQueueCache dummyQueueCache;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	TimeFormatter timeFormatter;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <QueueSubjectSorter> queueSubjectSorterProvider;

	// state

	List <QueueInfo> queueInfos;

	// details

	@Override
	public
	Set <ScriptRef> scriptRefs () {

		return ImmutableSet.<ScriptRef> builder ()

			.addAll (
				super.scriptRefs ())

			.add (
				JqueryScriptRef.instance)

			.add (
				MagicTableScriptRef.instance)

			.build ();

	}

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

			List <QueueInfo> queueInfosTemp =
				queueSubjectSorterProvider.provide (
					transaction)

				.queueCache (
					dummyQueueCache)

				.loggedInUser (
					userConsoleLogic.userRequired (
						transaction))

				.sort (
					transaction)

				.availableQueues ()

			;

			queueInfos =
				new ArrayList <> ();

			for (
				QueueInfo queueInfo
					: queueInfosTemp
			) {

				if (
					! objectManager.canView (
						transaction,
						queueInfo.queue ())
				) {
					continue;
				}

				queueInfos.add (
					queueInfo);

			}

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

			htmlTableOpenList (
				formatWriter);

			ConsoleContextType queueContextType =
				consoleManager.contextType (
					"queue:object",
					true);

			ConsoleContext queueContext =
				consoleManager.relatedContextRequired (
					transaction,
					requestContext.consoleContextRequired (),
					queueContextType);

			htmlTableHeaderRowWrite (
				formatWriter,
				"Object",
				"Queue",
				"Available",
				null,
				"Claimed",
				null,
				"Preferred",
				null,
				"Waiting",
				null,
				"Total",
				null);

			for (
				QueueInfo queueInfo
					: queueInfos
			) {

				QueueRec queue =
					queueInfo.queue ();

				// table row open

				htmlTableRowOpen (
					formatWriter,
					htmlClassAttribute (
						"magic-table-row"),
					htmlDataAttribute (
						"target-href",
						requestContext.resolveContextUrlFormat (
							"%s",
							queueContext.pathPrefix (),
							"/%u",
							integerToDecimalString (
								queue.getId ()))));

				// details

				htmlTableCellWrite (
					formatWriter,
					objectManager.objectPath (
						transaction,
						objectManager.getParentRequired (
							transaction,
							queue)));

				htmlTableCellWrite (
					formatWriter,
					queue.getCode ());

				// available

				htmlTableCellWrite (
					formatWriter,
					integerToDecimalString (
						queueInfo.availableItems ()));

				htmlTableCellWrite (
					formatWriter,
					ifThenElseEmDash (
						moreThanZero (
							queueInfo.availableItems ()),
						() -> timeFormatter.prettyDuration (
							queueInfo.oldestAvailable (),
							transaction.now ())));

				// claimed

				htmlTableCellWrite (
					formatWriter,
					integerToDecimalString (
						queueInfo.claimedItems ()));

				htmlTableCellWrite (
					formatWriter,
					ifThenElseEmDash (
						moreThanZero (
							queueInfo.claimedItems ()),
						() -> timeFormatter.prettyDuration (
							queueInfo.oldestClaimed (),
							transaction.now ())));

				// preferred

				htmlTableCellWrite (
					formatWriter,
					integerToDecimalString (
						queueInfo.totalUnavailableItems ()));

				htmlTableCellWrite (
					formatWriter,
					ifThenElseEmDash (
						moreThanZero (
							queueInfo.totalUnavailableItems ()),
						() -> timeFormatter.prettyDuration (
							queueInfo.oldestUnavailable (),
							transaction.now ())));

				// waiting

				htmlTableCellWrite (
					formatWriter,
					integerToDecimalString (
						queueInfo.waitingItems ()));

				htmlTableCellWrite (
					formatWriter,
					ifThenElseEmDash (
						moreThanZero (
							queueInfo.waitingItems ()),
						() -> timeFormatter.prettyDuration (
							queueInfo.oldestWaiting (),
							transaction.now ())));

				// total

				htmlTableCellWrite (
					formatWriter,
					integerToDecimalString (
						queueInfo.totalItems ()));

				htmlTableCellWrite (
					formatWriter,
					ifThenElseEmDash (
						moreThanZero (
							queueInfo.totalItems ()),
						() -> timeFormatter.prettyDuration (
							queueInfo.oldest (),
							transaction.now ())));

				// table row close

				htmlTableRowClose (
					formatWriter);

			}

			// table close

			htmlTableClose (
				formatWriter);

		}

	}

}
