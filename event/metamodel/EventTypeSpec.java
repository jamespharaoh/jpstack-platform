package wbs.platform.event.metamodel;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataClass;

@Accessors (fluent = true)
@Data
@DataClass ("event-type")
public
class EventTypeSpec {

	@DataAttribute (
		required = true)
	String name;

	@DataAttribute (
		required = true)
	String text;

	@DataAttribute (
		required = true)
	Boolean admin;

}
