package wbs.platform.event.model;

import java.util.List;

import wbs.framework.database.Transaction;

public
interface EventLinkDaoMethods {

	List <EventLinkRec> findByTypeAndRef (
			Transaction parentTransaction,
			Long typeId,
			Long refId);

}