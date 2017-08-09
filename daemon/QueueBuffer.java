package wbs.platform.daemon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Coordinates a producer and a number of consumers, while also keeping track of
 * which items are currently being worked on. The producer is typically scanning
 * a database table periodically, and the consumers will remove the item from
 * the database when the item has been processed.
 */
public
class QueueBuffer<Key,Value> {

	static
	class BufferEntry<K,V> {

		K key;
		V item;

		boolean queued;

		BufferEntry (
				K newKey,
				V newItem) {

			key = newKey;
			item = newItem;
			queued = true;

		}

	}

	long fullSize;

	Map <Key, BufferEntry <Key, Value>> buffer =
		new HashMap<> ();

	List <BufferEntry <Key, Value>> queue =
		new LinkedList<> ();

	boolean full = false;

	Object fullLock =
		new Object ();

	/**
	 * Constructs a new, empty QueueBuffer with the given fullSize.
	 *
	 * @param fullSize
	 *            number of items in the buffer to consider it full; must be >=
	 *            1.
	 * @throws IllegalArgumentException
	 *             if fullSize is < 1.
	 */
	public
	QueueBuffer (
			long fullSize) {

		if (fullSize < 1)
			throw new IllegalArgumentException (
				"fullSize must be >= 1");

		this.fullSize = fullSize;

	}

	/**
	 * Returns the value of fullsize.
	 *
	 * @return the value.
	 */
	public
	long getFullSize () {
		return fullSize;
	}

	/**
	 * Adds a new item to this QueueBuffer.
	 *
	 * @param key
	 *            the item's key.
	 * @param item
	 *            the item itself.
	 * @throws IllegalStateException
	 *             if the key is already present.
	 */
	public
	void add (
			Key key,
			Value item) {

		BufferEntry<Key,Value> bufferEntry =
			new BufferEntry<Key,Value> (
				key,
				item);

		synchronized (buffer) {

			if (buffer.containsKey (key))
				throw new IllegalStateException ();

			buffer.put (
				key,
				bufferEntry);

			if (buffer.size () == fullSize) {

				synchronized (fullLock) {
					full = true;
				}

			}

			queue.add (bufferEntry);

			buffer.notify ();

		}

	}

	/**
	 * Retrieves the next item from the queue. If there are none available,
	 * blocks until one is.
	 *
	 * @return the next item in the queue.
	 */
	public
	Value next ()
		throws InterruptedException {

		synchronized (buffer) {

			while (queue.isEmpty ())
				buffer.wait ();

			BufferEntry<Key,Value> bufferEntry =
				queue.get (0);

			queue.remove (0);

			bufferEntry.queued = false;

			return bufferEntry.item;

		}

	}

	/**
	 * Removes the key from the buffer. Called when a client has finished
	 * processing the item, after updating the underlying queue.
	 *
	 * @param key
	 *            the key of the item which has been processed.
	 * @throws NoSuchElementException
	 *             if <code>key</code> is not in the buffer.
	 * @throws IllegalStateException
	 *             if <code>key</code> is in the buffer but has not been
	 *             returned by next (), and istherefore still in the queue.
	 */
	public
	void remove (
			Key key) {

		synchronized (buffer) {
			if (!buffer.containsKey(key))
				throw new NoSuchElementException();
			BufferEntry<Key, Value> bufferEntry = buffer.get(key);
			if (bufferEntry.queued)
				throw new IllegalStateException();
			buffer.remove(key);
			if (buffer.size() == fullSize - 1) {
				synchronized (fullLock) {
					full = false;
					fullLock.notifyAll();
				}
			}
		}
	}

	/**
	 * Returns only when there are less than fullSize members in the buffer.
	 */
	public
	void waitNotFull ()
		throws InterruptedException {

		synchronized (fullLock) {

			while (full)
				fullLock.wait ();

		}

	}

	/**
	 * Returns the current set of keys in the buffer.
	 *
	 * @return a set containing each key currently present in the buffer.
	 */
	public
	Set<Key> getKeys () {

		synchronized (buffer) {

			return new HashSet<Key> (
				buffer.keySet ());

		}

	}

}
