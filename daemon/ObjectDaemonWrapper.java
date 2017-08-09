package wbs.platform.daemon;

import static wbs.utils.collection.CollectionUtils.emptyList;
import static wbs.utils.etc.Misc.doNothing;
import static wbs.utils.etc.Misc.sleepForDuration;
import static wbs.utils.etc.NumberUtils.notEqualToZero;
import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.string.StringUtils.pluralise;
import static wbs.utils.string.StringUtils.stringFormat;

import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.joda.time.Duration;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.LifecycleStart;
import wbs.framework.component.annotations.LifecycleStop;
import wbs.framework.component.annotations.NormalLifecycleSetup;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.exception.ExceptionLogger;
import wbs.framework.exception.GenericExceptionResolution;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;
import wbs.framework.object.ObjectManager;

import wbs.platform.background.logic.BackgroundLogic;
import wbs.platform.background.logic.BackgroundProcessHelper;

import wbs.utils.random.RandomLogic;
import wbs.utils.thread.ThreadManager;

@Accessors (fluent = true)
public
class ObjectDaemonWrapper <IdType> {

	// singleton dependencies

	@SingletonDependency
	private
	BackgroundLogic backgroundLogic;

	@SingletonDependency
	private
	ExceptionLogger exceptionLogger;

	@ClassSingletonDependency
	private
	LogContext logContext;

	@SingletonDependency
	private
	ObjectManager objectManager;

	@SingletonDependency
	private
	RandomLogic randomLogic;

	@SingletonDependency
	private
	ThreadManager threadManager;

	// properties

	@Getter @Setter
	ObjectDaemon <IdType> objectDaemon;

	// state

	private
	BackgroundProcessHelper backgroundProcessHelper;

	private
	Thread mainLoopThread;

	// life cycle

	@NormalLifecycleSetup
	public
	void setup (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"setup");

		) {

			backgroundProcessHelper =
				backgroundLogic.registerBackgroundProcess (
					taskLogger,
					objectDaemon.backgroundProcessName (),
					this);

		}

	}

	@LifecycleStart
	public
	void start (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"start");

		) {

			mainLoopThread =
				threadManager.makeThread (
					this::mainLoop,
					backgroundProcessHelper.threadName ());

			mainLoopThread.start ();

		}

	}

	@LifecycleStop
	public
	void stop (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"stop");

		) {

			mainLoopThread.interrupt ();

			mainLoopThread.join ();

		} catch (InterruptedException interruptedException) {

			doNothing ();

		}

	}

	// private implementation

	private
	void mainLoop () {

		Duration sleepDuration =
			backgroundProcessHelper.calculateFirstDelay ();

		while (! Thread.interrupted ()) {

			try {

				sleepForDuration (
					sleepDuration);

				runOnce ();

			} catch (InterruptedException exception) {

				return;

			}

			sleepDuration =
				backgroundProcessHelper.calculateSubsequentDelay ();

		}

	}

	private
	void runOnce ()
		throws InterruptedException {

		try (

			OwnedTaskLogger taskLogger =
				logContext.createTaskLogger (
					"runOnce",
					backgroundProcessHelper.debugEnabled ());

		) {

			if (
				! backgroundProcessHelper.setBackgroundProcessStart (
					taskLogger)
			) {
				return;
			}

			long numProcessed = 0;

			try {

				List <IdType> objectIds =
					getObjectIds (
						taskLogger);

				for (
					IdType objectId
						: objectIds
				) {

					if (Thread.interrupted ()) {
						break;
					}

					processObject (
						taskLogger,
						objectId);

					numProcessed ++;

				}

			} finally {

				if (
					notEqualToZero (
						numProcessed)
				) {

					taskLogger.noticeFormat (
						"Processed %s",
						pluralise (
							numProcessed,
							objectDaemon.itemNameSingular (),
							objectDaemon.itemNamePlural ()));

				}

				backgroundProcessHelper.setBackgroundProcessStop (
					taskLogger);

			}

		}

	}

	private
	List <IdType> getObjectIds (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"getObjectIds");

		) {

			try {

				return objectDaemon.findObjectIds (
					taskLogger);

			} catch (Exception exception) {

				String errorSummary =
					stringFormat (
						"Error running background process %s",
						objectDaemon.backgroundProcessName ());

				taskLogger.errorFormatException (
					exception,
					"%s",
					errorSummary);

				exceptionLogger.logThrowableWithSummary (
					taskLogger,
					"daemon",
					objectDaemon.backgroundProcessName (),
					errorSummary,
					exception,
					optionalAbsent (),
					GenericExceptionResolution.tryAgainLater);

				return emptyList ();

			}

		}

	}

	private
	void processObject (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull IdType objectId) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"processObject");

		) {

			try {

				objectDaemon.processObject (
					taskLogger,
					objectId);

			} catch (Exception exception) {

				String errorSummary =
					stringFormat (
						"Error running background process %s",
						objectDaemon.backgroundProcessName ());

				taskLogger.errorFormatException (
					exception,
					"%s",
					errorSummary);

				exceptionLogger.logThrowableWithSummary (
					taskLogger,
					"daemon",
					objectDaemon.backgroundProcessName (),
					errorSummary,
					exception,
					optionalAbsent (),
					GenericExceptionResolution.tryAgainLater);


			}

		}

	}

}
