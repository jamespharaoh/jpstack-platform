package wbs.platform.user.console;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.console.module.ConsoleSpec;
import wbs.console.supervisor.SupervisorConfigSpec;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataClass;
import wbs.framework.data.annotations.DataParent;

@Accessors (fluent = true)
@Data
@DataClass ("user-stats-grouper")
@PrototypeComponent ("supervisorUserStatsGrouperSpec")
public
class SupervisorUserStatsGrouperSpec
	implements ConsoleSpec {

	@DataParent
	SupervisorConfigSpec supervisorConfigSpec;

	@DataAttribute (required = true)
	String name;

}
