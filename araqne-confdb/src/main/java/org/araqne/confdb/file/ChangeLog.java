/*
 * Copyright 2011 Future Systems, Inc.
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
package org.araqne.confdb.file;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.araqne.api.CollectionTypeHint;
import org.araqne.codec.EncodingRule;
import org.araqne.confdb.CommitLog;
import org.araqne.confdb.CommitOp;
import org.araqne.confdb.ConfigChange;

/**
 * database level change set log
 * 
 * @author xeraph
 * 
 */
class ChangeLog implements CommitLog {
	private long rev;

	private Date created = new Date();

	private String committer;

	private String message;

	private int manifestId;

	@CollectionTypeHint(ConfigChange.class)
	private List<ConfigChange> changeset = new ArrayList<ConfigChange>();

	@Override
	public long getRev() {
		return rev;
	}

	public void setRev(long rev) {
		this.rev = rev;
	}

	@Override
	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	@Override
	public String getCommitter() {
		return committer;
	}

	public void setCommitter(String committer) {
		this.committer = committer;
	}

	@Override
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public List<ConfigChange> getChangeSet() {
		return changeset;
	}

	public void setChangeSet(List<ConfigChange> changeset) {
		this.changeset = changeset;
	}

	public int getManifestId() {
		return manifestId;
	}

	public void setManifestId(int manifestId) {
		this.manifestId = manifestId;
	}

	public byte[] serialize() {
		Map<String, Object> m = new HashMap<String, Object>();
		// "rev" is doc id of revision log (do not require serialize)
		m.put("created", created);
		m.put("committer", committer);
		m.put("msg", message);
		m.put("manifest_id", manifestId);
		m.put("col_names", serializeCollectionNames());
		m.put("changeset", serializeChangeset());

		ByteBuffer bb = ByteBuffer.allocate(EncodingRule.lengthOf(m));
		EncodingRule.encode(bb, m);
		return bb.array();
	}

	private List<Object> serializeChangeset() {
		List<Object> l = new ArrayList<Object>(changeset.size());
		for (ConfigChange c : changeset) {
			l.add(new Object[] { c.getOperation().getCode(), c.getColId(), c.getDocId() });
		}
		return l;
	}

	private List<Object> serializeCollectionNames() {
		Map<String, Integer> m = new HashMap<String, Integer>();
		for (ConfigChange c : changeset)
			m.put(c.getColName(), c.getColId());

		List<Object> cols = new ArrayList<Object>();
		for (String name : m.keySet()) {
			cols.add(new Object[] { m.get(name), name });
		}
		return cols;
	}

	private static Map<Integer, String> parseCollectionNames(Object[] l) {
		Map<Integer, String> m = new HashMap<Integer, String>();
		if (l == null)
			return m;

		for (Object o : l) {
			Object[] arr = (Object[]) o;
			m.put((Integer) arr[0], (String) arr[1]);
		}
		return m;
	}

	/**
	 * get manifest id from change log binary. manual parsing for speed-up
	 */
	public static int getManifest(byte[] b) {
		ByteBuffer bb = ByteBuffer.wrap(b);
		bb.get(); // type (9)
		EncodingRule.decodeRawNumber(bb); // skip map length part

		// enumerate keys of map
		while (true) {
			// parse map key
			String s = EncodingRule.decodeString(bb);
			if (s.equals("manifest_id")) {
				return EncodingRule.decodeInt(bb);
			} else {
				// parse map value
				byte type = bb.get();
				switch(type) {
				case EncodingRule.NULL_TYPE:
					break;
				case EncodingRule.BOOLEAN_TYPE:
					bb.get();
					break;
				case EncodingRule.INT16_TYPE:
					bb.position((int) (bb.position() + 2));
					break;
				case EncodingRule.INT32_TYPE:
					bb.position((int) (bb.position() + 4));
					break;
				case EncodingRule.INT64_TYPE:
					bb.position((int) (bb.position() + 8));
					break;
				case EncodingRule.DATE_TYPE:
					bb.position((int) (bb.position() + 8));
					break;
				case EncodingRule.IP4_TYPE:
					bb.position((int) (bb.position() + 4));
					break;
				case EncodingRule.IP6_TYPE:
					bb.position((int) (bb.position() + 16));
					break;
				case EncodingRule.ZINT16_TYPE:
					bb.position((int) (bb.position() + 2));
					break;
				case EncodingRule.ZINT32_TYPE:
					bb.position((int) (bb.position() + 4));
					break;
				case EncodingRule.ZINT64_TYPE:
					bb.position((int) (bb.position() + 8));
					break;
				case EncodingRule.FLOAT_TYPE:
					bb.position((int) (bb.position() + 4));
					break;
				case EncodingRule.DOUBLE_TYPE:
					bb.position((int) (bb.position() + 8));
					break;
					
				case EncodingRule.BLOB_TYPE:
				case EncodingRule.MAP_TYPE:
				case EncodingRule.STRING_TYPE:
				case EncodingRule.ARRAY_TYPE:
					long l = EncodingRule.decodeRawNumber(bb);
					bb.position((int) (bb.position() + l));
					break;
				default:
					throw new IllegalStateException("unexpected type: " + type);
				}
			}
		}
	}

	public static ChangeLog deserialize(byte[] b) {
		ByteBuffer bb = ByteBuffer.wrap(b);
		Map<String, Object> m = EncodingRule.decodeMap(bb);

		ChangeLog c = new ChangeLog();
		c.setCreated((Date) m.get("created"));
		c.setCommitter((String) m.get("committer"));
		c.setMessage((String) m.get("msg"));
		c.setManifestId((Integer) m.get("manifest_id"));

		Map<Integer, String> colNames = parseCollectionNames((Object[]) m.get("col_names"));
		c.setChangeSet(parseConfigChanges((Object[]) m.get("changeset"), colNames));

		return c;
	}

	private static List<ConfigChange> parseConfigChanges(Object[] list, Map<Integer, String> colNames) {
		List<ConfigChange> l = new ArrayList<ConfigChange>(list.length);
		for (Object o : list) {
			if (o instanceof Map) {
				// legacy format support
				@SuppressWarnings("unchecked")
				Map<String, Object> m = (Map<String, Object>) o;
				ConfigChange c = new ConfigChange();
				c.setOperation(CommitOp.valueOf((String) m.get("operation")));
				c.setColId((Integer) m.get("col_id"));
				c.setColName(colNames.get(c.getColId()));
				c.setDocId((Integer) m.get("doc_id"));
				l.add(c);
			} else {
				Object[] arr = (Object[]) o;
				ConfigChange c = new ConfigChange();
				c.setOperation(CommitOp.parse((Integer) arr[0]));
				c.setColId((Integer) arr[1]);
				c.setColName(colNames.get(c.getColId()));
				c.setDocId((Integer) arr[2]);
				l.add(c);
			}
		}
		return l;
	}

	@Override
	public String toString() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (ConfigChange c : changeset) {
			if (i++ != 0)
				sb.append(", ");
			sb.append(c.toString());
		}

		String createdDate = ", date=";
		if (created != null)
			createdDate += dateFormat.format(created);

		return "rev=" + rev + createdDate + ", committer=" + committer + ", msg=" + message + ", changeset=["
				+ sb.toString() + "]";
	}
}
