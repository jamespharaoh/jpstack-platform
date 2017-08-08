package wbs.platform.hooks.logic;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.NormalLifecycleSetup;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

@SingletonComponent ("hooksManager")
public
class HooksManager {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	List <HooksProxy> proxies =
		Collections.emptyList ();

	@SingletonDependency
	List <HooksTarget> targets =
		Collections.emptyList ();

	// life cycle

	@NormalLifecycleSetup
	public
	void setup (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"setup");

		) {

			for (
				HooksProxy proxy
					: proxies
			) {

				initProxy (
					taskLogger,
					proxy);

			}

		}

	}

	public
	void initProxy (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull HooksProxy proxy) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"initProxy");

		) {

			Object delegate =
				createDelegate (
					proxy.getParentClass (),
					ImmutableList.copyOf (
						Iterables.filter (
							targets,
							proxy.getTargetClass ())));

			proxy.setDelegate (
				delegate);

		}

	}

	public
	Object createDelegate (
			Class<?> parentClass,
			final Collection<?> targets) {

		Class<?> proxyClass =
			Proxy.getProxyClass (
				parentClass.getClassLoader (),
				new Class [] { parentClass });

		InvocationHandler invocationHandler =
			new InvocationHandler () {

			@Override
			public
			Object invoke (
					Object target,
					Method method,
					Object[] args)
				throws Throwable {

				if (targets != null) {
					for (Object hook : targets) {
						method.invoke (hook, args);
					}
				}

				return null;

			}

		};

		Constructor<?> constructor;

		try {

			constructor =

			proxyClass.getConstructor (
				new Class [] {
					InvocationHandler.class
				});

		} catch (NoSuchMethodException exception) {

			throw new RuntimeException (
				exception);

		}

		Object proxyObject;
		try {

			proxyObject =
				constructor.newInstance (
					invocationHandler);

		} catch (InvocationTargetException exception) {

			throw new RuntimeException (
				exception);

		} catch (InstantiationException exception) {

			throw new RuntimeException (
				exception);

		} catch (IllegalAccessException exception) {

			throw new RuntimeException (
				exception);

		}

		return parentClass.cast (
			proxyObject);

	}

}
