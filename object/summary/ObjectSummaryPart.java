package wbs.platform.object.summary;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.html.ScriptRef;
import wbs.console.part.AbstractPagePart;
import wbs.console.part.PagePart;
import wbs.console.part.PagePartFactory;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.utils.string.FormatWriter;

@Accessors (fluent = true)
@PrototypeComponent ("objectSummaryPart")
public
class ObjectSummaryPart
	extends AbstractPagePart {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// properties

	@Getter @Setter
	List <PagePartFactory> partFactories;

	// state

	List <PagePart> parts;

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

			parts =
				new ArrayList<> ();

			for (
				PagePartFactory partFactory
					: partFactories
			) {

				PagePart pagePart =
					partFactory.buildPagePart (
						transaction);

				pagePart.prepare (
					transaction);

				parts.add (
					pagePart);

			}

		}

	}

	@Override
	public
	Set <ScriptRef> scriptRefs () {

		return parts.stream ()

			.flatMap (
				part ->
					part.scriptRefs ().stream ())

			.collect (
				Collectors.toSet ())

		;

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

			for (
				PagePart part
					: parts
			) {

				part.renderHtmlHeadContent (
					transaction,
					formatWriter);

			}

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

			for (
				PagePart part
					: parts
			) {

				part.renderHtmlBodyContent (
					transaction,
					formatWriter);

			}

		}

	}

}
