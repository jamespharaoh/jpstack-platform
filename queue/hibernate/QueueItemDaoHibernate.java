package wbs.platform.queue.hibernate;

import static wbs.utils.collection.CollectionUtils.collectionIsNotEmpty;
import static wbs.utils.etc.NullUtils.isNotNull;
import static wbs.utils.string.StringUtils.stringFormat;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Optional;

import lombok.NonNull;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;
import org.hibernate.transform.Transformers;
import org.hibernate.type.LongType;
import org.hibernate.type.Type;
import org.joda.time.Interval;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.hibernate.HibernateDaoLegacy;
import wbs.framework.logging.LogContext;

import wbs.platform.queue.model.QueueItemDao;
import wbs.platform.queue.model.QueueItemRec;
import wbs.platform.queue.model.QueueItemSearch;
import wbs.platform.queue.model.QueueItemState;
import wbs.platform.queue.model.QueueItemStats;
import wbs.platform.queue.model.QueueItemStatsSearch;
import wbs.platform.queue.model.QueueItemUserStats;
import wbs.platform.queue.model.QueueItemViewRec;
import wbs.platform.queue.model.QueueRec;
import wbs.platform.queue.model.UserQueueReport;
import wbs.platform.user.model.UserRec;

public
class QueueItemDaoHibernate
	extends HibernateDaoLegacy
	implements QueueItemDao {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// implementation

	@Override
	public
	Criteria searchCriteria (
			@NonNull Transaction parentTransaction,
			@NonNull QueueItemSearch search) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"searchCriteria");

		) {

			Criteria criteria =
				createCriteria (
					transaction,
					QueueItemRec.class,
					"_queueItem")

				.createAlias (
					"_queueItem.queueSubject",
					"_queueSubject")

				.createAlias (
					"_queueSubject.queue",
					"_queue")

				.createAlias (
					"_queueItem.processedUser",
					"_processedUser",
					JoinType.LEFT_OUTER_JOIN)

				.createAlias (
					"_processedUser.slice",
					"_processedUserSlice",
					JoinType.LEFT_OUTER_JOIN)

				.createAlias (
					"_queueItem.queueItemClaim",
					"_queueItemClaim",
					JoinType.LEFT_OUTER_JOIN)

				.createAlias (
					"_queueItemClaim.user",
					"_claimedUser",
					JoinType.LEFT_OUTER_JOIN);

			if (
				isNotNull (
					search.sliceId ())
			) {

				criteria.add (
					Restrictions.eq (
						"_queue.slice.id",
						search.sliceId ()));

			}

			if (
				isNotNull (
					search.queueParentTypeId ())
			) {

				criteria.add (
					Restrictions.eq (
						"_queue.parentType.id",
						search.queueParentTypeId ()));

			}

			if (
				isNotNull (
					search.queueTypeId ())
			) {

				criteria.add (
					Restrictions.eq (
						"_queue.queueType.id",
						search.queueTypeId ()));

			}

			if (
				isNotNull (
					search.queueId ())
			) {

				criteria.add (
					Restrictions.eq (
						"_queue.id",
						search.queueId ()));

			}

			if (
				isNotNull (
					search.createdTime ())
			) {

				criteria.add (
					Restrictions.ge (
						"_queueItem.createdTime",
						search.createdTime ().start ()));

				criteria.add (
					Restrictions.lt (
						"_queueItem.createdTime",
						search.createdTime ().end ()));

			}

			if (
				isNotNull (
					search.claimedUserId ())
			) {

				criteria.add (
					Restrictions.eq (
						"_claimedUser.id",
						search.claimedUserId ()));

			}

			if (
				isNotNull (
					search.processedUserId ())
			) {

				criteria.add (
					Restrictions.eq (
						"_processedUser.id",
						search.processedUserId ()));

			}

			if (
				isNotNull (
					search.state ())
			) {

				criteria.add (
					Restrictions.eq (
						"_queueItem.state",
						search.state ()));

			}

			if (search.filter ()) {

				List <Criterion> filterCriteria =
					new ArrayList<> ();

				if (
					collectionIsNotEmpty (
						search.filterQueueIds ())
				) {

					filterCriteria.add (
						Restrictions.in (
							"_queue.id",
							search.filterQueueIds ()));

				}

				criteria.add (
					Restrictions.or (
						filterCriteria.toArray (
							new Criterion [] {})));

			}

			return criteria;

		}

	}

	@Override
	public
	List <Long> searchIds (
			@NonNull Transaction parentTransaction,
			@NonNull QueueItemSearch search) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"searchIds");

		) {

			Criteria criteria =
				searchCriteria (
					transaction,
					search);

			criteria.addOrder (
				Order.desc (
					"_queueItem.createdTime"));

			criteria.setProjection (
				Projections.id ());

			return findMany (
				transaction,
				Long.class,
				criteria);

		}

	}

	@Override
	public
	List <QueueItemRec> find (
			@NonNull Transaction parentTransaction,
			@NonNull List <QueueItemState> states) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"find");

		) {

			return findMany (
				transaction,
				QueueItemRec.class,

				createCriteria (
					transaction,
					QueueItemRec.class,
					"_queueItem")

				.add (
					Restrictions.in (
						"_queueItem.state",
						states))

			);

		}

	}

	@Override
	public
	List <QueueItemRec> findByCreatedTime (
			@NonNull Transaction parentTransaction,
			@NonNull Interval createdTime) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findByCreatedTime");

		) {

			return findMany (
				transaction,
				QueueItemRec.class,

				createCriteria (
					transaction,
					QueueItemRec.class,
					"_queueItem")

				.add (
					Restrictions.ge (
						"_queueItem.createdTime",
						createdTime.getStart ()))

				.add (
					Restrictions.lt (
						"_queueItem.createdTime",
						createdTime.getEnd ()))

			);

		}

	}

	@Override
	public
	List <QueueItemRec> findByCreatedTime (
			@NonNull Transaction parentTransaction,
			@NonNull QueueRec queue,
			@NonNull Interval createdTime) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findByCreatedTime");

		) {

			return findMany (
				transaction,
				QueueItemRec.class,

				createCriteria (
					transaction,
					QueueItemRec.class,
					"_queueItem")

				.createAlias (
					"_queueItem.queueSubject",
					"_queueSubject")

				.add (
					Restrictions.eq (
						"_queueSubject.queue",
						queue))

				.add (
					Restrictions.ge (
						"_queueItem.createdTime",
						createdTime.getStart ()))

				.add (
					Restrictions.lt (
						"_queueItem.createdTime",
						createdTime.getEnd ()))

			);

		}

	}

	@Override
	public
	List <QueueItemRec> findByProcessedTime (
			@NonNull Transaction parentTransaction,
			@NonNull Interval processedTime) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findByProcessedTime");

		) {

			return findMany (
				transaction,
				QueueItemRec.class,

				createCriteria (
					transaction,
					QueueItemRec.class,
					"_queueItem")

				.add (
					Restrictions.ge (
						"_queueItem.processedTime",
						processedTime.getStart ()))

				.add (
					Restrictions.lt (
						"_queueItem.processedTime",
						processedTime.getEnd ()))

			);

		}

	}

	@Override
	public
	List <QueueItemRec> findByProcessedTime (
			@NonNull Transaction parentTransaction,
			@NonNull UserRec user,
			@NonNull Interval processedTime) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findByProcessedTime");

		) {

			return findMany (
				transaction,
				QueueItemRec.class,

				createCriteria (
					transaction,
					QueueItemRec.class,
					"_queueItem")

				.add (
					Restrictions.eq (
						"_queueItem.processedUser",
						user))

				.add (
					Restrictions.ge (
						"_queueItem.processedTime",
						processedTime.getStart ()))

				.add (
					Restrictions.lt (
						"_queueItem.processedTime",
						processedTime.getEnd ()))

			);

		}

	}

	@Override
	public
	Criteria searchUserQueueReportCriteria (
			@NonNull Transaction parentTransaction,
			@NonNull QueueItemSearch search) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"searchUserQueueReportCriteria");

		) {

			Criteria criteria =
				searchCriteria (
					transaction,
					search);

			criteria.add (
				Restrictions.isNotNull (
					"_queueItem.processedUser"));

			criteria.setProjection (
				Projections.projectionList ()

				.add (
					Projections.property (
						"_queueItem.processedUser"),
					"user")

				.add (
					Projections.count (
						"_queueItem.id"),
					"messageCount")

				.add (
					Projections.min (
						"_queueItem.createdTime"),
					"firstMessage")

				.add (
					Projections.max (
						"_queueItem.createdTime"),
					"lastMessage")

				.add (
					Projections.sqlProjection (
						stringFormat (
							"avg (CASE WHEN {alias}.processed_time IS NULL THEN ",
							"NULL ELSE EXTRACT (EPOCH FROM ({alias}.",
							"processed_time - {alias}.created_time)) END) AS ",
							"time_to_process"),
						new String [] {
							"time_to_process",
						},
						new Type [] {
							LongType.INSTANCE,
						}),
					"timeToProcess")

				.add (
					Projections.groupProperty (
						"_queueItem.processedUser"))

			);

			criteria.setResultTransformer (
				Transformers.aliasToBean (
					UserQueueReport.class));

			return criteria;

		}

	}

	@Override
	public
	List <Long> searchUserQueueReportIds (
			@NonNull Transaction parentTransaction,
			@NonNull QueueItemSearch search) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"searchUserQueueReportIds");

		) {

			Criteria criteria =
				searchCriteria (
					transaction,
					search);

			criteria.add (
				Restrictions.isNotNull (
					"_queueItem.processedUser"));

			criteria.setProjection (
				Projections.projectionList ()

				.add (
					Projections.distinct (
						Projections.property (
							"_queueItem.processedUser.id")))

				.add (
					Projections.groupProperty (
						"_queueItem.processedUser"))

				.add (
					Projections.groupProperty (
						"_processedUserSlice.code"))

				.add (
					Projections.groupProperty (
						"_processedUser.username"))

			);

			criteria.addOrder (
				Order.asc (
					"_processedUserSlice.code"));

			criteria.addOrder (
				Order.asc (
					"_processedUser.username"));

			return findIdsOnly (
				criteria.list ());

		}

	}

	@Override
	public
	List <Optional <UserQueueReport>> searchUserQueueReports (
			@NonNull Transaction parentTransaction,
			@NonNull QueueItemSearch search,
			@NonNull List<Long> objectIds) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"searchUserQueueReports");

		) {

			Criteria criteria =
				searchUserQueueReportCriteria (
					transaction,
					search);

			criteria.add (
				Restrictions.in (
					"_processedUser.id",
					objectIds));

			return findOrdered (
				transaction,
				UserQueueReport.class,
				objectIds,
				criteria.list ());

		}

	}

	@Override
	public
	List <QueueItemStats> searchStats (
			@NonNull Transaction parentTransaction,
			@NonNull QueueItemStatsSearch search) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"searchStats");

		) {

			Criteria criteria =
				createCriteria (
					transaction,
					QueueItemViewRec.class,
					"_queueItemView")

				.createAlias (
					"_queueItemView.queue",
					"_queue")

				.createAlias (
					"_queueItemView.processedByUser",
					"_processedByUser",
					JoinType.LEFT_OUTER_JOIN)

			;

			// apply criteria

			if (
				isNotNull (
					search.queueIds ())
			) {

				criteria.add (
					Restrictions.in (
						"_queue.id",
						search.queueIds ()));

			}

			if (
				isNotNull (
					search.userIds ())
			) {

				criteria.add (
					Restrictions.in (
						"_processedByUser.id",
						search.userIds ()));

			}

			if (
				isNotNull (
					search.timestamp ())
			) {

				criteria.add (
					Restrictions.ge (
						"_queueItemView.timestamp",
						search.timestamp ().start ()));

				criteria.add (
					Restrictions.lt (
						"_queueItemView.timestamp",
						search.timestamp ().end ()));

			}

			// apply filter

			criteria.add (
				Restrictions.or (

				Restrictions.in (
					"_queue.id",
					search.filterQueueIds ()),

				Restrictions.in (
					"_processedByUser.id",
					search.filterUserIds ())

			));

			// apply projection

			criteria.setProjection (
				Projections.projectionList ()

				.add (
					Projections.groupProperty (
						"_queueItemView.queue"),
					"queue")

				.add (
					Projections.sum (
						"_queueItemView.numCreated"),
					"numCreated")

				.add (
					Projections.sum (
						"_queueItemView.numProcessed"),
					"numProcessed")

				.add (
					Projections.sum (
						"_queueItemView.numPreferred"),
					"numPreferred")

				.add (
					Projections.sum (
						"_queueItemView.numNotPreferred"),
					"numNotPreferred")

			);

			criteria.setResultTransformer (
				Transformers.aliasToBean (
					QueueItemStats.class));

			// return

			return findMany (
				transaction,
				QueueItemStats.class,
				criteria);

		}

	}

	@Override
	public
	List <QueueItemUserStats> searchUserStats (
			@NonNull Transaction parentTransaction,
			@NonNull QueueItemStatsSearch search) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"searchUserStats");

		) {

			Criteria criteria =
				createCriteria (
					transaction,
					QueueItemViewRec.class,
					"_queueItemView")

				.createAlias (
					"_queueItemView.queue",
					"_queue")

				.createAlias (
					"_queueItemView.processedByUser",
					"_processedByUser")

			;

			// apply criteria

			if (
				isNotNull (
					search.queueIds ())
			) {

				criteria.add (
					Restrictions.in (
						"_queue.id",
						search.queueIds ()));

			}

			if (
				isNotNull (
					search.userIds ())
			) {

				criteria.add (
					Restrictions.in (
						"_processedByUser.id",
						search.userIds ()));

			}

			if (
				isNotNull (
					search.timestamp ())
			) {

				criteria.add (
					Restrictions.ge (
						"_queueItemView.timestamp",
						search.timestamp ().start ()));

				criteria.add (
					Restrictions.lt (
						"_queueItemView.timestamp",
						search.timestamp ().end ()));

			}

			// apply filter

			criteria.add (
				Restrictions.or (

				Restrictions.in (
					"_queue.id",
					search.filterQueueIds ()),

				Restrictions.in (
					"_processedByUser.id",
					search.filterUserIds ())

			));

			// apply projection

			criteria.setProjection (
				Projections.projectionList ()

				.add (
					Projections.groupProperty (
						"_queueItemView.queue"),
					"queue")

				.add (
					Projections.groupProperty (
						"_queueItemView.processedByUser"),
					"user")

				.add (
					Projections.sum (
						"_queueItemView.numProcessed"),
					"numProcessed")

			);

			criteria.setResultTransformer (
				Transformers.aliasToBean (
					QueueItemUserStats.class));

			// return

			return findMany (
				transaction,
				QueueItemUserStats.class,
				criteria);

		}

	}

}
