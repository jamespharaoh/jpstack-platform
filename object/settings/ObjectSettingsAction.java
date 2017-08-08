package wbs.platform.object.settings;

import static wbs.utils.collection.CollectionUtils.collectionIsNotEmpty;
import static wbs.utils.collection.MapUtils.emptyMap;
import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIsPresent;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;

import java.util.List;

import com.google.common.base.Optional;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.action.ConsoleAction;
import wbs.console.forms.core.ConsoleForm;
import wbs.console.forms.core.ConsoleFormType;
import wbs.console.helper.core.ConsoleHelper;
import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.lookup.ObjectLookup;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.Database;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.PermanentRecord;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.utils.data.Pair;
import wbs.utils.etc.PropertyUtils;

import wbs.web.responder.WebResponder;

@Accessors (fluent = true)
@PrototypeComponent ("objectSettingsAction")
public
class ObjectSettingsAction <
	ObjectType extends Record <ObjectType>,
	ParentType extends Record <ParentType>
>
	extends ConsoleAction {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	// properties

	@Getter @Setter
	ObjectLookup <ObjectType> objectLookup;

	@Getter @Setter
	ConsoleHelper <ObjectType> consoleHelper;

	@Getter @Setter
	ComponentProvider <WebResponder> detailsResponderProvider;

	@Getter @Setter
	ComponentProvider <WebResponder> accessDeniedResponderProvider;

	@Getter @Setter
	String editPrivKey;

	@Getter @Setter
	String objectRefName;

	@Getter @Setter
	String objectType;

	@Getter @Setter
	ConsoleFormType <ObjectType> formType;

	// state

	ObjectType object;
	ParentType parent;

	// details

	@Override
	public
	WebResponder backupResponder (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"backupResponder");

		) {

			return detailsResponderProvider.provide (
				taskLogger);

		}

	}

	// implementation

	@Override
	public
	WebResponder goReal (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"goReal");

		) {

			// check access

			if (! requestContext.canContext (editPrivKey)) {

				requestContext.addError (
					"Access denied");

				return accessDeniedResponderProvider.provide (
					transaction);

			}

			// lookup objects

			object =
				objectLookup.lookupObject (
					transaction,
					requestContext.consoleContextStuffRequired ());

			// perform update

			ConsoleForm <ObjectType> form =
				formType.buildAction (
					transaction,
					emptyMap (),
					object);

			form.update (
				transaction);

			if (form.errors ()) {

				form.reportErrors (
					transaction);

				return null;

			}

			// perform data validation

			List <Pair <Record <?>, String>> errors =
				consoleHelper.hooks ().verifyData (
					transaction,
					object,
					false);

			if (
				collectionIsNotEmpty (
					errors)
			) {

				for (
					Pair <Record <?>, String> error
						: errors
				) {

					requestContext.addError (
						error.right ());

				}

				return null;

			}

			// check changes were made

			if (! form.updates ()) {

				requestContext.addWarning (
					"No changes made");

				return null;

			}

			// create events

			if (object instanceof PermanentRecord) {

				form.runUpdateHooks (
					transaction,
					(PermanentRecord <?>) object,
					optionalAbsent (),
					optionalAbsent ());

			} else {

				PermanentRecord <?> linkObject =
					genericCastUnchecked (
						objectManager.getParentRequired (
							transaction,
							object));

				Object objectRef =
					PropertyUtils.propertyGetAuto (
						object,
						objectRefName);

				form.runUpdateHooks (
					transaction,
					linkObject,
					optionalOf (
						objectRef),
					optionalOf (
						objectType));

			}

			// call object hooks

			consoleHelper.hooks ().beforeUpdate (
				transaction,
				object);

			// commit

			transaction.commit ();

			requestContext.addNotice (
				"Settings updated");

			return detailsResponderProvider.provide (
				transaction);

		}

	}

	void prepareParent (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"prepareParent");

		) {

			ConsoleHelper <ParentType> parentHelper =
				objectManager.consoleHelperForClassRequired (
					consoleHelper.parentClassRequired ());

			if (parentHelper.isRoot ()) {

				parent =
					parentHelper.findRequired (
						transaction,
						0l);

				return;

			}

			Optional <Long> parentIdOptional =
				requestContext.stuffInteger (
					parentHelper.idKey ());

			if (
				optionalIsPresent (
					parentIdOptional)
			) {

				// use specific parent

				parent =
					parentHelper.findRequired (
						transaction,
						optionalGetRequired (
							parentIdOptional));

				return;

			}

		}

	}

	/*
	void prepareFieldSet (
			@NonNull TaskLogger parentTaskLogger) {

		formFieldSet =
			formFieldsProvider.getFieldsForObject (
				parentTaskLogger,
				object);

	}
	*/

}
