package wbs.platform.object.list;

import static wbs.utils.collection.CollectionUtils.collectionStream;
import static wbs.utils.collection.MapUtils.emptyMap;
import static wbs.utils.etc.DebugUtils.debugFormat;
import static wbs.utils.etc.NullUtils.ifNull;
import static wbs.utils.etc.NullUtils.isNotNull;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIf;
import static wbs.utils.etc.OptionalUtils.optionalIsPresent;
import static wbs.utils.etc.OptionalUtils.presentInstances;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.string.StringUtils.camelToSpaces;
import static wbs.utils.string.StringUtils.capitalise;
import static wbs.utils.string.StringUtils.objectToString;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.web.utils.HtmlAttributeUtils.htmlClassAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlDataAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlNameAttributeFormat;
import static wbs.web.utils.HtmlAttributeUtils.htmlValueAttribute;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphClose;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphOpen;
import static wbs.web.utils.HtmlFormUtils.htmlFormClose;
import static wbs.web.utils.HtmlFormUtils.htmlFormOpenGetAction;
import static wbs.web.utils.HtmlFormUtils.htmlFormOpenPostAction;
import static wbs.web.utils.HtmlInputUtils.htmlInputSubmitWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellOpen;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableHeaderCellWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenList;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowOpen;
import static wbs.web.utils.HtmlUtils.htmlLinkWrite;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.joda.time.Interval;

import wbs.console.context.ConsoleContext;
import wbs.console.context.ConsoleContextType;
import wbs.console.forms.core.ConsoleForm;
import wbs.console.forms.core.ConsoleFormType;
import wbs.console.forms.core.FormFieldSet;
import wbs.console.helper.core.ConsoleHelper;
import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.html.MagicTableScriptRef;
import wbs.console.html.ObsoleteDateField;
import wbs.console.html.ObsoleteDateLinks;
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

import wbs.platform.object.criteria.CriteriaSpec;

import wbs.utils.string.FormatWriter;

@Accessors (fluent = true)
@PrototypeComponent ("objectListPart")
public
class ObjectListPart <
	ObjectType extends Record <ObjectType>,
	ParentType extends Record <ParentType>
