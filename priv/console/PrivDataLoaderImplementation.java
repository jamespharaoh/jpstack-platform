package wbs.platform.priv.console;

import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIsPresent;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.etc.ResultUtils.getError;
import static wbs.utils.etc.ResultUtils.isError;
import static wbs.utils.etc.ResultUtils.isSuccess;
import static wbs.utils.etc.ResultUtils.resultValueRequired;
import static wbs.utils.string.StringUtils.stringEqualSafe;
import static wbs.utils.string.StringUtils.stringFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.google.common.base.Optional;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import wbs.console.priv.UserPrivData;
import wbs.console.priv.UserPrivData.ObjectData;
import wbs.console.priv.UserPrivData.PrivPair;
import wbs.console.priv.UserPrivData.SharedData;
import wbs.console.priv.UserPrivData.UserData;
import wbs.console.priv.UserPrivDataLoader;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.NormalLifecycleSetup;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.Database;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.GlobalId;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;
import wbs.framework.object.ObjectManager;

import wbs.platform.group.model.GroupRec;
import wbs.platform.object.core.model.ObjectTypeObjectHelper;
import wbs.platform.object.core.model.ObjectTypeRec;
import wbs.platform.priv.model.PrivObjectHelper;
import wbs.platform.priv.model.PrivRec;
import wbs.platform.updatelog.logic.UpdateManager;
import wbs.platform.updatelog.logic.UpdateManager.UpdateGetter;
import wbs.platform.user.model.UserObjectHelper;
import wbs.platform.user.model.UserPrivRec;
import wbs.platform.user.model.UserRec;

import fj.data.Either;

