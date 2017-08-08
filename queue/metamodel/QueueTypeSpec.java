package wbs.platform.queue.metamodel;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataClass;
import wbs.framework.entity.meta.model.ModelMetaData;

@Accessors (fluent = true)
@Data
@DataClass ("queue-type")
@PrototypeComponent ("queueTypeSpec")
@ModelMetaData
public
class QueueTypeSpec {

	@DataAttribute
	String parent;

	@DataAttribute (
		required = true)
	String name;

	@DataAttribute (
		required = true)
	String description;

	@DataAttribute (
		required = true)
	String subject;

	@DataAttribute (
		required = true)
	String ref;

	@DataAttribute (
		required = true)
	String preferredUserDelay;

	@DataAttribute (
		required = true)
	String supervisorPriv;

	@DataAttribute
	Long defaultPriority;

}
