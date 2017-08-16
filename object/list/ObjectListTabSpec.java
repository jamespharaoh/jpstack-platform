package wbs.platform.object.list;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

import wbs.console.module.ConsoleSpec;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataChildren;
import wbs.framework.data.annotations.DataClass;
import wbs.framework.entity.record.Record;

import wbs.platform.object.criteria.CriteriaSpec;

@Accessors (fluent = true)
@Data
@DataClass ("list-tab")
@PrototypeComponent ("objectListTabSpec")
public
class ObjectListTabSpec <ObjectType extends Record <ObjectType>>
	implements ConsoleSpec {

	// attributes

	@DataAttribute
	String name;

	@DataAttribute (
		required = true)
	String label;

	// children

	@DataChildren (
		direct = true)
	List <CriteriaSpec <ObjectType>> criterias =
		new ArrayList<> ();

	// utils

	public
	ObjectListTabSpec <ObjectType> addCriteria (
			@NonNull CriteriaSpec <ObjectType> criteria) {

		criterias.add (
			criteria);

		return this;

	}

}
