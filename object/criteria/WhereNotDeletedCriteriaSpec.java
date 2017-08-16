package wbs.platform.object.criteria;

import lombok.NonNull;
import lombok.experimental.Accessors;

import wbs.console.helper.core.ConsoleHelper;
import wbs.console.module.ConsoleSpec;
import wbs.console.priv.UserPrivChecker;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataClass;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;

@Accessors (fluent = true)
@DataClass ("where-not-deleted")
@PrototypeComponent ("whereNotDeletedCriteriaSpec")
public
class WhereNotDeletedCriteriaSpec <RecordType extends Record <RecordType>>
	implements
		ConsoleSpec,
		CriteriaSpec <RecordType> {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// implementation

	@Override
	public
	boolean evaluate (
			@NonNull Transaction parentTransaction,
			@NonNull ConsoleRequestContext requestContext,
			@NonNull UserPrivChecker privChecker,
			@NonNull ConsoleHelper <RecordType> consoleHelper,
			@NonNull RecordType object) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"evaluate");

		) {

			return ! consoleHelper.getDeleted (
				transaction,
				object,
				false);

		}

	}

}
