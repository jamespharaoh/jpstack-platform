package wbs.platform.queue.model;

import java.util.List;

import wbs.framework.database.Transaction;

public
interface QueueTypeDaoMethods {

	QueueTypeRec findRequired (
			Transaction parentTransaction,
			Long id);

	List <QueueTypeRec> findAll (
			Transaction parentTransaction);

}