package com.rationalcoding.xml;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.rationalcoding.dto.StudentDTO;

@Service
public class StudentDAO {
	private JdbcTemplate jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}
	
	public int importStudentRecords(final List<StudentDTO> students) {

		String insertSql = "INSERT INTO student_grades (student_id,course_id,course_grade,date_modified) VALUES (?, ?, ?,?)";

		final Date currentDate = new Date(System.currentTimeMillis());

		int[] results = jdbcTemplate.batchUpdate(insertSql, new BatchPreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement studentInsertPreparedStatement, int i) throws SQLException {
				StudentDTO student = students.get(i);
				studentInsertPreparedStatement.setString(1, student.getStudentId());
				studentInsertPreparedStatement.setString(2, student.getCourseId());
				studentInsertPreparedStatement.setString(3, student.getCourseGrade());
				studentInsertPreparedStatement.setDate(4, currentDate);
			}

			@Override
			public int getBatchSize() {
				return students.size();
			}
		});
		int totalRecordsInserted = 0;
		for(int count :results){
			totalRecordsInserted += count;
		}
		return totalRecordsInserted;
	}

}
