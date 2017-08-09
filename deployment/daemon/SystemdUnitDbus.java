package wbs.platform.deployment.daemon;

import static wbs.utils.etc.TypeUtils.genericCastUnchecked;

import lombok.NonNull;

import org.freedesktop.DBus;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusInterfaceName;
import org.freedesktop.dbus.DBusMemberName;
import org.freedesktop.dbus.exceptions.DBusException;

public
class SystemdUnitDbus {

	// state

	@SuppressWarnings ("unused")
	private final
	DBusConnection dbusConnection;

	private final
	SystemdUnitDbusInterface dbusUnitInterface;

	private final
	DBus.Properties dbusPropertiesInterface;

	// constructors

	public
	SystemdUnitDbus (
			@NonNull DBusConnection dbusConnection,
			@NonNull SystemdUnitDbusInterface dbusUnitInterface,
			@NonNull DBus.Properties dbusPropertiesInterface) {

		this.dbusConnection =
			dbusConnection;

		this.dbusUnitInterface =
			dbusUnitInterface;

		this.dbusPropertiesInterface =
			dbusPropertiesInterface;

	}

	public static
	SystemdUnitDbus get (
			@NonNull DBusConnection dbusConnection,
			@NonNull String path) {

		try {

			return new SystemdUnitDbus (
				dbusConnection,
				genericCastUnchecked (
					dbusConnection.getRemoteObject (
						"org.freedesktop.systemd1",
						path,
						SystemdUnitDbusInterface.class)),
				genericCastUnchecked (
					dbusConnection.getRemoteObject (
						"org.freedesktop.systemd1",
						path,
						DBus.Properties.class)));

		} catch (DBusException exception) {

			throw new RuntimeException (
				exception);

		}

	}

	// dbus properties

	private
	Object getProperty (
			@NonNull String name) {

		return dbusPropertiesInterface.Get (
			dbusInterfaceName,
			name);

	}

	public
	String loadState () {
		return (String) getProperty ("LoadState");
	}

	public
	String activeState () {
		return (String) getProperty ("ActiveState");
	}

	public
	String subState () {
		return (String) getProperty ("SubState");
	}

	// dbus methods

	public
	void restart (
			@NonNull String mode) {

		dbusUnitInterface.restart (
			mode);

	}

	public
	void start (
			@NonNull String mode) {

		dbusUnitInterface.start (
			mode);

	}

	// dbus interface

	@DBusInterfaceName (dbusInterfaceName)
	public static
	interface SystemdUnitDbusInterface
		extends DBusInterface {

		@DBusMemberName ("Start")
		Object start (
				String mode);

		@DBusMemberName ("Restart")
		Object restart (
				String mode);

	}

	// constants

	public final static
	String dbusInterfaceName =
		"org.freedesktop.systemd1.Unit";

}