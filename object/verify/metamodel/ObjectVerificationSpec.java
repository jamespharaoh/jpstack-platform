package wbs.platform.object.verify.metamodel;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataClass;
import wbs.framework.data.annotations.DataParent;
import wbs.framework.entity.meta.model.ModelDataSpec;
import wbs.framework.entity.meta.model.RecordSpec;

@Accessors (fluent = true)
@Data
@DataClass ("object-verification")
@PrototypeComponent ("objectVerificationSpec")
public
class ObjectVerificationSpec
	implements ModelDataSpec {

	@DataParent
	RecordSpec recordSpec;

	@DataAttribute (
		name = "queue",
		required = true)
	String queueName;

	@DataAttribute (
		required = true)
	Boolean recurse;

}
