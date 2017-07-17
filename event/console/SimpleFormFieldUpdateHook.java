package wbs.platform.event.console;

import static wbs.utils.etc.OptionalUtils.optionalOr;

import com.google.common.base.Optional;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.forms.types.FormFieldUpdateHook;
import wbs.console.forms.types.FormUpdateResult;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.CommonRecord;
import wbs.framework.entity.record.EphemeralRecord;
import wbs.framework.entity.record.EventRecord;
import wbs.framework.entity.record.MajorRecord;
import wbs.framework.entity.record.MinorRecord;
import wbs.framework.entity.record.Record;
import wbs.framework.entity.record.RootRecord;
import wbs.framework.logging.LogContext;

import wbs.platform.event.logic.EventLogic;
import wbs.platform.user.console.UserConsoleLogic;

@Accessors (fluent = true)
@PrototypeComponent ("createUpdateEventFormFieldUpdateHook")
public
class SimpleFormFieldUpdateHook <Container extends Record <?>, Generic, Native>
	implements FormFieldUpdateHook <Container, Generic, Native> {

	// singleton dependencies

	@SingletonDependency
	EventLogic eventLogic;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	// properties

	@Getter @Setter
	String fieldName;

	// implementation

	@Override
	public
	void onUpdate (
			@NonNull Transaction parentTransaction,
			@NonNull ConsoleRequestContext requestContext,
			@NonNull FormUpdateResult <Generic, Native> updateResult,
			@NonNull Container container,
			@NonNull Record <?> linkObject,
			@NonNull Optional <Object> objectRef,
			@NonNull Optional <String> objectType) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"onUpdate");

		) {

			// determine actual field name

			String updatedFieldName =
				optionalOr (
					updateResult.updatedFieldName (),
					fieldName);

			// determine if it's an admin event

			// TODO this needs to be much better

			String adminPrefix;

			if (
				container instanceof RootRecord
				|| container instanceof MajorRecord
				|| container instanceof MinorRecord
				|| container instanceof EphemeralRecord
			) {

				adminPrefix = "admin_";

			} else if (
				container instanceof CommonRecord
				|| container instanceof EventRecord
			) {

				adminPrefix = "";

			} else {

				throw new RuntimeException ();

			}

			// create an event

			if (objectRef.isPresent ()) {

				if (updateResult.newNativeValue ().isPresent ()) {

					eventLogic.createEvent (
						transaction,
						adminPrefix + "object_field_updated_in",
						userConsoleLogic.userRequired (
							transaction),
						updatedFieldName,
						objectRef.get (),
						objectType.get (),
						linkObject,
						updateResult.newNativeValue ().get ());

				} else {

					eventLogic.createEvent (
						transaction,
						adminPrefix + "object_field_nulled_in",
						userConsoleLogic.userRequired (
							transaction),
						updatedFieldName,
						objectRef.get (),
						objectType.get (),
						linkObject);

				}

			} else {

				if (updateResult.newNativeValue ().isPresent ()) {

					eventLogic.createEvent (
						transaction,
						adminPrefix + "object_field_updated",
						userConsoleLogic.userRequired (
							transaction),
						updatedFieldName,
						linkObject,
						updateResult.newNativeValue ().get ());

				} else {

					eventLogic.createEvent (
						transaction,
						adminPrefix + "object_field_nulled",
						userConsoleLogic.userRequired (
							transaction),
						updatedFieldName,
						linkObject);

				}

			}

		}

	}

}
