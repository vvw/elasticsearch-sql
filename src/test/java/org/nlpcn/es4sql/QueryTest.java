package org.nlpcn.es4sql;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;
import org.junit.Assert;
import org.junit.Test;
import org.nlpcn.es4sql.exception.SqlParseException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.nlpcn.es4sql.TestsConstants.DATE_FORMAT;
import static org.nlpcn.es4sql.TestsConstants.TEST_INDEX;


public class QueryTest {
	
	private SearchDao searchDao = new SearchDao() ;

	@Test
	public void equallityTest() throws SqlParseException {
		SearchHits response = query(String.format("select * from %s where city = 'Nogal' LIMIT 1000", TEST_INDEX));
		SearchHit[] hits = response.getHits();

		// assert the results is correct according to accounts.json data.
		Assert.assertEquals(1, response.getTotalHits());
		Assert.assertEquals("Nogal", hits[0].getSource().get("city"));
	}


	// TODO search 'quick fox' still matching 'quick fox brown' this is wrong behavior.
	@Test
	public void equallityTest_phrase() throws SqlParseException {
		SearchHits response = query(String.format("SELECT * FROM %s WHERE phrase = 'quick fox here' LIMIT 1000", TEST_INDEX));
		SearchHit[] hits = response.getHits();

		// assert the results is correct according to accounts.json data.
		Assert.assertEquals(1, response.getTotalHits());
		Assert.assertEquals("quick fox here", hits[0].getSource().get("phrase"));
	}


	@Test
	public void greaterThanTest() throws IOException, SqlParseException {
		int someAge = 25;
		SearchHits response = query(String.format("SELECT * FROM %s WHERE age > %s LIMIT 1000", TEST_INDEX, someAge));
		SearchHit[] hits = response.getHits();
		for(SearchHit hit : hits) {
			int age = (int) hit.getSource().get("age");
			assertThat(age, greaterThan(someAge));
		}
	}

	@Test
	public void greaterThanOrEqualTest() throws IOException, SqlParseException {
		int someAge = 25;
		SearchHits response = query(String.format("SELECT * FROM %s WHERE age >= %s LIMIT 1000", TEST_INDEX, someAge));
		SearchHit[] hits = response.getHits();

		boolean isEqualFound = false;
		for(SearchHit hit : hits) {
			int age = (int) hit.getSource().get("age");
			assertThat(age, greaterThanOrEqualTo(someAge));

			if(age == someAge)
				isEqualFound = true;
		}

		Assert.assertTrue(String.format("at least one of the documents need to contains age equal to %s", someAge), isEqualFound);
	}


	@Test
	public void lessThanTest() throws IOException, SqlParseException {
		int someAge = 25;
		SearchHits response = query(String.format("SELECT * FROM %s WHERE age < %s LIMIT 1000", TEST_INDEX, someAge));
		SearchHit[] hits = response.getHits();
		for(SearchHit hit : hits) {
			int age = (int) hit.getSource().get("age");
			assertThat(age, lessThan(someAge));
		}
	}

	@Test
	public void lessThanOrEqualTest() throws IOException, SqlParseException {
		int someAge = 25;
		SearchHits response = query(String.format("SELECT * FROM %s WHERE age <= %s LIMIT 1000", TEST_INDEX, someAge));
		SearchHit[] hits = response.getHits();

		boolean isEqualFound = false;
		for(SearchHit hit : hits) {
			int age = (int) hit.getSource().get("age");
			assertThat(age, lessThanOrEqualTo(someAge));

			if(age == someAge)
				isEqualFound = true;
		}

		Assert.assertTrue(String.format("at least one of the documents need to contains age equal to %s", someAge), isEqualFound);
	}



	@Test
	public void orTest() throws IOException, SqlParseException {
		SearchHits response = query(String.format("SELECT * FROM %s WHERE gender='F' OR gender='M' LIMIT 1000", TEST_INDEX));
		// Assert all documents from accounts.json is returned.
		Assert.assertEquals(1000, response.getTotalHits());
	}


	@Test
	public void andTest() throws IOException, SqlParseException {
		SearchHits response = query(String.format("SELECT * FROM %s WHERE age=32 AND gender='M' LIMIT 1000", TEST_INDEX));
		SearchHit[] hits = response.getHits();
		for(SearchHit hit : hits) {
			Assert.assertEquals(32, hit.getSource().get("age"));
			Assert.assertEquals("M", hit.getSource().get("gender"));
		}
	}



	@Test
	public void likeTest() throws IOException, SqlParseException {
		SearchHits response = query(String.format("SELECT * FROM %s WHERE firstname LIKE 'amb%%' LIMIT 1000", TEST_INDEX));
		SearchHit[] hits = response.getHits();

		// assert the results is correct according to accounts.json data.
		Assert.assertEquals(1, response.getTotalHits());
		Assert.assertEquals("Amber", hits[0].getSource().get("firstname"));
	}


	@Test
	public void limitTest() throws IOException, SqlParseException {
		SearchHits response = query(String.format("SELECT * FROM %s LIMIT 30", TEST_INDEX));
		SearchHit[] hits = response.getHits();

		// assert the results is correct according to accounts.json data.
		Assert.assertEquals(30, hits.length);
	}
	
	@Test
	public void betweenTest() throws IOException, SqlParseException {
		int min = 27;
		int max = 30;
		SearchHits response = query(String.format("SELECT * FROM %s WHERE age BETWEEN %s AND %s LIMIT 1000", TEST_INDEX, min, max));
		SearchHit[] hits = response.getHits();
		for(SearchHit hit : hits) {
			int age = (int) hit.getSource().get("age");
			assertThat(age, allOf(greaterThanOrEqualTo(min), lessThanOrEqualTo(max)));
		}
	}


