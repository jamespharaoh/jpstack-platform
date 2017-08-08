package wbs.platform.service.hibernate;

import java.util.List;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.hibernate.HibernateDao;
import wbs.framework.logging.LogContext;

import wbs.platform.service.model.ServiceTypeDao;
import wbs.platform.service.model.ServiceTypeRec;

public
class ServiceTypeDaoHibernate
	extends HibernateDao
	implements ServiceTypeDao {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// implementation

	@Override
	public
	List <ServiceTypeRec> findAll (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findAll");

		) {

			return findMany (
				transaction,
				ServiceTypeRec.class,

				createCriteria (
					transaction,
					ServiceTypeRec.class)

			);

		}

	}

	@Override
	public
	ServiceTypeRec findRequired (
			@NonNull Transaction parentTransaction,
			@NonNull Long serviceTypeId) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findRequired");

		) {

			return get (
				transaction,
				ServiceTypeRec.class,
				serviceTypeId);

		}

	}

}
