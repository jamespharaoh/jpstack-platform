package wbs.platform.queue.console;

import static wbs.utils.collection.MapUtils.mapContainsKey;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.string.StringUtils.stringEqualSafe;
import static wbs.utils.string.StringUtils.stringFormat;

import java.util.HashMap;
import java.util.Map;

import lombok.NonNull;

import org.joda.time.Duration;

import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.NormalLifecycleSetup;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.annotations.StrongPrototypeDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.queue.metamodel.QueueTypeSpec;
import wbs.platform.queue.model.QueueItemRec;
import wbs.platform.queue.model.QueueRec;
import wbs.platform.queue.model.QueueSubjectRec;
import wbs.platform.queue.model.QueueTypeRec;

import wbs.web.responder.WebResponder;

@SingletonComponent ("queueManager")
public
class QueueManager {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	QueueConsoleLogic queueConsoleLogic;

	@StrongPrototypeDependency
	Map <String, QueueConsolePlugin> queueHelpersByBeanName;

	// state

	Map <String, QueueConsolePlugin> queueHelpers =
		new HashMap<> ();

	// lifecycle

	@NormalLifecycleSetup
	public
	void init (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"init");

		) {

			// initialise queuePageFactories by querying each factory

			for (
				Map.Entry <String, QueueConsolePlugin> entry
					: queueHelpersByBeanName.entrySet ()
			) {

				String beanName =
					entry.getKey ();

				QueueConsolePlugin queueHelper =
					entry.getValue ();

				for (
					String queueTypeCode
						: queueHelper.queueTypeCodes (
							taskLogger)
				) {

					if (
						mapContainsKey (
							queueHelpers,
							queueTypeCode)
					) {

						throw new RuntimeException (
							"Duplicated queue page factory: " + queueTypeCode);

					}

					queueHelpers.put (
						queueTypeCode,
						queueHelper);

					taskLogger.debugFormat (
						"Adding queue page factory %s from %s",
						queueTypeCode,
						beanName);

				}

			}

			taskLogger.noticeFormat (
				"Added %s queue page factories for %s queue types",
				integerToDecimalString (
					queueHelpersByBeanName.size ()),
				integerToDecimalString (
					queueHelpers.size ()));

		}

	}

	// implementation

	public
	WebResponder getItemResponder (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ConsoleRequestContext  requestContext,
			@NonNull QueueItemRec queueItem) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"getItemResponder");

		) {

			QueueSubjectRec queueSubject =
				queueItem.getQueueSubject ();

			QueueRec queue =
				queueSubject.getQueue ();

			QueueTypeRec queueType =
				queue.getQueueType ();

			String key =
				stringFormat (
					"%s.%s",
					queueType.getParentType ().getCode (),
					queueType.getCode ());

			QueueConsolePlugin queuePageFactory =
				queueHelpers.get (
					key);

			if (queuePageFactory == null) {

				throw new RuntimeException (
					stringFormat (
						"Queue page factory not found: %s",
						key));

			}

			return queuePageFactory.makeResponder (
				taskLogger,
				queueItem);

		}

	}

	public
	Duration getPreferredUserDelay (
			@NonNull Transaction parentTransaction,
			@NonNull QueueRec queue) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"getPreferredUserDelay");

		) {

			Record <?> queueParent =
				objectManager.getParentRequired (
					transaction,
					queue);

			QueueTypeSpec queueTypeSpec =
				queueConsoleLogic.queueTypeSpec (
					transaction,
					queue.getQueueType ());

			if (
				stringEqualSafe (
					queueTypeSpec.preferredUserDelay (),
					"0")
			) {
				return Duration.ZERO;
			}

			Long preferredUserDelay =
				genericCastUnchecked (
					objectManager.dereferenceRequired (
						transaction,
						queueParent,
						queueTypeSpec.preferredUserDelay ()));

			return Duration.standardSeconds (
				preferredUserDelay);

		}

	}

}
