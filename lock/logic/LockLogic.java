package wbs.platform.lock.logic;

import wbs.framework.database.Transaction;

public
interface LockLogic {

	void magicLock (
			Transaction parentTransaction,
			Object ... objects);

}