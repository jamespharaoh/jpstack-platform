package wbs.console.forms;

import static wbs.framework.utils.etc.LogicUtils.equalSafe;

import javax.inject.Provider;

import com.google.common.base.Optional;

import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonComponent;

@SingletonComponent ("identityFormFieldPluginProvider")
@SuppressWarnings ({ "rawtypes", "unchecked" })
public
class IdentityFormFieldPluginProvider
	implements FormFieldPluginProvider {

	// prototype dependencies

	@PrototypeDependency
	Provider <IdentityFormFieldNativeMapping>
	identityFormFieldNativeMappingProvider;

	// implementation

	@Override
	public
	Optional getNativeMapping (
			FormFieldBuilderContext context,
			Class containerClass,
			String fieldName,
			Class genericClass,
			Class nativeClass) {

		if (
			equalSafe (
				genericClass,
				nativeClass)
		) {

			return Optional.of (
				(FormFieldNativeMapping)
				identityFormFieldNativeMappingProvider.get ());

		} else {

			return Optional.absent ();

		}

	}

}
