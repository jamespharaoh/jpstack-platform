package wbs.platform.object.verify.logic;

import static wbs.utils.collection.CollectionUtils.collectionDoesNotHaveTwoElements;
import static wbs.utils.collection.CollectionUtils.collectionIsNotEmpty;
import static wbs.utils.collection.CollectionUtils.collectionSize;
import static wbs.utils.collection.CollectionUtils.listFirstElementRequired;
import static wbs.utils.collection.CollectionUtils.listSecondElementRequired;
import static wbs.utils.collection.IterableUtils.iterableFindExactlyOneRequired;
import static wbs.utils.collection.MapUtils.mapItemForKeyRequired;
import static wbs.utils.etc.EnumUtils.enumNotEqualSafe;
import static wbs.utils.etc.NullUtils.isNotNull;
import static wbs.utils.etc.NullUtils.isNull;
import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIsPresent;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.string.StringUtils.pluralise;
import static wbs.utils.string.StringUtils.stringSplitColon;

import java.util.List;

import com.google.common.base.Optional;

import lombok.NonNull;

import org.joda.time.Duration;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.meta.model.ModelMetaLoader;
import wbs.framework.entity.meta.model.RecordSpec;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.object.ObjectHelper;
import wbs.framework.object.ObjectManager;

import wbs.platform.object.core.model.ObjectTypeObjectHelper;
import wbs.platform.object.core.model.ObjectTypeRec;
import wbs.platform.object.verify.metamodel.ObjectVerificationSpec;
import wbs.platform.object.verify.model.ObjectVerificationObjectHelper;
import wbs.platform.object.verify.model.ObjectVerificationRec;
import wbs.platform.queue.logic.QueueLogic;
import wbs.platform.queue.model.QueueItemRec;
import wbs.platform.queue.model.QueueItemState;
import wbs.platform.user.model.UserRec;

import wbs.utils.data.Pair;
import wbs.utils.random.RandomLogic;

@SingletonComponent ("objectVerifyLogic")
public
class ObjectVerifyLogicImplementation
	implements ObjectVerifyLogic {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ModelMetaLoader modelMetaLoader;

	@SingletonDependency
	ObjectManager objectManager;

	@SingletonDependency
	ObjectTypeObjectHelper objectTypeHelper;

	@SingletonDependency
	ObjectVerificationObjectHelper objectVerificationHelper;

	@SingletonDependency
	QueueLogic queueLogic;

	@SingletonDependency
	RandomLogic randomLogic;

	// public implementation

	@Override
	public <Type extends Record <Type>>
	ObjectVerificationRec createOrUpdateObjectVerification (
			@NonNull Transaction parentTransaction,
			@NonNull Type object) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createObjectVerification");

		) {

			ObjectHelper <Type> objectHelper =
				objectManager.objectHelperForObjectRequired (
					object);

			ObjectTypeRec objectType =
				objectTypeHelper.findRequired (
					transaction,
					objectHelper.objectTypeId ());

			Optional <ObjectVerificationRec> verificationOptional =
				objectVerificationHelper.findByParent (
					transaction,
					objectType,
					object.getId ());

			if (
				optionalIsPresent (
					verificationOptional)
			) {

				ObjectVerificationRec verification =
					optionalGetRequired (
						verificationOptional);

				return verification

					.setNextRun (
						transaction.now ())

				;

			} else {

				return objectVerificationHelper.insert (
					transaction,
					objectVerificationHelper.createInstance ()

					.setParentType (
						objectType)

					.setParentId (
						object.getId ())

					.setNextRun (
						transaction.now ())

				);

			}

		}

	}

	@Override
	public
	void performVerification (
			@NonNull Transaction parentTransaction,
			@NonNull ObjectVerificationRec verification,
			@NonNull Optional <UserRec> user) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"performVerification");

		) {

			Record <?> object =
				objectManager.findObjectRequired (
					transaction,
					verification.getParentType ().getId (),
					verification.getParentId ());

			performVerificationReal (
				transaction,
				verification,
				genericCastUnchecked (
					object),
				user);

		}

	}

	// private implementation

	private <Type extends Record <Type>>
	void performVerificationReal (
			@NonNull Transaction parentTransaction,
			@NonNull ObjectVerificationRec verification,
			@NonNull Type object,
			@NonNull Optional <UserRec> user) {

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
					verificationSpec.recurse (),
					false);

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

						.setValid (
							false)

					;

					verification.getQueueItem ().setDetails (
						pluralise (
							collectionSize (
								errors),
							"validation error",
							"validation errors"));

				}

			} else {

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

						if (
							optionalIsPresent (
								user)
						) {

							queueLogic.processQueueItem (
								transaction,
								verification.getQueueItem (),
								optionalGetRequired (
									user));

						} else {

							queueLogic.cancelQueueItem (
								transaction,
								verification.getQueueItem ());

						}

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

						.setValid (
							true)

					;

				}

			}

		}

	}

}
