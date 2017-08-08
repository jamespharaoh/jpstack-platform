package wbs.platform.rpc.core;

import static wbs.utils.string.StringUtils.stringFormat;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import lombok.experimental.Accessors;

@Accessors (fluent = true)
@Data
public
class RpcDefinition {

	String name;
	boolean required;
	Object defaultValue;
	RpcType type;
	RpcDefinition[] members;
	Map<String, RpcDefinition> membersByName;
	RpcChecker checker;

	public
	RpcDefinition (
			String newName,
			boolean newRequired,
			Object newDefaultValue,
			RpcType newType,
			RpcChecker newChecker,
			RpcDefinition... newMembers) {

		if (newName == null || newType == null)
			throw new NullPointerException ();

		if (newType == RpcType.rList && newMembers.length != 1) {

			throw new IllegalArgumentException (
				"Must specify exactly one member for list types");

		}

		name = newName;
		required = newRequired;
		defaultValue = newDefaultValue;
		type = newType;
		members = newMembers;

		membersByName =
			new HashMap<String,RpcDefinition> ();

		for (RpcDefinition member
				: members) {

			if (membersByName.containsKey (
					member.name ())) {

				throw new RuntimeException (
					stringFormat (
						"Duplicated member name: %s",
						member.name ()));

			}

			membersByName.put (
				member.name (),
				member);

		}

		checker =
			newChecker;

	}

}
