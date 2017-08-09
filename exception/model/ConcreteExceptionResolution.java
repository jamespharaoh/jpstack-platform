package wbs.platform.exception.model;

public
enum ConcreteExceptionResolution {

	tryAgainNow,
	tryAgainLater,
	ignoreWithUserWarning,
	ignoreWithThirdPartyWarning,
	ignoreWithLoggedWarning,
	ignoreWithNoWarning,
	fatalError;

}
