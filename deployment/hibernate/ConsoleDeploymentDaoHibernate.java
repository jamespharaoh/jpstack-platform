package wbs.platform.deployment.hibernate;

import java.util.List;

import lombok.NonNull;

import org.hibernate.criterion.Restrictions;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.hibernate.HibernateDao;
import wbs.framework.logging.LogContext;

import wbs.platform.deployment.model.ConsoleDeploymentDaoMethods;
import wbs.platform.deployment.model.ConsoleDeploymentRec;

public
class ConsoleDeploymentDaoHibernate
	extends HibernateDao
	implements ConsoleDeploymentDaoMethods {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// implementation

	@Override
	public
	List <ConsoleDeploymentRec> findByHostNotDeleted (
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
				ConsoleDeploymentRec.class,

				createCriteria (
					transaction,
					ConsoleDeploymentRec.class)

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
