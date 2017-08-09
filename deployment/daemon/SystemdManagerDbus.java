package wbs.platform.deployment.daemon;

import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.string.StringUtils.objectToString;

import lombok.NonNull;

import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusInterfaceName;
import org.freedesktop.dbus.DBusMemberName;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.exceptions.DBusException;

public
class SystemdManagerDbus {

	private final
	DBusConnection dbusConnection;

	private final
	SystemdManagerDbusInterface dbusManagerInterface;

	public
	SystemdManagerDbus (
			@NonNull DBusConnection dbusConnection,
			@NonNull SystemdManagerDbusInterface dbusManagerInterface) {

		this.dbusConnection =
			dbusConnection;

		this.dbusManagerInterface =
			dbusManagerInterface;

	}

	public static
	SystemdManagerDbus get (
			@NonNull DBusConnection dbusConnection) {

		try {

			return new SystemdManagerDbus (
				dbusConnection,
				genericCastUnchecked (
					dbusConnection.getRemoteObject (
						"org.freedesktop.systemd1",
						"/org/freedesktop/systemd1",
						SystemdManagerDbusInterface.class)));

		} catch (DBusException exception) {

			throw new RuntimeException (
				exception);

		}

	}

	public
	SystemdUnitDbus getUnit (
			@NonNull String name) {

		return SystemdUnitDbus.get (
			dbusConnection,
			objectToString (
				dbusManagerInterface.getUnit (
					name)));

	}

	// dbus interface

	@DBusInterfaceName ("org.freedesktop.systemd1.Manager")
	public static
	interface SystemdManagerDbusInterface
		extends DBusInterface {

		@DBusMemberName ("GetUnit")
		Path getUnit (
				String name);

	}

}
