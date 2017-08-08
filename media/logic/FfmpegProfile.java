package wbs.platform.media.logic;

import java.util.List;

import lombok.NonNull;

public abstract
class FfmpegProfile {

	String fileExtension;

	public
	FfmpegProfile (
			@NonNull String fileExtension) {

		this.fileExtension =
			fileExtension;

	}

	public abstract
	List <List <String>> toFfmpeg ();

}
