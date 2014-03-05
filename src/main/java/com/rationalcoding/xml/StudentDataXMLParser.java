package com.rationalcoding.xml;

import java.io.File;
import java.io.FileNotFoundException;

import javax.xml.stream.XMLStreamException;

public interface StudentDataXMLParser {
	
	public int importStudentData(File studentDataFile) throws XMLStreamException, FileNotFoundException, Exception;

}
