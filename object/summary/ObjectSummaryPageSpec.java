package wbs.platform.object.summary;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.console.module.ConsoleModuleSpec;
import wbs.console.module.ConsoleSpec;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAncestor;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataChildren;
import wbs.framework.data.annotations.DataClass;

@Accessors (fluent = true)
@Data
@DataClass ("object-summary-page")
@PrototypeComponent ("objectSummaryPageSpec")
public
class ObjectSummaryPageSpec
	implements ConsoleSpec {

	// attributes

	@DataAncestor
	ConsoleModuleSpec consoleSpec;

	@DataAttribute (
		name = "form")
	String formTypeName;

	@DataAttribute
	String privKey;

	@DataChildren (
		direct = true)
	List <Object> builders =
		new ArrayList<> ();

}
