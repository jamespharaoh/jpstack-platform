package wbs.platform.updatelog.hibernate;

import lombok.NonNull;

import org.hibernate.criterion.Restrictions;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.hibernate.HibernateDao;
import wbs.framework.logging.LogContext;

import wbs.platform.updatelog.model.UpdateLogDao;
import wbs.platform.updatelog.model.UpdateLogRec;

@SingletonComponent ("updateLogDao")
public
class UpdateLogDaoHibernate
	extends HibernateDao
	implements UpdateLogDao {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// implementations

	@Override
	public
	UpdateLogRec findByTableAndRef (
			@NonNull Transaction parentTransaction,
			@NonNull String table,
			@NonNull Long ref) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findByTableAndRef");

		) {

			return findOneOrNull (
				transaction,
				UpdateLogRec.class,

				createCriteria (
					transaction,
					UpdateLogRec.class,
					"_updateLog")

				.add (
					Restrictions.eq (
						"_updateLog.code",
						table))

				.add (
					Restrictions.eq (
						"_updateLog.ref",
						ref))

			);

		}

	}

}
