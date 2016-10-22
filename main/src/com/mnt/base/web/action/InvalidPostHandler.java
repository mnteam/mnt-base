package com.mnt.base.web.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface InvalidPostHandler {

	void handle(HttpServletRequest req, HttpServletResponse resp, String requestContent);
}
