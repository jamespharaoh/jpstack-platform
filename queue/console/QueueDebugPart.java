package wbs.platform.queue.console;

import static wbs.utils.collection.MapUtils.emptyMap;
import static wbs.utils.etc.LogicUtils.booleanToString;
import static wbs.utils.etc.LogicUtils.booleanToYesNo;
import static wbs.utils.etc.LogicUtils.ifNotNullThenElse;
import static wbs.utils.etc.LogicUtils.ifNotNullThenElseEmDash;
import static wbs.utils.etc.NullUtils.isNotNull;
import static wbs.utils.etc.NumberUtils.integerNotEqualSafe;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.OptionalUtils.optionalOrNull;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.web.utils.HtmlAttributeUtils.htmlColumnSpanAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlRowSpanAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlStyleAttribute;
import static wbs.web.utils.HtmlStyleUtils.htmlStyleRuleEntry;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellOpen;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableHeaderRowWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenList;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowOpen;
import static wbs.web.utils.HtmlUtils.htmlLinkWrite;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import lombok.NonNull;

import org.joda.time.Duration;
import org.joda.time.Instant;

import wbs.console.forms.core.ConsoleForm;
import wbs.console.forms.core.ConsoleFormType;
import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.module.ConsoleModule;
import wbs.console.part.AbstractPagePart;
import wbs.console.priv.UserPrivChecker;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.NamedDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;

import wbs.platform.object.core.console.ObjectTypeConsoleHelper;
import wbs.platform.object.core.model.ObjectTypeRec;
import wbs.platform.queue.console.QueueSubjectSorter.QueueInfo;
import wbs.platform.queue.console.QueueSubjectSorter.SubjectInfo;
import wbs.platform.queue.logic.MasterQueueCache;
import wbs.platform.user.console.UserConsoleHelper;
import wbs.platform.user.console.UserConsoleLogic;

import wbs.utils.string.FormatWriter;

