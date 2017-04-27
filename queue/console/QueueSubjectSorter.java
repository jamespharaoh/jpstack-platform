package wbs.platform.queue.console;

import static wbs.utils.collection.CollectionUtils.collectionIsNotEmpty;
import static wbs.utils.etc.EnumUtils.enumEqualSafe;
import static wbs.utils.etc.EnumUtils.enumNameSpaces;
import static wbs.utils.etc.EnumUtils.enumNotInSafe;
import static wbs.utils.etc.LogicUtils.ifNotNullThenElse;
import static wbs.utils.etc.LogicUtils.ifThenElse;
import static wbs.utils.etc.LogicUtils.referenceEqualWithClass;
import static wbs.utils.etc.Misc.isNotNull;
import static wbs.utils.etc.Misc.isNull;
import static wbs.utils.etc.Misc.lessThan;
import static wbs.utils.etc.NullUtils.ifNull;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.OptionalUtils.optionalEqualAndPresentWithClass;
import static wbs.utils.etc.OptionalUtils.optionalFromNullable;
import static wbs.utils.etc.OptionalUtils.optionalNotEqualAndPresentWithClass;
import static wbs.utils.string.StringUtils.joinWithCommaAndSpace;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.time.TimeUtils.earlierThan;
import static wbs.utils.time.TimeUtils.laterThan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Provider;

import lombok.Data;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.apache.commons.lang3.builder.CompareToBuilder;

import org.joda.time.Duration;
import org.joda.time.Instant;

import wbs.console.priv.UserPrivChecker;
import wbs.console.priv.UserPrivCheckerBuilder;

import wbs.framework.activitymanager.ActiveTask;
import wbs.framework.activitymanager.ActivityManager;
import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.BorrowedTransaction;
import wbs.framework.database.Database;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.TaskLogger;
import wbs.framework.object.ObjectManager;

import wbs.platform.queue.logic.QueueCache;
import wbs.platform.queue.logic.QueueLogic;
import wbs.platform.queue.model.QueueItemRec;
import wbs.platform.queue.model.QueueItemState;
import wbs.platform.queue.model.QueueObjectHelper;
import wbs.platform.queue.model.QueueRec;
import wbs.platform.queue.model.QueueSubjectRec;
import wbs.platform.scaffold.model.SliceRec;
import wbs.platform.user.model.UserRec;

@Accessors (fluent = true)
@PrototypeComponent ("queueSubjectSorter")
public
class QueueSubjectSorter {

	// singleton dependencies

	@SingletonDependency
	ActivityManager activityManager;

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ObjectManager objectManager;

	@SingletonDependency
	QueueObjectHelper queueHelper;

	@SingletonDependency
	QueueLogic queueLogic;

	@SingletonDependency
	QueueManager queueManager;

	// prototype dependencies

	@PrototypeDependency
	Provider <UserPrivCheckerBuilder> userPrivCheckerBuilderProvider;

	// inputs

	@Setter
	QueueCache queueCache;

	@Setter
	QueueRec queue;

	@Setter
	UserRec loggedInUser;

	@Setter
	UserRec effectiveUser;

	// state

	UserPrivChecker loggedInUserPrivChecker;
	UserPrivChecker effectiveUserPrivChecker;

	BorrowedTransaction transaction;

	Set <SubjectInfo> subjectInfos =
		new HashSet<> ();

	Map <QueueRec, QueueInfo> queueInfos =
		new HashMap<> ();

	SortedQueueSubjects result =
		new SortedQueueSubjects ();

	// implementation

