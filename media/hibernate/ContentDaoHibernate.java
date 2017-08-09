package wbs.platform.media.hibernate;

import java.util.List;

import lombok.NonNull;

import org.hibernate.criterion.Restrictions;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.hibernate.HibernateDao;
import wbs.framework.logging.LogContext;

import wbs.platform.media.model.ContentDao;
import wbs.platform.media.model.ContentRec;

public
class ContentDaoHibernate
	extends HibernateDao
	implements ContentDao {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// implementation

	@Override
	public
	List <ContentRec> findByShortHash (
			@NonNull Transaction parentTransaction,
			@NonNull Long shortHash) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findByShortHash");

		) {

			return findMany (
				transaction,
				ContentRec.class,

				createCriteria (
					transaction,
					ContentRec.class,
					"_content")

				.add (
					Restrictions.eq (
						"_content.hash",
						shortHash))

			);

		}

	}

}
