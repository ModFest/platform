package net.modfest.platform.misc;

import lombok.Getter;
import net.modfest.platform.pojo.PlatformErrorResponse;

@Getter
public class PlatformStandardException extends Exception {
	private final PlatformErrorResponse.ErrorType type;
	private final Object data;

	public PlatformStandardException(PlatformErrorResponse.ErrorType type, Object data) {
		this.type = type;
		this.data = data;
	}

	@Override
	public String getMessage() {
		return this.type+": "+this.data;
	}
}
