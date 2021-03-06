/*
 * Copyright 2009 NCHOVY
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.araqne.cron.test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.junit.Test;
import org.araqne.cron.Schedule;
import org.araqne.cron.impl.NextOccurenceCalculator;
import org.osgi.framework.InvalidSyntaxException;

import static org.junit.Assert.*;

public class NextOccurTest {

	@SuppressWarnings("deprecation")
	@Test
	public void testNext1() throws InvalidSyntaxException, Exception {

		Date date = new Date(Date.parse("Feb 27, 2009 8:14 PM"));
		System.out.println("============ test1 : " + date + "=============");
		Schedule sche1 = new Schedule.Builder("awef").build();
		Schedule sche2 = new Schedule.Builder("awef").buildHourly();
		Schedule sche3 = new Schedule.Builder("awef").buildDaily();
		Schedule sche4 = new Schedule.Builder("awef").buildWeekly();
		Schedule sche5 = new Schedule.Builder("awef").buildYearly();
		Schedule sche6 = new Schedule.Builder("awef").build("*/30 * * * *");
		Schedule sche7 = new Schedule.Builder("awef").build("3,17,20 * * * *");
		Schedule sche8 = new Schedule.Builder("awef").build("3,17,20 */3 * * *");
		Schedule sche9 = new Schedule.Builder("awef").build("0 0 5,28 * *");
		Schedule sche10 = new Schedule.Builder("awef").build("0 0 5,28 3 *");
		Schedule sche11 = new Schedule.Builder("awef").build("0 0 5,28 3 1");
		Schedule sche12 = new Schedule.Builder("awef").build("* * 31 * 0");

		SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.ENGLISH);

		assertEquals(sdf.format(NextOccurenceCalculator.getNextOccurence(sche1, date)), "Fri Feb 27 20:14:00 2009");
		assertEquals(sdf.format(NextOccurenceCalculator.getNextOccurence(sche2, date)), "Fri Feb 27 21:00:00 2009");
		assertEquals(sdf.format(NextOccurenceCalculator.getNextOccurence(sche3, date)), "Sat Feb 28 00:00:00 2009");
		assertEquals(sdf.format(NextOccurenceCalculator.getNextOccurence(sche4, date)), "Sun Mar 01 00:00:00 2009");
		assertEquals(sdf.format(NextOccurenceCalculator.getNextOccurence(sche5, date)), "Fri Jan 01 00:00:00 2010");
		assertEquals(sdf.format(NextOccurenceCalculator.getNextOccurence(sche6, date)), "Fri Feb 27 20:30:00 2009");
		assertEquals(sdf.format(NextOccurenceCalculator.getNextOccurence(sche7, date)), "Fri Feb 27 20:17:00 2009");
		assertEquals(sdf.format(NextOccurenceCalculator.getNextOccurence(sche8, date)), "Fri Feb 27 21:03:00 2009");
		assertEquals(sdf.format(NextOccurenceCalculator.getNextOccurence(sche9, date)), "Sat Feb 28 00:00:00 2009");
		assertEquals(sdf.format(NextOccurenceCalculator.getNextOccurence(sche10, date)), "Thu Mar 05 00:00:00 2009");
		assertEquals(sdf.format(NextOccurenceCalculator.getNextOccurence(sche11, date)), "Mon Mar 02 00:00:00 2009");
		assertEquals(sdf.format(NextOccurenceCalculator.getNextOccurence(sche12, date)), "Sun Mar 01 00:00:00 2009");
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testNext2() throws InvalidSyntaxException, Exception {

		Date date = new Date(Date.parse("Aug 6, 2009 1:11 AM"));
		System.out.println("============ test2 : " + date + "=============");
		Schedule sche1 = new Schedule.Builder("awef").build();
		Schedule sche2 = new Schedule.Builder("awef").buildHourly();
		Schedule sche3 = new Schedule.Builder("awef").buildDaily();
		Schedule sche4 = new Schedule.Builder("awef").buildWeekly();
		Schedule sche5 = new Schedule.Builder("awef").buildYearly();
		Schedule sche6 = new Schedule.Builder("awef").build("*/30 * * * *");
		Schedule sche7 = new Schedule.Builder("awef").build("3,17,20 * * * *");
		Schedule sche8 = new Schedule.Builder("awef").build("3,17,20 */3 * * *");
		Schedule sche9 = new Schedule.Builder("awef").build("0 0 5,28 * *");
		Schedule sche10 = new Schedule.Builder("awef").build("0 0 5,28 3 *");
		Schedule sche11 = new Schedule.Builder("awef").build("0 0 5,28 3 1");

		SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.ENGLISH);

		assertEquals(sdf.format(NextOccurenceCalculator.getNextOccurence(sche1, date)), "Thu Aug 06 01:11:00 2009");
		assertEquals(sdf.format(NextOccurenceCalculator.getNextOccurence(sche2, date)), "Thu Aug 06 02:00:00 2009");
		assertEquals(sdf.format(NextOccurenceCalculator.getNextOccurence(sche3, date)), "Fri Aug 07 00:00:00 2009");
		assertEquals(sdf.format(NextOccurenceCalculator.getNextOccurence(sche4, date)), "Sun Aug 09 00:00:00 2009");
		assertEquals(sdf.format(NextOccurenceCalculator.getNextOccurence(sche5, date)), "Fri Jan 01 00:00:00 2010");
		assertEquals(sdf.format(NextOccurenceCalculator.getNextOccurence(sche6, date)), "Thu Aug 06 01:30:00 2009");
		assertEquals(sdf.format(NextOccurenceCalculator.getNextOccurence(sche7, date)), "Thu Aug 06 01:17:00 2009");
		assertEquals(sdf.format(NextOccurenceCalculator.getNextOccurence(sche8, date)), "Thu Aug 06 03:03:00 2009");
		assertEquals(sdf.format(NextOccurenceCalculator.getNextOccurence(sche9, date)), "Fri Aug 28 00:00:00 2009");
		assertEquals(sdf.format(NextOccurenceCalculator.getNextOccurence(sche10, date)), "Fri Mar 05 00:00:00 2010");
		assertEquals(sdf.format(NextOccurenceCalculator.getNextOccurence(sche11, date)), "Mon Mar 01 00:00:00 2010");
	}

	@Test
	public void testDaily() throws InvalidSyntaxException, Exception {
		Date date1 = new Date();
		Schedule sche = new Schedule.Builder("aef").buildDaily();

		System.out.println("============ daily : " + date1 + "=============");
		for (int i = 0; i < 20; i++) {
			date1 = NextOccurenceCalculator.getNextOccurence(sche, date1);
			System.out.println(date1);
			date1 = new Date(date1.getTime() + 60 * 1000);
		}
	}

	@Test
	public void testWeekly() throws InvalidSyntaxException, Exception {
		Date date1 = new Date();
		Schedule sche = new Schedule.Builder("aef").buildWeekly();

		System.out.println("============ weekly : " + date1 + "=============");
		for (int i = 0; i < 20; i++) {
			date1 = NextOccurenceCalculator.getNextOccurence(sche, date1);
			System.out.println(date1);
			date1 = new Date(date1.getTime() + 60 * 1000);
		}
	}

	@Test
	public void testMondayAnd10th() throws InvalidSyntaxException, Exception {
		Date date1 = new Date();
		Schedule sche = new Schedule.Builder("aef").build("0 0 */10 * 1");

		System.out.println("============ mondayAnd10th : " + date1 + "=============");
		for (int i = 0; i < 20; i++) {
			date1 = NextOccurenceCalculator.getNextOccurence(sche, date1);
			System.out.println(date1);
			date1 = new Date(date1.getTime() + 60 * 1000);
		}
	}

	@SuppressWarnings("deprecation")
	@Test
	public void test31th() throws InvalidSyntaxException, Exception {
		Date date1 = new Date(Date.parse("Feb 27, 2009 8:14 PM"));
		Schedule sche = new Schedule.Builder("aef").build("0 0 31 * *");

		System.out.println("============ 31th : " + date1 + "=============");
		for (int i = 0; i < 20; i++) {
			date1 = NextOccurenceCalculator.getNextOccurence(sche, date1);
			System.out.println(date1);
			Calendar cal = Calendar.getInstance();
			cal.setTime(date1);
			assertTrue(cal.get(Calendar.DAY_OF_MONTH) == 31);
			date1 = new Date(date1.getTime() + 60 * 1000);
		}
	}
}
