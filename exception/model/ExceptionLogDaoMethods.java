package wbs.platform.exception.model;

import java.util.List;

import org.joda.time.Instant;

import wbs.framework.database.Transaction;

public
interface ExceptionLogDaoMethods {

	Long countWithAlert (
			Transaction parentTransaction);

	Long countWithAlertAndFatal (
			Transaction parentTransaction);

	List <Long> searchIds (
			Transaction parentTransaction,
			ExceptionLogSearch search);

	List <ExceptionLogRec> findOldLimit (
			Transaction parentTransaction,
			Instant cutoffTime,
			Long maxResults);

}