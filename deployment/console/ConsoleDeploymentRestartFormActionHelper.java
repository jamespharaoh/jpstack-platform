package wbs.platform.deployment.console;

import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphWriteFormat;

import com.google.common.base.Optional;

import lombok.NonNull;

import wbs.console.formaction.ConsoleFormActionHelper;
import wbs.console.priv.UserPrivChecker;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.platform.deployment.model.ConsoleDeploymentRec;
import wbs.platform.event.logic.EventLogic;
import wbs.platform.user.console.UserConsoleLogic;

import wbs.utils.string.FormatWriter;

import wbs.web.responder.WebResponder;

@PrototypeComponent ("consoleDeploymentRestartFormActionHelper")
public
class ConsoleDeploymentRestartFormActionHelper
	implements ConsoleFormActionHelper <Object, Object> {

	// singleton dependencies

	@SingletonDependency
	ConsoleDeploymentConsoleHelper consoleDeploymentHelper;

	@SingletonDependency
	EventLogic eventLogic;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	@SingletonDependency
	UserPrivChecker userPrivChecker;

	// public implementation

	@Override
	public
	Permissions canBePerformed (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"canBePerformed");

		) {

			ConsoleDeploymentRec consoleDeployment =
				consoleDeploymentHelper.findFromContextRequired (
					transaction);

			boolean show =
				userPrivChecker.canRecursive (
					transaction,
					consoleDeployment,
					"restart");

			boolean enable =
				! consoleDeployment.getRestart ();

			return new Permissions ()
				.canView (show)
				.canPerform (enable);

		}

	}

	@Override
	public
	void writePreamble (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter,
			@NonNull Boolean submit) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"writePreamble");

		) {

			ConsoleDeploymentRec consoleDeployment =
				consoleDeploymentHelper.findFromContextRequired (
					transaction);

			if (consoleDeployment.getRestart ()) {

				htmlParagraphWriteFormat (
					formatWriter,
					"There is already a restart scheduled for this console ",
					"deployment, but it has not yet taken place. If this ",
					"message persists, please contact support.");

			} else {

				htmlParagraphWriteFormat (
					formatWriter,
					"Trigger a restart for this console deployment");

			}

		}

	}

	@Override
	public
	Optional <WebResponder> processFormSubmission (
			@NonNull Transaction parentTransaction,
			@NonNull Object formState) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"processFormSubmission");

		) {

			// load data

			ConsoleDeploymentRec consoleDeployment =
				consoleDeploymentHelper.findFromContextRequired (
					transaction);

			// check state

			if (consoleDeployment.getRestart ()) {

				requestContext.addNotice (
					"Restart already scheduled for this console deployment");

				return optionalAbsent ();

			}

			// perform update

			consoleDeployment

				.setRestart (
					true);

			eventLogic.createEvent (
				transaction,
				"console_deployment_restarted",
				userConsoleLogic.userRequired (
					transaction),
				consoleDeployment);

			// commit and return

			transaction.commit ();

			requestContext.addNotice (
				"Console deployment restart triggered");

			return optionalAbsent ();

		}

	}

}
