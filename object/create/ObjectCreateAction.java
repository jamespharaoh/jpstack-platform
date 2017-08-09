package wbs.platform.object.create;

import static wbs.utils.collection.CollectionUtils.collectionHasOneItem;
import static wbs.utils.collection.CollectionUtils.collectionHasTwoItems;
import static wbs.utils.collection.CollectionUtils.listFirstElementRequired;
import static wbs.utils.collection.CollectionUtils.listSecondElementRequired;
import static wbs.utils.collection.MapUtils.emptyMap;
import static wbs.utils.etc.LogicUtils.ifNotNullThenElse;
import static wbs.utils.etc.NullUtils.isNotNull;
import static wbs.utils.etc.NullUtils.isNull;
import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIsNotPresent;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.etc.OptionalUtils.optionalOrNull;
import static wbs.utils.etc.PropertyUtils.propertyClassForObject;
import static wbs.utils.etc.PropertyUtils.propertySetAuto;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.string.StringUtils.capitalise;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.string.StringUtils.stringSplitSlash;
import static wbs.utils.time.TimeUtils.instantToDateNullSafe;

import java.util.Date;
import java.util.List;

import com.google.common.base.Optional;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.joda.time.Instant;

import wbs.console.action.ConsoleAction;
import wbs.console.context.ConsoleContext;
import wbs.console.context.ConsoleContextType;
import wbs.console.forms.core.ConsoleForm;
import wbs.console.forms.core.ConsoleFormType;
import wbs.console.helper.core.ConsoleHelper;
import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.module.ConsoleManager;
import wbs.console.priv.UserPrivChecker;
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

import wbs.platform.event.logic.EventLogic;
import wbs.platform.object.core.model.ObjectTypeObjectHelper;
import wbs.platform.scaffold.model.RootObjectHelper;
import wbs.platform.text.model.TextObjectHelper;
import wbs.platform.updatelog.logic.UpdateManager;
import wbs.platform.user.console.UserConsoleLogic;

import wbs.web.responder.WebResponder;

@Accessors (fluent = true)
@PrototypeComponent ("objectCreateAction")
public
class ObjectCreateAction <
	ObjectType extends Record <ObjectType>,
	ParentType extends Record <ParentType>
