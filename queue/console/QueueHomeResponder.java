package wbs.platform.queue.console;

import static wbs.utils.collection.CollectionUtils.collectionIsEmpty;
import static wbs.utils.etc.LogicUtils.ifNotNullThenElse;
import static wbs.utils.etc.NullUtils.isNotNull;
import static wbs.utils.etc.NullUtils.isNull;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.NumberUtils.moreThanZero;
import static wbs.utils.etc.OptionalUtils.optionalIf;
import static wbs.utils.etc.OptionalUtils.presentInstances;
import static wbs.utils.string.StringUtils.joinWithSpace;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.web.utils.HtmlAttributeUtils.htmlAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlClassAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlColumnSpanAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlDataAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlStyleAttribute;
import static wbs.web.utils.HtmlBlockUtils.htmlDivClose;
import static wbs.web.utils.HtmlBlockUtils.htmlDivOpen;
import static wbs.web.utils.HtmlBlockUtils.htmlDivWrite;
import static wbs.web.utils.HtmlBlockUtils.htmlHeadingTwoWrite;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphClose;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphOpen;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphWrite;
import static wbs.web.utils.HtmlBlockUtils.htmlSpanClose;
import static wbs.web.utils.HtmlBlockUtils.htmlSpanOpen;
import static wbs.web.utils.HtmlBlockUtils.htmlSpanWrite;
import static wbs.web.utils.HtmlFormUtils.htmlFormClose;
import static wbs.web.utils.HtmlFormUtils.htmlFormOpenPostAction;
import static wbs.web.utils.HtmlScriptUtils.htmlScriptBlockWrite;
import static wbs.web.utils.HtmlStyleUtils.htmlStyleBlockClose;
import static wbs.web.utils.HtmlStyleUtils.htmlStyleBlockOpen;
import static wbs.web.utils.HtmlStyleUtils.htmlStyleRuleClose;
import static wbs.web.utils.HtmlStyleUtils.htmlStyleRuleEntry;
import static wbs.web.utils.HtmlStyleUtils.htmlStyleRuleEntryWrite;
import static wbs.web.utils.HtmlStyleUtils.htmlStyleRuleOpen;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellOpen;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellWriteHtml;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableHeaderRowWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpen;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenList;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowOpen;
import static wbs.web.utils.HtmlUtils.htmlEncodeNonBreakingWhitespace;
import static wbs.web.utils.HtmlUtils.htmlLinkWrite;
import static wbs.web.utils.HtmlUtils.htmlLinkWriteInline;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import lombok.NonNull;

import wbs.console.context.ConsoleApplicationScriptRef;
import wbs.console.html.MagicTableScriptRef;
import wbs.console.html.ScriptRef;
import wbs.console.misc.JqueryScriptRef;
import wbs.console.priv.UserPrivChecker;
import wbs.console.request.ConsoleRequestContext;
import wbs.console.responder.ConsoleHtmlResponder;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.object.ObjectManager;

import wbs.platform.queue.console.QueueSubjectSorter.QueueInfo;
import wbs.platform.queue.logic.DummyQueueCache;
import wbs.platform.queue.model.QueueItemClaimObjectHelper;
import wbs.platform.queue.model.QueueItemClaimRec;
import wbs.platform.queue.model.QueueItemRec;
import wbs.platform.queue.model.QueueRec;
import wbs.platform.queue.model.QueueSubjectRec;
import wbs.platform.scaffold.model.SliceRec;
import wbs.platform.user.console.UserConsoleLogic;
import wbs.platform.user.model.UserObjectHelper;

import wbs.utils.string.FormatWriter;

