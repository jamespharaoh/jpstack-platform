package wbs.platform.user.hibernate;

import java.util.List;

import lombok.NonNull;

import org.hibernate.criterion.Restrictions;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.hibernate.HibernateDaoLegacy;
import wbs.framework.logging.LogContext;

import wbs.platform.priv.model.PrivRec;
import wbs.platform.user.model.UserPrivDao;
import wbs.platform.user.model.UserPrivRec;
import wbs.platform.user.model.UserRec;

public
class UserPrivDaoHibernate
	extends HibernateDaoLegacy
	implements UserPrivDao {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// implementation

	@Override
	public
	UserPrivRec find (
			@NonNull Transaction parentTransaction,
			@NonNull UserRec user,
			@NonNull PrivRec priv) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"find");

		) {

			return findOneOrNull (
				transaction,
				UserPrivRec.class,

				createCriteria (
					transaction,
					UserPrivRec.class,
					"_userPriv")

				.add (
					Restrictions.eq (
						"_userPriv.user",
						user))

				.add (
					Restrictions.eq (
						"_userPriv.priv",
						priv))

			);

		}

	}

	@Override
	public
	List <UserPrivRec> find (
			@NonNull Transaction parentTransaction,
			@NonNull PrivRec priv) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"find");

		) {

			return findMany (
				transaction,
				UserPrivRec.class,

				createCriteria (
					transaction,
					UserPrivRec.class,
					"_userPriv")

				.add (
					Restrictions.eq (
						"_userPriv.priv",
						priv))

			);

		}

	}

}
