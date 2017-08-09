package wbs.platform.queue.logic;

import java.util.List;

import wbs.framework.database.Transaction;

import wbs.platform.queue.model.QueueItemRec;
import wbs.platform.queue.model.QueueRec;
import wbs.platform.queue.model.QueueSubjectRec;

public
interface QueueCache {

	QueueItemRec findQueueItemByIndexRequired (
			Transaction parentTransaction,
			QueueSubjectRec subject,
			Long index);

	List <QueueSubjectRec> findQueueSubjects (
			Transaction parentTransaction);

	List <QueueSubjectRec> findQueueSubjects (
			Transaction parentTransaction,
			QueueRec queue);

}