@PrototypeComponent ("queueHomeResponder")
public
class QueueHomeResponder
	extends ConsoleHtmlResponder {

	// singleton dependencies

	@SingletonDependency
	DummyQueueCache dummyQueueCache;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ObjectManager objectManager;

	@SingletonDependency
	UserPrivChecker privChecker;

	@SingletonDependency
	QueueItemClaimObjectHelper queueItemClaimHelper;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	@SingletonDependency
	UserObjectHelper userHelper;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <QueueSubjectSorter> queueSubjectSorterProvider;

	// state

	boolean queueOptionsEnabled = true;

	List <QueueItemClaimRec> myClaimedItems;
	List <QueueInfo> queueInfos;

	Set <QueueRec> queues;

	// details

	@Override
	public
	Set <ScriptRef> scriptRefs () {

		if (queueOptionsEnabled) {

			return ImmutableSet.<ScriptRef> builder ()

				.addAll (
					super.scriptRefs ())

				.add (
					JqueryScriptRef.instance)

				.add (
					MagicTableScriptRef.instance)

				.add (
					ConsoleApplicationScriptRef.javascript (
						"/js/js-yaml-2.1.1.js"))

				.add (
					ConsoleApplicationScriptRef.javascript (
						"/js/wbs.js"))

				.add (
					ConsoleApplicationScriptRef.javascript (
						"/js/queue-home.js"))

				.build ();

		} else {

			return ImmutableSet.<ScriptRef> builder ()

				.addAll (
					super.scriptRefs ())

				.add (
					JqueryScriptRef.instance)

				.add (
					MagicTableScriptRef.instance)

				.build ();

		}

	}

	// implementation

	@Override
	protected
	void prepare (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"prepare");

		) {

			myClaimedItems =
				queueItemClaimHelper.findClaimed (
					transaction,
					userConsoleLogic.userRequired (
						transaction));

			// load queue list

			List <QueueInfo> queueInfosTemp =
				queueSubjectSorterProvider.provide (
					transaction)

				.queueCache (
					dummyQueueCache)

				.loggedInUser (
					userConsoleLogic.userRequired (
						transaction))

				.effectiveUser (
					userConsoleLogic.userRequired (
						transaction))

				.sort (
					transaction)

				.availableQueues ();

			queueInfos =
				new ArrayList<> ();

			for (
				QueueInfo queueInfo
					: queueInfosTemp
			) {

				if (queueInfo.availableItems () == 0)
					continue;

				queueInfos.add (queueInfo);

			}

			// unclaimed items

			queues =
				ImmutableSet.<QueueRec> builder ()

				.addAll (
					queueInfos.stream ()

					.filter (
						queueInfo ->
							moreThanZero (
								queueInfo.availableItems ()))

					.map (
						queueInfo ->
							queueInfo.queue ())

					.iterator ()

				)

				.addAll (
					myClaimedItems.stream ()

					.map (
						QueueItemClaimRec::getQueueItem)

					.map (
						QueueItemRec::getQueueSubject)

					.map (
						QueueSubjectRec::getQueue)

					.iterator ()

				)

				.build ();

		}

	}

	@Override
	protected
	void renderHtmlHeadContents (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderHtmlHeadContents");

		) {

			super.renderHtmlHeadContents (
				transaction,
				formatWriter);

			htmlScriptBlockWrite (
				formatWriter,
				"top.show_inbox (true)");

			renderStyles (
				formatWriter);

		}

	}

	private
	void renderStyles (
			@NonNull FormatWriter formatWriter) {

		if (
			collectionIsEmpty (
				queues)
		) {
			return;
		}

		htmlStyleBlockOpen (
			formatWriter);

		for (
			QueueRec queue
				: queues.stream ()
					.sorted ()
					.collect (Collectors.toList ())
		) {

			if (

				isNull (
					queue.getBackgroundColour ())

				&& isNull (
					queue.getForegroundColour ())

			) {
				continue;
			}

			htmlStyleRuleOpen (
				formatWriter,
				stringFormat (
					"table.list tr.queue-%h td",
					integerToDecimalString (
						queue.getId ())));

			if (
				isNotNull (
					queue.getBackgroundColour ())
			) {

				htmlStyleRuleEntryWrite (
					formatWriter,
					"background-color",
					queue.getBackgroundColour ());

			}

			if (
				isNotNull (
					queue.getForegroundColour ())
			) {

				htmlStyleRuleEntryWrite (
					formatWriter,
					"color",
					queue.getForegroundColour ());

			}

			htmlStyleRuleClose (
				formatWriter);

		}

		htmlStyleBlockClose (
			formatWriter);

	}

	protected
	void goQueues (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"goQueues");

		) {

			htmlHeadingTwoWrite (
				formatWriter,
				"Queues");

			// nothing to claim

			if (queueInfos.isEmpty ()) {

				htmlParagraphWrite (
					formatWriter,
					"Nothing to claim");

				return;

			}

			// render queue stuff

			renderOptions (
				formatWriter);

			renderQueueItems (
				transaction,
				formatWriter);

		}

	}

	private
	void renderOptions (
			@NonNull FormatWriter formatWriter) {

		// templates start

		htmlDivOpen (
			formatWriter,
			htmlAttribute (
				"style",
				"display: none"));

		// option set template

		htmlParagraphOpen (
			formatWriter,
			htmlClassAttribute (
				"optionSet"));

		htmlSpanWrite (
			formatWriter,
			"",
			htmlClassAttribute (
				"optionSetName"));

		htmlSpanWrite (
			formatWriter,
			"",
			htmlClassAttribute (
				"optionSetOptions"));

		htmlParagraphClose (
			formatWriter);

		// option template

		htmlSpanOpen (
			formatWriter,
			htmlClassAttribute (
				"option"));

		formatWriter.writeLineFormat (
			"<input",
			" type=\"checkbox\"",
			" class=\"optionCheckbox\"",
			">");

		formatWriter.writeLineFormat (
			"<label",
			" class=\"optionLabel\"",
			"></label>");

		htmlSpanClose (
			formatWriter);

		htmlDivClose (
			formatWriter);

		// option sets

		htmlDivWrite (
			formatWriter,
			"",
			htmlClassAttribute (
				"optionSets"));

		// enable/disable link

		htmlParagraphOpen (
			formatWriter);

		htmlSpanWrite (
			formatWriter,
			"",
			htmlClassAttribute (
				"disabledInfo"));

		formatWriter.writeIndent ();

		formatWriter.writeFormat (
			"(");

		htmlLinkWriteInline (
			formatWriter,
			"javascript:void (0)",
			"",
			htmlClassAttribute (
				"showHideLink"));

		formatWriter.writeFormat (
			")");

		formatWriter.writeNewline ();

		htmlParagraphClose (
			formatWriter);

	}

	private
	void renderQueueItems (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderQueueItems");

		) {

			// table open

			htmlTableOpen (
				formatWriter,
				htmlClassAttribute (
					"list",
					"queueItemTable"));

			htmlTableHeaderRowWrite (
				formatWriter,
				"Claim",
				"Type",
				"Object",
				"Queue",
				"Num",
				"Pri",
				"Oldest");

			if (queueOptionsEnabled) {

				htmlTableRowOpen (
					formatWriter,
					htmlClassAttribute (
						"loadingRow"));

				htmlTableCellWrite (
					formatWriter,
					"loading...",
					htmlColumnSpanAttribute (6l));

				htmlTableRowClose (
					formatWriter);

			}

			for (
				QueueInfo queueInfo
					: queueInfos
			) {

				if (queueInfo.availableItems () == 0)
					continue;

				QueueRec queue =
					queueInfo.queue ();

				Record <?> parent =
					objectManager.getParentRequired (
						transaction,
						queue);

				Optional <SliceRec> slice =
					objectManager.getAncestor (
						transaction,
						SliceRec.class,
						queue);

				String parentTypeCode =
					objectManager.getObjectTypeCode (
						transaction,
						parent);

				String parentCode =
					objectManager.getCode (
						transaction,
						parent);

				// table row open

				htmlTableRowOpen (
					formatWriter,

					htmlClassAttribute (
						joinWithSpace (
							"queueItemRow",
							stringFormat (
								"queue-%h",
								integerToDecimalString (
									queue.getId ())))),

					htmlStyleAttribute (
						presentInstances (
							optionalIf (
								queueOptionsEnabled,
								() -> htmlStyleRuleEntry (
									"display",
									"none")))),

					htmlDataAttribute (
						"parent-object-type-code",
						parentTypeCode),

					htmlDataAttribute (
						"parent-object-code",
						parentCode),

					htmlDataAttribute (
						"queue-type-code",
						queue.getQueueType ().getCode ()),

					htmlDataAttribute (
						"queue-code",
						queue.getCode ()),

					htmlDataAttribute (
						"slice-code",
						ifNotNullThenElse (
							slice,
							() -> slice.get ().getCode (),
							() -> "")),

					htmlDataAttribute (
						"oldest-timestamp",
						queueInfo.oldestAvailable ().toString ())

				);

				// claim cell

				htmlTableCellOpen (
					formatWriter);

				htmlFormOpenPostAction (
					formatWriter,
					requestContext.resolveApplicationUrl (
						"/queues/queue.claim"));

				formatWriter.writeLineFormat (
					"<input",
					" type=\"hidden\"",
					" name=\"queue_id\"",
					" value=\"%h\"",
					integerToDecimalString (
						queue.getId ()),
					">");

				formatWriter.writeLineFormat (
					"<input",
					" type=\"submit\"",
					" value=\"claim\"",
					">");

				htmlFormClose (
					formatWriter);

				htmlTableCellClose (
					formatWriter);

				// type code

				htmlTableCellWrite (
					formatWriter,
					parentTypeCode);

				// object path

				htmlTableCellWrite (
					formatWriter,
					objectManager.objectPathMini (
						transaction,
						parent,
						userConsoleLogic.sliceRequired (
							transaction)));

				// queue code

				htmlTableCellWrite (
					formatWriter,
					queue.getCode ());

				// available items

				htmlTableCellWrite (
					formatWriter,
					integerToDecimalString (
						queueInfo.availableItems ()));

				// priority

				if (
					privChecker.canRecursive (
						transaction,
						objectManager.getParentRequired (
							transaction,
							queueInfo.queue ()),
						"supervisor")
				) {

					htmlTableCellWrite (
						formatWriter,
						integerToDecimalString (
							queueInfo.highestPriorityAvailable ()));

				} else {

					htmlTableCellWrite (
						formatWriter,
						"");

				}

				// oldest

				htmlTableCellWriteHtml (
					formatWriter,
					htmlEncodeNonBreakingWhitespace (
						userConsoleLogic.prettyDuration (
							transaction,
							queueInfo.oldestAvailable (),
							transaction.now ())),
					htmlClassAttribute (
						"queueItemOldest"));

				// table row close

				htmlTableRowClose (
					formatWriter);

			}

			// table close

			htmlTableClose (
				formatWriter);

		}

	}

	protected
	void goMyItems (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"goMyItems");

		) {

			if (
				collectionIsEmpty (
					myClaimedItems)
			) {
				return;
			}

			htmlHeadingTwoWrite (
				formatWriter,
				"My items");

			htmlTableOpenList (
				formatWriter);

			htmlTableHeaderRowWrite (
				formatWriter,
				"Unclaim",
				"Object",
				"Queue",
				"Timestamp",
				"Source",
				"Details");

			int maxItems = 100;

			for (
				QueueItemClaimRec queueItemClaim
					: myClaimedItems
			) {

				QueueItemRec queueItem =
					queueItemClaim.getQueueItem ();

				QueueSubjectRec queueSubject =
					queueItem.getQueueSubject ();

				QueueRec queue =
					queueSubject.getQueue ();

				htmlTableRowOpen (
					formatWriter,

					htmlClassAttribute (
						"magic-table-row",
						stringFormat (
							"queue-%h",
							integerToDecimalString (
								queue.getId ()))),

					htmlDataAttribute (
						"target-href",
						requestContext.resolveApplicationUrlFormat (
							"/queues",
							"/queue.item",
							"?id=%s",
							integerToDecimalString (
								queueItem.getId ())))

				);

				htmlTableCellOpen (
					formatWriter);

				htmlFormOpenPostAction (
					formatWriter,
					requestContext.resolveApplicationUrl (
						"/queues/queue.unclaim"));

				formatWriter.writeLineFormat (
					"<input",
					" type=\"hidden\"",
					" name=\"queueItemId\"",
					" value=\"%h\"",
					integerToDecimalString (
						queueItem.getId ()),
					">");

				formatWriter.writeLineFormat (
					"<input",
					" type=\"submit\"",
					" value=\"unclaim\"",
					">");

				htmlFormClose (
					formatWriter);

				htmlTableCellClose (
					formatWriter);

				htmlTableCellWrite (
					formatWriter,
					objectManager.objectPath (
						transaction,
						objectManager.getParentRequired (
							transaction,
							queue),
						userConsoleLogic.sliceRequired (
							transaction)));

				htmlTableCellWrite (
					formatWriter,
					queue.getCode ());

				htmlTableCellWrite (
					formatWriter,
					userConsoleLogic.timestampWithTimezoneString (
						transaction,
						queueItem.getCreatedTime ()));

				htmlTableCellWrite (
					formatWriter,
					queueItem.getSource ());

				htmlTableCellWrite (
					formatWriter,
					queueItem.getDetails ());

				htmlTableRowClose (
					formatWriter);

				if (maxItems -- == 0)
					break;

			}

			htmlTableClose (
				formatWriter);

		}

	}

	@Override
	protected
	void renderHtmlBodyContents (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderHtmlBodyContents");

		) {

			renderLinks (
				transaction,
				formatWriter);

			requestContext.flushNotices (
				formatWriter);

			goQueues (
				transaction,
				formatWriter);

			goMyItems (
				transaction,
				formatWriter);

		}

	}

	private
	void renderLinks (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderLinks");

		) {

			htmlParagraphOpen (
				formatWriter,
				htmlClassAttribute (
					"links"));

			htmlLinkWrite (
				formatWriter,
				requestContext.resolveApplicationUrl (
					"/queues/queue.home"),
				"Refresh");

			htmlLinkWrite (
				formatWriter,
				"#",
				"Close",

				htmlAttribute (
					"onclick",
					"top.show_inbox (false);")

			);

			htmlParagraphClose (
				formatWriter);

		}

	}

}
