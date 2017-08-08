package wbs.platform.object.search;

import static wbs.utils.etc.TypeUtils.classInstantiate;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphClose;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphOpen;
import static wbs.web.utils.HtmlFormUtils.htmlFormClose;
import static wbs.web.utils.HtmlFormUtils.htmlFormOpenPostAction;
import static wbs.web.utils.HtmlScriptUtils.htmlScriptBlockClose;
import static wbs.web.utils.HtmlScriptUtils.htmlScriptBlockOpen;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenDetails;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.forms.core.ConsoleForm;
import wbs.console.forms.core.ConsoleFormHintsLogic;
import wbs.console.forms.core.ConsoleFormType;
import wbs.console.helper.core.ConsoleHelper;
import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.html.ScriptRef;
import wbs.console.misc.JqueryScriptRef;
import wbs.console.part.AbstractPagePart;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;

import wbs.platform.user.console.UserConsoleLogic;
import wbs.platform.user.console.UserSessionLogic;

import wbs.utils.string.FormatWriter;

@Accessors (fluent = true)
@PrototypeComponent ("objectSearchPart")
public
class ObjectSearchPart <
	ObjectType extends Record <ObjectType>,
	SearchType extends Serializable
>
	extends AbstractPagePart {

	// singleton dependencies

	@SingletonDependency
	ConsoleFormHintsLogic consoleFormHintsLogic;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	@SingletonDependency
	UserSessionLogic userSessionLogic;

	// properties

	@Getter @Setter
	ConsoleHelper <ObjectType> consoleHelper;

	@Getter @Setter
	Class <SearchType> searchClass;

	@Getter @Setter
	String sessionKey;

	@Getter @Setter
	ConsoleFormType <SearchType> searchFormType;

	@Getter @Setter
	String fileName;

	@Getter @Setter

	// state

	SearchType cleanSearch;
	SearchType currentSearch;

	Map <String, Object> formHints;

	ConsoleForm <SearchType> searchForm;

	// details

	@Override
	public
	Set <ScriptRef> scriptRefs () {

		return ImmutableSet.<ScriptRef> builder ()

			.addAll (
				super.scriptRefs ())

			.add (
				JqueryScriptRef.instance)

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

			ImmutableMap.Builder <String, Object> formHintsBuilder =
				ImmutableMap.builder ();

			consoleFormHintsLogic.prepareParentHints (
				transaction,
				formHintsBuilder,
				consoleHelper);

			formHints =
				formHintsBuilder.build ();

			currentSearch =
				classInstantiate (
					searchClass);

			searchForm =
				searchFormType.buildResponse (
					transaction,
					formHints,
					currentSearch);

			searchForm.setDefaults (
				transaction);

			cleanSearch =
				classInstantiate (
					searchClass);

			searchForm.setDefaults (
				transaction,
				cleanSearch);

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

			// form open

			htmlFormOpenPostAction (
				formatWriter,
				requestContext.resolveLocalUrl (
					"/" + fileName));

			// search fields

			htmlTableOpenDetails (
				formatWriter);

			searchForm.outputFormRows (
				transaction,
				formatWriter);

			htmlTableClose (
				formatWriter);

			// form controls

			htmlParagraphOpen (
				formatWriter);

			formatWriter.writeLineFormat (
				"<input",
				" type=\"submit\"",
				" value=\"search\"",
				">");

			formatWriter.writeLineFormat (
				"<input",
				" type=\"button\"",
				" value=\"reset form\"",
				" onclick=\"resetSearchForm (); return false;\"",
				">");

			htmlParagraphClose (
				formatWriter);

			// form close

			htmlFormClose (
				formatWriter);

			// form field scripts

			htmlScriptBlockOpen (
				formatWriter);

			formatWriter.writeLineFormatIncreaseIndent (
				"function resetSearchForm () {");

			searchForm.outputFormReset (
				transaction,
				formatWriter,
				cleanSearch);

			formatWriter.writeLineFormatDecreaseIndent (
				"}");

			htmlScriptBlockClose (
				formatWriter);

		}

	}

}
