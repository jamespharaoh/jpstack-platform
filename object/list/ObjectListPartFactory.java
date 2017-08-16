package wbs.platform.object.list;

import static wbs.utils.collection.MapUtils.mapIsNotEmpty;
import static wbs.utils.etc.LogicUtils.ifThenElse;
import static wbs.utils.string.StringUtils.stringFormat;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

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
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.object.criteria.WhereDeletedCriteriaSpec;
import wbs.platform.object.criteria.WhereICanManageCriteriaSpec;
import wbs.platform.object.criteria.WhereNotDeletedCriteriaSpec;

@Accessors (fluent = true)
public
class ObjectListPartFactory <RecordType extends Record <RecordType>>
	implements PagePartFactory {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <ObjectListTabSpec <RecordType>>
		objectListTabSpecProvider;

	@PrototypeDependency
	ComponentProvider <ObjectListPart <RecordType, ?>> objectListPartProvider;

	@PrototypeDependency
	ComponentProvider <WhereDeletedCriteriaSpec <RecordType>>
		whereDeletedCriteriaSpecProvider;

	@PrototypeDependency
	ComponentProvider <WhereICanManageCriteriaSpec <RecordType>>
		whereICanManageCriteriaSpecProvider;

	@PrototypeDependency
	ComponentProvider <WhereNotDeletedCriteriaSpec <RecordType>>
		whereNotDeletedCriteriaSpecProvider;

	// properties

	@Getter @Setter
	ConsoleHelper <RecordType> consoleHelper;

	@Getter @Setter
	String typeCode;

	@Getter @Setter
	String localName;

	@Getter @Setter
	ConsoleFormType <RecordType> formType;

	@Getter @Setter
	Map <String, ObjectListTabSpec <RecordType>> listTabSpecs;

	@Getter @Setter
	Map <String, ObjectListBrowserSpec> listBrowserSpecs;

	@Getter @Setter
	String targetContextTypeName;

	@Getter @Setter
	Boolean moveControls;

	@Getter @Setter
	Boolean editControl;

	@Getter @Setter
	Boolean deleteControl;

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

			return objectListPartProvider.provide (
				transaction,
				objectListPart ->
					objectListPart

				.consoleHelper (
					consoleHelper)

				.typeCode (
					typeCode)

				.localName (
					localName)

				.listTabSpecs (
					ifThenElse (
						mapIsNotEmpty (
							listTabSpecs),
						() -> listTabSpecs,
						() -> defaultListTabSpecs (
							transaction)))

				.formType (
					formType)

				.listBrowserSpecs (
					listBrowserSpecs)

				.targetContextTypeName (
					targetContextTypeName)

				.moveControls (
					moveControls)

				.editControl (
					editControl)

				.deleteControl (
					deleteControl)

			);

		}

	}

	private
	Map <String, ObjectListTabSpec <RecordType>> defaultListTabSpecs (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"defaultListTabSpecs");

		) {

			if (consoleHelper.deletedExists ()) {

				return ImmutableMap.<String, ObjectListTabSpec <RecordType>>
					builder ()

					.put (
						"all",
						objectListTabSpecProvider.provide (
							taskLogger)

						.name (
							"all")

						.label (
							stringFormat (
								"All %s",
								consoleHelper.shortNamePlural ()))

						.addCriteria (
							whereNotDeletedCriteriaSpecProvider.provide (
								taskLogger))

					)

					.put (
						"deleted",
						objectListTabSpecProvider.provide (
							taskLogger)

						.name (
							"deleted")

						.label (
							"Deleted")

						.addCriteria (
							whereDeletedCriteriaSpecProvider.provide (
								taskLogger))

						.addCriteria (
							whereICanManageCriteriaSpecProvider.provide (
								taskLogger))

					)

					.build ();

			} else {

				return ImmutableMap.<String, ObjectListTabSpec <RecordType>>
					builder ()

					.put (
						"all",
						objectListTabSpecProvider.provide (
							taskLogger)

							.name (
								"all")

							.label (
								stringFormat (
									"All %s",
									consoleHelper.shortNamePlural ()))

					)

					.build ()

				;

			}

		}

	}

}
