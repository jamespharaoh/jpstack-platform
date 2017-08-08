package wbs.platform.queue.console;

import static wbs.utils.string.StringUtils.stringFormat;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.module.ConsoleManager;

import wbs.framework.component.annotations.SingletonDependency;

@Accessors (fluent = true)
public abstract
class AbstractQueueConsolePlugin
	implements QueueConsolePlugin {

	// singleton dependencies

	@SingletonDependency
	ConsoleManager consoleManager;

	// properties

	@Getter @Setter
	List <String> queueTypeCodes =
		new ArrayList<> ();

	// implementation

	protected
	void queueTypeCode (
			String parentType,
			String queueType) {

		queueTypeCodes.add (
			stringFormat (
				"%s.%s",
				parentType,
				queueType));

	}

}
