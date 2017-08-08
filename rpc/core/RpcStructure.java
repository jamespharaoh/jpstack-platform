package wbs.platform.rpc.core;

import static wbs.utils.string.StringUtils.stringFormat;

import java.util.LinkedHashMap;
import java.util.Map;

public
class RpcStructure
	extends RpcElem {

	private final
	Map<String,RpcElem> members;

	public
	RpcStructure (
			String name,
			RpcElem... newMembers) {

		super (
			name,
			RpcType.rStructure);

		members =
			new LinkedHashMap<String,RpcElem> ();

		for (RpcElem member
				: newMembers) {

			add (
				member);

		}

	}

	public
	void add (
			RpcElem... newMembers) {

		for (RpcElem member
				: newMembers) {

			if (members.containsKey (
					member.getName ())) {

				throw new RuntimeException (
					stringFormat (
						"Duplicated structure member: %s",
						member.getName ()));

			}

			members.put (
				member.getName (),
				member);

		}

	}

	@Override
	public
	Map<String,RpcElem> getValue () {

		return members;

	}

	@Override
	public
	Map<String,Object> getNative () {

		Map<String,Object> ret =
			new LinkedHashMap<String,Object> ();

		for (Map.Entry<String,RpcElem> ent
				: members.entrySet ()) {

			ret.put (
				ent.getKey (),
				ent.getValue ().getNative ());

		}

		return ret;

	}

}
