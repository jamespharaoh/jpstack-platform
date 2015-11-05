package wbs.console.forms;

import static wbs.framework.utils.etc.Misc.referenceNotEqual;

import javax.inject.Inject;

import lombok.NonNull;
import lombok.experimental.Accessors;

import com.google.common.base.Optional;

import wbs.console.helper.ConsoleObjectManager;
import wbs.framework.application.annotations.PrototypeComponent;

@Accessors (fluent = true)
@PrototypeComponent ("identityFormFieldAccessor")
public
class IdentityFormFieldAccessor<Container>
	implements FormFieldAccessor<Container,Container> {

	// properties

	@Inject
	ConsoleObjectManager consoleObjectManager;

	// implementation

	@Override
	public
	Optional<Container> read (
			@NonNull Container container) {

		return Optional.of (
			container);

	}

	@Override
	public
	void write (
			@NonNull Container container,
			@NonNull Optional<Container> nativeValue) {

		if (
			referenceNotEqual (
				container,
				nativeValue.get ())
		) {

			throw new RuntimeException ();

		}

	}

}