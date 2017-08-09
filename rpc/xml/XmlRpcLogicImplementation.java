package wbs.platform.rpc.xml;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.web.context.RequestContext;

@SingletonComponent ("xmlRpcLogic")
public
class XmlRpcLogicImplementation
	implements XmlRpcLogic {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// public implementation

	@Override
	public
	void xmlRpcHeaders (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull RequestContext requestContext) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"xmlApiHeaders");

		) {

			requestContext.setHeader (
				"Access-Control-Allow-Origin",
				"*");

			requestContext.setHeader (
				"Access-Control-Allow-Methods",
				"POST, OPTIONS");

			requestContext.setHeader (
				"Access-Control-Allow-Headers",
				"CONTENT-TYPE, X-REQUESTED-WITH");

			requestContext.setHeader (
				"Access-Control-Max-Age",
				"86400");

			requestContext.setHeader (
				"Allow",
				"POST, OPTIONS");

		}

	}

}
