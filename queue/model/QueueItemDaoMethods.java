package wbs.platform.queue.model;

import java.util.List;

import com.google.common.base.Optional;

import org.hibernate.Criteria;
import org.joda.time.Interval;

import wbs.framework.database.Transaction;

import wbs.platform.user.model.UserRec;

public
interface QueueItemDaoMethods {

	Criteria searchCriteria (
			Transaction parentTransaction,
			QueueItemSearch search);

	List <Long> searchIds (
			Transaction parentTransaction,
			QueueItemSearch search);

	Criteria searchUserQueueReportCriteria (
			Transaction parentTransaction,
			QueueItemSearch search);

	List <Long> searchUserQueueReportIds (
			Transaction parentTransaction,
			QueueItemSearch search);

	List <Optional <UserQueueReport>> searchUserQueueReports (
			Transaction parentTransaction,
			QueueItemSearch search,
			List <Long> ids);

	List <QueueItemRec> find (
			Transaction parentTransaction,
			List <QueueItemState> state);

	List <QueueItemRec> findByCreatedTime (
			Transaction parentTransaction,
			Interval createdTimeInterval);

	List <QueueItemRec> findByCreatedTime (
			Transaction parentTransaction,
			QueueRec queue,
			Interval createdTimeInterval);

	List <QueueItemRec> findByProcessedTime (
			Transaction parentTransaction,
			Interval processedTimeInterval);

	List <QueueItemRec> findByProcessedTime (
			Transaction parentTransaction,
			UserRec user,
			Interval processedTimeInterval);

	List <QueueItemStats> searchStats (
			Transaction parentTransaction,
			QueueItemStatsSearch search);

	List <QueueItemUserStats> searchUserStats (
			Transaction parentTransaction,
			QueueItemStatsSearch search);

}