package wbs.platform.object.verify.daemon;

import static wbs.utils.collection.CollectionUtils.collectionDoesNotHaveTwoElements;
import static wbs.utils.collection.CollectionUtils.collectionIsEmpty;
import static wbs.utils.collection.CollectionUtils.collectionIsNotEmpty;
import static wbs.utils.collection.CollectionUtils.collectionSize;
import static wbs.utils.collection.CollectionUtils.listFirstElementRequired;
import static wbs.utils.collection.CollectionUtils.listSecondElementRequired;
import static wbs.utils.collection.IterableUtils.iterableFindExactlyOneRequired;
import static wbs.utils.collection.MapUtils.mapItemForKeyRequired;
import static wbs.utils.etc.EnumUtils.enumNotEqualSafe;
import static wbs.utils.etc.NullUtils.isNotNull;
import static wbs.utils.etc.NullUtils.isNull;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.string.StringUtils.pluralise;
import static wbs.utils.string.StringUtils.stringSplitColon;

import java.util.List;

import lombok.NonNull;

import org.joda.time.Duration;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.meta.model.ModelMetaLoader;
import wbs.framework.entity.meta.model.RecordSpec;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.TaskLogger;
import wbs.framework.object.ObjectHelper;
import wbs.framework.object.ObjectManager;

import wbs.platform.daemon.ObjectDaemon;
import wbs.platform.object.verify.metamodel.ObjectVerificationSpec;
import wbs.platform.object.verify.model.ObjectVerificationObjectHelper;
import wbs.platform.object.verify.model.ObjectVerificationRec;
import wbs.platform.queue.logic.QueueLogic;
import wbs.platform.queue.model.QueueItemRec;
import wbs.platform.queue.model.QueueItemState;
import wbs.platform.queue.model.QueueObjectHelper;

import wbs.utils.data.Pair;
import wbs.utils.random.RandomLogic;

import shn.product.model.ShnProductObjectHelper;

@SingletonComponent ("shnProductVerifyObjectDaemonHelper")
public
class ObjectVerificationObjectDaemonHelper
	implements ObjectDaemon <Long> {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ModelMetaLoader modelMetaLoader;

	@SingletonDependency
	ObjectManager objectManager;

	@SingletonDependency
	ObjectVerificationObjectHelper objectVerificationHelper;

	@SingletonDependency
	ShnProductObjectHelper productHelper;

	@SingletonDependency
	QueueObjectHelper queueHelper;

	@SingletonDependency
	QueueLogic queueLogic;

	@SingletonDependency
	RandomLogic randomLogic;

	// details

	@Override
	public
	String backgroundProcessName () {
		return "object-verification.update";
	}

	@Override
	public
	String itemNameSingular () {
		return "object verification";
	}

	@Override
	public
	String itemNamePlural () {
		return "object verifications";
	}

	@Override
	public
	LogContext logContext () {
		return logContext;
	}

	// public implementation

	@Override
	public
	List <Long> findObjectIds (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTransaction transaction =
				database.beginReadOnly (
					logContext,
					parentTaskLogger,
					"findObjectIds");

		) {

			return objectVerificationHelper.findIdsPendingLimit (
				transaction,
				transaction.now (),
				16384l);

		}

	}

	@Override
	public
	void processObject (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull Long objectVerificationId) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"processObject");

		) {

			ObjectVerificationRec verification =
				objectVerificationHelper.findRequired (
					transaction,
					objectVerificationId);

			Record <?> object =
				objectManager.findObjectRequired (
					transaction,
					verification.getParentType ().getId (),
					verification.getParentId ());

			processObjectReal (
				transaction,
				verification,
				genericCastUnchecked (
					object));

			transaction.commit ();

		}

	}

	// private implementation

	private <Type extends Record <Type>>
	void processObjectReal (
			@NonNull Transaction parentTransaction,
			@NonNull ObjectVerificationRec verification,
			@NonNull Type object) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"processObjectReal");

		) {

			ObjectHelper <Type> objectHelper =
				objectManager.objectHelperForObjectRequired (
					object);

			RecordSpec recordSpec =
				mapItemForKeyRequired (
					modelMetaLoader.recordSpecs (),
					objectHelper.objectTypeHyphen ());

			ObjectVerificationSpec verificationSpec =
				iterableFindExactlyOneRequired (
					recordSpec.children (),
					ObjectVerificationSpec.class);

			List <Pair <Record <?>, String>> errors =
				objectHelper.hooks ().verifyData (
					transaction,
					object,
					verificationSpec.recurse ());

			if (

				collectionIsNotEmpty (
					errors)

			) {

				if (
					isNull (
						verification.getQueueItem ())
				) {

					// change from valid to invalid

					List <String> queueNameParts =
						stringSplitColon (
							verificationSpec.queueName ());

					if (
						collectionDoesNotHaveTwoElements (
							queueNameParts)
					) {
						throw new RuntimeException ();
					}

					Record <?> queueParent =
						genericCastUnchecked (
							objectManager.dereferenceRequired (
								transaction,
								object,
								listFirstElementRequired (
									queueNameParts)));

					QueueItemRec queueItem =
						queueLogic.createQueueItem (
							transaction,
							queueParent,
							listSecondElementRequired (
								queueNameParts),
							verification,
							verification,
							objectManager.objectPath (
								transaction,
								object),
							pluralise (
								collectionSize (
									errors),
								"validation error",
								"validation errors"));

					verification

						.setLastRun (
							transaction.now ())

						.setNextRun (
							transaction.now ().plus (
								randomLogic.randomDuration (
									Duration.standardMinutes (10l),
									Duration.standardMinutes (1l))))

						.setLastFailure (
							transaction.now ())

						.setValid (
							false)

						.setQueueItem (
							queueItem)

					;

				} else {

					// still invalid

					verification

						.setLastRun (
							transaction.now ())

						.setNextRun (
							transaction.now ().plus (
								randomLogic.randomDuration (
									Duration.standardMinutes (10l),
									Duration.standardMinutes (1l))))

						.setLastFailure (
							transaction.now ())

					;

					verification.getQueueItem ().setDetails (
						pluralise (
							collectionSize (
								errors),
							"validation error",
							"validation errors"));

				}

			} else if (

				collectionIsEmpty (
					errors)

				&& (

					isNull (
						verification.getValid ())

					|| ! verification.getValid ()

				)

			) {

				if (
					isNotNull (
						verification.getQueueItem ())
				) {

					// change from invalid to valid

					verification

						.setLastRun (
							transaction.now ())

						.setNextRun (
							transaction.now ().plus (
								randomLogic.randomDuration (
									Duration.standardMinutes (10l),
									Duration.standardMinutes (1l))))

						.setLastSuccess (
							transaction.now ())

						.setValid (
							true)

					;

					if (
						enumNotEqualSafe (
							verification.getQueueItem ().getState (),
							QueueItemState.claimed)
					) {

						queueLogic.cancelQueueItem (
							transaction,
							verification.getQueueItem ());

						verification.setQueueItem (
							null);

					}

				} else {

					// still valid

					verification

						.setLastRun (
							transaction.now ())

						.setNextRun (
							transaction.now ().plus (
								randomLogic.randomDuration (
									Duration.standardMinutes (10l),
									Duration.standardMinutes (1l))))

						.setLastSuccess (
							transaction.now ())

					;

				}

			}

		}

	}

}
