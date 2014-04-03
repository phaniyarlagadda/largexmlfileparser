package com.rationalcoding.xml;

/**
 * Exception class for signalling an unknown element in input XML file.
 * @author yarlagadda
 *
 */
public class UnknownXMlElementException extends Exception{

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4258429692387886020L;

	public UnknownXMlElementException(String msg) {
		super(msg);
	}

}
