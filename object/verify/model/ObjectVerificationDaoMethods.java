package wbs.platform.object.verify.model;

import java.util.List;

import org.joda.time.Instant;

import wbs.framework.database.Transaction;

public
interface ObjectVerificationDaoMethods {

	List <Long> findIdsPendingLimit (
			Transaction parentTransaction,
			Instant timestamp,
			Long maxItems);

}
