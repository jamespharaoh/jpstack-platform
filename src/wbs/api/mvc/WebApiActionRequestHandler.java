package wbs.api.mvc;

import static wbs.utils.etc.OptionalUtils.optionalAbsent;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.exception.ExceptionLogger;
import wbs.framework.exception.GenericExceptionResolution;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.web.context.RequestContext;
import wbs.web.handler.RequestHandler;
import wbs.web.responder.Responder;

@Accessors (fluent = true)
@PrototypeComponent ("webApiActionRequestHandler")
public
class WebApiActionRequestHandler
	implements RequestHandler {

	// singleton dependencies

	@SingletonDependency
	ExceptionLogger exceptionLogger;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	RequestContext requestContext;

	// properties

	@Getter @Setter
	WebApiAction action;

	// public implementation

	@Override
	public
	void handle (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"handle");

		) {

			try {

				Responder responder =
					action.go (
						taskLogger);

				if (responder != null) {

					responder.execute (
						parentTaskLogger);

					return;

				}

			} catch (Exception exception) {

				exceptionLogger.logThrowable (
					taskLogger,
					"webapi",
					requestContext.requestUri (),
					exception,
					optionalAbsent (),
					GenericExceptionResolution.ignoreWithThirdPartyWarning);

			}

			Responder responder =
				action.makeFallbackResponder ()
					.get ();

			responder.execute (
				taskLogger);

		}

	}

}