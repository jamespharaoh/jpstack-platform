package wbs.platform.affiliate.hibernate;

import java.util.List;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.hibernate.HibernateDaoLegacy;
import wbs.framework.logging.LogContext;

import wbs.platform.affiliate.model.AffiliateTypeDao;
import wbs.platform.affiliate.model.AffiliateTypeRec;

public
class AffiliateTypeDaoHibernate
	extends HibernateDaoLegacy
	implements AffiliateTypeDao {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// implementation

	@Override
	public
	List <AffiliateTypeRec> findAll (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findAll");

		) {

			return findMany (
				transaction,
				AffiliateTypeRec.class,

				createCriteria (
					transaction,
					AffiliateTypeRec.class)

			);

		}

	}

	@Override
	public
	AffiliateTypeRec findRequired (
			@NonNull Transaction parentTransaction,
			@NonNull Long affiliateTypeId) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findRequired");

		) {

			return get (
				transaction,
				AffiliateTypeRec.class,
				affiliateTypeId);

		}

	}

}
