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

import wbs.platform.deployment.model.DaemonDeploymentRec;
import wbs.platform.event.logic.EventLogic;
import wbs.platform.user.console.UserConsoleLogic;

import wbs.utils.string.FormatWriter;

import wbs.web.responder.WebResponder;

@PrototypeComponent ("daemonDeploymentRestartFormActionHelper")
public
class DaemonDeploymentRestartFormActionHelper
	implements ConsoleFormActionHelper <Object, Object> {

	// singleton dependencies

	@SingletonDependency
	DaemonDeploymentConsoleHelper daemonDeploymentHelper;

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

			DaemonDeploymentRec daemonDeployment =
				daemonDeploymentHelper.findFromContextRequired (
					transaction);

			boolean show =
				userPrivChecker.canRecursive (
					transaction,
					daemonDeployment,
					"restart");

			boolean enable =
				! daemonDeployment.getRestart ();

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

			DaemonDeploymentRec daemonDeployment =
				daemonDeploymentHelper.findFromContextRequired (
					transaction);

			if (daemonDeployment.getRestart ()) {

				htmlParagraphWriteFormat (
					formatWriter,
					"There is already a restart scheduled for this daemon ",
					"deployment, but it has not yet taken place. If this ",
					"message persists, please contact support.");

			} else {

				htmlParagraphWriteFormat (
					formatWriter,
					"Trigger a restart for this daemon deployment");

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

			DaemonDeploymentRec daemonDeployment =
				daemonDeploymentHelper.findFromContextRequired (
					transaction);

			// check state

			if (daemonDeployment.getRestart ()) {

				requestContext.addNotice (
					"Restart already scheduled for this daemon deployment");

				return optionalAbsent ();

			}

			// perform update

			daemonDeployment

				.setRestart (
					true);

			eventLogic.createEvent (
				transaction,
				"daemon_deployment_restarted",
				userConsoleLogic.userRequired (
					transaction),
				daemonDeployment);

			// commit and return

			transaction.commit ();

			requestContext.addNotice (
				"Daemon deployment restart triggered");

			return optionalAbsent ();

		}

	}

}
