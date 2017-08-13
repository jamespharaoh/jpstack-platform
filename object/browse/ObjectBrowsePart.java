package wbs.platform.object.browse;

import static wbs.utils.collection.CollectionUtils.collectionStream;
import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIf;
import static wbs.utils.etc.OptionalUtils.optionalIsPresent;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.etc.OptionalUtils.presentInstances;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.web.utils.HtmlAttributeUtils.htmlClassAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlDataAttribute;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenList;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowOpen;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.context.ConsoleContext;
import wbs.console.context.ConsoleContextType;
import wbs.console.forms.core.ConsoleForm;
import wbs.console.forms.core.ConsoleFormType;
import wbs.console.helper.core.ConsoleHelper;
import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.html.MagicTableScriptRef;
import wbs.console.html.ScriptRef;
import wbs.console.misc.JqueryScriptRef;
import wbs.console.module.ConsoleManager;
import wbs.console.part.AbstractPagePart;
import wbs.console.priv.UserPrivChecker;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.GlobalId;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.utils.string.FormatWriter;

@Accessors (fluent = true)
@PrototypeComponent ("objectBrowsePart")
public
class ObjectBrowsePart <ObjectType extends Record <ObjectType>>
	extends AbstractPagePart {

	// singleton dependencies

	@SingletonDependency
	ConsoleManager consoleManager;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	UserPrivChecker privChecker;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	// properties

	@Getter @Setter
	ConsoleHelper <ObjectType> consoleHelper;

	@Getter @Setter
	String typeCode;

	@Getter @Setter
	String localName;

	@Getter @Setter
	ConsoleFormType <ObjectType> formContextBuilder;

	@Getter @Setter
	String targetContextTypeName;

	// state

	ObjectType currentObject;
	List <ObjectType> allObjects;

	ConsoleForm <Object> formContext;

	ConsoleContext targetContext;

	// details

	@Override
	public
	Set <ScriptRef> scriptRefs () {

		return ImmutableSet.<ScriptRef> builder ()

			.addAll (
				super.scriptRefs ())

			.add (
				JqueryScriptRef.instance)

			.add (
				MagicTableScriptRef.instance)

			.build ();

	}

	// implementation

	@Override
	public
	void prepare (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"prepare");

		) {

			prepareCurrentObject (
				transaction);

			prepareAllObjects (
				transaction);

			prepareTargetContext (
				transaction);

		}

	}

	void prepareCurrentObject (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"prepareCurrentObject");

		) {

			Optional <Long> objectIdOptional =
				requestContext.stuffInteger (
					consoleHelper.objectTypeCamel () + "Id");

			if (
				optionalIsPresent (
					objectIdOptional)
			) {

				currentObject =
					consoleHelper.findRequired (
						transaction,
						optionalGetRequired (
							objectIdOptional));

			}

		}

	}

	void prepareAllObjects (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"prepareAllObjects");

		) {

			// locate via parent

			ConsoleHelper <?> parentHelper =
				objectManager.consoleHelperForClassRequired (
					consoleHelper.parentClassRequired ());

			Optional <Long> parentIdOptional =
				requestContext.stuffInteger (
					parentHelper.idKey ());

			if (
				optionalIsPresent (
					parentIdOptional)
			) {

				GlobalId parentGlobalId =
					new GlobalId (
						parentHelper.objectTypeId (),
						optionalGetRequired (
							parentIdOptional));

				if (typeCode != null) {

					allObjects =
						consoleHelper.findByParentAndType (
							transaction,
							parentGlobalId,
							typeCode);

				} else {

					allObjects =
						consoleHelper.findByParent (
							transaction,
							parentGlobalId);

				}

				return;

			}

			// locate via grand parent

			ConsoleHelper <?> grandParentHelper =
				objectManager.consoleHelperForClassRequired (
					parentHelper.parentClassRequired ());

			Optional <Long> grandParentIdOptional =
				requestContext.stuffInteger (
					grandParentHelper.idKey ());

			if (
				optionalIsPresent (
					grandParentIdOptional)
			) {

				GlobalId grandParentGlobalId =
					GlobalId.of (
						grandParentHelper.objectTypeId (),
						optionalGetRequired (
							grandParentIdOptional));

				List <? extends Record<?>> parentObjects =
					parentHelper.findByParent (
						transaction,
						grandParentGlobalId);

				allObjects =
					parentObjects.stream ()

					.flatMap (
						parentObject -> {

						GlobalId parentGlobalId =
							new GlobalId (
								parentHelper.objectTypeId (),
								parentObject.getId ());

						if (typeCode != null) {

							return collectionStream (
								consoleHelper.findByParentAndType (
									transaction,
									parentGlobalId,
									typeCode));

						} else {

							return collectionStream (
								consoleHelper.findByParent (
									transaction,
									parentGlobalId));

						}

					})

					.collect (
						Collectors.toList ());

				return;

			}

			// return all

			if (typeCode != null) {

				throw new RuntimeException ();

			} else {

				allObjects =
					consoleHelper.findAll (
						transaction);

			}

		}

	}

	void prepareTargetContext (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"prepareTargetContext");

		) {

			ConsoleContextType targetContextType =
				consoleManager.contextType (
					targetContextTypeName,
					true);

			targetContext =
				consoleManager.relatedContextRequired (
					taskLogger,
					requestContext.consoleContextRequired (),
					targetContextType);

		}

	}

	@Override
	public
	void renderHtmlBodyContent (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderHtmlBodyContent");

		) {

			// table open

			htmlTableOpenList (
				formatWriter);

			// table header

			htmlTableRowOpen (
				formatWriter);

			formContext.outputTableHeadings (
				transaction,
				formatWriter);

			htmlTableRowClose (
				formatWriter);

			// table content

			for (
				ObjectType object
					: allObjects
			) {

				htmlTableRowOpen (
					formatWriter,

					htmlClassAttribute (
						presentInstances (

						optionalOf (
							"magic-table-row"),

						optionalIf (
							object == currentObject,
							() -> "selected")

					)),

					htmlDataAttribute (
						"target-href",
						requestContext.resolveContextUrl (
							stringFormat (
								"%s",
								targetContext.pathPrefix (),
								"/%s",
								consoleHelper.getPathId (
									transaction,
									object))))

				);

				formContext.outputTableCellsList (
					transaction,
					formatWriter,
					false);

				htmlTableRowClose (
					formatWriter);

			}

			// table close

			htmlTableClose (
				formatWriter);

		}

	}

}
