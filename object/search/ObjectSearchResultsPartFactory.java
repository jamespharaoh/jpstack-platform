package wbs.platform.object.search;

import java.util.Map;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.helper.core.ConsoleHelper;
import wbs.console.part.PagePart;
import wbs.console.part.PagePartFactory;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.IdObject;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;

@Accessors (fluent = true)
public
class ObjectSearchResultsPartFactory <
	ObjectType extends Record <ObjectType>,
	ResultType extends IdObject
>
	implements PagePartFactory {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <ObjectSearchResultsPart <ObjectType, ResultType>>
		objectSearchResultsPartProvider;

	// properties

	@Getter @Setter
	ConsoleHelper <ObjectType> consoleHelper;

	@Getter @Setter
	Map <String, ObjectSearchResultsMode <ResultType>> resultsModes;

	@Getter @Setter
	Class <ResultType> resultsClass;

	@Getter @Setter
	String resultsDaoMethodName;

	@Getter @Setter
	String sessionKey;

	@Getter @Setter
	Long itemsPerPage;

	@Getter @Setter
	String targetContextTypeName;

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

			return objectSearchResultsPartProvider.provide (
				transaction,
				objectSearchResultsPart ->
					objectSearchResultsPart

				.consoleHelper (
					consoleHelper)

				.resultsModes (
					resultsModes)

				.resultsClass (
					resultsClass)

				.resultsDaoMethodName (
					resultsDaoMethodName)

				.sessionKey (
					sessionKey)

				.itemsPerPage (
					itemsPerPage)

				.targetContextTypeName (
					targetContextTypeName)

			);

		}

	}

}
