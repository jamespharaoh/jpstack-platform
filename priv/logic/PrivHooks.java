package wbs.platform.priv.logic;

import static wbs.utils.etc.Misc.doesNotContain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.NormalLifecycleSetup;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.TaskLogger;
import wbs.framework.object.ObjectHelper;
import wbs.framework.object.ObjectHooks;

import wbs.platform.object.core.model.ObjectTypeDao;
import wbs.platform.object.core.model.ObjectTypeRec;
import wbs.platform.priv.model.PrivRec;
import wbs.platform.priv.model.PrivTypeDao;
import wbs.platform.priv.model.PrivTypeRec;

public
class PrivHooks
	implements ObjectHooks <PrivRec> {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ObjectTypeDao objectTypeDao;

	@SingletonDependency
	PrivTypeDao privTypeDao;

	// state

	Map <Long, List <Long>> privTypeIdsByParentTypeId =
		new HashMap<> ();

	// lifecycle

	@NormalLifecycleSetup
	public
	void setup (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTransaction transaction =
				database.beginReadOnly (
					logContext,
					parentTaskLogger,
					"setup");

		) {

			// preload object types

			objectTypeDao.findAll (
				transaction);

			// load priv types and construct index

			privTypeIdsByParentTypeId =
				privTypeDao.findAll (
					transaction)

				.stream ()

				.collect (
					Collectors.groupingBy (

					privType ->
						privType.getParentObjectType ().getId (),

					Collectors.mapping (
						privType ->
							privType.getId (),
						Collectors.toList ()))

				);

		}

	}

	// implementation

	@Override
	public
	void createSingletons (
			@NonNull Transaction parentTransaction,
			@NonNull ObjectHelper<PrivRec> privHelper,
			@NonNull ObjectHelper<?> parentHelper,
			@NonNull Record<?> parent) {

		if (
			doesNotContain (
				privTypeIdsByParentTypeId.keySet (),
				parentHelper.objectTypeId ())
		) {
			return;
		}

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createSingletons");

		) {

			ObjectTypeRec parentType =
				objectTypeDao.findById (
					transaction,
					parentHelper.objectTypeId ());

			for (
				Long privTypeId
					: privTypeIdsByParentTypeId.get (
						parentHelper.objectTypeId ())
			) {

				PrivTypeRec privType =
					privTypeDao.findRequired (
						transaction,
						privTypeId);

				privHelper.insert (
					transaction,
					privHelper.createInstance ()

					.setPrivType (
						privType)

					.setCode (
						privType.getCode ())

					.setParentType (
						parentType)

					.setParentId (
						parent.getId ())

				);

			}

		}

	}

}