@SingletonComponent ("privDataLoader")
public
class PrivDataLoaderImplementation
	implements UserPrivDataLoader {

	// singleton dependencies

	@SingletonDependency
	DataSource dataSource;

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ObjectManager objectManager;

	@SingletonDependency
	ObjectTypeObjectHelper objectTypeHelper;

	@SingletonDependency
	PrivObjectHelper privHelper;

	@SingletonDependency
	UpdateManager updateManager;

	@SingletonDependency
	UserObjectHelper userHelper;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <UserPrivData> userPrivDataProvider;

	// properties

	@Getter @Setter
	int reloadTimeSeconds = 60 * 15;

	// state

	UpdateGetter <SharedData> privDataCache;

	Map <Long, UpdateGetter <UserData>> userDataCachesByUserId =
		new HashMap<>();

	// lifecycle

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

			privDataCache =
				updateManager.makeUpdateGetterAdaptor (
					new PrivDataReloader (),
					reloadTimeSeconds * 1000,
					"privs",
					0);

		}

	}

	// implementation

	SharedData getPrivData (
			@NonNull TaskLogger parentTaskLogger) {

		return privDataCache.provide (
			parentTaskLogger);

	}

	private
	UserData getUserData (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull Long userId) {

		ComponentProvider <UserData> userDataCache =
			getUserDataCache (
				userId);

		return userDataCache.provide (
			parentTaskLogger);

	}

	@Override
	public
	UserPrivData getUserPrivData (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull Long userId) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"getUserPrivData");

		) {

			UserPrivData userPrivData =
				userPrivDataProvider.provide (
					taskLogger);

			userPrivData.sharedData =
				getPrivData (
					taskLogger);

			userPrivData.userData =
				getUserData (
					taskLogger,
					userId);

			return userPrivData;

		}

	}

	@Override
	public synchronized
	void refresh (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"refresh");

		) {

			taskLogger.noticeFormat (
				"Refreshing");

			privDataCache.forceUpdate ();
			userDataCachesByUserId.clear ();

		}

	}

	private synchronized
	UpdateGetter <UserData> getUserDataCache (
			Long userId) {

		UpdateGetter<UserData> ret =
			userDataCachesByUserId.get (
				userId);

		if (ret == null) {

			ret =
				updateManager.makeUpdateGetterAdaptor (
					new UserDataReloader (
						userId),
					reloadTimeSeconds * 1000,
					"user_privs",
					userId);

			userDataCachesByUserId.put (
				userId,
				ret);

		}

		return ret;

	}

	// ================================= priv data reloader

	private
	class PrivDataReloader
		implements ComponentProvider <SharedData> {

		@Override
		public
		SharedData provide (
				@NonNull TaskLogger parentTaskLogger) {

			try (

				OwnedTransaction transaction =
					database.beginReadOnly (
						logContext,
						parentTaskLogger,
						"PrivDataReloader.provide");

			) {

				SharedData newData =
					new SharedData ();

				// get privs, filter those we can't get parent for

				List <PrivRec> privs =
					new ArrayList<> ();

				for (
					PrivRec priv
						: privHelper.findAll (
							transaction)
				) {

					Either <Optional <Record <?>>, String> privParentOrError =
						objectManager.getParentOrError (
							transaction,
							priv);

					if (
						isSuccess (
							privParentOrError)
					) {

						privs.add (
							priv);

					} else {

						transaction.warningFormat (
							"Error getting parent for priv %s: ",
							integerToDecimalString (
								priv.getId ()),
							"type = %s, ",
							priv.getParentType ().getCode (),
							"id = %s: ",
							integerToDecimalString (
								priv.getParentId ()),
							"%s",
							getError (
								privParentOrError));

					}

				}

				// build objectDatasByObjectId

				for (
					PrivRec priv
						: privs
				) {

					GlobalId objectId =
						new GlobalId (
							priv.getParentType ().getId (),
							priv.getParentId ());

					ObjectData objectData =
						newData.objectDatasByObjectId.get (
							objectId);

					if (objectData == null) {

						objectData =
							new ObjectData ();

						newData.objectDatasByObjectId.put (
							objectId,
							objectData);

					}

					objectData.privIdsByCode.put (
						priv.getCode (),
						priv.getId ());

				}

				// sort out objectTypeIdsByClassName

				Collection <ObjectTypeRec> objectTypes =
					objectTypeHelper.findAll (
						transaction);

				for (
					ObjectTypeRec objectType
						: objectTypes
				) {

					Optional <Class <?>> objectClassOptional =
						objectManager.objectClassForTypeCode (
							objectType.getCode ());

					if (
						optionalIsPresent (
							objectClassOptional)
					) {

						newData.objectTypeIdsByClassName.put (
							objectClassOptional.get ().getName (),
							objectType.getId ());

					} else {

						transaction.warningFormat (
							"Ignoring unknown object type %s",
							objectType.getCode ());

					}

				}

				// do chainedPrivIds and managePrivIds

				for (
					PrivRec priv
						: privs
				) {

					Either <Optional <Record <?>>, String> parentOrError =
						objectManager.getParentOrError (
							transaction,
							priv);

					if (
						isError (
							parentOrError)
					) {

						transaction.warningFormat (
							"Error getting parent for priv %s: %s",
							integerToDecimalString (
								priv.getId ()),
							getError (
								parentOrError));

						continue;

					}

					Record <?> parent =
						optionalGetRequired (
							resultValueRequired (
								parentOrError));

					try {

						// do chainedPrivIds

						Either <Optional <Record <?>>, String> grandParentOrError =
							objectManager.getParentOrError (
								transaction,
								parent);

						if (
							isError (
								parentOrError)
						) {

							transaction.warningFormat (
								"Error getting grandparent for priv %s: %s",
								integerToDecimalString (
									priv.getId ()),
								getError (
									parentOrError));

							continue;

						}

						if (
							optionalIsPresent (
								resultValueRequired (
									grandParentOrError))
						) {

							Record <?> grandParent =
								optionalGetRequired (
									resultValueRequired (
										grandParentOrError));

							Optional <Long> chainedPrivIdOptional =
								getChainedPrivId (
									transaction,
									newData,
									grandParent,
									priv.getCode ());

							if (
								optionalIsPresent (
									chainedPrivIdOptional)
							) {

								Long chainedPrivId =
									optionalGetRequired (
										chainedPrivIdOptional);

								newData.chainedPrivIds.put (
									priv.getId (),
									chainedPrivId);

							}

						}

						// do managePrivIds and object data managePrivId

						GlobalId objectId =
							objectManager.getGlobalId (
								transaction,
								parent);

						ObjectData objectData =
							newData.objectDatasByObjectId.get (
								objectId);

						if (
							stringEqualSafe (
								priv.getCode (),
								"manage")
						) {

							newData.managePrivIds.put (
								priv.getId (),
								priv.getId ());

							objectData.managePrivId =
								priv.getId ();

						} else {

							Optional <Long> managePrivIdOptional =
								getChainedPrivId (
									transaction,
									newData,
									parent,
									"manage");

							if (
								optionalIsPresent (
									managePrivIdOptional)
							) {

								Long managePrivId =
									optionalGetRequired (
										managePrivIdOptional);

								newData.managePrivIds.put (
									priv.getId (),
									managePrivId);

								objectData.managePrivId =
									managePrivId;

							}

						}

					} catch (Exception exception) {

						throw new RuntimeException (
							stringFormat (
								"Error loading priv %s",
								integerToDecimalString (
									priv.getId ())),
							exception);

					}

				}

				newData.objectTypeCodesById =
					Collections.unmodifiableMap (
						objectTypeHelper.findAll (
							transaction)

					.stream ()

					.collect (
						Collectors.toMap (
							ObjectTypeRec::getId,
							ObjectTypeRec::getCode)

				));

				// return

				return newData;

			}

		}

		/**
		 * Searches upwards through the object heirachy for a priv of the given
		 * name. Uses newData.privIdsByCodeByObjectId so only searches objects
		 * included there. That is sufficient for our purposes here.
		 */
		private
		Optional <Long> getChainedPrivId (
				@NonNull Transaction parentTransaction,
				@NonNull SharedData newData,
				@NonNull Record <?> object,
				@NonNull String code) {

			try (

				NestedTransaction transaction =
					parentTransaction.nestTransaction (
						logContext,
						"getChainedPrivId");

			) {

				Optional <Record <?>> currentObjectOptional =
					optionalOf (
						object);

				while (
					optionalIsPresent (
						currentObjectOptional)
				) {

					Record <?> currentObject =
						optionalGetRequired (
							currentObjectOptional);

					GlobalId currentObjectId =
						objectManager.getGlobalId (
							transaction,
							currentObject);

					ObjectData currentObjectData =
						newData.objectDatasByObjectId.get (
							currentObjectId);

					if (currentObjectData != null) {

						Long chainedPrivId =
							currentObjectData.privIdsByCode.get (
								code);

						if (chainedPrivId != null) {

							return optionalOf (
								chainedPrivId);

						}

					}

					Either <Optional <Record <?>>, String> nextObjectOrError =
						objectManager.getParentOrError (
							transaction,
							currentObject);

					if (
						isError (
							nextObjectOrError)
					) {

						transaction.warningFormat (
							"Error getting parent for object %s ",
							integerToDecimalString (
								currentObjectId.typeId ()),
							"of type %s: ",
							integerToDecimalString (
								currentObjectId.objectId ()),
							"%s",
							getError (
								nextObjectOrError));

						continue;

					}

					currentObjectOptional =
						resultValueRequired (
							nextObjectOrError);

				}

				return optionalAbsent ();

			}

		}

	}

	// ================================= user data reloader

	private
	class UserDataReloader
		implements ComponentProvider <UserData> {

		private final
		Long userId;

		private
		UserDataReloader (
				Long newUserId) {

			userId =
				newUserId;

		}

		@Override
		public
		UserData provide (
				@NonNull TaskLogger parentTaskLogger) {

			try (

				OwnedTransaction transaction =
					database.beginReadOnly (
						logContext,
						parentTaskLogger,
						"UserDataReloader.provide");

			) {

				UserData newData =
					new UserData ();

				// get user

				UserRec user =
					userHelper.findRequired (
						transaction,
						userId);

				// do user-specific privs

				for (
					UserPrivRec userPriv
						: user.getUserPrivs ()
				) {

					PrivRec priv =
						userPriv.getPriv ();

					PrivPair privPair =
						new PrivPair ();

					privPair.can =
						userPriv.getCan ();

					newData.privPairsByPrivId.put (
						priv.getId (),
						privPair);

				}

				// do group privs

				for (GroupRec group
						: user.getGroups ()) {

					for (PrivRec priv
							: group.getPrivs ()) {

						PrivPair privPair =
							newData.privPairsByPrivId.get (
								priv.getId ());

						if (privPair == null) {

							privPair =
								new PrivPair ();

							newData.privPairsByPrivId.put (
								priv.getId (),
								privPair);

						}

						privPair.can = true;

					}

				}

				// end timer

				return newData;

			}

		}

	}

}
