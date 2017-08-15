package wbs.platform.object.list;

import static wbs.utils.etc.DebugUtils.debugFormat;
import static wbs.utils.etc.NumberUtils.equalToZero;
import static wbs.utils.etc.NumberUtils.parseIntegerRequired;
import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIsNotPresent;
import static wbs.utils.etc.OptionalUtils.optionalIsPresent;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.string.StringUtils.stringExtract;

import java.util.Map;

import com.google.common.base.Optional;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.action.ConsoleAction;
import wbs.console.helper.core.ConsoleHelper;
import wbs.console.helper.manager.ConsoleObjectManager;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.Database;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.event.logic.EventLogic;
import wbs.platform.user.console.UserConsoleLogic;

import wbs.web.responder.WebResponder;

@Accessors (fluent = true)
public
class ObjectListAction <Type extends Record <Type>>
	extends ConsoleAction {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@SingletonDependency
	EventLogic eventLogic;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	// properties

	@Getter @Setter
	ConsoleHelper <Type> consoleHelper;

	@Getter @Setter
	ComponentProvider <WebResponder> responderProvider;

	// protected implementation

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

	@Override
	protected
	WebResponder goReal (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"goReal");

		) {

			for (
				Map.Entry <String, String> parameterEntry
					: requestContext.parameterMapSimple ().entrySet ()
			) {

				String parameterName =
					parameterEntry.getKey ();

debugFormat ("PARAM: %s", parameterName);

				Optional <String> moveUpIdStringOptional =
					stringExtract (
						"move-up-",
						"",
						parameterName);

				Optional <String> moveDownIdStringOptional =
					stringExtract (
						"move-down-",
						"",
						parameterName);

				Optional <String> editIdStringOptional =
					stringExtract (
						"edit-",
						"",
						parameterName);

				Optional <String> deleteIdStringOptional =
					stringExtract (
						"delete-",
						"",
						parameterName);

				Optional <String> undeleteIdStringOptional =
					stringExtract (
						"undelete-",
						"",
						parameterName);

				if (
					optionalIsPresent (
						moveUpIdStringOptional)
				) {

					performMoveUp (
						taskLogger,
						parseIntegerRequired (
							optionalGetRequired (
								moveUpIdStringOptional)));

				} else if (
					optionalIsPresent (
						moveDownIdStringOptional)
				) {

					performMoveDown (
						taskLogger,
						parseIntegerRequired (
							optionalGetRequired (
								moveDownIdStringOptional)));

				} else if (
					optionalIsPresent (
						editIdStringOptional)
				) {

					performEdit (
						taskLogger,
						parseIntegerRequired (
							optionalGetRequired (
								editIdStringOptional)));

				} else if (
					optionalIsPresent (
						deleteIdStringOptional)
				) {

					performDelete (
						taskLogger,
						parseIntegerRequired (
							optionalGetRequired (
								deleteIdStringOptional)));

				} else if (
					optionalIsPresent (
						undeleteIdStringOptional)
				) {

					performUndelete (
						taskLogger,
						parseIntegerRequired (
							optionalGetRequired (
								undeleteIdStringOptional)));

				}

			}

			return null;

		}

	}

	// private implementatation

	private
	void performMoveUp (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull Long id) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"performMoveUp");

		) {

			Type object =
				consoleHelper.findRequired (
					transaction,
					id);

			if (
				consoleHelper.getDeleted (
					transaction,
					object,
					true)
			) {

				requestContext.addError (
					"Cannot move deleted object");

				return;

			}

			Long index =
				consoleHelper.getIndex (
					object);

			if (
				equalToZero (
					index)
			) {

				requestContext.addError (
					"Already at top");

				return;

			}

			Record <?> parent =
				consoleHelper.getParentRequired (
					transaction,
					object);

			Type previousObject =
				consoleHelper.findByIndexRequired (
					transaction,
					parent,
					index - 1l);

			consoleHelper.setIndex (
				previousObject,
				-1l);

			consoleHelper.setIndex (
				object,
				-2l);

			transaction.flush ();

			consoleHelper.setIndex (
				previousObject,
				index);

			consoleHelper.setIndex (
				object,
				index - 1l);

			eventLogic.createEvent (
				transaction,
				"admin_object_field_updated",
				userConsoleLogic.userRequired (
					transaction),
				consoleHelper.indexFieldName (),
				object,
				index - 1);

			eventLogic.createEvent (
				transaction,
				"admin_object_field_updated",
				userConsoleLogic.userRequired (
					transaction),
				consoleHelper.indexFieldName (),
				previousObject,
				index);

			transaction.commit ();

			requestContext.addNotice (
				"Object moved up");

		}

	}

	private
	void performMoveDown (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull Long id) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"performMoveDown");

		) {

			Type object =
				consoleHelper.findRequired (
					transaction,
					id);

			if (
				consoleHelper.getDeleted (
					transaction,
					object,
					true)
			) {

				requestContext.addError (
					"Cannot move deleted object");

				return;

			}

			Long index =
				consoleHelper.getIndex (
					object);

			Record <?> parent =
				consoleHelper.getParentRequired (
					transaction,
					object);

			Optional <Type> nextObjectOptional =
				consoleHelper.findByIndex (
					transaction,
					parent,
					index + 1l);

			if (
				optionalIsNotPresent (
					nextObjectOptional)
			) {

				requestContext.addError (
					"Already at bottom");

				return;

			}

			Type nextObject =
				optionalGetRequired (
					nextObjectOptional);

			if (
				consoleHelper.getDeleted (
					transaction,
					nextObject,
					false)
			) {

				requestContext.addError (
					"Already at bottom");

				return;

			}

			consoleHelper.setIndex (
				nextObject,
				-1l);

			consoleHelper.setIndex (
				object,
				-2l);

			transaction.flush ();

			consoleHelper.setIndex (
				nextObject,
				index);

			consoleHelper.setIndex (
				object,
				index + 1l);

			eventLogic.createEvent (
				transaction,
				"admin_object_field_updated",
				userConsoleLogic.userRequired (
					transaction),
				consoleHelper.indexFieldName (),
				nextObject,
				index);

			eventLogic.createEvent (
				transaction,
				"admin_object_field_updated",
				userConsoleLogic.userRequired (
					transaction),
				consoleHelper.indexFieldName (),
				object,
				index + 1l);

			transaction.commit ();

			requestContext.addNotice (
				"Object moved down");

		}

	}

	private
	void performEdit (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull Long id) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"performEdit");

		) {

			Type object =
				consoleHelper.findRequired (
					transaction,
					id);

			requestContext.addWarning (
				"TODO");

		}

	}

	private
	void performDelete (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull Long id) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"performDelete");

		) {

			Type object =
				consoleHelper.findRequired (
					transaction,
					id);

			if (
				consoleHelper.getDeleted (
					transaction,
					object,
					false)
			) {

				requestContext.addWarning (
					"Object is already deleted");

				return;

			}

			consoleHelper.setDeleted (
				object,
				true);

			consoleHelper.update (
				transaction,
				object);

			eventLogic.createEvent (
				transaction,
				"admin_object_field_updated",
				userConsoleLogic.userRequired (
					transaction),
				consoleHelper.deletedFieldName (),
				object,
				true);

			transaction.commit ();

			requestContext.addNotice (
				"Object deleted");

		}

	}

	private
	void performUndelete (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull Long id) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"performUndelete");

		) {

			Type object =
				consoleHelper.findRequired (
					transaction,
					id);

			if (
				! consoleHelper.getDeleted (
					transaction,
					object,
					false)
			) {

				requestContext.addWarning (
					"Object is not deleted");

				return;

			}

			Record <?> parent =
				consoleHelper.getParentRequired (
					transaction,
					object);

			ConsoleHelper <?> parentHelper =
				objectManager.consoleHelperForObjectRequired (
					genericCastUnchecked (
						parent));

			if (
				parentHelper.getDeleted (
					transaction,
					genericCastUnchecked (
						parent),
					true)
			) {

				requestContext.addError (
					"Cannot modify item because parent is deleted");

				return;

			}

			consoleHelper.setDeleted (
				object,
				false);

			consoleHelper.update (
				transaction,
				object);

			eventLogic.createEvent (
				transaction,
				"admin_object_field_updated",
				userConsoleLogic.userRequired (
					transaction),
				consoleHelper.deletedFieldName (),
				object,
				false);

			transaction.commit ();

			requestContext.addNotice (
				"Object undeleted");

		}

	}

}
