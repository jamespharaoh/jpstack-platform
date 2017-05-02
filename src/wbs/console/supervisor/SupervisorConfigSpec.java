package wbs.console.supervisor;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.console.module.ConsoleModuleData;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataChildren;
import wbs.framework.data.annotations.DataClass;

@Accessors (fluent = true)
@Data
@DataClass ("supervisor-config")
@PrototypeComponent ("supervisorConfigSpec")
@ConsoleModuleData
public
class SupervisorConfigSpec {

	@DataAttribute (
		required = true)
	String name;

	@DataAttribute (
		required = true)
	String label;

	@DataAttribute
	Long offsetHours;

	@DataChildren (
		direct = true)
	List <Object> builders =
		new ArrayList<> ();

}
