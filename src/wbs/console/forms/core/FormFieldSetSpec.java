package wbs.console.forms.core;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import wbs.console.module.ConsoleModuleSpec;
import wbs.console.module.ConsoleSpec;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataChildren;
import wbs.framework.data.annotations.DataClass;
import wbs.framework.data.annotations.DataParent;

@Accessors (fluent = true)
@Data
@EqualsAndHashCode (of = "name")
@ToString (of = "name")
@DataClass ("field-set")
@PrototypeComponent ("formFieldSetSpec")
public
class FormFieldSetSpec
	implements ConsoleSpec {

	// tree attributes

	@DataParent
	ConsoleModuleSpec consoleModule;

	// attributes

	@DataAttribute (
		required = true)
	String name;

	@DataAttribute (
		name = "class")
	String className;

	@DataAttribute
	String objectName;

	// children

	@DataChildren (direct = true)
	List <Object> formFieldSpecs =
		new ArrayList<> ();

}