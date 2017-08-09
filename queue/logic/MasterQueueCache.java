package wbs.platform.queue.logic;

import static wbs.utils.collection.MapUtils.mapItemForKeyRequired;
import static wbs.utils.etc.Misc.requiredValue;
import static wbs.utils.etc.NullUtils.isNotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import lombok.NonNull;

import org.apache.commons.lang3.tuple.Pair;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.platform.queue.model.QueueItemObjectHelper;
import wbs.platform.queue.model.QueueItemRec;
import wbs.platform.queue.model.QueueItemState;
import wbs.platform.queue.model.QueueRec;
import wbs.platform.queue.model.QueueSubjectObjectHelper;
import wbs.platform.queue.model.QueueSubjectRec;

@PrototypeComponent ("masterQueueCache")
public
class MasterQueueCache
	implements QueueCache {

	// TODO this is probably not needed with generic caching layer

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	QueueItemObjectHelper queueItemHelper;

	@SingletonDependency
	QueueSubjectObjectHelper queueSubjectHelper;

	// state

	Map <Pair <Long, Long>, QueueItemRec> queueItemsBySubjectAndIndex;

	List <QueueSubjectRec> queueSubjects;

	Map <Long, List <QueueSubjectRec>> queueSubjectsByQueue;

	// implementation

	public
	MasterQueueCache setup (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"setup");

		) {

			// create queue item index

			queueItemsBySubjectAndIndex =
				ImmutableMap.copyOf (

				queueItemHelper.find (
					transaction,
					ImmutableList.of (
						QueueItemState.pending,
						QueueItemState.claimed))

				.stream ()

				.filter (
					queueItem ->
						isNotNull (
							queueItem.getQueueSubject ()))

				.collect (
					Collectors.toMap (

					queueItem ->
						Pair.of (
							queueItem.getQueueSubject ().getId (),
							queueItem.getIndex ()),

					queueItem ->
						queueItem)

				)

			);

			// create queue subject indexes

			queueSubjects =
				ImmutableList.copyOf (
					queueSubjectHelper.findActive (
						transaction));

			queueSubjectsByQueue =
				ImmutableMap.copyOf (
					queueSubjects.stream ()

				.collect (
					Collectors.groupingBy (
						queueSubject ->
							queueSubject.getQueue ().getId ()))

			);

			// return

			return this;

		}

	}

	@Override
	public
	QueueItemRec findQueueItemByIndexRequired (
			@NonNull Transaction parentTransaction,
			@NonNull QueueSubjectRec subject,
			@NonNull Long index) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findQueueItemByIndexRequired");

		) {

			return mapItemForKeyRequired (
				queueItemsBySubjectAndIndex,
				Pair.of (
					subject.getId (),
					index));

		}

	}

	@Override
	public
	List <QueueSubjectRec> findQueueSubjects (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findQueueSubjects");

		) {

			return requiredValue (
				queueSubjects);

		}

	}

	@Override
	public
	List <QueueSubjectRec> findQueueSubjects (
			@NonNull Transaction parentTransaction,
			@NonNull QueueRec queue) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findQueueSubjects");

		) {

			return queueSubjectsByQueue.get (
				queue.getId ());

		}

	}

}
