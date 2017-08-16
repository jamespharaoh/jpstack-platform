package wbs.platform.object.settings;

import static wbs.utils.collection.MapUtils.emptyMap;
import static wbs.utils.collection.SetUtils.emptySet;
import static wbs.utils.etc.LogicUtils.ifNotNullThenElse;
import static wbs.utils.string.StringUtils.capitalise;
import static wbs.web.utils.HtmlBlockUtils.htmlHeadingThreeWriteFormat;
import static wbs.web.utils.HtmlBlockUtils.htmlHeadingTwoWrite;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphClose;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphOpen;
import static wbs.web.utils.HtmlFormUtils.htmlFormClose;
import static wbs.web.utils.HtmlFormUtils.htmlFormOpenPostAction;
import static wbs.web.utils.HtmlFormUtils.htmlFormOpenPostActionEncoding;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenDetails;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.forms.core.ConsoleForm;
import wbs.console.forms.core.ConsoleFormType;
import wbs.console.helper.core.ConsoleHelper;
import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.html.HtmlLink;
import wbs.console.html.ScriptRef;
import wbs.console.lookup.ObjectLookup;
import wbs.console.part.AbstractPagePart;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;

import wbs.platform.object.summary.ObjectSummaryErrorsPart;
import wbs.platform.scaffold.model.RootObjectHelper;

import wbs.utils.string.FormatWriter;

@Accessors (fluent = true)
@PrototypeComponent ("objectSettingsPart")
public
class ObjectSettingsPart <
	ObjectType extends Record <ObjectType>,
	ParentType extends Record <ParentType>
>
	extends AbstractPagePart {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	RootObjectHelper rootHelper;

	@SingletonDependency
	ComponentProvider <ObjectSummaryErrorsPart <ObjectType, ?>>
		errorsPartProvider;

	// properties

	@Getter @Setter
	ObjectLookup <ObjectType> objectLookup;

	@Getter @Setter
	ConsoleHelper <ObjectType> consoleHelper;

	@Getter @Setter
	String editPrivKey;

	@Getter @Setter
	String localName;

	@Getter @Setter
	ConsoleFormType <ObjectType> formType;

	@Getter @Setter
	String removeLocalName;

	// state

	boolean canEdit;

	ConsoleForm <ObjectType> form;

	ObjectSummaryErrorsPart <ObjectType, ?> errorsPart;

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

			canEdit = (

				editPrivKey != null

				&& requestContext.canContext (
					editPrivKey)

			);

			form =
				formType.buildResponse (
					transaction,
					emptyMap (),
					objectLookup.lookupObject (
						transaction,
						requestContext.consoleContextStuffRequired ()));

			errorsPart =
				errorsPartProvider.provide (
					transaction)

				.consoleHelper (
					consoleHelper)

			;

			errorsPart.prepare (
				transaction);

		}

	}

	@Override
	public
	Set <ScriptRef> scriptRefs () {

		return ImmutableSet.<ScriptRef> builder ()

			.addAll (
				errorsPart.scriptRefs ())

			.addAll (
				ifNotNullThenElse (
					form,
					() -> form.allFields ().scriptRefs (),
					() -> emptySet ()))

			.build ()

		;

	}

	@Override
	public
	Set <HtmlLink> links () {

		return ImmutableSet.<HtmlLink> builder ()

			.addAll (
				errorsPart.links ())

			.addAll (
				ifNotNullThenElse (
					form,
					() -> form.allFields ().styles (),
					() -> emptySet ()))

			.build ()

		;

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

			errorsPart.renderHtmlBodyContent (
				transaction,
				formatWriter);

			htmlHeadingThreeWriteFormat (
				formatWriter,
				"%s settings",
				capitalise (
					consoleHelper.friendlyNameSingular ()));

			if (canEdit) {

				String enctype =
					"application/x-www-form-urlencoded";

				try {

					if (form.fileUpload ()) {

						enctype =
							"multipart/form-data";

					}

				} catch (Exception exception) {

					enctype =
						"application/x-www-form-urlencoded";

				}

				htmlFormOpenPostActionEncoding (
					formatWriter,
					requestContext.resolveLocalUrl (
						localName),
					enctype);

				renderButtons (
					transaction,
					formatWriter);

			}

			htmlTableOpenDetails (
				formatWriter);

			form.outputFormRows (
				transaction,
				formatWriter);

			htmlTableClose (
				formatWriter);

			if (canEdit) {

				renderButtons (
					transaction,
					formatWriter);

				htmlFormClose (
					formatWriter);

				if (consoleHelper.ephemeral ()) {

					htmlHeadingTwoWrite (
						formatWriter,
						"Remove");

					htmlFormOpenPostAction (
						formatWriter,
						requestContext.resolveLocalUrl (
							removeLocalName));

					formatWriter.writeLineFormat (
						"<input",
						" type=\"submit\"",
						" value=\"remove\"",
						">");

					htmlFormClose (
						formatWriter);

				}

			}

		}

	}

	@Override
	public
	void renderHtmlHeadContent (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderHtmlHeadContent");

		) {

			errorsPart.renderHtmlHeadContent (
				transaction,
				formatWriter);

		}

	}

	// private implementation

	private
	void renderButtons (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderButtons");

		) {

			htmlParagraphOpen (
				formatWriter);

			formatWriter.writeLineFormat (
				"<input",
				" type=\"submit\"",
				" value=\"save changes\"",
				">");

			htmlParagraphClose (
				formatWriter);

		}

	}

}
