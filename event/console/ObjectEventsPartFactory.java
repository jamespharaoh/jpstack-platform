package wbs.platform.event.console;

import static wbs.utils.etc.TypeUtils.genericCastUnchecked;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.lookup.ObjectLookup;
import wbs.console.part.PagePart;
import wbs.console.part.PagePartFactory;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.PermanentRecord;
import wbs.framework.logging.LogContext;

@Accessors (fluent = true)
public
class ObjectEventsPartFactory <ObjectType>
	implements PagePartFactory {

	// singleton dependencies

	@SingletonDependency
	EventConsoleLogic eventConsoleLogic;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	// properties

	@Getter @Setter
	ObjectLookup <ObjectType> objectLookup;

	// implementation

	@Override
	public
	PagePart buildPagePart (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"buildPagePart");

		) {

			PermanentRecord <?> object =
				genericCastUnchecked (
					objectLookup.lookupObject (
						transaction,
						requestContext.consoleContextStuffRequired ()));

			return eventConsoleLogic.makeEventsPart (
				transaction,
				object);

		}

	}

}
