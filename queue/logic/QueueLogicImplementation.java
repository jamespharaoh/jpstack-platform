package wbs.platform.queue.logic;

import static wbs.utils.etc.EnumUtils.enumNameSpaces;
import static wbs.utils.etc.EnumUtils.enumNotInSafe;
import static wbs.utils.etc.NullUtils.ifNull;
import static wbs.utils.etc.NullUtils.isNotNull;
import static wbs.utils.etc.NullUtils.isNull;
import static wbs.utils.etc.NumberUtils.integerNotEqualSafe;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.NumberUtils.toJavaIntegerRequired;
import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.etc.OptionalUtils.optionalIsNotPresent;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.time.TimeUtils.laterThan;

import java.util.List;

import com.google.common.base.Optional;

import lombok.NonNull;

import org.joda.time.Duration;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.object.ObjectManager;

import wbs.platform.object.core.model.ObjectTypeObjectHelper;
import wbs.platform.queue.model.QueueItemClaimStatus;
import wbs.platform.queue.model.QueueItemObjectHelper;
import wbs.platform.queue.model.QueueItemRec;
import wbs.platform.queue.model.QueueItemState;
import wbs.platform.queue.model.QueueObjectHelper;
import wbs.platform.queue.model.QueueRec;
import wbs.platform.queue.model.QueueSubjectObjectHelper;
import wbs.platform.queue.model.QueueSubjectRec;
import wbs.platform.queue.model.QueueTypeObjectHelper;
import wbs.platform.queue.model.QueueTypeRec;
import wbs.platform.scaffold.logic.SliceLogic;
import wbs.platform.scaffold.model.SliceRec;
import wbs.platform.user.model.UserRec;

