package wbs.platform.updatelog.model;

import wbs.framework.database.Transaction;

public
interface UpdateLogDaoMethods {

	UpdateLogRec findByTableAndRef (
			Transaction parentTransaction,
			String table,
			Long ref);

}