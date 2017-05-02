package wbs.console.formaction;

import static wbs.utils.collection.IterableUtils.iterableFindExactlyOneRequired;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.string.StringUtils.stringEqualSafe;

import java.util.List;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.action.ConsoleAction;
import wbs.console.forms.FormFieldLogic;
import wbs.console.forms.FormFieldLogic.UpdateResultSet;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.TaskLogger;

import wbs.web.responder.Responder;

@PrototypeComponent ("contextFormActionsAction")
@Accessors (fluent = true)
public
class ConsoleFormActionsAction
	extends ConsoleAction {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@SingletonDependency
	FormFieldLogic formFieldLogic;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	// properties

	@Getter @Setter
	List <ConsoleFormAction <?, ?>> formActions;

	@Getter @Setter
	String responderName;

	// implementation

	@Override
	protected
	Responder backupResponder (
			@NonNull TaskLogger parentTaskLogger) {

		return responder (
			responderName);

	}

	@Override
	protected
	Responder goReal (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"goReal");

		) {

			String formName =
				requestContext.formRequired (
					"form.name");

			ConsoleFormAction <?, ?> formAction =
				iterableFindExactlyOneRequired (
					candidateFormAction ->
						stringEqualSafe (
							candidateFormAction.name (),
							formName),
					formActions);

			Object formState =
				formAction.helper ().constructFormState (
					transaction);

			formAction.helper ().updatePassiveFormState (
				transaction,
				genericCastUnchecked (
					formState));

			UpdateResultSet updateResultSet =
				formFieldLogic.update (
					transaction,
					requestContext,
					formAction.formFields (),
					formState,
					ImmutableMap.of (),
					formName);

			if (updateResultSet.errorCount () > 0) {
				return null;
			}

			Optional <Responder> responder =
				formAction.helper ().processFormSubmission (
					transaction,
					genericCastUnchecked (
						formState));

			return responder.orNull ();

		}

	}

}