>
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
	Map <String, ObjectListBrowserSpec> listBrowserSpecs;

	@Getter @Setter
	Map <String, ObjectListTabSpec <ObjectType>> listTabSpecs;

	@Getter @Setter
	ConsoleFormType <ObjectType> formType;

	@Getter @Setter
	String targetContextTypeName;

	@Getter @Setter
	Boolean editControl;

	@Getter @Setter
	Boolean moveControls;

	@Getter @Setter
	Boolean deleteControl;

	// state

	FormFieldSet <ObjectType> fields;

	ObsoleteDateField dateField;

	Optional <ObjectListBrowserSpec> currentListBrowserSpec;

	ObjectListTabSpec <ObjectType> currentListTabSpec;
	ObjectType currentObject;

	List <ObjectType> allObjects;
	List <ObjectType> selectedObjects;

	ConsoleContext targetContext;
	ParentType parent;

	ConsoleForm <ObjectType> form;

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

			prepareBrowserSpec (
				transaction);

			prepareTabSpec (
				transaction);

			prepareCurrentObject (
				transaction);

			prepareAllObjects (
				transaction);

			prepareSelectedObjects (
				transaction);

			prepareTargetContext (
				transaction);

			prepareFormContext (
				transaction);

		}

	}

	private
	void prepareFormContext (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"prepareFormContext");

		) {

			form =
				formType.buildResponse (
					transaction,
					emptyMap (),
					allObjects);

		}

	}

	void prepareBrowserSpec (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"prepareBrowserSpec");

		) {

			if (listBrowserSpecs ().isEmpty ()) {

				currentListBrowserSpec =
					Optional.absent ();

			} else if (listBrowserSpecs ().size () > 1) {

				throw new RuntimeException ("TODO");

			} else {

				currentListBrowserSpec =
					Optional.of (
						listBrowserSpecs.values ().iterator ().next ());

				dateField =
					ObsoleteDateField.parse (
						requestContext.parameterOrNull (
							"date"));

				if (dateField.date == null) {

					requestContext.addError (
						"Invalid date");

					return;

				}

			}

		}

	}

	private
	void prepareTabSpec (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"prepareTabSpec");

		) {

			String currentTabName =
				requestContext.parameterOrDefault (
					"tab",
					listTabSpecs.values ().iterator ().next ().name ());

			currentListTabSpec =
				listTabSpecs.get (
					currentTabName);

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

			Long objectId =
				(Long)
				requestContext.stuff (
					consoleHelper.objectTypeCamel () + "Id");

			if (
				isNotNull (
					objectId)
			) {

				currentObject =
					consoleHelper.findRequired (
						transaction,
						objectId);

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

			if (consoleHelper.parentTypeIsFixed ()) {

				ConsoleHelper <ParentType> parentHelper =
					genericCastUnchecked (
						objectManager.consoleHelperForClassRequired (
							consoleHelper.parentClassRequired ()));

				Long parentId =
					(Long)
					requestContext.stuff (
						parentHelper.idKey ());

				if (
					isNotNull (
						parentId)
				) {

					parent =
						parentHelper.findRequired (
							transaction,
							parentId);

					prepareAllObjectsViaParent (
						transaction,
						parentHelper,
						parentId);

					return;

				}

				// locate via grand parent

				if (
					! parentHelper.isRoot ()
					&& parentHelper.parentTypeIsFixed ()
				) {

					ConsoleHelper<?> grandParentHelper =
						objectManager.consoleHelperForClassRequired (
							parentHelper.parentClassRequired ());

					Optional <Long> grandParentIdOptional =
						requestContext.stuffInteger (
							grandParentHelper.idKey ());

					if (
						optionalIsPresent (
							grandParentIdOptional)
					) {

						prepareAllObjectsViaGrandParent (
							transaction,
							parentHelper,
							grandParentHelper,
							optionalGetRequired (
								grandParentIdOptional));

						return;

					}

				}

			}

			// return all

			if (typeCode != null) {

				throw new RuntimeException ();

			} else if (currentListBrowserSpec.isPresent ()) {

				throw new RuntimeException ("TODO");

			} else {

				allObjects =
					consoleHelper.findAll (
						transaction);

			}

		}

	}

	void prepareAllObjectsViaParent (
			@NonNull Transaction parentTransaction,
			@NonNull ConsoleHelper <?> parentHelper,
			@NonNull Long parentId) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"prepareAllObjectsViaParent");

		) {

			if (currentListBrowserSpec.isPresent ()) {

				throw new RuntimeException ("TODO");

			} else {

				GlobalId parentGlobalId =
					new GlobalId (
						parentHelper.objectTypeId (),
						parentId);

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

			}

		}

	}

	void prepareAllObjectsViaGrandParent (
			@NonNull Transaction parentTransaction,
			@NonNull ConsoleHelper <?> parentHelper,
			@NonNull ConsoleHelper <?> grandParentHelper,
			@NonNull Long grandParentId) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"prepareAllObjectsViaGrandParent");

		) {

			if (currentListBrowserSpec.isPresent ()) {

				ObjectListBrowserSpec browserSpec =
					currentListBrowserSpec.get ();

				Record <?> grandParentObject =
					grandParentHelper.findOrThrow (
						transaction,
						grandParentId,
						() -> new NullPointerException (
							stringFormat (
								"Can't find grand parent object %s with id %s",
								grandParentHelper.objectTypeHyphen (),
								integerToDecimalString (
									grandParentId))));

				String daoMethodName =
					stringFormat (
						"findBy",
						capitalise (
							browserSpec.fieldName ()));

				Method daoMethod;

				try {

					daoMethod =
						consoleHelper.getClass ().getMethod (
							daoMethodName,
							grandParentHelper.objectClass (),
							Interval.class);

				} catch (NoSuchMethodException exception) {

					throw new RuntimeException (
						stringFormat (
							"DAO method not found: %s.%s (%s, Interval)",
							stringFormat (
								"%sHelper",
								consoleHelper.objectTypeCamel ()),
							daoMethodName,
							grandParentHelper.objectClass ().getSimpleName ()));

				}

				try {

					List <ObjectType> allObjectsTemp =
						genericCastUnchecked (
							daoMethod.invoke (
								consoleHelper,
								grandParentObject,
								dateField.date.toInterval ()));

					allObjects =
						allObjectsTemp;

				} catch (InvocationTargetException exception) {

					Throwable targetException =
						exception.getTargetException ();

					if (targetException instanceof RuntimeException) {

						throw (RuntimeException) targetException;

					} else {

						throw new RuntimeException (
							targetException);

					}

				} catch (IllegalAccessException exception) {

					throw new RuntimeException (
						exception);

				}

			} else {

				GlobalId grandParentGlobalId =
					new GlobalId (
						grandParentHelper.objectTypeId (),
						grandParentId);

				List <? extends Record <?>> parentObjects =
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

			}

		}

	}

	private
	void prepareSelectedObjects (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"prepareSelectedObjects");

		) {

			// select which objects we want to display

			selectedObjects =
				new ArrayList <ObjectType> ();

		OUTER:

			for (
				ObjectType object
					: allObjects
			) {

				if (
					! consoleHelper.canView (
						transaction,
						privChecker,
						object)
				) {

debugFormat ("Can't view: %s", objectToString (object));

					continue;

				}

				for (
					CriteriaSpec <ObjectType> criteriaSpec
						: currentListTabSpec.criterias ()
				) {

					if (
						! criteriaSpec.evaluate (
							transaction,
							requestContext,
							privChecker,
							consoleHelper,
							object)
					) {

debugFormat ("Doesn't match: %s", objectToString (object));

						continue OUTER;

					}

				}

debugFormat ("Selected: %s", objectToString (object));

				selectedObjects.add (
					object);

			}

			Collections.sort (
				selectedObjects,
				consoleHelper.defaultOrdering ());

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

			goBrowser (
				transaction,
				formatWriter);

			goTabs (
				transaction,
				formatWriter);

			goList (
				transaction,
				formatWriter);

		}

	}

	void goBrowser (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"goBrowser");

		) {

			if (! currentListBrowserSpec.isPresent ()) {
				return;
			}

			ObjectListBrowserSpec browserSpec =
				currentListBrowserSpec.get ();

			String localUrl =
				requestContext.resolveLocalUrlFormat (
					"/%s",
					localName,
					"?tab=%u",
					currentListTabSpec.name ());

			htmlFormOpenGetAction (
				formatWriter,
				localUrl);

			htmlParagraphOpen (
				formatWriter,
				htmlClassAttribute (
					"links"));

			formatWriter.writeLineFormat (
				"%h",
				ifNull (
					browserSpec.label (),
					capitalise (
						camelToSpaces (
							browserSpec.fieldName ()))));

			formatWriter.writeLineFormat (
				"<input",
				" type=\"text\"",
				" name=\"date\"",
				" value=\"%h\"",
				dateField.text,
				">");

			formatWriter.writeLineFormat (
				"<input",
				" type=\"submit\"",
				" value=\"ok\"",
				">");

			ObsoleteDateLinks.dailyBrowserLinks (
				formatWriter,
				localUrl,
				requestContext.formData (),
				dateField.date);

			htmlParagraphClose (
				formatWriter);

			htmlFormClose (
				formatWriter);

		}

	}

	void goTabs (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"goTabs");

		) {

			if (
				listTabSpecs == null
				|| listTabSpecs.size () <= 1
			) {
				return;
			}

			htmlParagraphOpen (
				formatWriter,
				htmlClassAttribute (
					"links"));

			for (
				ObjectListTabSpec <ObjectType> listTabSpec
					: listTabSpecs.values ()
			) {

				htmlLinkWrite (
					formatWriter,
					requestContext.resolveLocalUrl (
						stringFormat (
							"/%u",
							localName,
							"?tab=%u",
							listTabSpec.name ())),
					listTabSpec.label (),
					presentInstances (
						optionalIf (
							listTabSpec == currentListTabSpec,
							() -> htmlClassAttribute (
								"selected"))));

			}

			htmlParagraphClose (
				formatWriter);

		}

	}

	void goList (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"goList");

		) {

			if (
				moveControls
				|| editControl
				|| deleteControl
			) {

				htmlFormOpenPostAction (
					formatWriter,
					requestContext.resolveLocalUrlFormat (
						"/%s",
						localName,
						"?tab=%u",
						currentListTabSpec.name ()));

			}

			htmlTableOpenList (
				formatWriter);

			htmlTableRowOpen (
				formatWriter);

			form.outputTableHeadings (
				transaction,
				formatWriter);

			if (
				moveControls
				|| editControl
				|| deleteControl
			) {

				htmlTableHeaderCellWrite (
					formatWriter,
					"");

			}

			htmlTableRowClose (
				formatWriter);

			// render rows

			for (
				ObjectType object
					: selectedObjects
			) {

				htmlTableRowOpen (
					formatWriter,

					htmlClassAttribute (
						presentInstances (

						Optional.of (
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

				form.outputTableCellsList (
					transaction,
					formatWriter,
					object,
					false);

				if (
					moveControls
					|| editControl
					|| deleteControl
				) {

					htmlTableCellOpen (
						formatWriter,
						htmlClassAttribute (
							"mini-controls"));

					if (moveControls) {

						htmlInputSubmitWrite (
							formatWriter,
							htmlNameAttributeFormat (
								"move-up-%s",
								integerToDecimalString (
									object.getId ())),
							htmlValueAttribute (
								"\u2191"));

						htmlInputSubmitWrite (
							formatWriter,
							htmlNameAttributeFormat (
								"move-down-%s",
								integerToDecimalString (
									object.getId ())),
							htmlValueAttribute (
								"\u2193"));

					}

					if (editControl) {

						htmlInputSubmitWrite (
							formatWriter,
							htmlNameAttributeFormat (
								"edit-%s",
								integerToDecimalString (
									object.getId ())),
							htmlValueAttribute (
								"\ud83d\udd27"));

					}

					if (deleteControl) {

						if (
							consoleHelper.getDeleted (
								transaction,
								object,
								false)
						) {

							htmlInputSubmitWrite (
								formatWriter,
								htmlNameAttributeFormat (
									"undelete-%s",
									integerToDecimalString (
										object.getId ())),
								htmlValueAttribute (
									"\ud83d\uddd8"));

						} else {

							htmlInputSubmitWrite (
								formatWriter,
								htmlNameAttributeFormat (
									"delete-%s",
									integerToDecimalString (
										object.getId ())),
								htmlValueAttribute (
									"\ud83d\uddd9"));

						}

					}

					htmlTableCellClose (
						formatWriter);

				}

				htmlTableRowClose (
					formatWriter);

			}

			htmlTableClose (
				formatWriter);

			if (
				moveControls
				|| editControl
				|| deleteControl
			) {

				htmlFormClose (
					formatWriter);

			}

		}

	}

}
