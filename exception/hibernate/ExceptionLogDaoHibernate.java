package wbs.platform.exception.hibernate;

import static wbs.utils.etc.NullUtils.isNotNull;
import static wbs.utils.etc.NumberUtils.toJavaIntegerRequired;

import java.util.List;

import lombok.NonNull;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;
import org.joda.time.Instant;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.hibernate.HibernateDaoLegacy;
import wbs.framework.logging.LogContext;

import wbs.platform.exception.model.ExceptionLogDao;
import wbs.platform.exception.model.ExceptionLogRec;
import wbs.platform.exception.model.ExceptionLogSearch;

public
class ExceptionLogDaoHibernate
	extends HibernateDaoLegacy
	implements ExceptionLogDao {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// implementation

	@Override
	public
	Long countWithAlert (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"countWithAlert");

		) {

			return findOneOrNull (
				transaction,
				Long.class,

				createCriteria (
					transaction,
					ExceptionLogRec.class,
					"_exceptionLog")

				.add (
					Restrictions.eq (
						"_exceptionLog.alert",
						true))

				.setProjection (
					Projections.rowCount ())

			);

		}

	}

	@Override
	public
	Long countWithAlertAndFatal (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"countWithAlertAndFatal");

		) {

			return findOneOrNull (
				transaction,
				Long.class,

				createCriteria (
					transaction,
					ExceptionLogRec.class,
					"_exceptionLog")

				.add (
					Restrictions.eq (
						"_exceptionLog.alert",
						true))

				.add (
					Restrictions.eq (
						"_exceptionLog.fatal",
						true))

				.setProjection (
					Projections.rowCount ())

			);

		}

	}

	@Override
	public
	List <Long> searchIds (
			@NonNull Transaction parentTransaction,
			@NonNull ExceptionLogSearch search) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"searchIds");

		) {

			Criteria criteria =
				createCriteria (
					transaction,
					ExceptionLogRec.class,
					"_exceptionLog")

				.createAlias (
					"_exceptionLog.user",
					"_user",
					JoinType.LEFT_OUTER_JOIN);

			if (
				isNotNull (
					search.timestamp ())
			) {

				criteria.add (
					Restrictions.ge (
						"_exceptionLog.timestamp",
						search.timestamp ().start ()));

				criteria.add (
					Restrictions.lt (
						"_exceptionLog.timestamp",
						search.timestamp ().end ()));

			}

			if (
				isNotNull (
					search.typeId ())
			) {

				criteria.add (
					Restrictions.eq (
						"_exceptionLog.type.id",
						search.typeId ()));

			}

			if (
				isNotNull (
					search.userSliceId ())
			) {

				criteria.add (
					Restrictions.eq (
						"_user.slice.id",
						search.userSliceId ()));

			}

			if (
				isNotNull (
					search.userId ())
			) {

				criteria.add (
					Restrictions.eq (
						"_user.id",
						search.userId ()));

			}

			if (
				isNotNull (
					search.sourceContains ())
			) {

				criteria.add (
					Restrictions.ilike (
						"_exceptionLog.source",
						"%" + search.sourceContains () + "%"));

			}

			if (
				isNotNull (
					search.summaryContains ())
			) {

				criteria.add (
					Restrictions.ilike (
						"_exceptionLog.summary",
						"%" + search.summaryContains () + "%"));

			}

			if (
				isNotNull (
					search.dumpContains ())
			) {

				criteria.add (
					Restrictions.ilike (
						"_exceptionLog.dump",
						"%" + search.dumpContains () + "%"));

			}

			if (
				isNotNull (
					search.alert ())
			) {

				criteria.add (
					Restrictions.eq (
						"_exceptionLog.alert",
						search.alert ()));

			}

			if (
				isNotNull (
					search.fatal ())
			) {

				criteria.add (
					Restrictions.eq (
						"_exceptionLog.fatal",
						search.fatal ()));

			}

			if (
				isNotNull (
					search.resolution ())
			) {

				criteria.add (
					Restrictions.eq (
						"_exceptionLog.resolution",
						search.resolution ()));

			}

			if (search.order () != null) {

				switch (search.order ()) {

				case timestampDesc:

					criteria.addOrder (
						Order.desc (
							"_exceptionLog.timestamp"));

					break;

				default:

					throw new IllegalArgumentException ();

				}

			}

			if (search.maxResults () != null) {

				criteria.setMaxResults (
					toJavaIntegerRequired (
						search.maxResults ()));

			}

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
	List <ExceptionLogRec> findOldLimit (
			@NonNull Transaction parentTransaction,
			@NonNull Instant cutoffTime,
			@NonNull Long maxResults) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findOldLimit");

		) {

			return findMany (
				transaction,
				ExceptionLogRec.class,

				createCriteria (
					transaction,
					ExceptionLogRec.class,
					"_exceptionLog")

				.add (
					Restrictions.lt (
						"_exceptionLog.timestamp",
						cutoffTime))

				.addOrder (
					Order.asc (
						"_exceptionLog.timestamp"))

				.setMaxResults (
					toJavaIntegerRequired (
						maxResults))

			);

		}

	}

}
