package wbs.platform.servlet;

import static wbs.utils.collection.CollectionUtils.listFirstElementRequired;
import static wbs.utils.etc.NullUtils.ifNull;
import static wbs.utils.etc.NullUtils.isNotNull;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.NumberUtils.parseIntegerRequired;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.string.StringUtils.keyEqualsString;
import static wbs.utils.string.StringUtils.stringEqualSafe;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.string.StringUtils.stringSplitComma;
import static wbs.utils.string.StringUtils.stringSplitSimple;
import static wbs.utils.time.TimeUtils.earlierThan;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.ImmutableList;

import lombok.NonNull;

import org.joda.time.Duration;
import org.joda.time.Instant;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.BootstrapComponentManager;
import wbs.framework.component.manager.ComponentManager;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.component.tools.ComponentManagerBuilder;
import wbs.framework.component.tools.ThreadLocalProxyComponentFactory;
import wbs.framework.logging.Log4jLogTargetFactory;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.LoggingLogic;
import wbs.framework.logging.LoggingLogicImplementation;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;
import wbs.framework.servlet.ComponentFilterProxy;

import wbs.utils.random.RandomLogic;

import info.faljse.SDNotify.SDNotify;
import wbs.web.context.RequestContextImplementation;

public
class WbsServletListener
	implements
		ServletContextListener,
		ServletRequestListener {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	LoggingLogic loggingLogic;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <ComponentManagerBuilder> componentManagerBuilderProvider;

	// state

	ServletContext servletContext;

	ComponentManager componentManager;

	Thread watchdogThread;

	RandomLogic randomLogic;

	// public implementation

	@Override
	public
	void contextInitialized (
			@NonNull ServletContextEvent event) {

		LoggingLogic loggingLogic =
			new LoggingLogicImplementation (
				false,
				ImmutableList.of (
					new Log4jLogTargetFactory ()));

		try (

			BootstrapComponentManager bootstrapComponentManager =
				new BootstrapComponentManager (
					loggingLogic);

			OwnedTaskLogger taskLogger =
				bootstrapComponentManager.bootstrapTaskLogger (
					this);

		) {

			try {

				bootstrapComponentManager.registerStandardClasses (
					taskLogger);

				bootstrapComponentManager.bootstrapComponent (
					taskLogger,
					this);

				bootstrapComponentManager.registerPluginBootstrapComponents (
					taskLogger,
					stringSplitComma (
						event.getServletContext ().getInitParameter (
							"layerNames")));

				registerWebComponents (
					taskLogger,
					bootstrapComponentManager);

				contextInitializedReal (
					taskLogger,
					event);

			} catch (Throwable exception) {

				taskLogger.errorFormatException (
					exception,
					"Failed to initialise component manager, shutting down");

				shutdown ();

			}

		}

	}

	@Override
	public
	void contextDestroyed (
			@NonNull ServletContextEvent event) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.createTaskLogger (
					"contextDestroyed");

		) {

			// update systemd status

			SDNotify.sendStatus (
				"Shutting down");

			// clean up components

			if (
				isNotNull (
					componentManager)
			) {

				taskLogger.noticeFormat (
					"Closing component manager");

				componentManager.close ();

			}

			// systemd integration

			if (
				isNotNull (
					watchdogThread)
			) {

				taskLogger.noticeFormat (
					"Stopping systemd watchdog");

				watchdogThread.interrupt ();

				try {

					watchdogThread.join ();

				} catch (InterruptedException interruptedException) {

					throw new RuntimeException (
						interruptedException);

				}

			}

		}

	}

	@Override
	public
	void requestInitialized (
			@NonNull ServletRequestEvent event) {

		HttpServletRequest request =
			genericCastUnchecked (
				event.getServletRequest ());

		boolean debug =
			Arrays.stream (
				ifNull (
					request.getCookies (),
					new Cookie [] {}
				)
			).anyMatch (
				cookie ->
					stringEqualSafe (
						cookie.getName (),
						"wbs-debug")
					&& stringEqualSafe (
						cookie.getValue (),
						"yes"));

		TaskLogger.implicitArgument.store (
			logContext.createTaskLogger (
				"requestInitialized",
				ImmutableList.of (
					keyEqualsString (
						"method",
						request.getMethod ()),
					keyEqualsString (
						"requestUri",
						request.getRequestURI ())),
				optionalOf (
					debug)));

		TaskLogger.implicitArgument.retrieveAndInvokeVoid (
			taskLogger ->
				requestInitializedReal (
					taskLogger,
					event));

	}

	@Override
	public
	void requestDestroyed (
			@NonNull ServletRequestEvent event) {

		try {

			TaskLogger.implicitArgument.retrieveAndInvokeVoid (
				taskLogger ->
					requestDestroyedReal (
						taskLogger,
						event));

		} finally {

			TaskLogger.implicitArgument.retrieve ().close ();

		}

	}

	// private implementation

	private
	void registerWebComponents (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull BootstrapComponentManager bootstrapComponentManager) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"registerWebComponents");

		) {

			bootstrapComponentManager.registerClass (
				taskLogger,
				ComponentFilterProxy.class,
				ComponentFilterProxy.class);

		}

	}

	private
	void contextInitializedReal (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ServletContextEvent event) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"contextInitializedReal");

		) {

			taskLogger.noticeFormat (
				"Initialising components");

			SDNotify.sendStatus (
				"Initialising components");

			servletContext =
				event.getServletContext ();

			String primaryProjectName =
				servletContext.getInitParameter (
					"primaryProjectName");

			String primaryProjectPackageName =
				servletContext.getInitParameter (
					"primaryProjectPackageName");

			String componentDefinitionOutputPath =
				servletContext.getInitParameter (
					"componentDefinitionOutputPath");

			List <String> layerNames =
				stringSplitComma (
					servletContext.getInitParameter (
						"layerNames"));

			componentManager =
				componentManagerBuilderProvider.provide (
					taskLogger)

				.primaryProjectName (
					primaryProjectName)

				.primaryProjectPackageName (
					primaryProjectPackageName)

				.layerNames (
					layerNames)

				.configNames (
					Collections.emptyList ())

				.outputPath (
					componentDefinitionOutputPath)

				.addSingletonComponent (
					"servletContext",
					ServletContext.class,
					event.getServletContext ())

				.build (
					taskLogger);

			servletContext.setAttribute (
				"wbs-application-context",
				componentManager);

			randomLogic =
				componentManager.getComponentRequired (
					taskLogger,
					"randomLogic",
					RandomLogic.class);

			// systemd integration

			SDNotify.sendNotify ();

			watchdogThread =
				new Thread (
					this::watchdogThread);

			watchdogThread.start ();

			SDNotify.sendStatus (
				"Running");

		}

	}

	private
	void requestInitializedReal (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ServletRequestEvent event) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"requestInitializedReal");

		) {

			boolean setServletContext = false;
			boolean setServletRequest = false;

			List <String> setRequestComponentNames =
				new ArrayList<> ();

			boolean success = false;

			try {

				RequestContextImplementation.servletContextThreadLocal.set (
					servletContext);

				setServletContext = true;

				RequestContextImplementation.servletRequestThreadLocal.set (
					(HttpServletRequest)
					event.getServletRequest ());

				setServletRequest = true;

				List <Object> targetComponents =
					new ArrayList<> ();

				for (
					String requestComponentName
						: componentManager.requestComponentNames ()
				) {

					ThreadLocalProxyComponentFactory.Control control =
						genericCastUnchecked (
							componentManager.getComponentRequired (
								taskLogger,
								requestComponentName,
								Object.class));

					String targetComponentName =
						stringFormat (
							"%sTarget",
							requestComponentName);

					Object targetComponent =
						componentManager.getComponentRequired (
							taskLogger,
							targetComponentName,
							Object.class,
							false);

					control.threadLocalProxySet (
						targetComponent);

					setRequestComponentNames.add (
						requestComponentName);

					targetComponents.add (
						targetComponent);

				}

				targetComponents.forEach (
					targetComponent ->
						componentManager.initializeComponent (
							taskLogger,
							targetComponent));

				success = true;

			} finally {

				if (! success) {

					for (
						String requestComponentName
							: setRequestComponentNames
					) {

						ThreadLocalProxyComponentFactory.Control control =
							genericCastUnchecked (
								componentManager.getComponentRequired (
									taskLogger,
									requestComponentName,
									Object.class));

						control.threadLocalProxyReset ();

					}

					if (setServletRequest) {

						RequestContextImplementation
							.servletRequestThreadLocal
							.remove ();

					}

					if (setServletContext) {

						RequestContextImplementation
							.servletContextThreadLocal
							.remove ();

					}

				}

			}

		}

	}

	private
	void requestDestroyedReal (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ServletRequestEvent event) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"requestDestroyedReal");

		) {

			for (
				String requestComponentName
					: componentManager.requestComponentNames ()
			) {

				ThreadLocalProxyComponentFactory.Control control =
					genericCastUnchecked (
						componentManager.getComponentRequired (
							taskLogger,
							requestComponentName,
							Object.class));

				control.threadLocalProxyReset ();

			}

			RequestContextImplementation
				.servletRequestThreadLocal
				.remove ();

		}

	}

	private
	void watchdogThread () {

		Instant restartTime =
			Instant.now ().plus (
				randomLogic.randomDuration (
				restartFrequency,
				restartFrequencyDeviation));

		while (
			earlierThan (
				Instant.now (),
				restartTime)
		) {

			try {

				Thread.sleep (1000);

			} catch (InterruptedException exception) {

				return;

			}

			SDNotify.sendWatchdog ();

		}

		shutdown ();

	}

	private
	void shutdown () {

		try (

			OwnedTaskLogger taskLogger =
				logContext.createTaskLogger (
					"shutdown");

		) {

			// send sigterm

			taskLogger.noticeFormat (
				"Automatic restart");

			String processName =
				ManagementFactory.getRuntimeMXBean ().getName ();

			List <String> processNameParts =
				stringSplitSimple (
					"@",
					processName);

			Long processId =
				parseIntegerRequired (
					listFirstElementRequired (
						processNameParts));

			try {

				Runtime.getRuntime ().exec (
					stringFormat (
						"kill -SIGTERM %s",
						integerToDecimalString (
							processId)));

			} catch (IOException ioException) {

				taskLogger.noticeFormatException (
					ioException,
					"Failed to kill -SIGTERM, calling System.exit (1)");

				System.exit (1);

			}

		}

	}

	// constants

	public final static
	Duration restartFrequency =
		Duration.standardHours (
			4l);

	public final static
	Duration restartFrequencyDeviation =
		Duration.standardMinutes (
			30l);

}
