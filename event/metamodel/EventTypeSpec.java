package wbs.platform.event.metamodel;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataClass;
import wbs.framework.entity.meta.model.ModelMetaData;


@Accessors (fluent = true)
@Data
@DataClass ("event-type")
@PrototypeComponent ("eventTypeSpec")
@ModelMetaData
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
