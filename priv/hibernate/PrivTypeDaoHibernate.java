package wbs.platform.priv.hibernate;

import java.util.List;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.hibernate.HibernateDao;
import wbs.framework.logging.LogContext;

import wbs.platform.priv.model.PrivTypeDao;
import wbs.platform.priv.model.PrivTypeRec;

public
class PrivTypeDaoHibernate
	extends HibernateDao
	implements PrivTypeDao {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// implementation

	@Override
	public
	List <PrivTypeRec> findAll (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findAll");

		) {

			return findMany (
				transaction,
				PrivTypeRec.class,

				createCriteria (
					transaction,
					PrivTypeRec.class)

			);

		}

	}

	@Override
	public
	PrivTypeRec findRequired (
			@NonNull Transaction parentTransaction,
			@NonNull Long privTypeId) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findRequired");

		) {

			return get (
				transaction,
				PrivTypeRec.class,
				privTypeId);

		}

	}

}
