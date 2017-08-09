package wbs.platform.deployment.hibernate;

import java.util.List;

import lombok.NonNull;

import org.hibernate.criterion.Restrictions;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.hibernate.HibernateDao;
import wbs.framework.logging.LogContext;

import wbs.platform.deployment.model.DaemonDeploymentDaoMethods;
import wbs.platform.deployment.model.DaemonDeploymentRec;

public
class DaemonDeploymentDaoHibernate
	extends HibernateDao
	implements DaemonDeploymentDaoMethods {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// implementation

	@Override
	public
	List <DaemonDeploymentRec> findByHostNotDeleted (
			@NonNull Transaction parentTransaction,
			@NonNull String host) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findByHostNotDeleted");

		) {

			return findMany (
				transaction,
				DaemonDeploymentRec.class,

				createCriteria (
					transaction,
					DaemonDeploymentRec.class)

				.add (
					Restrictions.eq (
						"host",
						host))

				.add (
					Restrictions.eq (
						"deleted",
						false))

			);

		}

	}

}
