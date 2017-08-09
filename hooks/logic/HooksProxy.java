package wbs.platform.hooks.logic;

public
interface HooksProxy {

	Class<?> getParentClass ();

	Class<?> getProxyClass ();

	Class<?> getTargetClass ();

	void setDelegate (
			Object delegate);

}
