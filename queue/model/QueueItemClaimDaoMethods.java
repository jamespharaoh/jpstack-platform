package wbs.platform.queue.model;

import java.util.List;

import wbs.framework.database.Transaction;

import wbs.platform.user.model.UserRec;

public
interface QueueItemClaimDaoMethods {

	List <QueueItemClaimRec> findClaimed (
			Transaction parentTransaction);

	List <QueueItemClaimRec> findClaimed (
			Transaction parentTransaction,
			UserRec user);

}