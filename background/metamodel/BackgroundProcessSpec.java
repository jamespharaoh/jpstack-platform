package wbs.platform.background.metamodel;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataClass;
import wbs.framework.entity.meta.model.ModelMetaData;

@Accessors (fluent = true)
@Data
@DataClass ("background-process")
@PrototypeComponent ("backgroundProcessSpec")
@ModelMetaData
public
class BackgroundProcessSpec {

	@DataAttribute (
		required = true)
	String name;

	@DataAttribute (
		name = "object-type",
		required = true)
	String objectTypeCode;

	@DataAttribute (
		required = true)
	String description;

	@DataAttribute (
		required = true)
	String frequency;

}