	public
	SortedQueueSubjects sort (
			@NonNull TaskLogger parentTaskLogger) {

		TaskLogger taskLogger =
			logContext.nestTaskLogger (
				parentTaskLogger,
				"sort");

		try (

			ActiveTask activeTask =
				activityManager.start (
					"logic",
					stringFormat (
						"%s.%s (%s)",
						getClass ().getSimpleName (),
						"sort",
						joinWithCommaAndSpace (
							stringFormat (
								"queue=%s",
								ifNotNullThenElse (
									queue,
									() -> queue.toString (),
									() -> "null"),
							stringFormat (
								"effectiveUser=%s",
								ifNotNullThenElse (
									effectiveUser,
									() -> effectiveUser.toString (),
									() -> "null"))))),
					this);

		) {

			loggedInUserPrivChecker =
				userPrivCheckerBuilderProvider.get ()

				.userId (
					loggedInUser.getId ())

				.build (
					taskLogger);

			if (
				isNotNull (
					effectiveUser)
			) {

				effectiveUserPrivChecker =
					userPrivCheckerBuilderProvider.get ()

					.userId (
						effectiveUser.getId ())

					.build (
						taskLogger);

			}

			transaction =
				database.currentTransaction ();

			// process queue subjects

			List <QueueSubjectRec> queueSubjects =
				ifThenElse (
					isNotNull (
						queue),

				() ->
					queueCache.findQueueSubjects (
						queue),

				() ->
					queueCache.findQueueSubjects ()

			);

			queueSubjects.forEach (
				queueSubject ->
					processSubject (
						taskLogger,
						queueSubject));

			// convert subjects to list, filter and sort

			result.availableSubjects =
				subjectInfos.stream ()

				.filter (
					subjectInfo ->
						effectiveUser == null || subjectInfo.available)

				.sorted (
					effectiveUser != null
						? SubjectInfo.effectiveTimeComparator
						: SubjectInfo.createdTimeComparator)

				.collect (
					Collectors.toList ());

			// convert queues to list, filter and sort

			result.allQueues =
				queueInfos.values ().stream ()

				.sorted (
					effectiveUser != null
						? QueueInfo.oldestAvailableComparator
						: QueueInfo.oldestComparator)

				.filter (
					queueInfo ->
						collectionIsNotEmpty (
							queueInfo.subjectInfos))

				.collect (
					Collectors.toList ());

			// filter available queues

			result.availableQueues =
				result.allQueues.stream ()

				.filter (
					queueInfo ->
						effectiveUser == null
							|| queueInfo.availableItems > 0)

				.collect (
					Collectors.toList ());

			// sort subjects in each queue

			result.allQueues.forEach (
				queueInfo ->
					Collections.sort (
						queueInfo.subjectInfos,
						effectiveUser != null
							? SubjectInfo.effectiveTimeComparator
							: SubjectInfo.createdTimeComparator));

			// and return

			return result;

		}

	}

