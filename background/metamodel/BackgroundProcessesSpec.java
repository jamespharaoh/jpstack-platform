package wbs.platform.background.metamodel;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataChildren;
import wbs.framework.data.annotations.DataClass;
import wbs.framework.entity.meta.model.ModelMetaData;

@Accessors (fluent = true)
@Data
@DataClass ("background-processes")
@PrototypeComponent ("backgroundProcessesSpec")
@ModelMetaData
public
class BackgroundProcessesSpec {

	@DataChildren (
		direct = true)
	List <BackgroundProcessSpec> backgroundProcesses =
		new ArrayList<> ();

}
