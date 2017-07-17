package wbs.platform.event.logic;

import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIsPresent;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.fixtures.FixturesLogic;
import wbs.framework.logging.LogContext;
import wbs.framework.object.ObjectHelper;

import wbs.platform.text.model.TextObjectHelper;
import wbs.platform.text.model.TextRec;

@SingletonComponent ("eventFixtureLogic")
public
class EventFixtureLogicImplementation
	implements EventFixtureLogic {

	// singleton dependencies

	@SingletonDependency
	EventLogic eventLogic;

	@SingletonDependency
	FixturesLogic fixturesLogic;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	TextObjectHelper textHelper;

	// public implementation

	@Override
	public
	void createEvents (
			@NonNull Transaction parentTransaction,
			@NonNull String fixtureProviderName,
			@NonNull Record <?> parent,
			@NonNull Record <?> object,
			@NonNull Map <String, Object> fields) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createEvents");

		) {

			eventLogic.createEvent (
				transaction,
				"admin_object_created_by_fixture",
				fixtureProviderName,
				object,
				parent);

			fields.forEach (
				(fieldName, fieldValue) -> {

				if (fieldValue instanceof Optional) {

					Optional <?> fieldValueOptional =
						(Optional <?>) fieldValue;

					if (
						optionalIsPresent (
							fieldValueOptional)
					) {

						eventLogic.createEvent (
							transaction,
							"admin_object_field_updated_by_fixture",
							fixtureProviderName,
							fieldName,
							object,
							optionalGetRequired (
								fieldValueOptional));

					}

				} else {

					eventLogic.createEvent (
						transaction,
						"admin_object_field_updated_by_fixture",
						fixtureProviderName,
						fieldName,
						object,
						fieldValue);

				}

			});

		}

	}

	@Override
	public <Type extends Record <Type>>
	Type createRecordAndEvents (
			@NonNull Transaction parentTransaction,
			@NonNull String fixtureProviderName,
			@NonNull ObjectHelper <Type> objectHelper,
			@NonNull Record <?> parent,
			@NonNull Map <String, String> unresolvedParams,
			@NonNull Set <String> ignoreParams) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createRecordAndEvents");

		) {

			Map <Class <?>, Function <String, ?>> recordLookups =
				ImmutableMap.<Class <?>, Function <String, ?>> builder ()

				.put (
					TextRec.class,
					text ->
						textHelper.findOrCreate (
							transaction,
							text))

				.build ()

			;

			Map <String, Object> resolvedParams =
				fixturesLogic.resolveParams (
					transaction,
					objectHelper,
					unresolvedParams,
					ignoreParams,
					recordLookups);

			Type object =
				fixturesLogic.createRecord (
					transaction,
					objectHelper,
					parent,
					resolvedParams);

			createEvents (
				transaction,
				fixtureProviderName,
				parent,
				object,
				resolvedParams);

			return object;

		}

	}

}
