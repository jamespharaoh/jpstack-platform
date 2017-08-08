package wbs.platform.queue.model;

import java.util.List;

import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;

public
interface QueueSubjectDaoMethods {

	QueueSubjectRec find (
			Transaction parentTransaction,
			QueueRec queue,
			Record <?> object);

	List <QueueSubjectRec> findActive (
			Transaction parentTransaction);

	List <QueueSubjectRec> findActive (
			Transaction parentTransaction,
			QueueRec queue);

}