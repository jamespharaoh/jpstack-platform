package wbs.platform.text.console;

import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.etc.OptionalUtils.optionalOf;

import com.google.common.base.Optional;

import lombok.NonNull;
import lombok.experimental.Accessors;

import wbs.console.forms.types.ConsoleFormNativeMapping;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.platform.text.model.TextObjectHelper;
import wbs.platform.text.model.TextRec;

@Accessors (fluent = true)
@PrototypeComponent ("textFormFieldNativeMapping")
public
class TextFormFieldNativeMapping <Container>
	implements ConsoleFormNativeMapping <Container, String, TextRec> {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	TextObjectHelper textHelper;

	// implementation

	@Override
	public
	Optional <TextRec> genericToNative (
			@NonNull Transaction parentTransaction,
			@NonNull Container container,
			@NonNull Optional <String> genericValue) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"genericToNative");

		) {

			if (! genericValue.isPresent ()) {
				return optionalAbsent ();
			}

			return Optional.of (
				textHelper.findOrCreate (
					transaction,
					genericValue.get ()));

		}

	}

	@Override
	public
	Optional <String> nativeToGeneric (
			@NonNull Transaction parentTransaction,
			@NonNull Container container,
			@NonNull Optional <TextRec> nativeValue) {

		if (! nativeValue.isPresent ()) {
			return optionalAbsent ();
		}

		return optionalOf (
			nativeValue.get ().getText ());

	}

}
