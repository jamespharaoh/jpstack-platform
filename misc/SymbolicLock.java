package wbs.platform.misc;

import java.util.HashSet;
import java.util.Set;

/**
 * Quick and dirty class to establish a symbolic namespace of virtual locks. A
 * virtual lock exists for each symbol supplied, these are testing by equality.
 * Quick and dirty because it won't scale very well.
 */
public
class SymbolicLock<T> {

	Set<T> locks =
		new HashSet<T> ();

	public synchronized
	void aquire (
			T symbol)
		throws InterruptedException {

		while (locks.contains (symbol))
			wait ();

	}

	public synchronized
	void release (
			T symbol) {

		locks.remove (
			symbol);

		notifyAll ();

	}

	public synchronized
	HeldLock easy (
			T symbol) {

		try {
			aquire (symbol);
		} catch (InterruptedException e) {
			throw new RuntimeException (e);
		}

		return new HeldLock () {
			@Override

			public
			void close () {
				release (symbol);
			}

		};

	}

	public static
	interface HeldLock
		extends AutoCloseable {

		@Override
		void close ();

	}

}