package wbs.smsapps.forwarder.api;

import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import lombok.NonNull;

import wbs.api.mvc.ApiAction;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.TaskLogger;

import wbs.platform.text.web.TextResponder;

import wbs.smsapps.forwarder.logic.ForwarderNotFoundException;
import wbs.smsapps.forwarder.logic.IncorrectPasswordException;
import wbs.smsapps.forwarder.logic.ReportableException;
import wbs.smsapps.forwarder.model.ForwarderRec;

import wbs.web.context.RequestContext;
import wbs.web.responder.Responder;

@PrototypeComponent ("forwarderInAction")
public
class ForwarderInAction
	extends ApiAction {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@SingletonDependency
	ForwarderApiLogic forwarderApiLogic;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	RequestContext requestContext;

	// prototype dependencies

	@PrototypeDependency
	Provider <TextResponder> textResponderProvider;

	// implementation

	@Override
	protected
	Responder goApi (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"goApi");

		) {

			String slice =
				requestContext.parameterRequired (
					"slice");

			String code =
				requestContext.parameterRequired (
					"code");

			String password =
				requestContext.parameterRequired (
					"password");

			String action =
				requestContext.parameterRequired (
					"action");

			ForwarderRec forwarder;

			try {

				try {

					forwarder =
						forwarderApiLogic.lookupForwarder (
							transaction,
							requestContext,
							slice,
							code,
							password);

				} catch (ForwarderNotFoundException exception) {

					throw new ReportableException (
						"Forwarder not found: " + code);

				} catch (IncorrectPasswordException exception) {

					throw new ReportableException (
						"Password incorrect");

				}

				transaction.commit ();

				if (action.equalsIgnoreCase ("get")) {

					return forwarderApiLogic.controlActionGet (
						transaction,
						requestContext,
						forwarder);

				} else if (action.equalsIgnoreCase ("borrow")) {

					return forwarderApiLogic.controlActionBorrow (
						transaction,
						requestContext,
						forwarder);

				} else if (action.equalsIgnoreCase ("unqueue")) {

					return forwarderApiLogic.controlActionUnqueue (
						transaction,
						requestContext,
						forwarder);

				} else {

					throw new ReportableException (
						"Unknown action: " + action);

				}

			} catch (ReportableException exception) {

				transaction.errorFormatException (
					exception,
					"Error doing 'in'");

				for (
					Map.Entry <String, List <String>> entry
						: requestContext.parameterMap ().entrySet ()
				) {

					String name =
						entry.getKey ();

					List <String> values =
						entry.getValue ();

					for (
						String value
							: values
					) {

						transaction.errorFormat (
							"Param %s: %s",
							 name,
							 value);

					}

				}

				return textResponderProvider.get ()
					.text ("ERROR\n" + exception.getMessage () + "\n");

			}

		}

	}

}
