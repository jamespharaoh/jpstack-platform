package wbs.platform.media.console;

import static wbs.utils.etc.Misc.doNothing;
import static wbs.utils.etc.Misc.shouldNeverHappen;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.NumberUtils.moreThanZero;
import static wbs.utils.etc.NumberUtils.parseIntegerRequired;
import static wbs.utils.etc.OptionalUtils.optionalIsPresent;
import static wbs.utils.etc.OptionalUtils.optionalMapRequiredOrDefault;
import static wbs.utils.etc.OptionalUtils.presentInstances;
import static wbs.utils.etc.ResultUtils.successResult;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.web.utils.HtmlAttributeUtils.htmlClassAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlColumnSpanAttribute;
import static wbs.web.utils.HtmlStyleUtils.htmlStyleRuleEntry;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellOpen;

import java.io.IOException;
import java.util.Map;

import com.google.common.base.Optional;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;

import wbs.console.forms.types.FormFieldRenderer;
import wbs.console.forms.types.FormFieldSubmission;
import wbs.console.forms.types.FormType;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.platform.media.logic.MediaLogic;
import wbs.platform.media.model.MediaRec;

import wbs.utils.io.RuntimeIoException;
import wbs.utils.string.FormatWriter;

import fj.data.Either;

@PrototypeComponent ("imageFormFieldRenderer")
@Accessors (fluent = true)
public
class ImageFormFieldRenderer <Container>
	implements FormFieldRenderer <Container, MediaRec> {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	MediaConsoleLogic mediaConsoleLogic;

	@SingletonDependency
	MediaConsoleHelper mediaHelper;

	@SingletonDependency
	MediaLogic mediaLogic;

	// properties

	@Getter @Setter
	String name;

	@Getter @Setter
	String label;

	@Getter @Setter
	Boolean nullable;

	@Getter @Setter
	Boolean showFilename;

	// details

	@Getter
	boolean fileUpload = true;

	// implementation

	@Override
	public
	void renderFormTemporarilyHidden (
			@NonNull FormFieldSubmission submission,
			@NonNull FormatWriter htmlWriter,
			@NonNull Container container,
			@NonNull Map <String, Object> hints,
			@NonNull Optional <MediaRec> interfaceValue,
			@NonNull FormType formType,
			@NonNull String formName) {

		doNothing ();

	}

	@Override
	public
	void renderFormInput (
			@NonNull Transaction parentTransaction,
			@NonNull FormFieldSubmission submission,
			@NonNull FormatWriter htmlWriter,
			@NonNull Container container,
			@NonNull Map <String, Object> hints,
			@NonNull Optional <MediaRec> interfaceValueOptional,
			@NonNull FormType formType,
			@NonNull String formName) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderFormInput");

		) {

			if (
				optionalIsPresent (
					interfaceValueOptional)
			) {

				htmlWriter.writeLineFormat (
					"<input",
					" type=\"hidden\"",
					" name=\"%h.%h:id\"",
					formName,
					name (),
					" value=\"%h\"",
					optionalMapRequiredOrDefault (
						interfaceValue ->
							integerToDecimalString (
								interfaceValue.getId ()),
						interfaceValueOptional,
						""),
					"/>");

			}

			if (interfaceValueOptional.isPresent ()) {

				renderHtmlComplex (
					transaction,
					htmlWriter,
					container,
					hints,
					interfaceValueOptional);

				htmlWriter.writeFormat (
					"<br>\n");

			}

			htmlWriter.writeFormat (
				"<input",
				" type=\"file\"",
				" name=\"%h.%h\"",
				formName,
				name (),
				"><br>\n");

			if (
				interfaceValueOptional.isPresent ()
				&& nullable ()
			) {

				htmlWriter.writeFormat (
					"<input",
					" type=\"submit\"",
					" name=\"%h.%h:remove\"",
					formName,
					name (),
					" value=\"remove image\"",
					">\n");

			}

		}

	}

	@Override
	public
	void renderFormReset (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter javascriptWriter,
			@NonNull Container container,
			@NonNull Optional <MediaRec> interfaceValue,
			@NonNull String formName) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderFormReset");

		) {

			javascriptWriter.writeLineFormat (
				"$(\"#%j.%j\").replaceWith (",
				formName,
				name);

			javascriptWriter.writeLineFormat (
				"\t$(\"#%j.%j\").clone (true));",
				formName,
				name);

		}

	}

	@Override
	public
	boolean formValuePresent (
			@NonNull FormFieldSubmission submission,
			@NonNull String formName) {

		return (

			submission.hasParameter (
				stringFormat (
					"%s.%s:remove",
					formName,
					name ()))

		) || (

			submission.hasParameter (
				stringFormat (
					"%s.%s:id",
					formName,
					name ()))

		) || (

			submission.multipart ()

			&& submission.hasFileItem (
				stringFormat (
					"%s.%s",
					formName,
					name ()))

			&& moreThanZero (
				submission.fileItem (
					stringFormat (
						"%s.%s",
						formName,
						name ())
				).getSize ())

		);

	}

	MediaRec formValue (
			@NonNull Transaction parentTransaction,
			@NonNull FormFieldSubmission submission,
			@NonNull String formName) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"formValue");

		) {

			if (
				submission.hasParameter (
					stringFormat (
						"%s.%s:remove",
						formName,
						name ()))
			) {

				return null;

			} else if (

				submission.multipart ()

				&& submission.hasFileItem (
					stringFormat (
						"%s.%s",
						formName,
						name ()))

				&& moreThanZero (
					submission.fileItem (
						stringFormat (
							"%s.%s",
							formName,
							name ())
					).getSize ())

			) {

				FileItem fileItem =
					submission.fileItem (
						stringFormat (
							"%s.%s",
							formName,
							name ()));

				try {

					byte[] data =
						IOUtils.toByteArray (
							fileItem.getInputStream ());

					return mediaLogic.createMediaFromImageRequired (
						transaction,
						data,
						"image/jpeg",
						fileItem.getName ());

				} catch (IOException exception) {

					throw new RuntimeIoException (
						exception);

				}

			} else if (

				submission.hasParameter (
					stringFormat (
						"%s.%s:id",
						formName,
						name ()))

			) {

				return mediaHelper.findRequired (
					transaction,
					parseIntegerRequired (
						submission.parameter (
							stringFormat (
								"%s.%s:id",
								formName,
								name ()))));

			} else {

				throw shouldNeverHappen ();

			}

		}

	}

	@Override
	public
	Either <Optional <MediaRec>, String> formToInterface (
			@NonNull Transaction parentTransaction,
			@NonNull FormFieldSubmission submission,
			@NonNull String formName) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"formToInterface");

		) {

			return successResult (
				Optional.fromNullable (
					formValue (
						transaction,
						submission,
						formName)));

		}

	}

	@Override
	public
	void renderHtmlSimple (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter htmlWriter,
			@NonNull Container container,
			@NonNull Map <String, Object> hints,
			@NonNull Optional <MediaRec> interfaceValue,
			boolean link) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderHtmlSimple");

		) {

			if (! interfaceValue.isPresent ()) {
				return;
			}

			mediaConsoleLogic.writeMediaThumb32 (
				transaction,
				htmlWriter,
				interfaceValue.get ());

			if (showFilename) {

				htmlWriter.writeLineFormat (
					"%h",
					interfaceValue.get ().getFilename ());

			}

		}

	}

	@Override
	public
	void renderHtmlComplex (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter htmlWriter,
			@NonNull Container container,
			@NonNull Map<String,Object> hints,
			@NonNull Optional<MediaRec> interfaceValue) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderHtmlComplex");

		) {

			if (! interfaceValue.isPresent ()) {
				return;
			}

			mediaConsoleLogic.writeMediaThumb100 (
				transaction,
				htmlWriter,
				interfaceValue.get ());

			if (showFilename) {

				htmlWriter.writeLineFormat (
					"<br>");

				htmlWriter.writeLineFormat (
					"%h (%h bytes)",
					interfaceValue.get ().getFilename (),
					integerToDecimalString (
						interfaceValue.get ().getContent ().getData ().length));

			}

		}

	}

	@Override
	public
	void renderHtmlTableCellList (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter,
			@NonNull Container container,
			@NonNull Map <String, Object> hints,
			@NonNull Optional <MediaRec> interfaceValue,
			@NonNull Boolean link,
			@NonNull Long colspan) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderHtmlTableCellList");

		) {

			htmlTableCellOpen (
				formatWriter,
				htmlStyleRuleEntry (
					"text-align",
					listAlign ().name ()),
				htmlColumnSpanAttribute (
					colspan),
				htmlClassAttribute (
					presentInstances (
						htmlClass (
							interfaceValue))));

			renderHtmlSimple (
				transaction,
				formatWriter,
				container,
				hints,
				interfaceValue,
				link);

			htmlTableCellClose (
				formatWriter);

		}

	}

	@Override
	public
	void renderHtmlTableCellProperties (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter,
			@NonNull Container container,
			@NonNull Map <String, Object> hints,
			@NonNull Optional <MediaRec> interfaceValue,
			@NonNull Boolean link,
			@NonNull Long colspan) {


		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderHtmlTableCellProperties");

		) {

			htmlTableCellOpen (
				formatWriter,
				htmlStyleRuleEntry (
					"text-align",
					propertiesAlign ().name ()),
				htmlColumnSpanAttribute (
					colspan));

			renderHtmlSimple (
				transaction,
				formatWriter,
				container,
				hints,
				interfaceValue,
				link);

			htmlTableCellClose (
				formatWriter);

		}

	}

}
