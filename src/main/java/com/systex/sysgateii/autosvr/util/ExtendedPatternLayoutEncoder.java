/**
 * 
 */
package com.systex.sysgateii.autosvr.util;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;

/**
 * @author MatsidairaSyume
 *
 */
public class ExtendedPatternLayoutEncoder extends PatternLayoutEncoder {
	@Override
	public void start() {
		// put your converter
		PatternLayout.defaultConverterMap.put("process_id", ProcessIdConverter.class.getName());
		super.start();
	}
}
