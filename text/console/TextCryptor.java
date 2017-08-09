package wbs.platform.text.console;

import lombok.NonNull;

import wbs.console.request.Cryptor;
import wbs.console.request.CryptorFactory;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.tools.ComponentFactory;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

@SingletonComponent ("textCryptor")
public
class TextCryptor
	implements ComponentFactory <Cryptor> {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	CryptorFactory cryptorFactory;

	// implementation

	@Override
	public
	Cryptor makeComponent (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"makeComponent");

		) {

			return cryptorFactory.makeCryptor (
				taskLogger,
				"text");

		}

	}

}
