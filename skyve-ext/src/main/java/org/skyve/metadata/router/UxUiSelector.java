package org.skyve.metadata.router;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import org.skyve.impl.metadata.repository.router.TaggingUxUiSelector;
import org.skyve.web.UserAgentType;

public interface UxUiSelector extends TaggingUxUiSelector {
	/**
	 * Called to determine what UxUi to return when the user agent / device type is detected / determined (not previewing but normal system operation)
	 * 
	 * @param userAgentType
	 * @param request
	 * @return
	 */
	public @Nonnull UxUi select(@Nonnull UserAgentType userAgentType, @Nonnull HttpServletRequest request);
	
	/**
	 * Called to determine what UxUi to return when the user agent / device type is emulated (in preview mode)
	 * 
	 * @param userAgentType
	 * @param request
	 * @return
	 */
	default public @Nonnull UxUi emulate(@Nonnull UserAgentType userAgentType, @Nonnull HttpServletRequest request) {
		return select(userAgentType, request);
	}
}
