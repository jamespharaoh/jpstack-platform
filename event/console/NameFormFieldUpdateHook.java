package wbs.platform.event.console;

import static wbs.utils.string.CodeUtils.simplifyToCodeRequired;
import static wbs.utils.string.StringUtils.stringNotEqualSafe;

import com.google.common.base.Optional;

import lombok.NonNull;

import wbs.console.forms.types.FormFieldUpdateHook;
import wbs.console.forms.types.FormUpdateResult;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;

import wbs.platform.event.logic.EventLogic;
import wbs.platform.user.console.UserConsoleLogic;

@PrototypeComponent ("createNameEventFormFieldUpdateHook")
public
class NameFormFieldUpdateHook
	implements FormFieldUpdateHook <Record <?>, String, String> {

	// singeton dependencies

	@SingletonDependency
	EventLogic eventLogic;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	// implementation

	@Override
	public
	void onUpdate (
			@NonNull Transaction parentTransaction,
			@NonNull ConsoleRequestContext requestContext,
			@NonNull FormUpdateResult <String, String> updateResult,
			@NonNull Record <?> container,
			@NonNull Record <?> linkObject,
			@NonNull Optional <Object> objectRef,
			@NonNull Optional <String> objectType) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"onUpdate");

		) {

			// don't create event on initial creation

			if (! updateResult.oldNativeValue ().isPresent ()) {
				return;
			}

			// derive codes

			String oldCode =
				simplifyToCodeRequired (
					updateResult.oldNativeValue ().get ());

			String newCode =
				simplifyToCodeRequired (
					updateResult.newNativeValue ().get ());

			boolean codeChanged =
				stringNotEqualSafe (
					oldCode,
					newCode);

			// create an event

			if (objectRef.isPresent ()) {

				eventLogic.createEvent (
					transaction,
					"object_name_changed_in",
					userConsoleLogic.userRequired (
						transaction),
					objectRef.get (),
					objectType.get (),
					linkObject,
					updateResult.oldNativeValue ().get (),
					updateResult.newNativeValue ().get ());

				if (codeChanged) {

					eventLogic.createEvent (
						transaction,
						"object_code_changed_in",
						userConsoleLogic.userRequired (
							transaction),
						objectRef.get (),
						objectType.get (),
						linkObject,
						oldCode,
						newCode);

				}

			} else {

				eventLogic.createEvent (
					transaction,
					"object_name_changed",
					userConsoleLogic.userRequired (
						transaction),
					linkObject,
					updateResult.oldNativeValue ().get (),
					updateResult.newNativeValue ().get ());

				if (codeChanged) {

					eventLogic.createEvent (
						transaction,
						"object_code_changed",
						userConsoleLogic.userRequired (
							transaction),
						linkObject,
						oldCode,
						newCode);

				}

			}

		}

	}

}