@SingletonComponent ("queueLogic")
public
class QueueLogicImplementation
	implements QueueLogic {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ObjectManager objectManager;

	@SingletonDependency
	ObjectTypeObjectHelper objectTypeHelper;

	@SingletonDependency
	QueueObjectHelper queueHelper;

	@SingletonDependency
	QueueItemObjectHelper queueItemHelper;

	@SingletonDependency
	QueueSubjectObjectHelper queueSubjectHelper;

	@SingletonDependency
	QueueTypeObjectHelper queueTypeHelper;

	@SingletonDependency
	SliceLogic sliceLogic;

	// implementation

	@Override
	public
	QueueRec findQueue (
			@NonNull Transaction parentTransaction,
			@NonNull Record <?> parentObject,
			@NonNull String code) {

		return queueHelper.findByCodeRequired (
			parentTransaction,
			parentObject,
			code);

	}

	@Override
	public
	QueueItemRec createQueueItem (
			@NonNull Transaction parentTransaction,
			@NonNull QueueSubjectRec queueSubject,
			@NonNull Record<?> refObject,
			@NonNull String source,
			@NonNull String details) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createQueueItem");

		) {

			QueueRec queue =
				queueSubject.getQueue ();

			QueueTypeRec queueType =
				queue.getQueueType ();

			// sanity check

			if (
				integerNotEqualSafe (
					objectManager.getObjectTypeId (
						transaction,
						refObject),
					queueType.getRefType ().getId ())
			) {

				throw new IllegalArgumentException ();

			}

			// create the queue item

			boolean waiting =
				queueSubject.getActiveItems () > 0;

			QueueItemRec queueItem =
				queueItemHelper.insert (
					transaction,
					queueItemHelper.createInstance ()

				.setQueueSubject (
					queueSubject)

				.setIndex (
					queueSubject.getTotalItems ())

				.setQueue (
					queue)

				.setSource (
					source)

				.setDetails (
					details)

				.setRefObjectId (
					refObject.getId ())

				.setState (
					waiting
						? QueueItemState.waiting
						: QueueItemState.pending)

				.setCreatedTime (
					transaction.now ())

				.setPendingTime (
					waiting
						? null
						: transaction.now ())

				.setPriority (
					ifNull (
						queue.getDefaultPriority (),
						0l))

			);

			// update queue subject

			queueSubject

				.setTotalItems (
					queueSubject.getTotalItems () + 1)

				.setActiveItems (
					queueSubject.getActiveItems () + 1);

			// update slice

			Optional<SliceRec> optionalSlice =
				objectManager.getAncestor (
					transaction,
					SliceRec.class,
					queue);

			if (
				optionalIsNotPresent (
					optionalSlice)
			) {

				transaction.warningFormat (
					"Unable to determine slice for queue %s",
					integerToDecimalString (
						queue.getId ()));

			}

			sliceLogic.updateSliceInactivityTimestamp (
				transaction,
				optionalSlice.get (),
				Optional.of (
					transaction.now ()));

			// and return

			return queueItem;

		}

	}

	@Override
	public
	QueueItemRec createQueueItem (
			@NonNull Transaction parentTransaction,
			@NonNull QueueRec queue,
			@NonNull Record<?> subjectObject,
			@NonNull Record<?> refObject,
			@NonNull String source,
			@NonNull String details) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createQueueItem");

		) {

			QueueSubjectRec queueSubject =
				findOrCreateQueueSubject (
					transaction,
					queue,
					subjectObject);

			return createQueueItem (
				transaction,
				queueSubject,
				refObject,
				source,
				details);

		}

	}

	@Override
	public
	QueueSubjectRec findOrCreateQueueSubject (
			@NonNull Transaction parentTransaction,
			@NonNull QueueRec queue,
			@NonNull Record<?> object) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findOrCreateQueueSubject");

		) {

			QueueTypeRec queueType =
				queue.getQueueType ();

			// sanity check

			if (
				integerNotEqualSafe (
					objectManager.getObjectTypeId (
						transaction,
						object),
					queueType.getSubjectType ().getId ())
			) {

				throw new IllegalArgumentException (
					stringFormat (
						"Queue %s expected subject type %s, got %s",
						objectManager.objectPath (
							transaction,
							queue),
						queueType.getSubjectType ().getCode (),
						objectManager.getObjectTypeCode (
							transaction,
							object)));

			}

			// lookup existing

			QueueSubjectRec queueSubject =
				queueSubjectHelper.find (
					transaction,
					queue,
					object);

			if (queueSubject != null)
				return queueSubject;

			// create new

			queueSubject =
				queueSubjectHelper.insert (
					transaction,
					queueSubjectHelper.createInstance ()

				.setQueue (
					queue)

				.setObjectId (
					object.getId ())

			);

			return queueSubject;

		}

	}

	@Override
	public
	void cancelQueueItem (
			@NonNull Transaction parentTransaction,
			@NonNull QueueItemRec queueItem) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"cancelQueueItem");

		) {

			QueueSubjectRec queueSubject =
				queueItem.getQueueSubject ();

			// sanity checks

			long currentItemIndex =
				+ queueSubject.getTotalItems ()
				- queueSubject.getActiveItems ();

			if (
				integerNotEqualSafe (
					queueItem.getIndex (),
					currentItemIndex)
			) {
				throw new IllegalStateException ();
			}

			if (
				enumNotInSafe (
					queueItem.getState (),
					QueueItemState.pending,
					QueueItemState.claimed)) {

				throw new RuntimeException (
					stringFormat (
						"Cannot cancel queue item in state: %s",
						enumNameSpaces (
							queueItem.getState ())));

			}

			// update queue item claim

			if (
				isNotNull (
					queueItem.getQueueItemClaim ())
			) {

				queueItem.getQueueItemClaim ()

					.setEndTime (
						transaction.now ())

					.setStatus (
						QueueItemClaimStatus.cancelled);

			}

			// update the queue item

			queueItem

				.setState (
					QueueItemState.cancelled)

				.setCancelledTime (
					transaction.now ())

				.setQueueItemClaim (
					null);

			// update the queue subject

			queueSubject

				.setActiveItems (
					queueSubject.getActiveItems () - 1);

			// activate next queue item (if any)

			if (queueSubject.getActiveItems () > 0) {

				long nextItemIndex =
					+ queueSubject.getTotalItems ()
					- queueSubject.getActiveItems ();

				QueueItemRec nextQueueItem =
					queueItemHelper.findByIndexRequired (
						transaction,
						queueSubject,
						nextItemIndex);

				if (nextQueueItem.getState () != QueueItemState.waiting)
					throw new IllegalStateException ();

				nextQueueItem

					.setState (
						QueueItemState.pending)

					.setPendingTime (
						transaction.now ());

			}

		}

	}

	@Override
	public
	void processQueueItem (
			@NonNull Transaction parentTransaction,
			@NonNull QueueItemRec queueItem,
			@NonNull UserRec user) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"processQueueItem");

		) {

			QueueSubjectRec queueSubject =
				queueItem.getQueueSubject ();

			// sanity checks

			long currentItemIndex =
				+ queueSubject.getTotalItems ()
				- queueSubject.getActiveItems ();

			if (queueItem.getIndex () != currentItemIndex) {

				throw new IllegalStateException (
					stringFormat (
						"Cannot process queue item %s ",
						integerToDecimalString (
							queueItem.getId ()),
						"with index %s ",
						integerToDecimalString (
							queueItem.getIndex ()),
						"for queue subject %s ",
						integerToDecimalString (
							queueSubject.getId ()),
						"whose total is %s ",
						integerToDecimalString (
							queueSubject.getTotalItems ()),
						"and active is %s, ",
						integerToDecimalString (
							queueSubject.getActiveItems ()),
						"implying a current item index of %s",
						integerToDecimalString (
							currentItemIndex)));

			}

			if (queueItem.getState () != QueueItemState.claimed) {

				throw new RuntimeException (
					stringFormat (
						"Cannot process queue item %s in state: %s",
						integerToDecimalString (
							queueItem.getId ()),
						enumNameSpaces (
							queueItem.getState ())));

			}

			if (queueItem.getQueueItemClaim ().getUser () != user) {

				throw new RuntimeException (
					"Trying to process item belonging to another user");

			}

			// update queue item claim

			queueItem.getQueueItemClaim ()

				.setEndTime (
					transaction.now ())

				.setStatus (
					QueueItemClaimStatus.processed);

			// update the queue item

			queueItem

				.setState (
					QueueItemState.processed)

				.setProcessedTime (
					transaction.now ())

				.setProcessedUser (
					user)

				.setProcessedByPreferredUser (
					queueSubject.getPreferredUser () == null
						? null
						: queueSubject.getPreferredUser () == user)

				.setQueueItemClaim (
					null);

			// update the queue subject

			queueSubject

				.setActiveItems (
					queueSubject.getActiveItems () - 1)

				.setPreferredUser (
					user);

			// update slice

			sliceLogic.updateSliceInactivityTimestamp (
				transaction,
				user.getSlice (),
				optionalAbsent ());

			// activate next queue item (if any)

			if (queueSubject.getActiveItems () > 0) {

				long nextItemIndex =
					+ queueSubject.getTotalItems ()
					- queueSubject.getActiveItems ();

				QueueItemRec nextQueueItem =
					queueItemHelper.findByIndexRequired (
						transaction,
						queueSubject,
						nextItemIndex);

				if (nextQueueItem.getState () != QueueItemState.waiting)
					throw new IllegalStateException ();

				nextQueueItem

					.setState (
						QueueItemState.pending)

					.setPendingTime (
						transaction.now ());

			}

		}

	}

	@Override
	public
	List <QueueItemRec> getActiveQueueItems (
			@NonNull Transaction parentTransaction,
			@NonNull QueueSubjectRec queueSubject) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"getActiveQueueItems");

		) {

			return queueSubject.getQueueItems ().subList (
				toJavaIntegerRequired (
					+ queueSubject.getTotalItems ()
					- queueSubject.getActiveItems ()),
				toJavaIntegerRequired (
					queueSubject.getTotalItems ()));

		}

	}

	@Override
	public
	boolean sliceHasQueueActivity (
			@NonNull Transaction parentTransaction,
			@NonNull SliceRec slice) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"sliceHasQueueActivity");

		) {

			// active if no config or stats

			if (

				isNull (
					slice.getQueueOverflowInactivityTime ())

				|| isNull (
					slice.getCurrentQueueInactivityTime ())

			) {
				return true;
			}

			// check for recent activity

			return laterThan (
				slice.getCurrentQueueInactivityTime ().plus (
					Duration.standardSeconds (
						slice.getQueueOverflowInactivityTime ())),
				transaction.now ());

		}

	}

	@Override
	public
	QueueRec findQueueByCodeRequired (
			@NonNull Transaction parentTransaction,
			@NonNull Record <?> queueParent,
			@NonNull String queueCode) {

		return queueHelper.findByCodeRequired (
			parentTransaction,
			queueParent,
			queueCode);

	}

}