	/*
	TODO when using not between on some field, documents that not contains this
	 field will return as well, That may considered a Wrong behaivor.
	 */
	@Test
	public void notBetweenTest() throws IOException, SqlParseException {
		int min = 20;
		int max = 37;
		SearchHits response = query(String.format("SELECT * FROM %s WHERE age NOT BETWEEN %s AND %s LIMIT 1000", TEST_INDEX, min, max));
		SearchHit[] hits = response.getHits();
		for(SearchHit hit : hits) {
			Map<String, Object> source = hit.getSource();

			// ignore document which not contains the age field.
			if(source.containsKey("age")) {
				int age = (int) hit.getSource().get("age");
				assertThat(age, not(allOf(greaterThanOrEqualTo(min), lessThanOrEqualTo(max))));
			}
		}
	}
	
	@Test
	public void inTest() throws IOException, SqlParseException{
		SearchHits response = query(String.format("SELECT age FROM %s WHERE age IN (20, 22) LIMIT 1000", TEST_INDEX));
		SearchHit[] hits = response.getHits();
		for(SearchHit hit : hits) {
			int age = (int) hit.field("age").getValue();
			assertThat(age, isOneOf(20, 22));
		}
	}

	@Test
	public void inTestWithStrings() throws IOException, SqlParseException{
		SearchHits response = query(String.format("SELECT phrase FROM %s WHERE phrase IN ('quick fox here', 'fox brown') LIMIT 1000", TEST_INDEX));
		SearchHit[] hits = response.getHits();
		Assert.assertEquals(2, response.getTotalHits());
		for(SearchHit hit : hits) {
			String phrase = hit.field("phrase").getValue();
			assertThat(phrase, isOneOf("quick fox here", "fox brown"));
		}
	}


	/* TODO when using not in on some field, documents that not contains this
	field will return as well, That may considered a Wrong behaivor.
	*/
	@Test
	public void notInTest() throws IOException, SqlParseException{
		SearchHits response = query(String.format("SELECT age FROM %s WHERE age NOT IN (20, 22) LIMIT 1000", TEST_INDEX));
		SearchHit[] hits = response.getHits();
		for(SearchHit hit : hits) {
			Map<String, SearchHitField> fields = hit.fields();

			// ignore document which not contains the age field.
			if(fields.containsKey("age")) {
				int age = (int) hit.field("age").getValue();
				assertThat(age, not(isOneOf(20, 22)));
			}
		}
	}
	
	
	@Test
	public void dateSearch() throws IOException, SqlParseException, ParseException {
		Calendar dateToCompare = Calendar.getInstance();
		dateToCompare.set(Calendar.YEAR, 2014);
		dateToCompare.set(Calendar.MONTH, 8);
		dateToCompare.set(Calendar.DAY_OF_MONTH, 18);

		SearchHits response = query(String.format("SELECT insert_time FROM %s/online WHERE insert_time < '2014-08-18'", TEST_INDEX));
		SearchHit[] hits = response.getHits();
		for(SearchHit hit : hits) {
			Map<String, SearchHitField> fields = hit.fields();
			Calendar insertTime = Calendar.getInstance();
			insertTime.setTime(new SimpleDateFormat(DATE_FORMAT).parse((String) fields.get("insert_time").getValue()));
			Assert.assertTrue("insert_time must be smaller then 2014-08-18", insertTime.before(dateToCompare));
		}
	}
	
	@Test
	public void dateBetweenSearch() throws IOException, SqlParseException{
		SearchRequestBuilder select = searchDao.explan("select insert_time from online where insert_time between '2014-08-18' and '2014-08-21' limit 3");
		System.out.println(select);
	}
	
	/**
	 * 是否存在查询
	 * @throws IOException
	 * @throws SqlParseException
	 */
	@Test
	public void missFilterSearch() throws IOException, SqlParseException{
		SearchRequestBuilder select = searchDao.explan("select insert_time from online where insert_time is not miss order by _score desc limit 10");
		System.out.println(select);
	}
	
	@Test
	public void missQuerySearch() throws IOException, SqlParseException{
		SearchRequestBuilder select = searchDao.explan("select insert_time from online where insert_time is not miss limit 10");
		System.out.println(select);
	}
	

	@Test
	public void boolQuerySearch() throws IOException, SqlParseException{
		SearchRequestBuilder select = searchDao.explan("select * from bank where (gender='m' and (age> 25 or account_number>5)) or (gender='w' and (age>30 or account_number < 8)) and email is not miss order by age,_score desc limit 10 ");
		System.out.println(select);
	}
	


	@Test
	public void countSearch() throws IOException, SqlParseException{
		SearchRequestBuilder select = searchDao.explan("select count(*) from bank where (gender='m' and (age> 25 or account_number>5)) or (gender='w' and (age>30 or account_number < 8)) and email is not miss");
		System.out.println(select);
	}
	
	/**
	 * table have '.' and type test
	 * @throws IOException
	 * @throws SqlParseException
	 */
	@Test
	public void searchTableTest() throws IOException, SqlParseException{
		SearchRequestBuilder select = searchDao.explan("select count(*) from doc/accounts,bank/doc limit 10");
		System.out.println(select);
	}


	private SearchHits query(String query) throws SqlParseException {
		SearchDao searchDao = MainTestSuite.getSearchDao();
		SearchRequestBuilder select = searchDao.explan(query);
		return select.get().getHits();
	}
}
