package wbs.platform.status.console;

import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.string.StringUtils.stringFormat;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import lombok.Getter;
import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.NormalLifecycleSetup;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

@SingletonComponent ("statusLineManager")
public
class StatusLineManager {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	Map <String, StatusLine> statusLinesByBeanName;

	// properties

	@Getter
	Collection <StatusLine> statusLines;

	// life cycle

	@NormalLifecycleSetup
	public
	void setup (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"setup");

		) {

			Set <String> statusLineNames =
				new HashSet<> ();

			ImmutableList.Builder <StatusLine> statusLinesBuilder =
				ImmutableList.builder ();

			taskLogger.debugFormat (
				"About to initialise status lines");

			for (
				Map.Entry <String, StatusLine> entry
					: statusLinesByBeanName.entrySet ()
			) {

				String beanName =
					entry.getKey ();

				StatusLine statusLine =
					entry.getValue ();

				String statusLineName =
					statusLine.typeName ();

				taskLogger.debugFormat (
					"Adding status line %s from %s",
					statusLineName,
					beanName);

				if (
					statusLineNames
						.contains (statusLineName)
				) {

					throw new RuntimeException (
						stringFormat (
							"Duplicated status line name %s in %s",
							statusLineName,
							beanName));

				}

				statusLineNames.add (
					statusLineName);

				statusLinesBuilder.add (
					statusLine);

				taskLogger.debugFormat (
					"Adding status line %s from %s",
					statusLineName,
					beanName);

			}

			statusLines =
				statusLinesBuilder.build ();

			taskLogger.noticeFormat (
				"Initialised %s status lines",
				integerToDecimalString (
					statusLines.size ()));

		}

	}

}
