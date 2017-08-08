package wbs.platform.rpc.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import lombok.NonNull;

import wbs.framework.logging.TaskLogger;

public
class RpcHandlerFactory
	implements RpcHandler {

	private
	Class <? extends RpcHandler> handlerClass;

	private
	Object parentObject;

	public
	RpcHandlerFactory (
			Class <? extends RpcHandler> newHandlerClass,
			Object newParentObject) {

		handlerClass =
			newHandlerClass;

		parentObject =
			newParentObject;

	}

	public
	RpcHandlerFactory (
			Class<? extends RpcHandler> newHandlerClass) {

		this (
			newHandlerClass,
			null);

	}

	@Override
	public
	RpcResult handle (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull RpcSource source) {

		RpcHandler handler =
			createHandler ();

		return handler.handle (
			parentTaskLogger,
			source);

	}

	private
	RpcHandler createHandler () {

		try {

			if (parentObject == null)
				return handlerClass.newInstance ();

			for (Constructor<?> constructor
					: handlerClass.getConstructors ()) {

				Class<?>[] params =
					constructor.getParameterTypes ();

				if (params.length == 0)
					continue;

				if (! params [0].isAssignableFrom (
						parentObject.getClass ()))
					continue;

				return handlerClass.cast (
					constructor.newInstance (
						parentObject));

			}

			throw new RuntimeException ();

		} catch (InstantiationException exception) {

			throw new RuntimeException (
				exception);

		} catch (InvocationTargetException exception) {

			throw new RuntimeException (
				exception);

		} catch (IllegalAccessException exception) {

			throw new RuntimeException (
				exception);

		}

	}

}
