package wbs.platform.event.model;

import java.util.List;

import wbs.framework.database.Transaction;

public
interface EventDaoMethods {

	List <Long> searchIds (
			Transaction parentTransaction,
			EventSearch eventSearch);

}
