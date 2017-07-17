package wbs.platform.object.search;

import java.io.Serializable;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.forms.core.ConsoleFormType;
import wbs.console.helper.core.ConsoleHelper;
import wbs.console.part.PagePart;
import wbs.console.part.PagePartFactory;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;

@Accessors (fluent = true)
public
class ObjectSearchPartFactory <
	ObjectType extends Record <ObjectType>,
	SearchType extends Serializable
>
	implements PagePartFactory {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <ObjectSearchPart <ObjectType, SearchType>>
		objectSearchPartProvider;

	// properties

	@Getter @Setter
	ConsoleHelper <ObjectType> consoleHelper;

	@Getter @Setter
	Class <SearchType> searchClass;

	@Getter @Setter
	String sessionKey;

	@Getter @Setter
	ConsoleFormType <SearchType> searchFormType;

	@Getter @Setter
	String fileName;

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

			return objectSearchPartProvider.provide (
				transaction)

				.consoleHelper (
					consoleHelper)

				.searchClass (
					searchClass)

				.sessionKey (
					sessionKey)

				.searchFormType (
					searchFormType)

				.fileName (
					fileName)

			;

		}

	}

}