@PrototypeComponent ("queueDebugPart")
public
class QueueDebugPart
	extends AbstractPagePart {

	// singleton dependencies

	@SingletonDependency
	@NamedDependency ("queueDebugFormType")
	ConsoleFormType <QueueDebugForm> formType;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	ObjectTypeConsoleHelper objectTypeHelper;

	@SingletonDependency
	@NamedDependency
	ConsoleModule queueConsoleModule;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	@SingletonDependency
	UserConsoleHelper userHelper;

	@SingletonDependency
	UserPrivChecker userPrivChecker;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <MasterQueueCache> masterQueueCacheProvider;

	@PrototypeDependency
	ComponentProvider <QueueSubjectSorter> queueSubjectSorterProvider;

	// state

	ConsoleForm <QueueDebugForm> form;

	List <QueueInfo> queueInfos;

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

			form =
				formType.buildResponse (
					transaction,
					emptyMap (),
					new QueueDebugForm ()

				.userId (
					userConsoleLogic.userIdRequired ())

			);

			form.update (
				transaction);

			SortedQueueSubjects sortedQueueSubjects =
				queueSubjectSorterProvider.provide (
					transaction)

				.queueCache (
					masterQueueCacheProvider.provide (
						transaction)

					.setup (
						transaction)

				)

				.loggedInUser (
					userConsoleLogic.userRequired (
						transaction))

				.effectiveUser (
					optionalOrNull (
						userHelper.find (
							transaction,
							form.value ().userId ())))

				.sort (
					transaction);

			queueInfos =
				sortedQueueSubjects.allQueues ().stream ()

				.filter (
					queueInfo ->
						userPrivChecker.canRecursive (
							transaction,
							queueInfo.queue (),
							"supervisor"))

				.collect (
					Collectors.toList ());

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

			form.outputFormTable (
				transaction,
				formatWriter,
				"get",
				requestContext.resolveLocalUrl (
					"/queue.debug"),
				"update");

			htmlTableOpenList (
				formatWriter);

			htmlTableHeaderRowWrite (
				formatWriter,
				"Queue",
				"Own operator activity",
				"Preferred user",
				"Overflow",
				"Conclusion");

			for (
				QueueInfo queueInfo
					: queueInfos
			) {

				if (

					isNotNull (
						form.value ().sliceId ())

					&& integerNotEqualSafe (
						form.value ().sliceId (),
						queueInfo.slice.getId ())

				) {
					continue;
				}

				renderQueueInfo (
					transaction,
					formatWriter,
					queueInfo);

			}

			htmlTableClose (
				formatWriter);

		}

	}

	private
	void renderQueueInfo (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter,
			@NonNull QueueInfo queueInfo) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderQueueInfo");

		) {

			Record <?> queueParent =
				objectManager.getParentRequired (
					transaction,
					queueInfo.queue ());

			ObjectTypeRec queueParentType =
				objectTypeHelper.findRequired (
					transaction,
					objectManager.getObjectTypeId (
						transaction,
						queueParent));

			htmlTableRowOpen (
				formatWriter,
				htmlColumnSpanAttribute (
					form.value ().allItems ()
						? 2l * queueInfo.subjectInfos.size ()
						: 2l));

			List <SubjectInfo> subjectInfos =
				form.value ().allItems ()
					? queueInfo.subjectInfos
					: ImmutableList.of (
						queueInfo.subjectInfos.get (0));

			int row = 0;

			for (
				SubjectInfo subjectInfo
					: subjectInfos
			) {

				if (row == 0) {

					// queue

					htmlTableCellOpen (
						formatWriter,
						htmlRowSpanAttribute (
							2l * subjectInfos.size ()),
						htmlStyleAttribute (
							htmlStyleRuleEntry (
								"vertical-align",
								"top")));

					formatWriter.writeLineFormat (
						"Slice: <a href=\"%h\">%h</a><br>",
						objectManager.localLink (
							transaction,
							queueInfo.slice ()),
						queueInfo.slice ().getCode ());

					formatWriter.writeLineFormat (
						"Type: <a href=\"%h\">%h</a><br>",
						objectManager.localLink (
							transaction,
							queueParentType),
						queueParentType.getCode ());

					formatWriter.writeLineFormat (
						"Service: <a href=\"%h\">%h</a><br>",
						objectManager.localLink (
							transaction,
							queueParent),
						objectManager.objectPathMini (
							transaction,
							queueParent,
							queueInfo.slice ()));

					formatWriter.writeLineFormat (
						"Queue: <a href=\"%h\">%h</a><br>",
						objectManager.localLink (
							transaction,
							queueInfo.queue ()),
						queueInfo.queue ().getCode ());

					formatWriter.writeLineFormat (
						"Reply priv: %h<br>",
						queueInfo.canReplyExplicit ()
							? "explicit"
							: queueInfo.canReplyImplicit ()
								? "implicit"
								: "no");

					formatWriter.writeLineFormat (
						"Reply overflow priv: %h<br>",
						queueInfo.canReplyOverflowExplicit ()
							? "explicit"
							: queueInfo.canReplyOverflowImplicit ()
								? "implicit"
								: "no");

					formatWriter.writeLineFormat (
						"Is overflow user: %h<br>",
						queueInfo.isOverflowUser ()
							? "yes"
							: "no");

					htmlTableCellClose (
						formatWriter);

					// operator activity

					htmlTableCellOpen (
						formatWriter,
						htmlRowSpanAttribute (
							2l * subjectInfos.size ()),
						htmlStyleAttribute (
							htmlStyleRuleEntry (
								"vertical-align",
								"top")));

					Instant inactivityUpdateTime =
						queueInfo.slice ()
							.getCurrentQueueInactivityUpdateTime ();

					formatWriter.writeLineFormat (
						"Last update: %h<br>",
						ifNotNullThenElseEmDash (
							inactivityUpdateTime,
							() -> userConsoleLogic
								.timestampWithoutTimezoneString (
									transaction,
									inactivityUpdateTime)));

					Instant currentInactivityTime =
						queueInfo.slice ().getCurrentQueueInactivityTime ();

					formatWriter.writeLineFormat (
						"Inactive since: %h<br>",
						ifNotNullThenElseEmDash (
							currentInactivityTime,
							() -> userConsoleLogic
								.timestampWithoutTimezoneString (
									transaction,
									currentInactivityTime)));

					formatWriter.writeLineFormat (
						"Configured inactivity time: %h<br>",
						ifNotNullThenElse (
							queueInfo.slice ()
								.getQueueOverflowInactivityTime (),
							() -> userConsoleLogic.prettyDuration (
								transaction,
								Duration.standardSeconds (
									queueInfo.slice ()
										.getQueueOverflowInactivityTime ())),
							() -> "disabled"));

					formatWriter.writeLineFormat (
						"Actual inactivity time: %h<br>",
						ifNotNullThenElse (
							queueInfo.slice ()
								.getCurrentQueueInactivityTime (),
							() -> userConsoleLogic.prettyDuration (
								transaction,
								queueInfo.slice ()
									.getCurrentQueueInactivityTime (),
								transaction.now ()),
							() -> "none"));

					formatWriter.writeLineFormat (
						"Conclusion: %h<br>",
						queueInfo.slice ().getCurrentQueueInactivityTime () != null
							? queueInfo.ownOperatorsActive ()
								? "own operators active"
								: "overflow active"
							: "no data");

					htmlTableCellClose (
						formatWriter);

				}

				// subject

				htmlTableCellOpen (
					formatWriter,
					htmlColumnSpanAttribute (3l),
					htmlStyleAttribute (
						htmlStyleRuleEntry (
							"text-align",
							"center"),
						htmlStyleRuleEntry (
							"background",
							"#dddddd")));

				formatWriter.writeLineFormat (
					"Subject:");

				htmlLinkWrite (
					formatWriter,
					objectManager.localLink (
						transaction,
						subjectInfo.subject ()),
					objectManager.objectPathMini (
						transaction,
						subjectInfo.subject (),
						queueInfo.queue ()));

				htmlTableCellClose (
					formatWriter);

				htmlTableRowClose (
					formatWriter);

				// preferred user

				htmlTableRowOpen (
					formatWriter);

				htmlTableCellOpen (
					formatWriter,
					htmlStyleAttribute (
						htmlStyleRuleEntry (
							"vertical-align",
							"top")));

				formatWriter.writeLineFormat (
					"Preferred by: %s",
					subjectInfo.preferred ()
						? subjectInfo.preferredByUs ()
							? "this user"
							: stringFormat (
								"<a href=\"%h\">%h</a>",
								objectManager.localLink (
									transaction,
									subjectInfo.preferredUser ()),
								objectManager.objectPathMini (
									transaction,
									subjectInfo.preferredUser ()))
						: "nobody");

				formatWriter.writeLineFormat (
					"Preferred by overflow user: %h<br>",
					booleanToYesNo (
						subjectInfo.preferredByOverflowOperator ()));

				formatWriter.writeLineFormat (
					"Configured delay: %h<br>",
					userConsoleLogic.prettyDuration (
						transaction,
						queueInfo.configuredPreferredUserDelay ()));

				formatWriter.writeLineFormat (
					"Actual delay: %h<br>",
					subjectInfo.actualPreferredUserDelay () != null
						? userConsoleLogic.prettyDuration (
							transaction,
							subjectInfo.actualPreferredUserDelay ())
						: "none");

				htmlTableCellClose (
					formatWriter);

				// overflow

				htmlTableCellOpen (
					formatWriter,
					htmlStyleAttribute (
						htmlStyleRuleEntry (
							"vertical-align",
							"top")));

				formatWriter.writeLineFormat (
					"Configured grace time: %h<br>",
					ifNotNullThenElse (
						queueInfo.slice ().getQueueOverflowGraceTime (),
						() -> userConsoleLogic.prettyDuration (
							transaction,
							Duration.standardSeconds (
								queueInfo.slice ()
									.getQueueOverflowGraceTime ())),
						() -> "none"));

				formatWriter.writeLineFormat (
					"Configured overload time: %h<br>",
					ifNotNullThenElse (
						queueInfo.slice ().getQueueOverflowOverloadTime (),
						() -> userConsoleLogic.prettyDuration (
							transaction,
							Duration.standardSeconds (
								queueInfo.slice ()
									.getQueueOverflowOverloadTime ())),
						() -> "none"));

				formatWriter.writeLineFormat (
					"Is overflow user: %h<br>",
					booleanToYesNo (
						queueInfo.isOverflowUser ()));

				formatWriter.writeLineFormat (
					"Own operators active: %h<br>",
					booleanToYesNo (
						queueInfo.ownOperatorsActive ()));

				formatWriter.writeLineFormat (
					"Actual overflow delay: %h<br>",
					ifNotNullThenElse (
						subjectInfo.overflowDelay (),
						() -> userConsoleLogic.prettyDuration (
							transaction,
							subjectInfo.overflowDelay ()),
						() -> "none"));

				htmlTableCellClose (
					formatWriter);

				// conclusion

				htmlTableCellOpen (
					formatWriter,
					htmlStyleAttribute (
						htmlStyleRuleEntry (
							"vertical-align",
							"top")));

				formatWriter.writeLineFormat (
					"Priority: %h<br>",
					integerToDecimalString (
						subjectInfo.priority ()));

				formatWriter.writeLineFormat (
					"Created time: %h<br>",
					userConsoleLogic.timestampWithoutTimezoneString (
						transaction,
						subjectInfo.createdTime ()));

				formatWriter.writeLineFormat (
					"Preferred user delay: %h<br>",
					ifNotNullThenElse (
						subjectInfo.actualPreferredUserDelay,
						() -> userConsoleLogic.prettyDuration (
							transaction,
							subjectInfo.actualPreferredUserDelay ()),
						() -> "none"));

				if (
					isNotNull (
						subjectInfo.overflowDelay)
				) {

					formatWriter.writeLineFormat (
						"Overflow delay: %h (%h)<br>",
						ifNotNullThenElse (
							subjectInfo.overflowDelay,
							() -> userConsoleLogic.prettyDuration (
								transaction,
								subjectInfo.overflowDelay ()),
							() -> "none"),
						booleanToString (
							queueInfo.ownOperatorsActive (),
							"overload",
							"grace"));

				} else {

					formatWriter.writeLineFormat (
						"Overflow delay: none<br>");

				}

				formatWriter.writeLineFormat (
					"Effective time: %h<br>",
					userConsoleLogic.timestampWithoutTimezoneString (
						transaction,
						subjectInfo.effectiveTime ()));

				if (subjectInfo.claimed ()) {

					formatWriter.writeLineFormat (
						"Claimed: yes, by <a href=\"%h\">%h</a><br>",
						objectManager.localLink (
							transaction,
							subjectInfo.claimedByUser ()),
						objectManager.objectPathMini (
							transaction,
							subjectInfo.claimedByUser ()));

				} else {

					formatWriter.writeLineFormat (
						"Claimed: no<br>");

				}

				if (subjectInfo.available ()) {

					formatWriter.writeLineFormat (
						"Available: yes, for %h<br>",
						userConsoleLogic.prettyDuration (
							transaction,
							subjectInfo.effectiveTime (),
							transaction.now ()));

				} else if (subjectInfo.claimed ()) {

					formatWriter.writeLineFormat (
						"Available: no, already claimed<br>");

				} else {

					formatWriter.writeLineFormat (
						"Available: no, for %h<br>",
						userConsoleLogic.prettyDuration (
							transaction,
							transaction.now (),
							subjectInfo.effectiveTime ()));

				}

				htmlTableCellClose (
					formatWriter);

				htmlTableRowClose (
					formatWriter);

				row ++;

			}

		}

	}

}
