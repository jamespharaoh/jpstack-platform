package wbs.platform.queue.logic;

import java.util.List;

import lombok.NonNull;

import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;

import wbs.platform.queue.model.QueueItemRec;
import wbs.platform.queue.model.QueueRec;
import wbs.platform.queue.model.QueueSubjectRec;
import wbs.platform.scaffold.model.SliceRec;
import wbs.platform.user.model.UserRec;

public
interface QueueLogic {

	QueueRec findQueueByCodeRequired (
			Transaction parentTransaction,
			Record <?> queueParent,
			String queueCode);

	QueueItemRec createQueueItem (
			Transaction parentTransaction,
			QueueSubjectRec queueSubject,
			Record <?> refObject,
			String source,
			String details);

	QueueItemRec createQueueItem (
			Transaction parentTransaction,
			QueueRec queue,
			Record <?> subjectObject,
			Record <?> refObject,
			String source,
			String details);

	default
	QueueItemRec createQueueItem (
			@NonNull Transaction parentTransaction,
			@NonNull Record <?> queueParent,
			@NonNull String queueCode,
			@NonNull Record <?> subjectObject,
			@NonNull Record <?> refObject,
			@NonNull String source,
			@NonNull String details) {

		return createQueueItem (
			parentTransaction,
			findQueueByCodeRequired (
				parentTransaction,
				queueParent,
				queueCode),
			subjectObject,
			refObject,
			source,
			details);

	}

	void cancelQueueItem (
			Transaction parentTransaction,
			QueueItemRec queueItem);

	void processQueueItem (
			Transaction parentTransaction,
			QueueItemRec queueItem,
			UserRec user);

	QueueRec findQueue (
			Transaction parentTransaction,
			Record <?> parentObject,
			String code);

	QueueSubjectRec findOrCreateQueueSubject (
			Transaction parentTransaction,
			QueueRec queue,
			Record <?> object);

	List <QueueItemRec> getActiveQueueItems (
			Transaction parentTransaction,
			QueueSubjectRec queueSubject);

	boolean sliceHasQueueActivity (
			Transaction parentTransaction,
			SliceRec slice);

}
