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
package org.araqne.api;

import java.util.Date;

public class PackageVersionHistory implements Comparable<PackageVersionHistory> {
	private Version version;
	private Date lastUpdated;

	public PackageVersionHistory(Version version, Date lastUpdated) {
		this.version = version;
		this.lastUpdated = lastUpdated;
	}

	public Version getVersion() {
		return version;
	}

	public void setVersion(Version version) {
		this.version = version;
	}

	public Date getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	@Override
	public String toString() {
		return String.format("PackageVersionHistory{version: %s, last updated: %s}", version,
				lastUpdated);
	}

	@Override
	public int compareTo(PackageVersionHistory o) {
		if (version.equals(o.version)) {
			return lastUpdated.compareTo(o.lastUpdated);
		} else {
			return version.compareTo(o.version);
		}
	}

}