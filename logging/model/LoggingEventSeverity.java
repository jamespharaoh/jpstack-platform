package wbs.platform.logging.model;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;

import lombok.NonNull;

import wbs.framework.logging.LogSeverity;

public
enum LoggingEventSeverity {

	debug,
	trace,
	logic,
	notice,
	warning,
	error,
	fatal;

	// implementation

	public static
	LoggingEventSeverity fromLogSeverity (
			@NonNull LogSeverity logSeverity) {

		return fromLogSeverity (
			logSeverity);

	}

	public static
	LoggingEventSeverity toLogSeverity (
			@NonNull LoggingEventSeverity loggingEventSeverity) {

		return toLogSeverity (
			loggingEventSeverity);

	}

	// constants

	public final static
	Map <LogSeverity, LoggingEventSeverity> fromLogSeverityMap =
		ImmutableMap.copyOf (
			Arrays.stream (
				LoggingEventSeverity.values ())

		.collect (
			Collectors.toMap (
				loggingEventSeverity ->
					LogSeverity.valueOf (
						loggingEventSeverity.name ()),
				loggingEventSeverity ->
					loggingEventSeverity))

	);

	public final static
	Map <LoggingEventSeverity, LogSeverity> toLogSeverityMap =
		ImmutableMap.copyOf (
			Arrays.stream (
				LoggingEventSeverity.values ())

		.collect (
			Collectors.toMap (
				loggingEventSeverity ->
					loggingEventSeverity,
				loggingEventSeverity ->
					LogSeverity.valueOf (
						loggingEventSeverity.name ())))

	);

}
