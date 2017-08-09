package wbs.platform.object.list;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.console.module.ConsoleSpec;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataClass;

@Accessors (fluent = true)
@Data
@DataClass ("list-browser")
@PrototypeComponent ("objectListBrowserSpec")
public
class ObjectListBrowserSpec
	implements ConsoleSpec {

	// attributes

	@DataAttribute (
		name = "field",
		required = true)
	String fieldName;

	@DataAttribute
	String label;

}
