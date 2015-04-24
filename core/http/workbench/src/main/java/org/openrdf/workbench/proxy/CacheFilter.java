/* 
 * Licensed to Aduna under one or more contributor license agreements.  
 * See the NOTICE.txt file distributed with this work for additional 
 * information regarding copyright ownership. 
 *
 * Aduna licenses this file to you under the terms of the Aduna BSD 
 * License (the "License"); you may not use this file except in compliance 
 * with the License. See the LICENSE.txt file distributed with this work 
 * for the full License.
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
 * implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.openrdf.workbench.proxy;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource cache expiry filter for Tomcat 6, based on code authored by Saket
 * Kumar.
 * 
 * @see <a href="http://bit.ly/tomcat-6-caching">Enable Caching in Tomcat 6</a>
 * @author Dale Visser
 */
public class CacheFilter implements Filter {

	private static final Logger LOGGER = LoggerFactory.getLogger(CacheFilter.class);

	/**
	 * HTTP header key for controlling caching of resources.
	 */
	private final static String CACHE_CONTROL = "Cache-Control";

	/**
	 * Maximum allowed expiry lifetime in seconds, set to one year according to
	 * the advice in RFC 2616.
	 * 
	 * @see <a href="https://www.ietf.org/rfc/rfc2616.txt">RFC 2616: HTTP/1.1</a>
	 */
	public final static long MAX_EXPIRY = (365 * 24 + 6) * 60 * 60;

	/**
	 * Minimum allowed expiry lifetime, zero, which corresponds to not caching at
	 * all.
	 */
	public final static long MIN_EXPIRY = 0;

	private Long expiry = null;

	/**
	 * Set a maximum expiry Cache-Control header applicable to the client and to
	 * intermediate caching servers.
	 */
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
		throws IOException, ServletException
	{
		if (null != expiry) {
			((HttpServletResponse)res).setHeader(CACHE_CONTROL, "max-age=" + expiry + ", public");
		}
		chain.doFilter(req, res);
	}

	/**
	 * Parse the Cache-Control configuration parameter as a long integer, and set
	 * the filter expiry value, modulo the minimum and maximum expiry
	 * constraints. If the configuration parameter is not present, or not a valid
	 * long integer value, then no Cache-Control headers will be applied by the
	 * filter.
	 * 
	 * @see #MIN_EXPIRY
	 * @see #MAX_EXPIRY
	 */
	@Override
	public void init(FilterConfig config)
		throws ServletException
	{
		try {
			long value = Long.parseLong(config.getInitParameter(CACHE_CONTROL));
			value = Math.max(MIN_EXPIRY, value);
			value = Math.min(value, MAX_EXPIRY);
			expiry = value; // create object here
		}
		catch (NumberFormatException nfe) {
			LOGGER.warn("Failed to parse " + CACHE_CONTROL + " value.", nfe);
			expiry = null;
		}
	}

	/**
	 * Make stored references available for garbage collection.
	 */
	@Override
	public void destroy() {
		expiry = null;
	}
}