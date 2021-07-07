package com.systex.sysgateii.autosvr.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;

/*
 * 20201028
 * MatsudairaSyume
 * utility for date,time parsing and processing
 */
public class DateTimeUtil {
	public static boolean MinDurationToCurrentTime(int duration, String previousDateTime) {
		boolean larger = false;
		if (previousDateTime == null || previousDateTime.trim().length() == 0 || duration <= 0)
			return larger;
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.[SSS][SS][S]");
		LocalDateTime fromtime = LocalDateTime.parse(previousDateTime, formatter);
		LocalDateTime time = LocalDateTime.now();
		Duration durationobj = Duration.between(fromtime, time);
		if (durationobj.toMinutes() >= duration)
			larger = true;
		return larger;
	}
}
