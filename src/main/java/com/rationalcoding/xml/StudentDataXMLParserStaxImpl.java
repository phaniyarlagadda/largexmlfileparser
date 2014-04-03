package com.rationalcoding.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.rationalcoding.dto.StudentDTO;

/**
 * Imports students records into database. Exceptions if any detected are
 * propagated to top.
 * 
 * @author yarlagadda
 * 
 */
@Service
public class StudentDataXMLParserStaxImpl implements StudentDataXMLParser {

	private PlatformTransactionManager transactionManager;
	private StudentDAO studentDAO;
	private static final int DEFAULT_BATCH_SIZE = 500;
	private static final String STUDENTS_ROOT_ELEMENT = "students";
	private static final String STUDENT_ELEMENT = "student";
	private static final String STUDENT_ID_ELEMENT = "studentid";
	private static final String COURSE_ID_ELEMENT = "courseid";
	private static final String GRADE_ELEMENT = "grade";

	@Autowired
	public void setStudentDAO(StudentDAO studentDAO) {
		this.studentDAO = studentDAO;
	}

	@Autowired
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	public int importStudentData(File studentDataFile) throws Exception {
		TransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
		TransactionStatus status = transactionManager.getTransaction(transactionDefinition);
		int totalRecordsImported = 0;
		try {
			totalRecordsImported = readXMLFile(studentDataFile);
			// commit the transaction if no exception is detected
			transactionManager.commit(status);
		} catch (FileNotFoundException fileNotFoundEx) {
			transactionManager.rollback(status);
			throw fileNotFoundEx;
		} catch (XMLStreamException streamEx) {
			transactionManager.rollback(status);
			throw streamEx;
		} catch (Exception ex) {
			transactionManager.rollback(status);
			throw ex;
		}

		return totalRecordsImported;
	}

	private int readXMLFile(File studentDataFile) throws FileNotFoundException, XMLStreamException,
			UnknownXMlElementException {
		int totalRecordsImported = 0;
		InputStream inFileStream = null;
		XMLEventReader eventReader = null;
		try {

			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			// Setup a new eventReader
			inFileStream = new FileInputStream(studentDataFile);
			eventReader = inputFactory.createXMLEventReader(inFileStream);
			// read the XML document
			String tagText = null;
			List<StudentDTO> students = null;
			StudentDTO studentDto = null;
			while (eventReader.hasNext()) {
				XMLEvent xmlEvent = eventReader.nextEvent();
				int eventyType = xmlEvent.getEventType();
				switch (eventyType) {
				case XMLStreamConstants.START_ELEMENT:
					StartElement startElement = xmlEvent.asStartElement();
					String startElementLocale = startElement.getName().getLocalPart();
					if (startElementLocale.equals(STUDENTS_ROOT_ELEMENT)) {
						students = new ArrayList<StudentDTO>();
					} else if (startElementLocale.equals(STUDENT_ELEMENT)) {
						studentDto = new StudentDTO();
					} else if (startElementLocale.equals(STUDENT_ID_ELEMENT)) {
						// do nothing
					} else if (startElementLocale.equals(COURSE_ID_ELEMENT)) {
						// do nothing
					} else if (startElementLocale.equals(GRADE_ELEMENT)) {
						// do nothing
					} else {
						throw new UnknownXMlElementException("Unknown element " + startElementLocale
								+ " detected.");
					}

					break;
				case XMLStreamConstants.CHARACTERS:
					tagText = xmlEvent.asCharacters().getData();
					break;
				case XMLStreamConstants.END_ELEMENT:
					EndElement endElement = xmlEvent.asEndElement();
					String endElementLocale = endElement.getName().getLocalPart();
					if (endElementLocale.equals(STUDENTS_ROOT_ELEMENT)) {
						if (students.size() > 0) {
							totalRecordsImported += studentDAO.importStudentRecords(students);
						}
					} else if (endElementLocale.equals(STUDENT_ELEMENT)) {
						students.add(studentDto);
						if (students.size() >= DEFAULT_BATCH_SIZE) {
							// import the batch and reset the list
							totalRecordsImported += studentDAO.importStudentRecords(students);
							students = new ArrayList<StudentDTO>();
						}
					} else if (endElementLocale.equals(STUDENT_ID_ELEMENT)) {
						studentDto.setStudentId(tagText);
					} else if (endElementLocale.equals(COURSE_ID_ELEMENT)) {
						studentDto.setCourseId(tagText);
					} else if (endElementLocale.equals(GRADE_ELEMENT)) {
						studentDto.setCourseGrade(tagText);
					} else {
						throw new UnknownXMlElementException("Unknown element " + endElementLocale
								+ " detected.");
					}
					break;
				default:
				}// end switch

			}// end while

		} catch (FileNotFoundException fileNotFoundEx) {
			throw fileNotFoundEx;
		} catch (XMLStreamException streamEx) {
			throw streamEx;

		} finally {
			// close the input stream quietly
			IOUtils.closeQuietly(inFileStream);
			if (eventReader != null) {
				eventReader.close();
			}
		}
		return totalRecordsImported;
	}

}
