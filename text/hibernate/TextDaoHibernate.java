package wbs.platform.text.hibernate;

import lombok.NonNull;

import org.hibernate.FlushMode;
import org.hibernate.criterion.Restrictions;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.hibernate.HibernateDao;
import wbs.framework.logging.LogContext;

import wbs.platform.text.model.TextDao;
import wbs.platform.text.model.TextRec;

@SingletonComponent ("textDao")
public
class TextDaoHibernate
	extends HibernateDao
	implements TextDao {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// implementations

	@Override
	public
	TextRec findByTextNoFlush (
			@NonNull Transaction parentTransaction,
			@NonNull String textValue) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findByTextNoFlush");

		) {

			return findOneOrNull (
				transaction,
				TextRec.class,

				createCriteria (
					transaction,
					TextRec.class,
					"_text")

				.add (
					Restrictions.eq (
						"_text.text",
						textValue))

				.setFlushMode (
					FlushMode.MANUAL)

			);

		}

	}

}
