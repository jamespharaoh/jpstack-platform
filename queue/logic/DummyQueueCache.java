package wbs.platform.queue.logic;

import java.util.List;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.platform.queue.model.QueueItemObjectHelper;
import wbs.platform.queue.model.QueueItemRec;
import wbs.platform.queue.model.QueueRec;
import wbs.platform.queue.model.QueueSubjectObjectHelper;
import wbs.platform.queue.model.QueueSubjectRec;

@SingletonComponent ("dummyQueueCache")
public
class DummyQueueCache
	implements QueueCache {

	// TODO this is probably not needed with generic caching layer

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	QueueItemObjectHelper queueItemHelper;

	@SingletonDependency
	QueueSubjectObjectHelper queueSubjectHelper;

	// implementation

	@Override
	public
	QueueItemRec findQueueItemByIndexRequired (
			@NonNull Transaction parentTransaction,
			@NonNull QueueSubjectRec subject,
			@NonNull Long index) {

		return queueItemHelper.findByIndexRequired (
			parentTransaction,
			subject,
			index);

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

			return queueSubjectHelper.findActive (
				transaction);

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

			return queueSubjectHelper.findActive (
				transaction,
				queue);

		}

	}

}
