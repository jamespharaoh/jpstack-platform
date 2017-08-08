package wbs.platform.media.console;

import static wbs.utils.etc.OptionalUtils.optionalIsNotPresent;
import static wbs.utils.etc.ResultUtils.successResult;
import static wbs.utils.etc.ResultUtils.successResultPresent;

import java.util.Map;

import com.google.common.base.Optional;

import lombok.NonNull;

import wbs.console.forms.types.FormFieldInterfaceMapping;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.platform.media.model.MediaRec;

import fj.data.Either;

@PrototypeComponent ("imageCsvFormFieldInterfaceMapping")
public
class ImageCsvFormFieldInterfaceMapping <Container>
	implements FormFieldInterfaceMapping <Container, MediaRec, String> {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// implementation

	@Override
	public
	Either <Optional <String>, String> genericToInterface (
			@NonNull Transaction parentTransaction,
			@NonNull Container container,
			@NonNull Map <String, Object> hints,
			@NonNull Optional <MediaRec> genericValue) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"genericToInterface");

		) {

			if (
				optionalIsNotPresent (
					genericValue)
			) {

				return successResultPresent (
					"");

			} else {

				return successResult (
					Optional.of (
						genericValue.get ().getFilename ()));

			}

		}

	}

}
