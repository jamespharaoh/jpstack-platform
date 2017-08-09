package wbs.platform.background.logic;

import wbs.framework.logging.TaskLogger;

public
interface BackgroundLogic {

	BackgroundProcessHelper registerBackgroundProcess (
			TaskLogger parentTaskLogger,
			String name,
			Object component);

}
