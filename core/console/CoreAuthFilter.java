package wbs.platform.core.console;

import static wbs.utils.string.StringUtils.stringInSafe;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import lombok.NonNull;

import wbs.console.misc.JqueryScriptRef;
import wbs.console.module.ConsoleManager;
import wbs.console.priv.UserPrivChecker;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.annotations.WeakSingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.Database;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;
import wbs.framework.servlet.ComponentFilterChain;
import wbs.framework.servlet.FilterComponent;

import wbs.platform.user.console.UserConsoleLogic;
import wbs.platform.user.console.UserSessionLogic;

import wbs.web.responder.WebResponder;

@SingletonComponent ("coreAuthFilter")
public
class CoreAuthFilter
	implements FilterComponent {

	// singleton dependencies

	@WeakSingletonDependency
	ConsoleManager consoleManager;

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	@SingletonDependency
	UserPrivChecker userPrivChecker;

	@SingletonDependency
	UserSessionLogic userSessionLogic;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <CoreLogonResponder> logonResponderProvider;

	// implementation

	@Override
	public
	void doFilter (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ServletRequest request,
			@NonNull ServletResponse response,
			@NonNull ComponentFilterChain chain) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"doFilter");

		) {

			String path =
				requestContext.servletPath ();

			// check the user is ok

			taskLogger.debugFormat (
				"Verify user session");

			boolean userOk =
				userSessionVerify (
					taskLogger,
					requestContext);

			if (userOk) {

				taskLogger.debugFormat (
					"User session verified");

				// and show the page

				chain.doFilter (
					taskLogger,
					request,
					response);

			} else {

				taskLogger.debugFormat (
					"User session not verified");

				// user not ok, either....

				if (path.equals ("/")) {

					// root path, either show logon page or process logon request

					if (requestContext.post ()) {

						chain.doFilter (
							taskLogger,
							request,
							response);

					} else {

						WebResponder logonResponder =
							logonResponderProvider.provide (
								taskLogger);

						logonResponder.execute (
							taskLogger);

					}

				} else if (

					stringInSafe (
						path,
						"/style/basic.css",
						"/favicon.ico",
						"/status.update",
						"/js/login.js",
						JqueryScriptRef.path)

				) {

					// these paths are available before login

					chain.doFilter (
						taskLogger,
						request,
						response);

				} else {

					// unauthorised access, redirect to the logon page

					requestContext.sendRedirect (
						requestContext.resolveApplicationUrl (
							"/"));

				}

			}

		}

	}

	// private implementation

	private
	boolean userSessionVerify (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ConsoleRequestContext requestContext) {

		try (

			OwnedTransaction transaction =
				database.beginReadOnly (
					logContext,
					parentTaskLogger,
					"userSessionVerify");
		) {

			return userSessionLogic.userSessionVerify (
				transaction,
				requestContext);

		}

	}

}
