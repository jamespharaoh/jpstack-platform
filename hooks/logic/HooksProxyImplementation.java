package wbs.platform.hooks.logic;

import lombok.Getter;
import lombok.NonNull;

public abstract
class HooksProxyImplementation<
		Parent,
		Proxy extends Parent,
		Target extends Parent>
	implements HooksProxy {

	@Getter
	private
	Class<Parent> parentClass;

	@Getter
	private
	Class<Proxy> proxyClass;

	@Getter
	private
	Class<Target> targetClass;

	protected
	HooksProxyImplementation (
			@NonNull Class<Parent> parentClass,
			@NonNull Class<Proxy> proxyClass,
			@NonNull Class<Target> targetClass) {

		this.parentClass =
			parentClass;

		this.proxyClass =
			proxyClass;

		this.targetClass =
			targetClass;

	}

}
