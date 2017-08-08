package wbs.platform.object.create;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.console.module.ConsoleModuleSpec;
import wbs.console.module.ConsoleSpec;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAncestor;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataClass;

@Accessors (fluent = true)
@Data
@DataClass ("object-create-page")
@PrototypeComponent ("objectCreatePageSpec")
public
class ObjectCreatePageSpec
	implements ConsoleSpec {

	// attributes

	@DataAncestor
	ConsoleModuleSpec consoleSpec;

	@DataAttribute
	String name;

	@DataAttribute
	String typeCode;

	@DataAttribute (
		name = "tab")
	String tabName;

	@DataAttribute
	String localFile;

	@DataAttribute (
		name = "responder")
	String responderName;

	@DataAttribute (
		name = "target-context-type")
	String targetContextTypeName;

	@DataAttribute (
		name = "target-responder")
	String targetResponderName;

	@DataAttribute (
		name = "form",
		required = true)
	String formTypeName;

	@DataAttribute (
		name = "create-time")
	String createTimeFieldName;

	@DataAttribute (
		name = "create-user")
	String createUserFieldName;

	@DataAttribute
	String privKey;

}