>
	extends ConsoleAction {

	// singleton dependencies

	@SingletonDependency
	ConsoleManager consoleManager;

	@SingletonDependency
	Database database;

	@SingletonDependency
	EventLogic eventLogic;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	ObjectTypeObjectHelper objectTypeHelper;

	@SingletonDependency
	UserPrivChecker privChecker;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	RootObjectHelper rootHelper;

	@SingletonDependency
	TextObjectHelper textHelper;

	@SingletonDependency
	UpdateManager updateManager;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	// properties

	@Getter @Setter
	ConsoleHelper <ObjectType> consoleHelper;

	@Getter @Setter
	String typeCode;

	@Getter @Setter
	ComponentProvider <WebResponder> responderProvider;

	@Getter @Setter
	String targetContextTypeName;

	@Getter @Setter
	ComponentProvider <WebResponder> targetResponderProvider;

	@Getter @Setter
	String createPrivDelegate;

	@Getter @Setter
	String createPrivCode;

	@Getter @Setter
	ConsoleFormType <ObjectType> formType;

	@Getter @Setter
	String createTimeFieldName;

	@Getter @Setter
	String createUserFieldName;

	// state

	ConsoleHelper <ParentType> parentHelper;
	ParentType parent;

	ConsoleForm <ObjectType> form;

	ConsoleContext targetContext;

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

			return responderProvider.provide (
				taskLogger);

		}

	}

	@Override
	protected
	WebResponder goReal (
			@NonNull TaskLogger parentTaskLogger) {

		// begin transaction

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"goReal");

		) {

			parentHelper =
				genericCastUnchecked (
					objectManager.consoleHelperForClassRequired (
						consoleHelper.parentClassRequired ()));

			// determine parent

			determineParent (
				transaction);

			if (
				isNull (
					parent)
			) {
				return null;
			}

			// check permissions

			if (
				isNotNull (
					createPrivCode)
			) {

				Record <?> createDelegate =
					ifNotNullThenElse (
						createPrivDelegate,
						() -> (Record <?>)
							objectManager.dereferenceRequired (
								transaction,
								parent,
								createPrivDelegate),
						() -> parent);

				if (
					! privChecker.canRecursive (
						transaction,
						createDelegate,
						createPrivCode)
				) {

					requestContext.addError (
						"Permission denied");

					return null;

				}

			}

			// setup form

			form =
				formType.buildAction (
					transaction,
					emptyMap (),
					consoleHelper.createInstance ());

			// set parent

			if (

				isNotNull (
					parent)

				&& ! parentHelper.isRoot ()

			) {

				consoleHelper.setParent (
					form.value (),
					parent);

			}

			// set type code

			if (consoleHelper.typeCodeExists ()) {

				propertySetAuto (
					form.value (),
					consoleHelper.typeCodeFieldName (),
					typeCode);

			}

			// perform updates

			form.update (
				transaction);

			if (form.errors ()) {

				form.reportErrors (
					transaction);

				return null;

			}

			// set create time

			if (createTimeFieldName != null) {

				Class <?> createTimeFieldClass =
					propertyClassForObject (
						form.value (),
						createTimeFieldName);

				if (createTimeFieldClass == Instant.class) {

					propertySetAuto (
						form.value (),
						createTimeFieldName,
						transaction.now ());

				} else if (createTimeFieldClass == Date.class) {

					propertySetAuto (
						form.value (),
						createTimeFieldName,
						instantToDateNullSafe (
							transaction.now ()));

				} else {

					throw new RuntimeException ();

				}

			}

			// set create user

			if (createUserFieldName != null) {

				propertySetAuto (
					form.value (),
					createUserFieldName,
					userConsoleLogic.userRequired (
						transaction));

			}

			// before create hook

			consoleHelper ().consoleHooks ().beforeCreate (
				transaction,
				form.value ());

			// insert

			consoleHelper.insert (
				transaction,
				form.value ());

			// after create hook

			consoleHelper ().consoleHooks ().afterCreate (
				transaction,
				form.value ());

			// create event

			Object objectRef =
				consoleHelper.codeExists ()
					? consoleHelper.getCode (form.value ())
					: form.value ().getId ();

			if (consoleHelper.ephemeral ()) {

				eventLogic.createEvent (
					transaction,
					"object_created_in",
					userConsoleLogic.userRequired (
						transaction),
					objectRef,
					consoleHelper.shortName (),
					parent);

			} else {

				eventLogic.createEvent (
					transaction,
					"object_created",
					userConsoleLogic.userRequired (
						transaction),
					form.value (),
					parent);

			}

			// update events

			if (form.value () instanceof PermanentRecord) {

				form.runUpdateHooks (
					transaction,
					(PermanentRecord <?>) form.value (),
					optionalAbsent (),
					optionalAbsent ());

			} else {

				form.runUpdateHooks (
					transaction,
					(PermanentRecord<?>) parent,
					optionalOf (
						objectRef),
					optionalOf (
						consoleHelper.shortName ()));

			}

			// signal update

			updateManager.signalUpdate (
				transaction,
				"user_privs",
				userConsoleLogic.userIdRequired ());

			updateManager.signalUpdate (
				transaction,
				"privs",
				0l);

			// commit transaction

			transaction.commit ();

			// prepare next page

			requestContext.addNotice (
				stringFormat (
					"%s created",
					capitalise (
						consoleHelper.shortName ())));

			requestContext.setEmptyFormData ();

			privChecker.refresh (
				transaction);

			List <String> targetContextTypeNameParts =
				stringSplitSlash (
					targetContextTypeName);

			if (
				collectionHasOneItem (
					targetContextTypeNameParts)
			) {

				ConsoleContextType targetContextType =
					consoleManager.contextType (
						targetContextTypeName,
						true);

				ConsoleContext targetContext =
					consoleManager.relatedContextRequired (
						transaction,
						requestContext.consoleContextRequired (),
						targetContextType);

				consoleManager.changeContext (
					transaction,
					privChecker,
					targetContext,
					"/" + form.value ().getId ());

			} else if (
				collectionHasTwoItems (
					targetContextTypeNameParts)
			) {

				ConsoleContextType targetParentContextType =
					consoleManager.contextType (
						listFirstElementRequired (
							targetContextTypeNameParts),
						true);

				ConsoleContext targetParentContext =
					consoleManager.relatedContextRequired (
						transaction,
						requestContext.consoleContextRequired (),
						targetParentContextType);

				ConsoleContextType targetContextType =
					consoleManager.contextType (
						listSecondElementRequired (
							targetContextTypeNameParts),
						true);

				ConsoleContext targetContext =
					consoleManager.relatedContextRequired (
						transaction,
						targetParentContext,
						targetContextType);

				consoleManager.changeContext (
					transaction,
					privChecker,
					targetContext,
					"/" + form.value ().getId ());

			}

			return targetResponderProvider.provide (
				transaction);

		}

	}

	void determineParent (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"determineParent");

		) {

			if (parentHelper.isRoot ()) {

				ParentType parentTemp1 =
					genericCastUnchecked (
						rootHelper.findRequired (
							transaction,
							0l));

				parent =
					parentTemp1;

				return;

			}

			// get parent id from context

			Optional <Long> parentIdOptional =
				requestContext.stuffInteger (
					parentHelper.idKey ());

			// or from form

			if (
				optionalIsNotPresent (
					parentIdOptional)
			) {

				String parentIdString =
					optionalOrNull (
						requestContext.form (
							stringFormat (
								"%s.%s",
								formType.formName (),
								consoleHelper.parentFieldName ())));

				if (
					isNotNull (
						parentIdString)
				) {

					parentIdOptional =
						optionalOf (
							Long.parseLong (
								parentIdString));

				}

			}

			// error if not found

			if (
				optionalIsNotPresent (
					parentIdOptional)
			) {

				requestContext.addError (
					"Must set parent");

				return;

			}

			// retrieve from database

			parent =
				parentHelper.findRequired (
					transaction,
					optionalGetRequired (
						parentIdOptional));

		}

	}

	/*
	void prepareFieldSet (
			@NonNull TaskLogger parentTaskLogger) {

		formFieldSet =
			parent != null
				? formFieldsProvider.getFieldsForParent (
					parentTaskLogger,
					parent)
				: formFieldsProvider.getStaticFields ();

	}
	*/

}
