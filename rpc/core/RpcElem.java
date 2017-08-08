package wbs.platform.rpc.core;

public abstract
class RpcElem {

	String name;
	RpcType type;

	protected RpcElem (
			String newName,
			RpcType newType) {

		name = newName;
		type = newType;

	}

	public
	String getName () {
		return name;
	}

	public
	RpcType getType () {
		return type;
	}

	public abstract
	Object getValue ();

	public abstract
	Object getNative ();

}
