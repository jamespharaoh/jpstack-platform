package wbs.platform.media.fixture;

import java.util.List;

import wbs.framework.database.Transaction;

import wbs.platform.media.model.MediaRec;

public
interface MediaFixtureLogic {

	List <MediaRec> testImages (
			Transaction parentTransaction);

}
