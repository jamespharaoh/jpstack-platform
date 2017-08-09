package wbs.platform.rpc.core;

import static wbs.utils.etc.EnumUtils.enumName;
import static wbs.utils.etc.EnumUtils.enumNotEqualSafe;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.string.StringUtils.stringNotEqualSafe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public
class RpcList
	extends RpcElem {

	private final
	String memberName;

	private final
	RpcType memberType;

	private final
	List<RpcElem> members;

	public
	RpcList (
			String name,
			String newMemberName,
			RpcType newMemberType,
			RpcElem... newMembers) {

		super (
			name,
			RpcType.rList);

		memberName =
			newMemberName;

		memberType =
			newMemberType;

		members =
			new ArrayList<RpcElem> ();

		for (RpcElem member
				: newMembers) {

			add (
				member);

		}

	}

	public
	RpcList (
			String name,
			String newMemberName,
			RpcType newMemberType,
			Collection<RpcElem> newMembers) {

		super (
			name,
			RpcType.rList);

		memberName =
			newMemberName;

		memberType =
			newMemberType;

		members =
			new ArrayList<RpcElem> ();

		for (
			RpcElem member
				: newMembers
		) {

			add (
				member);

		}

	}

	public
	void add (
			RpcElem newMember) {

		if (
			stringNotEqualSafe (
				newMember.getName (),
				memberName)
		) {

			throw new RuntimeException (
				stringFormat (
					"Member name should be: %s",
					memberName));

		}

		if (
			enumNotEqualSafe (
				newMember.getType (),
				memberType)
		) {

			throw new RuntimeException (
				stringFormat (
					"Member type should be: %s",
					enumName (
						memberType)));

		}

		members.add (
			newMember);

	}

	@Override
	public
	List<RpcElem> getValue () {

		return members;

	}

	@Override
	public
	List<Object> getNative () {

		List<Object> ret =
			new ArrayList<Object> ();

		for (
			RpcElem member
				: members
		) {

			ret.add (
				member.getNative ());

		}

		return ret;

	}

}
