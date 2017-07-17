package wbs.platform.rpc.php;

import java.util.Map;

import lombok.NonNull;

import wbs.api.mvc.StringMapResponderFactory;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.web.responder.WebResponder;

@SingletonComponent ("phpStringMapResponderFactory")
public
class PhpStringMapResponderFactory
	implements StringMapResponderFactory {

	// singleton components

	@ClassSingletonDependency
	LogContext logContext;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <PhpMapResponder> phpMapResponderProvider;

	// implementation

	@Override
	public
	WebResponder makeResponder (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull Map <String, ?> map) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"makeResponder");

		) {

			return phpMapResponderProvider.provide (
				taskLogger,
				phpMapResponder ->
					phpMapResponder

				.map (
					map)

			);

		}

	}

}
