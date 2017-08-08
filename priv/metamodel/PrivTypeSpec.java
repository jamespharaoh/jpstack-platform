package wbs.platform.priv.metamodel;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataClass;
import wbs.framework.entity.meta.model.ModelMetaData;

@Accessors (fluent = true)
@Data
@DataClass ("priv-type")
@PrototypeComponent ("privTypeSpec")
@ModelMetaData
public
class PrivTypeSpec {

	@DataAttribute
	String subject;

	@DataAttribute (
		required = true)
	String name;

	@DataAttribute (
		required = true)
	String description;

	@DataAttribute (
		required = true)
	Boolean template;

}
