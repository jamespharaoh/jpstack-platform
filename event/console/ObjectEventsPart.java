package wbs.platform.event.console;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.part.AbstractPagePart;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.GlobalId;
import wbs.framework.logging.LogContext;

import wbs.platform.event.model.EventLinkObjectHelper;
import wbs.platform.event.model.EventRec;
import wbs.platform.user.console.UserConsoleLogic;

import wbs.utils.string.FormatWriter;

@Accessors (fluent = true)
@PrototypeComponent ("objectEventsPart")
public
class ObjectEventsPart
	extends AbstractPagePart {

	// singleton dependencies

	@SingletonDependency
	EventConsoleLogic eventConsoleLogic;

	@SingletonDependency
	EventLinkObjectHelper eventLinkHelper;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	// properties

	@Getter @Setter
	Collection <GlobalId> dataObjectIds;

	// state

	List <EventRec> events;

	// implementation

	@Override
	public
	void prepare (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"prepare");

		) {

			events =
				dataObjectIds.stream ()

				.map (
					objectId ->
						eventLinkHelper.findByTypeAndRef (
							transaction,
							objectId.typeId (),
							objectId.objectId ()))

				.flatMap (
					eventLinks ->
						eventLinks.stream ())

				.map (
					eventLink ->
						eventLink.getEvent ())

				.sorted ()

				.collect (
					Collectors.toList ());

		}

	}

	@Override
	public
	void renderHtmlBodyContent (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderHtmlBodyContent");

		) {

			eventConsoleLogic.writeEventsTable (
				transaction,
				formatWriter,
				events);

		}

	}

}