	public
	void processSubject (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull QueueSubjectRec subject) {

		try (

			TaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"processSubject");

		) {

			// get queue info

			QueueInfo queueInfo =
				queueInfos.computeIfAbsent (
					subject.getQueue (),
					queue ->
						createQueueInfo (
							taskLogger,
							queue));

			// check we can see this queue

			if (
				! queueInfo.canReplyImplicit
				&& ! queueInfo.canReplyOverflowImplicit
			) {
				return;
			}

			// get subject info

			SubjectInfo subjectInfo =
				createSubjectInfo (
					parentTaskLogger,
					queueInfo,
					subject);

			subjectInfos.add (
				subjectInfo);

			queueInfo.subjectInfos.add (
				subjectInfo);

			// count stuff

			countTotal (
				queueInfo,
				subjectInfo);

			countWaiting (
				queueInfo,
				subjectInfo);

			// claimed items are not available

			if (
				enumEqualSafe (
					subjectInfo.state,
					QueueItemState.claimed)
			) {

				countClaimed (
					queueInfo,
					subjectInfo);

				return;

			}

			// update queue

			updateQueueInfo (
				queueInfo,
				subjectInfo);

			// count hidden or available items

			if (subjectInfo.available) {

				countAvailable (
					queueInfo,
					subjectInfo);

			} else {

				countUnavailable (
					queueInfo,
					subjectInfo);

			}

		}

	}

	private
	SubjectInfo createSubjectInfo (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull QueueInfo queueInfo,
			@NonNull QueueSubjectRec subject) {

		try (

			TaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"createSubjectInfo");

		) {

			// find next item

			QueueItemRec item =
				getNextItem (
					subject);

			// create subject info

			SubjectInfo subjectInfo =
				new SubjectInfo ();

			subjectInfo.subject = subject;
			subjectInfo.item = item;
			subjectInfo.createdTime = item.getCreatedTime ();
			subjectInfo.effectiveTime = item.getCreatedTime ();
			subjectInfo.priority = item.getPriority ();
			subjectInfo.state = item.getState ();

			// check preferred user

			subjectInfo.preferredUser =
				ifNull (
					subjectInfo.subject.getForcePreferredUser (),
					subjectInfo.subject.getPreferredUser ());

			subjectInfo.preferred =
				isNotNull (
					subjectInfo.preferredUser);

			subjectInfo.preferredByUs =
				optionalEqualAndPresentWithClass (
					UserRec.class,
					optionalFromNullable (
						subjectInfo.preferredUser),
					optionalFromNullable (
						effectiveUser));

			subjectInfo.preferredByOther =
				optionalNotEqualAndPresentWithClass (
					UserRec.class,
					optionalFromNullable (
						subjectInfo.preferredUser),
					optionalFromNullable (
						effectiveUser));

			if (subjectInfo.preferred) {

				UserPrivChecker preferredUserPrivChecker =
					userPrivCheckerBuilderProvider.get ()

					.userId (
						subjectInfo.preferredUser.getId ())

					.build (
						taskLogger);

				subjectInfo.preferredByOverflowOperator = (

					preferredUserPrivChecker.canRecursive (
						taskLogger,
						queueInfo.queue,
						"reply_overflow")

					&& ! preferredUserPrivChecker.canSimple (
						taskLogger,
						queueInfo.queue,
						"reply")

				);

				subjectInfo.preferredByOwnOperator =
					! subjectInfo.preferredByOverflowOperator;

			} else {

				subjectInfo.preferredByOverflowOperator = false;
				subjectInfo.preferredByOwnOperator = false;

			}

			// extend effective time due to preferred user

			if (

				subjectInfo.preferredByOther

				&& (
					queueInfo.isOverflowUser
					|| subjectInfo.preferredByOwnOperator
				)

			) {

				subjectInfo.actualPreferredUserDelay =
					queueInfo.configuredPreferredUserDelay;

				subjectInfo.effectiveTime =
					subjectInfo.effectiveTime.plus (
						queueInfo.configuredPreferredUserDelay);

			}

			// extend effective time due to overflow user

			if (queueInfo.isOverflowUser) {

				if (queueInfo.ownOperatorsActive) {

					subjectInfo.overflowDelay =
						Duration.standardSeconds (
							ifNull (
								queueInfo.slice.getQueueOverflowGraceTime (),
								0l));

					subjectInfo.effectiveTime =
						subjectInfo.effectiveTime.plus (
							subjectInfo.overflowDelay);

				} else {

					subjectInfo.overflowDelay =
						Duration.standardSeconds (
							ifNull (
								queueInfo.slice.getQueueOverflowInactivityTime (),
								0l));

					subjectInfo.effectiveTime =
						subjectInfo.effectiveTime.plus (
							subjectInfo.overflowDelay);

				}

			}

			// check claimed user

			if (
				enumEqualSafe (
					subjectInfo.state (),
					QueueItemState.claimed)
			) {

				subjectInfo.claimed = true;

				subjectInfo.claimedByUser =
					subjectInfo.item.getQueueItemClaim ().getUser ();

				subjectInfo.available = false;

			} else {

				subjectInfo.claimed = false;

				subjectInfo.available =
					laterThan (
						transaction.now (),
						subjectInfo.effectiveTime);

			}

			// return

			return subjectInfo;

		}

	}

	private
	QueueInfo createQueueInfo (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull QueueRec queue) {

		try (

			TaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"createQueueInfo");

		) {

			QueueInfo queueInfo =
				new QueueInfo ();

			queueInfo.queue =
				queue;

			queueInfo.slice =
				queue.getSlice ();

			queueInfo.configuredPreferredUserDelay =
				queueManager.getPreferredUserDelay (
					queue);

			// check permissions

			queueInfo.canReplyExplicit =
				checkPrivExplicit (
					taskLogger,
					queue,
					"reply");

			queueInfo.canReplyImplicit =
				checkPrivImplicit (
					taskLogger,
					queue,
					"reply");

			queueInfo.canReplyOverflowExplicit =
				checkPrivExplicit (
					taskLogger,
					queue,
					"reply_overflow");

			queueInfo.canReplyOverflowImplicit =
				checkPrivImplicit (
					taskLogger,
					queue,
					"reply_overflow");

			// check special states

			queueInfo.isOverflowUser =
				queueInfo.canReplyOverflowImplicit
				&& ! queueInfo.canReplyExplicit;

			queueInfo.ownOperatorsActive =
				queueLogic.sliceHasQueueActivity (
					queueInfo.slice);

			// return

			return queueInfo;

		}

	}

	private
	boolean checkPrivExplicit (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull Record <?> parent,
			@NonNull String privCode) {

		try (

			TaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"checkPrivExplicit");

		) {

			if (
				! loggedInUserPrivChecker.canRecursive (
					taskLogger,
					parent,
					"reply")
			) {
				return false;
			}

			if (
				isNull (
					effectiveUser)
			) {

				return true;

			} else {

				return effectiveUserPrivChecker.canSimple (
					taskLogger,
					parent,
					privCode);

			}

		}

	}

	private
	boolean checkPrivImplicit (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull Record <?> parent,
			@NonNull String privCode) {

		try (

			TaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"checkPrivImplicit");

		) {

			if (
				! loggedInUserPrivChecker.canRecursive (
					taskLogger,
					parent,
					"reply")
			) {
				return false;
			}

			if (
				isNull (
					effectiveUser)
			) {

				return true;

			} else {

				return effectiveUserPrivChecker.canRecursive (
					taskLogger,
					parent,
					privCode);

			}

		}

	}

	private
	void updateQueueInfo (
			@NonNull QueueInfo queueInfo,
			@NonNull SubjectInfo subjectInfo) {

		// check

		Instant createdTime =
			subjectInfo.createdTime;

		if (
			lessThan (
				subjectInfo.priority,
				queueInfo.highestPriority)
		) {

			queueInfo.highestPriority =
				subjectInfo.priority;

		}

		if (
			earlierThan (
				createdTime,
				queueInfo.oldest)
		) {

			queueInfo.oldest =
				createdTime;

		}

	}

	private
	QueueItemRec getNextItem (
			@NonNull QueueSubjectRec subject) {

		long nextItemIndex =
			+ subject.getTotalItems ()
			- subject.getActiveItems ();

		QueueItemRec item =
			queueCache.findQueueItemByIndexRequired (
				subject,
				nextItemIndex);

		if (
			enumNotInSafe (
				item.getState (),
				QueueItemState.pending,
				QueueItemState.claimed)
		) {

			throw new RuntimeException (
				stringFormat (
					"Queue item %s in invalid state \"%s\"",
					integerToDecimalString (
						item.getId ()),
					enumNameSpaces (
						item.getState ())));

		}

		return item;

	}

	private
	void countTotal (
			@NonNull QueueInfo queueInfo,
			@NonNull SubjectInfo subjectInfo) {

		result.totalItems +=
			subjectInfo.subject.getActiveItems ();

		queueInfo.totalItems +=
			subjectInfo.subject.getActiveItems ();

	}

	private
	void countWaiting (
			@NonNull QueueInfo queueInfo,
			@NonNull SubjectInfo subjectInfo) {

		result.waitingItems +=
			+ subjectInfo.subject.getActiveItems ()
			- 1;

		queueInfo.waitingItems +=
			+ subjectInfo.subject.getActiveItems ()
			- 1;

		if (
			earlierThan (
				subjectInfo.createdTime,
				queueInfo.oldestWaiting)
		) {

			queueInfo.oldestWaiting =
				subjectInfo.createdTime;

		}

		if (
			lessThan (
				subjectInfo.priority,
				queueInfo.highestPriorityWaiting)
		) {

			queueInfo.highestPriorityWaiting =
				subjectInfo.priority;

		}

	}

	private
	void countClaimed (
			@NonNull QueueInfo queueInfo,
			@NonNull SubjectInfo subjectInfo) {

		result.totalClaimedSubjects ++;
		queueInfo.claimedSubjects ++;

		result.totalClaimedItems ++;
		queueInfo.claimedItems ++;

		if (

			isNotNull (
				effectiveUser)

			&& referenceEqualWithClass (
				UserRec.class,
				effectiveUser,
				subjectInfo.item.getQueueItemClaim ().getUser ())

		) {

			result.userClaimedItems ++;
			queueInfo.userClaimedItems ++;

		}

		if (
			earlierThan (
				subjectInfo.createdTime,
				queueInfo.oldestClaimed)
		) {

			queueInfo.oldestClaimed =
				subjectInfo.createdTime;

		}

		if (
			lessThan (
				subjectInfo.priority (),
				queueInfo.highestPriorityClaimed)
		) {

			queueInfo.highestPriorityClaimed =
				subjectInfo.priority;

		}

	}

	private
	void countUnavailable (
			@NonNull QueueInfo queueInfo,
			@NonNull SubjectInfo subjectInfo) {

		result.totalUnavailableSubjects ++;
		result.totalUnavailableItems ++;

		queueInfo.totalUnavailableSubjects ++;
		queueInfo.totalUnavailableItems ++;

		if (
			earlierThan (
				subjectInfo.effectiveTime,
				queueInfo.oldestUnavailable)
		) {

			queueInfo.oldestUnavailable =
				subjectInfo.effectiveTime;

		}

	}

	private
	void countAvailable (
			@NonNull QueueInfo queueInfo,
			@NonNull SubjectInfo subjectInfo) {

		result.totalAvailableSubjects ++;
		queueInfo.availableSubjects ++;

		result.totalAvailableItems ++;
		queueInfo.availableItems ++;

		if (
			earlierThan (
				subjectInfo.effectiveTime,
				queueInfo.oldestAvailable)
		) {

			queueInfo.oldestAvailable =
				subjectInfo.effectiveTime;

		}

		if (
			lessThan (
				subjectInfo.priority,
				queueInfo.highestPriorityAvailable)
		) {

			queueInfo.highestPriorityAvailable =
				subjectInfo.priority;

		}

	}

	@Accessors (fluent = true)
	@Data
	public static
	class QueueInfo {

		QueueRec queue;
		SliceRec slice;

		Duration configuredPreferredUserDelay;

		List <SubjectInfo> subjectInfos =
			new ArrayList<> ();

		// all

		long highestPriority =
			Long.MAX_VALUE;

		Instant oldest =
			new Instant (
				Long.MAX_VALUE);

		// waiting

		long waitingItems = 0;

		long highestPriorityWaiting =
			Long.MAX_VALUE;

		Instant oldestWaiting =
			new Instant (
				Long.MAX_VALUE);

		// available

		long availableItems = 0;
		long availableSubjects = 0;

		long highestPriorityAvailable =
			Long.MAX_VALUE;

		Instant oldestAvailable =
			new Instant (
				Long.MAX_VALUE);

		// claimed

		long highestPriorityClaimed =
			Long.MAX_VALUE;

		Instant oldestClaimed =
			new Instant (
				Long.MAX_VALUE);

		// preferred

		Instant oldestUnavailable =
			new Instant (
				Long.MAX_VALUE);

		// counts

		long totalItems = 0;
		long claimedItems = 0;
		long userClaimedItems = 0;
		long myClaimedItems = 0;
		long claimedSubjects = 0;
		long totalUnavailableItems = 0;
		long totalUnavailableSubjects = 0;

		// permissions

		boolean canReplyExplicit;
		boolean canReplyImplicit;
		boolean canReplyOverflowExplicit;
		boolean canReplyOverflowImplicit;

		// other state

		boolean isOverflowUser;
		boolean ownOperatorsActive;

		// comparators

		public final static
		Comparator<QueueInfo> oldestAvailableComparator =
			new Comparator<QueueInfo> () {

			@Override
			public
			int compare (
					@NonNull QueueInfo left,
					@NonNull QueueInfo right) {

				return new CompareToBuilder ()

					.append (
						left.highestPriorityAvailable,
						right.highestPriorityAvailable)

					.append (
						left.oldestAvailable,
						right.oldestAvailable)

					.append (
						left.queue,
						right.queue)

					.toComparison ();

			}

		};

		public final static
		Comparator<QueueInfo> oldestComparator =
			new Comparator<QueueInfo> () {

			@Override
			public
			int compare (
					QueueInfo left,
					QueueInfo right) {

				return new CompareToBuilder ()

					.append (
						left.highestPriority,
						right.highestPriority)

					.append (
						left.oldest,
						right.oldest)

					.append (
						left.queue,
						right.queue)

					.toComparison ();

			}

		};

	}

	@Accessors (fluent = true)
	@Data
	public static
	class SubjectInfo {

		QueueSubjectRec subject;
		QueueItemRec item;

		Instant createdTime;
		Instant effectiveTime;
		Long priority;

		QueueItemState state;

		boolean preferred;
		UserRec preferredUser;
		boolean preferredByOther;
		boolean preferredByUs;
		boolean preferredByOwnOperator;
		boolean preferredByOverflowOperator;
		Duration actualPreferredUserDelay;

		boolean claimed;
		UserRec claimedByUser;

		Duration overflowDelay;

		boolean available;

		public final static
		Comparator<SubjectInfo> effectiveTimeComparator =
			new Comparator<SubjectInfo> () {

			@Override
			public
			int compare (
					SubjectInfo left,
					SubjectInfo right) {

				return new CompareToBuilder ()

					.append (
						left.priority,
						right.priority)

					.append (
						left.effectiveTime,
						right.effectiveTime)

					.append (
						left.subject,
						right.subject)

					.toComparison ();

			}

		};

		public final static
		Comparator<SubjectInfo> createdTimeComparator =
			new Comparator<SubjectInfo> () {

			@Override
			public
			int compare (
					SubjectInfo left,
					SubjectInfo right) {

				return new CompareToBuilder ()

					.append (
						left.item.getCreatedTime (),
						right.item.getCreatedTime ())

					.append (
						left.subject,
						right.subject)

					.toComparison ();

			}

		};

	}

}
