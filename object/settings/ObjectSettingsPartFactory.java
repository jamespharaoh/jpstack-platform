package wbs.platform.object.settings;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.forms.core.ConsoleFormType;
import wbs.console.helper.core.ConsoleHelper;
import wbs.console.lookup.ObjectLookup;
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
class ObjectSettingsPartFactory <
	ObjectType extends Record <ObjectType>
>
	implements PagePartFactory {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <ObjectSettingsPart <ObjectType, ?>>
		objectSettingsPartProvider;

	// properties

	@Getter @Setter
	ObjectLookup <ObjectType> objectLookup;

	@Getter @Setter
	ConsoleHelper <ObjectType> consoleHelper;

	@Getter @Setter
	String editPrivKey;

	@Getter @Setter
	String localName;

	@Getter @Setter
	ConsoleFormType <ObjectType> formType;

	@Getter @Setter
	String removeLocalName;

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

			return objectSettingsPartProvider.provide (
				transaction,
				objectSettingsPart ->
					objectSettingsPart

				.objectLookup (
					objectLookup)

				.consoleHelper (
					consoleHelper)

				.editPrivKey (
					editPrivKey)

				.localName (
					localName)

				.formType (
					formType)

				.removeLocalName (
					removeLocalName)

			);

		}

	}

}
