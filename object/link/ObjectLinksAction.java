package wbs.platform.object.link;

import static wbs.utils.collection.CollectionUtils.collectionIsNotEmpty;
import static wbs.utils.etc.Misc.contains;
import static wbs.utils.etc.Misc.doesNotContain;
import static wbs.utils.etc.NullUtils.isNotNull;
import static wbs.utils.etc.OptionalUtils.optionalIsPresent;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.action.ConsoleAction;
import wbs.console.helper.core.ConsoleHelper;
import wbs.console.priv.UserPrivChecker;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.Database;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.event.logic.EventLogic;
import wbs.platform.updatelog.logic.UpdateManager;
import wbs.platform.user.console.UserConsoleLogic;

import wbs.utils.etc.PropertyUtils;

import wbs.web.responder.WebResponder;

@Accessors (fluent = true)
@PrototypeComponent ("objectLinksAction")
public
class ObjectLinksAction
	extends ConsoleAction {

	// singleton dependencies

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	Database database;

	@SingletonDependency
	EventLogic eventLogic;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	UserPrivChecker privChecker;

	@SingletonDependency
	UpdateManager updateManager;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	// properties

	@Getter @Setter
	ComponentProvider <WebResponder> responderProvider;

	@Getter @Setter
	ConsoleHelper<?> contextHelper;

	@Getter @Setter
	String contextLinkField;

	@Getter @Setter
	ConsoleHelper<?> targetHelper;

	@Getter @Setter
	String targetLinkField;

	@Getter @Setter
	String addEventName;

	@Getter @Setter
	String removeEventName;

	@Getter @Setter
	EventOrder eventOrder;

	@Getter @Setter
	String contextUpdateSignalName;

	@Getter @Setter
	String targetUpdateSignalName;

	@Getter @Setter
	String successNotice;

	// details

	public static
	enum EventOrder {
		contextThenTarget,
		targetThenContext;
	}

	static
	Pattern oldGroupPattern =
		Pattern.compile ("(\\d+),(true|false)");

	// details

	@Override
	protected
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

			Record <?> contextObject =
				contextHelper.lookupObject (
					transaction,
					requestContext.consoleContextStuffRequired ());

			Set <Record <?>> contextLinks =
				genericCastUnchecked (
					PropertyUtils.propertyGetAuto (
						contextObject,
						contextLinkField));

			List <String> params =
				requestContext.parameterValues (
					"old_link");

			List <Record <?>> updatedTargetObjects =
				new ArrayList<> ();

			for (
				String param
					: params
			) {

				Matcher matcher =
					oldGroupPattern.matcher (
						param);

				if (! matcher.matches ()) {

					requestContext.addError (
						"Internal error");

					return null;

				}

				Long linkId =
					Long.parseLong (
						matcher.group (
							1));

				boolean oldIsMember =
					matcher.group (2).equals ("true");

				boolean newIsMember =
					optionalIsPresent (
						requestContext.parameter (
							"link_" + linkId));

				if (oldIsMember == newIsMember)
					continue;

				Record <?> targetObject =
					targetHelper.findRequired (
						transaction,
						linkId);

				Set <Record <?>> targetLinks =
					genericCastUnchecked (
						PropertyUtils.propertyGetAuto (
							targetObject,
							targetLinkField));

				if (
					! privChecker.canRecursive (
						transaction,
						targetObject,
						"manage")
				) {
					continue;
				}

				if (

					newIsMember

					&& doesNotContain (
						contextLinks,
						targetObject)

				) {

					contextLinks.add (
						targetObject);

					targetLinks.add (
						contextObject);

					if (eventOrder == EventOrder.contextThenTarget) {

						eventLogic.createEvent (
							transaction,
							addEventName,
							userConsoleLogic.userRequired (
								transaction),
							contextObject,
							targetObject);

					}

					if (eventOrder == EventOrder.targetThenContext) {

						eventLogic.createEvent (
							transaction,
							addEventName,
							userConsoleLogic.userRequired (
								transaction),
							targetObject,
							contextObject);

					}

				}

				if (

					oldIsMember

					&& contains (
						contextLinks,
						targetObject)

				) {

					contextLinks.remove (
						targetObject);

					targetLinks.remove (
						contextObject);

					if (eventOrder == EventOrder.contextThenTarget) {

						eventLogic.createEvent (
							transaction,
							removeEventName,
							userConsoleLogic.userRequired (
								transaction),
							contextObject,
							targetObject);

					}

					if (eventOrder == EventOrder.targetThenContext) {

						eventLogic.createEvent (
							transaction,
							removeEventName,
							userConsoleLogic.userRequired (
								transaction),
							targetObject,
							contextObject);

					}

				}

				updatedTargetObjects.add (
					targetObject);

			}

			if (
				collectionIsNotEmpty (
					updatedTargetObjects)
			) {

				if (
					isNotNull (
						contextUpdateSignalName)
				) {

					updateManager.signalUpdate (
						transaction,
						contextUpdateSignalName,
						contextObject.getId ());

				}

				if (
					isNotNull (
						targetUpdateSignalName)
				) {

					for (
						Record<?> targetObject
							: updatedTargetObjects
					) {

						updateManager.signalUpdate (
							transaction,
							targetUpdateSignalName,
							targetObject.getId ());

					}

				}

			}

			transaction.commit ();

			if (
				collectionIsNotEmpty (
					updatedTargetObjects)
			) {

				requestContext.addNotice (
					successNotice);

			}

			return null;

		}

	}

}
