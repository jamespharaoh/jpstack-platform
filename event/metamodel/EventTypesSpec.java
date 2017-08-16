package wbs.platform.event.metamodel;

import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.framework.data.annotations.DataChildren;
import wbs.framework.data.annotations.DataClass;

@Accessors (fluent = true)
@Data
@DataClass ("event-types")
public
class EventTypesSpec {

	@DataChildren (
		direct = true)
	List <EventTypeSpec> eventTypes;

}
