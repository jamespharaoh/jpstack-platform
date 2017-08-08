package wbs.platform.user.console;

import static wbs.utils.etc.LogicUtils.ifThenElse;
import static wbs.utils.etc.NullUtils.isNotNull;
import static wbs.utils.string.StringUtils.stringSplitComma;

import java.util.Collections;
import java.util.List;

import lombok.NonNull;

import wbs.console.request.ConsoleRequestContext;
import wbs.console.supervisor.SupervisorHelper;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.platform.scaffold.model.SliceRec;

@SingletonComponent ("supervisorHelper")
public
class SupervisorHelperImplementation
	implements SupervisorHelper {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	// implementation

	@Override
	public
	List <String> getSupervisorConfigNames (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"getSupervisorConfigNames");

		) {

			SliceRec slice =
				userConsoleLogic.sliceRequired (
					transaction);

			List <String> supervisorConfigNames =
				ifThenElse (
					isNotNull (
						slice.getSupervisorConfigNames ()),
					() -> stringSplitComma (
						slice.getSupervisorConfigNames ()),
					() -> Collections.emptyList ());

			return supervisorConfigNames;

		}

	}

}
