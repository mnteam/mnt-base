package com.mnt.base.web.action;

import java.util.Collection;

public interface ActionControllerManagerBase<C> {

	/**
	 * Setup available action controller.
	 * 
	 * @param controllers
	 */
	public void setControllers(Collection<C> controllers);
	
	/**
	 * Setup available simple object controller.
	 * 
	 * @param controllers
	 */
	public void setSimpleControllers(Collection<Object> controllers);
	
	
	/**
	 * Get the default action controller reference key
	 * 
	 * @return
	 */
	public String getDefaultActionController();

	/**
	 * Specify the default action controller if no controller matched by request uri.
	 * 
	 * @param defaultActionController
	 */
	public void setDefaultActionController(String defaultActionController);
	
	/**
	 * specify the action controller global access check for method privilege check.
	 * @param accessChecker
	 */
	public void setAccessChecker(AccessChecker accessChecker);
}
