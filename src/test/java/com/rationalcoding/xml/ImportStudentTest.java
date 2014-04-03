package com.rationalcoding.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import javax.sql.DataSource;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:spring/mvc-config.xml" })
@Transactional
public class ImportStudentTest {

	public static final String rootElement = "students";
	public static final String studentRootElement = "student";
	public static final String studentIdElement = "studentid";
	public static final String courseIdElement = "courseid";
	public static final String gradeElement = "grade";
	

	@Autowired
	private StudentDataXMLParserStaxImpl studentDataXMLParserStaxImpl;
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	@Test
	@Rollback(true)
	public void testSmallFile() {
		testImport(5);
	}
	
	@Test
	@Rollback(true)
	public void testMissingFile(){
		File inputFile = new File("students-temp.xml");
		try {
			studentDataXMLParserStaxImpl.importStudentData(inputFile);
			Assert.fail("File not found exception expected");
		} catch (FileNotFoundException e) {
			// expected
		} catch (XMLStreamException e) {
			Assert.fail("File not found exception expected but found stream exception");
		} catch (Exception e) {
			Assert.fail("File not found exception expected but found unknow exception");
		}
	}
	
	@Test
	@Rollback(true)
	public void testRecordsMultipleOfBatchSize() {
		testImport(500);
	}
	
	@Test
	@Rollback(true)
	public void testRecordsNotMultipleOfBatchSize() {
		testImport(800);
	}
	
	
	@Test
	public void testVeryLargeFile() {
		testImport(100000);
	}
	
	/**
	 * Util method to create input file and call import on service class.
	 * Asserts number of records imported
	 * @param recordsToImport
	 */
	private void testImport(int recordsToImport){
		File testResourceDirectory = new File("src/test/resources");
		testResourceDirectory.mkdirs();
		File inputFile = new File(testResourceDirectory,"students.xml");
		if(inputFile.exists()){
			inputFile.delete();
		}
		try {
			createStudentXMLFile(inputFile,recordsToImport);
		} catch (XMLStreamException e1) {
			Assert.fail("Unable to create input file for testing. Detected XML stream exception.");
		} catch (FileNotFoundException e) {
			Assert.fail("Unable to create input file for testing. Detected filenot found exception.");
		}
		int countBeforeImport = getTotalRecords();
		try {
			int recordsImported = studentDataXMLParserStaxImpl.importStudentData(inputFile);
			Assert.assertEquals(recordsToImport, recordsImported);
		} catch (FileNotFoundException e) {
			Assert.fail("File not found exception detected when parsing xml file");
		} catch (XMLStreamException e) {
			Assert.fail("XML stream exception detected when parsing xml file");
		} catch (Exception e) {
			Assert.fail("Exception detected when trying to import student records.");
		}
		int countAfterImport = getTotalRecords();
		Assert.assertEquals(countBeforeImport+recordsToImport, countAfterImport);
	}
	
	private int getTotalRecords(){
		return jdbcTemplate.queryForObject("select count(*) from student_grades", Integer.class);	
		
	}

	private void createStudentXMLFile(File fileName, int numOfRecords) throws XMLStreamException, FileNotFoundException {
		XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
		try {
			XMLEventWriter xmlEventWriter = xmlOutputFactory.createXMLEventWriter(new FileOutputStream(
					fileName), "UTF-8");
			XMLEventFactory eventFactory = XMLEventFactory.newInstance();
			XMLEvent end = eventFactory.createDTD("\n");
			StartDocument startDocument = eventFactory.createStartDocument();
			xmlEventWriter.add(startDocument);
			xmlEventWriter.add(end);
			StartElement configStartElement = eventFactory.createStartElement("", "", rootElement);
			xmlEventWriter.add(configStartElement);
			xmlEventWriter.add(end);
			// Write the element nodes
			for (int i = 0; i < numOfRecords; i++) {
				xmlEventWriter.add(eventFactory.createStartElement("", "", studentRootElement));
				xmlEventWriter.add(end);
				createNode(xmlEventWriter, studentIdElement,"student" + i);
				createNode(xmlEventWriter, courseIdElement,"course"+i);
				createNode(xmlEventWriter, gradeElement,"A");
				xmlEventWriter.add(eventFactory.createEndElement("", "", rootElement));
				xmlEventWriter.add(end);
			}

			xmlEventWriter.add(eventFactory.createEndElement("", "", rootElement));
			xmlEventWriter.add(end);
			xmlEventWriter.add(eventFactory.createEndDocument());
			xmlEventWriter.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw e;
		} catch (XMLStreamException ex) {
			ex.printStackTrace();
			throw ex;
		}
	}

	private void createNode(XMLEventWriter eventWriter, String element, String value)
			throws XMLStreamException {
		XMLEventFactory xmlEventFactory = XMLEventFactory.newInstance();
		XMLEvent end = xmlEventFactory.createDTD("\n");
		XMLEvent tab = xmlEventFactory.createDTD("\t");
		// Create Start node
		StartElement sElement = xmlEventFactory.createStartElement("", "", element);
		eventWriter.add(tab);
		eventWriter.add(sElement);
		// Create Content
		Characters characters = xmlEventFactory.createCharacters(value);
		eventWriter.add(characters);
		// Create End node
		EndElement eElement = xmlEventFactory.createEndElement("", "", element);
		eventWriter.add(eElement);
		eventWriter.add(end);

	}

}
