package com.systex.sysgateii.autosvr.dao;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.systex.sysgateii.autosvr.comm.Constants;
import com.systex.sysgateii.autosvr.util.DataConvert;
import com.systex.sysgateii.autosvr.util.Des;

public class GwDao {
	private static Logger log = LoggerFactory.getLogger(GwDao.class);
	// test
	private String selurl = "jdbc:db2://172.16.71.128:50000/BISDB";
	private String seluser = "BIS_USER";
	private String selpass = "bisuser";
	// test
	private Connection selconn = null;
	private PreparedStatement preparedStatement = null;
	private Vector<String> columnNames = null;
	private Vector<Integer> columnTypes = null;
	private ResultSet rs = null;
	private boolean verbose = true;
	private String sfn = "";
	private Vector<String> tbsdytblcolumnNames = null;
	private Vector<Integer> tbsdytblcolumnTypes = null;
	private ResultSet tbsdytblrs = null;
	/**
	 * 
	 */
	public GwDao() throws Exception {
		super();
		log.debug("using url:{} user:{} pass:{} start to connect to Database", this.selurl, this.seluser, this.selpass);
		selconn = getDB2Connection(selurl, seluser, selpass);
		log.debug("Connected to database successfully...");
	}

	public GwDao(String selurl, String seluser, String selpass, boolean v) throws Exception {
		super();
		this.selurl = selurl;
		this.seluser = seluser;
		this.selpass = selpass;
//		log.debug("Connecting to a selected database...");
		selconn = getDB2Connection(selurl, seluser, selpass);
//		log.debug("Connected selected database successfully...");
		this.verbose = v;
	}

	//20210118 MatsudairaSyuMe delete change for vulnerability scanning sql injection defense
	public int UPSERT(String fromTblName, String field, String updval, String keyname, String selkeyval)
			throws Exception {
		columnNames = new Vector<String>();
		columnTypes = new Vector<Integer>();
		if (fromTblName == null || fromTblName.trim().length() == 0 || field == null || field.trim().length() == 0
				|| keyname == null || keyname.trim().length() == 0)
			throw new Exception("given table name or field or keyname error =>" + fromTblName);
		log.debug(String.format("Select from table %s... where %s=%s", fromTblName, keyname, selkeyval));
		String keyset = "";
		String[] keynameary = keyname.split(",");
		String[] keyvalueary = selkeyval.split(",");
//		String[] keyvaluearynocomm = selkeyval.split(",");20210505 MatsudairaSyuMe Access Control: Database
		if (keynameary.length != keyvalueary.length)
			throw new Exception("given fields keyname can't correspond to keyvfield =>keynames [" + keyname + "] selkeyval [" + selkeyval + "]");
		else {
			for (int i = 0; i < keynameary.length; i++)
//				keyset = keyset + keynameary[i] + " = " + "?" + (i == (keynameary.length - 1) ? "" : " and ");//20210505 MatsudairaSyuMe Access Control: Database
				keyset = keyset + keynameary[i] + " = " + keyvalueary[i] + (i == (keynameary.length - 1) ? "" : " and ");
/*20210505 MatsudairaSyuMe Access Control: Database
  			for (int i = 0; i < keyvaluearynocomm.length; i++) {
				int s = keyvalueary[i].indexOf('\'');
				int l = keyvalueary[i].lastIndexOf('\'');
				if (s != l && s >= 0 && l >= 0 && s < l)
					keyvaluearynocomm[i] = keyvalueary[i].substring(s + 1, l);
			}
			*/
		}
		String selstr = "SELECT " + keyname + "," + field + " FROM " + fromTblName + " where " + keyset;
		//20210122 MatsudairaSyuMe
		String wowstr = Des.encode(Constants.DEFKNOCKING, selstr);
		log.debug("UPSERT selstr [{}]-->[{}]", selstr, wowstr);
		//----
		log.debug("update value [{}]", updval);
/*		String[] valary = updval.split(",");
		for (int i = 0; i < valary.length; i++) {
			int s = valary[i].indexOf('\'');
			int l = valary[i].lastIndexOf('\'');
			if (s != l && s >= 0 && l >= 0 && s < l)
				valary[i] = valary[i].substring(s + 1, l);
		}
*/
		//20210122 MatsudairaSyuMe
		/*20210505 MatsudairaSyuMe Access Control: Database
//		PreparedStatement stmt = selconn.prepareStatement(Des.decodeValue(Constants.DEFKNOCKING, wowstr));
		//----
		for (int i = 0; i < keyvalueary.length; i++) {
			if (keyvalueary[i].indexOf('\'') > -1 )
				stmt.setString(i + 1, keyvaluearynocomm[i]);
			else
				stmt.setInt(i + 1, Integer.valueOf(keyvaluearynocomm[i]));
		}
		ResultSet tblrs = stmt.executeQuery();
		*/
		Statement stmt = selconn.createStatement();
		ResultSet tblrs = stmt.executeQuery(Des.decodeValue(Constants.DEFKNOCKING, wowstr));
		int type = -1;
		int row = 0;
		//verbose=true;
		if (tblrs != null) {
			ResultSetMetaData rsmd = tblrs.getMetaData();
			int columnCount = 0;
			boolean updateMode = false;
			log.debug("table request fields {}", field);
			if (tblrs.next()) {
				log.debug("update mode");
				updateMode = true;
			} else
				log.debug("insert mode");
			while (columnCount < rsmd.getColumnCount()) {
				columnCount++;
				type = rsmd.getColumnType(columnCount);
				if (updateMode && field.indexOf(rsmd.getColumnName(columnCount).trim()) > -1) {
					columnNames.add(rsmd.getColumnName(columnCount));
					columnTypes.add(type);
					if (verbose)
						log.debug("updateMode ColumnName={} ColumnTypeName={} ", rsmd.getColumnName(columnCount), rsmd.getColumnTypeName(columnCount) );
				} else if (!updateMode && (field.indexOf(rsmd.getColumnName(columnCount).trim()) > -1 || keyname.indexOf(rsmd.getColumnName(columnCount).trim()) > -1)) {
					columnNames.add(rsmd.getColumnName(columnCount));
					columnTypes.add(type);					
					if (verbose)
						log.debug("insert Mode ColumnName={} ColumnTypeName={} ", rsmd.getColumnName(columnCount), rsmd.getColumnTypeName(columnCount) );
				}
			}
			String colNames = "";
			//String vals = "";20210505 MAtsudairaSyuMe Access Control: Database
			String vals = selkeyval + "," + updval;
			String updcolNames = "";
			//String updvals = ""; 20210505 MatsudairaSyuMe Access Control: Database
			String updvals = updval;
//			log.debug("given vals {} keyvaluearynocomm {}", Arrays.toString(valary), Arrays.toString(keyvaluearynocomm));20210505 MatsudairaSyuMe Access Control: Database

			for (columnCount = 0; columnCount < columnNames.size(); columnCount++) {
				if (colNames.trim().length() > 0) {
					colNames = colNames + "," + columnNames.get(columnCount);
					//vals = vals + ",?";20210505 MatsudairaSyuMe Access Control: Database
				} else {
					colNames = columnNames.get(columnCount);
					//vals = "?";20210505 MatsudairaSyuMe Access Control: Database
				}
				if (updateMode) {
					if (updcolNames.trim().length() > 0) {
						updcolNames = updcolNames + "," + columnNames.get(columnCount);
						//updvals = updvals + ",?";20210505 MatsudairaSyuMe Access Control: Database
					} else {
						updcolNames = columnNames.get(columnCount);
						//updvals = "?";20210505 MatsudairaSyuMe Access Control: Database
					}
				}
			}
			String SQL_INSERT = "INSERT INTO " + fromTblName + " (" + colNames + ") VALUES (" + vals + ")";
			String SQL_UPDATE = "UPDATE " + fromTblName + " SET (" + updcolNames + ") = (" + updvals + ") WHERE "
					+ keyset;
			//20210122 MatsudairaSyuMe
			wowstr = Des.encode(Constants.DEFKNOCKING, SQL_UPDATE);
			String wowstr1 = Des.encode(Constants.DEFKNOCKING, SQL_INSERT);
			//----
			//String[] insvalary = null;MatsudairaSyuMe Access Control: Database
			if (updateMode) {
				/*20210505 MatsudairaSyuMe Access Control: Database
				for (int i = 0; i < keynameary.length; i++) {
					columnCount++;
					if (verbose)
						log.debug("columnCount=[{}] ColumnName={} ColumnTypeName={} ", columnCount, keynameary[i], keyvalueary[i].indexOf('\'') > -1? "VARCHAR":"INTEGER");
					columnNames.add(keynameary[i]);
					if (keyvalueary[i].indexOf('\'') > -1)
						columnTypes.add(Types.VARCHAR);
					else
						columnTypes.add(Types.INTEGER);
				}
				insvalary = com.systex.sysgateii.gateway.util.dataUtil.concatArray(valary, keyvaluearynocomm);
				//20210122 MatsudairaSyuMe
				 */
				preparedStatement = selconn.prepareStatement(Des.decodeValue(Constants.DEFKNOCKING, wowstr));
				log.debug("record exist using update:{}", Des.decodeValue(Constants.DEFKNOCKING, wowstr));
				//----
				/*20210505 MatsudairaSyuMe Access Control: Database
				log.debug("record exist using valary:{} len={}", insvalary, insvalary.length);
				setValueps(preparedStatement, insvalary, false);
				 */

			} else {
				//20210122 MatsudairaSyuMe
				preparedStatement = selconn.prepareStatement(Des.decodeValue(Constants.DEFKNOCKING, wowstr1));
				log.debug("record not exist using insert:{}", Des.decodeValue(Constants.DEFKNOCKING, wowstr1));
				//----
				/*20210505 MatsudairaSyuMe Access Control: Database
				insvalary = com.systex.sysgateii.gateway.util.dataUtil.concatArray(keyvaluearynocomm, valary);
				setValueps(preparedStatement, insvalary, false);
				*/
			}
			row = preparedStatement.executeUpdate();
			tblrs.close();
		}
		return row;
	}
	//20210118 MatsudairaSyuMe delete change for vulnerability scanning sql injection defense
	public int UPDT(String fromTblName, String field, String updval, String keyname, String selkeyval)
			throws Exception {
		columnNames = new Vector<String>();
		columnTypes = new Vector<Integer>();
		// STEP 4: Execute a query
		//20200908 add check for field and keyname
		if (fromTblName == null || fromTblName.trim().length() == 0 || field == null || field.trim().length() == 0
				|| keyname == null || keyname.trim().length() == 0)
			throw new Exception("given table name or field or keyname error =>" + fromTblName);
		log.debug(String.format("Select from table %s... where %s=%s", fromTblName, keyname, selkeyval));
		
		String keyset = "";
		String[] keynameary = keyname.split(",");
		String[] keyvalueary = selkeyval.split(",");
//		String[] keyvaluearynocomm = selkeyval.split(",");
		log.debug("update value [{}]", updval);
/*		String[] valary = updval.split(",");
		for (int i = 0; i < valary.length; i++) {
			int s = valary[i].indexOf('\'');
			int l = valary[i].lastIndexOf('\'');
			if (s != l && s >= 0 && l >= 0 && s < l)
				valary[i] = valary[i].substring(s + 1, l);
		}
		*/
		
		if (keynameary.length != keyvalueary.length)
			throw new Exception("given fields keyname can't correspond to keyvfield =>keynames [" + keyname + "] selkeyval [" + selkeyval + "]");
		else {
			for (int i = 0; i < keynameary.length; i++)
				//keyset = keyset + keynameary[i] + " = " + "?" + (i == (keynameary.length - 1) ? "" : " and ");//20210505 MatsudairaSyuMe Access Control: Database
				keyset = keyset + keynameary[i] + " = " + keyvalueary[i] + (i == (keynameary.length - 1) ? "" : " and ");
			/*20210505 MatsudairaSyuMe Access Control: Database
			for (int i = 0; i < keyvaluearynocomm.length; i++) {
				int s = keyvalueary[i].indexOf('\'');
				int l = keyvalueary[i].lastIndexOf('\'');
				if (s != l && s >= 0 && l >= 0 && s < l)
					keyvaluearynocomm[i] = keyvalueary[i].substring(s + 1, l);
			}
			*/
		}
	
		String selstr = "SELECT " + field + " FROM " + fromTblName + " where " + keyset;
		//20210122 MatsudairaSyuMe
		String wowstr = Des.encode(Constants.DEFKNOCKING, selstr);
		log.debug("UPDT selstr []-->[{}]", wowstr);
		//----
		/*20210505 MatsudairaSyuMe Access Control: Database
		//20210122 MatsudairaSyuMe
		PreparedStatement stmt = selconn.prepareStatement(Des.decodeValue(Constants.DEFKNOCKING, wowstr));
		//----

		for (int i = 0; i < keyvalueary.length; i++) {
			if (keyvalueary[i].indexOf('\'') > -1 )
				stmt.setString(i + 1, keyvaluearynocomm[i]);
			else
				stmt.setInt(i + 1, Integer.valueOf(keyvaluearynocomm[i]));
		}
		ResultSet tblrs = stmt.executeQuery();
		*/
		Statement stmt = selconn.createStatement();
		ResultSet tblrs = stmt.executeQuery(Des.decodeValue(Constants.DEFKNOCKING, wowstr));

		int type = -1;
		int row = 0;

		if (tblrs != null) {
			ResultSetMetaData rsmd = tblrs.getMetaData();
			int columnCount = 0;
			while (columnCount < rsmd.getColumnCount()) {
				columnCount++;
				type = rsmd.getColumnType(columnCount);
				if (verbose)
					log.debug("columnCount=[{}] ColumnName={} ColumnTypeName={} ", columnCount, rsmd.getColumnName(columnCount), rsmd.getColumnTypeName(columnCount));
				columnNames.add(rsmd.getColumnName(columnCount));
				columnTypes.add(type);
			}
			String colNames = "";
//			String vals = "";20210505 MatsudairaSyuMe Access Control: Database
			String vals  = selkeyval + "," + updval;
			String updcolNames = "";
//			String updvals = "";20210505 MatsudairaSyuMe Access Control: Database
			String updvals = updval;
			log.debug("table fields {}", Arrays.toString(columnNames.toArray()));
//			log.debug("given keyvaluearynocomm {}", Arrays.toString(keyvaluearynocomm));20210505 MatsudairaSyuMe Access Control: Database

			for (columnCount = 0; columnCount < columnNames.size(); columnCount++) {
				if (colNames.trim().length() > 0) {
					colNames = colNames + "," + columnNames.get(columnCount);
					//vals = vals + ",?";20210505 MatsudairaSyuMe Access Control: Database
				} else {
					colNames = columnNames.get(columnCount);
					//vals = "?";20210505 MatsudairaSyuMe Access Control: Database
				}
				if (!columnNames.get(columnCount).equalsIgnoreCase(keyname)) {
					if (updcolNames.trim().length() > 0) {
						updcolNames = updcolNames + "," + columnNames.get(columnCount);
						//updvals = updvals + ",?";20210505 MatsudairaSyuMe Access Control: Database
					} else {
						updcolNames = columnNames.get(columnCount);
						//updvals = "?";20210505 MatsudairaSyuMe Access Control: Database
					}
				}
			}
			String SQL_INSERT = "INSERT INTO " + fromTblName + " (" + colNames + ") VALUES (" + vals + ")";
			String SQL_UPDATE = "UPDATE " + fromTblName + " SET (" + updcolNames + ") = (" + updvals + ") WHERE "
					+ keyset;
			//----
			//20210122 MatsudairaSyuMe
			wowstr = Des.encode(Constants.DEFKNOCKING, SQL_UPDATE);
			String wowstr1 = Des.encode(Constants.DEFKNOCKING, SQL_INSERT);
			//----

			//String[] insvalary = com.systex.sysgateii.gateway.util.dataUtil.concatArray(valary, keyvaluearynocomm);MatsudairaSyuMe Access Control: Database
			if (tblrs.next()) {
				/*20210505 MatsudairaSyuMe Access Control: Database
				for (int i = 0; i < keynameary.length; i++) {
					columnCount++;
					if (verbose)
						log.debug("columnCount=[{}] ColumnName={} ColumnTypeName={} ", columnCount, keynameary[i], keyvalueary[i].indexOf('\'') > -1? "VARCHAR":"INTEGER");
					columnNames.add(keynameary[i]);
					if (keyvalueary[i].indexOf('\'') > -1)
						columnTypes.add(Types.VARCHAR);
					else
						columnTypes.add(Types.INTEGER);
				}
				 */
				//20210122 MatsudairaSyuMe
				preparedStatement = selconn.prepareStatement(Des.decodeValue(Constants.DEFKNOCKING, wowstr));
				log.debug("record exist using update:{}", Des.decodeValue(Constants.DEFKNOCKING, wowstr));
				/*20210505 MatsudairaSyuMe Access Control: Database
				setValueps(preparedStatement, insvalary, false);
				
				//----
				log.debug("record exist using valary:{} len={}", insvalary, insvalary.length);
				*/
			} else {
				//20210122 MatsudairaSyuMe
				preparedStatement = selconn.prepareStatement(Des.decodeValue(Constants.DEFKNOCKING, wowstr1));
				log.debug("record not exist using insert:{}", Des.decodeValue(Constants.DEFKNOCKING, wowstr1));
				/*20210505 MatsudairaSyuMe Access Control: Database
				setValueps(preparedStatement, insvalary, false);
				*/
				//----
			}
			row = preparedStatement.executeUpdate();
			tblrs.close();
		}
		return row;
	}

	//20210118 MatsudairaSyuMe delete change for vulnerability scanning sql injection defense
	public String SELONEFLD(String fromTblName, String fieldn, String keyname, String keyvalue, boolean verbose)
			throws Exception {
		String rtnVal = "";
		tbsdytblcolumnNames = new Vector<String>();
		tbsdytblcolumnTypes = new Vector<Integer>();
		if (fromTblName == null || fromTblName.trim().length() == 0 || fieldn == null || fieldn.trim().length() == 0
				|| keyname == null || keyname.trim().length() == 0)
			return rtnVal;
		try {
			log.debug("keyname = keyvalue=[{}]",  keyname + "=" + keyvalue);
			String keyset = "";
			String[] keynameary = keyname.split(",");
			String[] keyvalueary = keyvalue.split(",");
			// String[] keyvaluearynocomm = keyvalue.split(",");20210505 MatsudairaSyuMe Access Control: Database
			if (keynameary.length != keyvalueary.length)
				throw new Exception("given fields keyname can't correspond to keyvfield =>keynames [" + keyname + "] keyvalues [" + keyvalue + "]");
			else {
				for (int i = 0; i < keynameary.length; i++)
					//keyset = keyset + keynameary[i] + " = " + "?" + (i == (keynameary.length - 1) ? "" : " and ");20210505 MatsudairaSyuMe Access Control: Database
					keyset = keyset + keynameary[i] + " = " + keyvalueary[i] + (i == (keynameary.length - 1) ? "" : " and ");
				/*20210505 MatsudairaSyuMe Access Control: Database
				for (int i = 0; i < keyvaluearynocomm.length; i++) {
					int s = keyvalueary[i].indexOf('\'');
					int l = keyvalueary[i].lastIndexOf('\'');
					if (s != l && s >= 0 && l >= 0 && s < l)
						keyvaluearynocomm[i] = keyvalueary[i].substring(s + 1, l);
				}
				*/
			}

			if ((keyname.indexOf(',') > -1) && (keyvalue.indexOf(',') > -1)
					&& (keynameary.length != keyvalueary.length))
				return rtnVal;
			String selstr = "SELECT " + fieldn + " FROM " + fromTblName + " where " + keyset;
			//20210122 MatsudairaSyuMe
			String wowstr = Des.encode(Constants.DEFKNOCKING, selstr);
			log.debug("SELONEFLD selstr [{}]-->[{}]",selstr, wowstr);
			/*20210505 MatsudairaSyuMe Access Control: Database
			PreparedStatement stmt = selconn.prepareStatement(Des.decodeValue(Constants.DEFKNOCKING, wowstr));
			//----

			for (int i = 0; i < keyvalueary.length; i++) {
				if (keyvalueary[i].indexOf('\'') > -1 )
					stmt.setString(i + 1, keyvaluearynocomm[i]);
				else
					stmt.setInt(i + 1, Integer.valueOf(keyvaluearynocomm[i]));
			}
			ResultSet tblrs = stmt.executeQuery();
			*/
			Statement stmt = selconn.createStatement();
			ResultSet tblrs = stmt.executeQuery(Des.decodeValue(Constants.DEFKNOCKING, wowstr));
			
			int type = -1;
			if (tblrs != null) {
				ResultSetMetaData rsmd = tblrs.getMetaData();
				int columnCount = 0;
				while (columnCount < rsmd.getColumnCount()) {
					columnCount++;
					type = rsmd.getColumnType(columnCount);
					if (verbose)
						log.debug("ColumnName={} ColumnTypeName={} ", rsmd.getColumnName(columnCount), rsmd.getColumnTypeName(columnCount) );
					tbsdytblcolumnNames.add(rsmd.getColumnName(columnCount));
					tbsdytblcolumnTypes.add(type);
				}
				int idx = 0;
				while (tblrs.next()) {
					if (idx == 0)
						rtnVal = tblrs.getString(fieldn);
					else
						rtnVal = rtnVal + "," + tblrs.getString(fieldn);
					idx++;
				}
				tblrs.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("error : {}", e.toString());
		}
		log.debug("return SELONEFLD=[{}]", rtnVal);
		return rtnVal;
	}
	//----
	//20210118 MatsudairaSyuMe delete change for vulnerability scanning sql injection defense
	public String[] SELMFLD(String fromTblName, String fieldsn, String keyname, String keyvalue, boolean verbose)
			throws Exception {
		String[] rtnVal = {};
		tbsdytblcolumnNames = new Vector<String>();
		tbsdytblcolumnTypes = new Vector<Integer>();
		if (fromTblName == null || fromTblName.trim().length() == 0 || fieldsn == null || fieldsn.trim().length() == 0
				|| keyname == null || keyname.trim().length() == 0)
			return rtnVal;
		try {
			log.debug("fieldsn=[{}] keyname = keyvalue : [{}]",  fieldsn, keyname + "=" + keyvalue);
			String[] fieldset = null;
			if (fieldsn.indexOf(',') > -1)
				fieldset = fieldsn.split(",");
			else {
				fieldset = new String[1];
				fieldset[0] = fieldsn;
			}
			String keyset = "";
			String[] keynameary = keyname.split(",");
			String[] keyvalueary = keyvalue.split(",");
			// String[] keyvaluearynocomm = keyvalue.split(",");20210505 MatsudairaSyuMe Access Control: Database
			if (keynameary.length != keyvalueary.length)
				throw new Exception("given fields keyname can't correspond to keyvfield =>keynames [" + keyname + "] keyvalues [" + keyvalue + "]");
			else {
				for (int i = 0; i < keynameary.length; i++)
//					keyset = keyset + keynameary[i] + " = " + "?" + (i == (keynameary.length - 1) ? "" : " and ");20210505 MatsudairaSyuMe Access Control: Database
					keyset = keyset + keynameary[i] + " = " + keyvalueary[i] + (i == (keynameary.length - 1) ? "" : " and ");
				/*20210505 MatsudairaSyuMe Access Control: Database
				for (int i = 0; i < keyvaluearynocomm.length; i++) {
					int s = keyvalueary[i].indexOf('\'');
					int l = keyvalueary[i].lastIndexOf('\'');
					if (s != l && s >= 0 && l >= 0 && s < l)
						keyvaluearynocomm[i] = keyvalueary[i].substring(s + 1, l);
				}
				*/
			}

			if ((keyname.indexOf(',') > -1) && (keyvalue.indexOf(',') > -1)
					&& (keynameary.length != keyvalueary.length))
				return rtnVal;

			String selstr = "SELECT " + fieldsn + " FROM " + fromTblName + " where " + keyset;
			//20210122 MatsudairaSyuMe
			String wowstr = Des.encode(Constants.DEFKNOCKING, selstr);
			log.debug("SELMFLD selstr [{}]-->[{}]", selstr, wowstr);
			/*20210505 MatsudairaSyuMe Access Control: Database
			PreparedStatement stmt = selconn.prepareStatement(Des.decodeValue(Constants.DEFKNOCKING, wowstr));
			//----
			for (int i = 0; i < keyvalueary.length; i++) {
				if (keyvalueary[i].indexOf('\'') > -1 )
					stmt.setString(i + 1, keyvaluearynocomm[i]);
				else
					stmt.setInt(i + 1, Integer.valueOf(keyvaluearynocomm[i]));
			}
			ResultSet tblrs = stmt.executeQuery();
			*/
			Statement stmt = selconn.createStatement();
			ResultSet tblrs = stmt.executeQuery(Des.decodeValue(Constants.DEFKNOCKING, wowstr));
			
			int type = -1;

			if (tblrs != null) {
				ResultSetMetaData rsmd = tblrs.getMetaData();
				int columnCount = 0;
				while (columnCount < rsmd.getColumnCount()) {
					columnCount++;
					type = rsmd.getColumnType(columnCount);
					if (verbose)
						log.debug("ColumnName={} ColumnTypeName={} ", rsmd.getColumnName(columnCount), rsmd.getColumnTypeName(columnCount) );
					tbsdytblcolumnNames.add(rsmd.getColumnName(columnCount));
					tbsdytblcolumnTypes.add(type);
				}
				int idx = 0;
				while (tblrs.next()) {
					if (idx <= 0)
						rtnVal = new String[1];
					else {
						String[] tmpv = rtnVal;
						rtnVal = new String[idx + 1];
						int j = 0;
						for (String s: tmpv) {
							rtnVal[j] = s;
							j++;
						}
					}
					for (int i = 0; i < fieldset.length; i++) {
						if (i == 0)
							rtnVal[idx] = tblrs.getString(fieldset[i]);
						else
							rtnVal[idx] = rtnVal[idx] + "," + tblrs.getString(fieldset[i]);
					}
					idx++;
				}
				tblrs.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("error : {}", e.toString());
		}
		if (verbose)
			log.debug("return SELMFLD length=[{}]", rtnVal.length);
		return rtnVal;
	}

	//20201019
	public String[] SELMFLDNOIDX(String fromTblName, String fieldsn, boolean verbose)
			throws Exception {
		String[] rtnVal = {};
		tbsdytblcolumnNames = new Vector<String>();
		tbsdytblcolumnTypes = new Vector<Integer>();
		if (fromTblName == null || fromTblName.trim().length() == 0 || fieldsn == null || fieldsn.trim().length() == 0)
			return rtnVal;
		try {
			log.debug("fieldsn=[{}]",  fieldsn);
			String[] fieldset = null;
			if (fieldsn.indexOf(',') > -1)
				fieldset = fieldsn.split(",");
			else {
				fieldset = new String[1];
				fieldset[0] = fieldsn;
			}
			java.sql.Statement stmt = selconn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT);
			//20210122 MatsudairaSyuMe
			String wowstr = Des.encode(Constants.DEFKNOCKING, "SELECT " + fieldsn + " FROM " + fromTblName);
			log.debug("SELMFLDNOIDX selstr []-->[{}]", wowstr);
			tbsdytblrs = ((java.sql.Statement) stmt).executeQuery(Des.decodeValue(Constants.DEFKNOCKING, wowstr));
			//----

			int type = -1;

			if (tbsdytblrs != null) {
				ResultSetMetaData rsmd = tbsdytblrs.getMetaData();
				int columnCount = 0;
				while (columnCount < rsmd.getColumnCount()) {
					columnCount++;
					type = rsmd.getColumnType(columnCount);
					if (verbose)
						log.debug("ColumnName={} ColumnTypeName={} ", rsmd.getColumnName(columnCount), rsmd.getColumnTypeName(columnCount) );
					tbsdytblcolumnNames.add(rsmd.getColumnName(columnCount));
					tbsdytblcolumnTypes.add(type);
				}
				int idx = 0;
				while (tbsdytblrs.next()) {
					if (idx <= 0)
						rtnVal = new String[1];
					else {
						String[] tmpv = rtnVal;
						rtnVal = new String[idx + 1];
						int j = 0;
						for (String s: tmpv) {
							rtnVal[j] = s;
							j++;
						}
					}
					for (int i = 0; i < fieldset.length; i++) {
						if (i == 0)
							rtnVal[idx] = tbsdytblrs.getString(fieldset[i]);
						else
							rtnVal[idx] = rtnVal[idx] + "," + tbsdytblrs.getString(fieldset[i]);
					}
					idx++;
				}
				tbsdytblrs.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("error : {}", e.toString());
		}
		log.debug("return SELMFLDNOIDX length=[{}]", rtnVal.length);
		return rtnVal;
	}

	//20210118 MatsudairaSyuMe delete change for vulnerability scanning sql injection defense
	public String[] INSSELChoiceKey(String fromTblName, String field, String selupdval, String keyname, String selkeyval, boolean usekey, boolean verbose) throws Exception {
		String[] rtnVal = null;
		columnNames = new Vector<String>();
		columnTypes = new Vector<Integer>();
		if (fromTblName == null || fromTblName.trim().length() == 0 || field == null || field.trim().length() == 0
				|| keyname == null || keyname.trim().length() == 0)
			throw new Exception("given table name or field or keyname error =>" + fromTblName);
		log.debug(String.format("first test Select from table %s... where %s=%s", fromTblName, keyname, selkeyval));
		String keyset = "";
		String selstr = "";
		String[] keynameary = keyname.split(",");
		String[] keyvalueary = selkeyval.split(",");
		String[] keyvaluearynocomm = selkeyval.split(",");
		if (keynameary.length != keyvalueary.length)
			throw new Exception("given fields keyname can't correspond to keyvfield =>keynames [" + keyname + "] selkayvals [" + selkeyval + "]");
		else {
			for (int i = 0; i < keynameary.length; i++)
//				keyset = keyset + keynameary[i] + " = " + "?" + (i == (keynameary.length - 1) ? "" : " and ");20210505 MatsudairaSyuMe Access Control: Database
				keyset = keyset + keynameary[i] + " = " + keyvalueary[i] + (i == (keynameary.length - 1) ? "" : " and ");
			for (int i = 0; i < keyvaluearynocomm.length; i++) {
				int s = keyvaluearynocomm[i].indexOf('\'');
				int l = keyvaluearynocomm[i].lastIndexOf('\'');
				if (s != l && s >= 0 && l >= 0 && s < l)
					keyvaluearynocomm[i] = keyvaluearynocomm[i].substring(s + 1, l);
			}
		}

		if (usekey) {
			selstr = "SELECT " + keyname + "," + field + " FROM " + fromTblName + " where " + keyset;
		} else {
			selstr = "SELECT " + field + " FROM " + fromTblName + " where " + keyset;
		}
		//20210122 MatsudairaSyuMe
		String wowstr = Des.encode(Constants.DEFKNOCKING, selstr);
		log.debug("sqlstr=[{}]-->[{}] selupdval value [{}] selkeyval [{}]", selstr, wowstr, selupdval, selkeyval);
		//----

		String[] valary = selupdval.split(",");
		String[] valarynocomm = selupdval.split(",");
		for (int i = 0; i < valary.length; i++) {
			int s = valarynocomm[i].indexOf('\'');
			int l = valarynocomm[i].lastIndexOf('\'');
			if (s != l && s >= 0 && l >= 0 && s < l)
				valarynocomm[i] = valarynocomm[i].substring(s + 1, l);
		}
		int type = -1;
		int row = 0;
		/*20210505 MatsudairaSyuMe Access Control: Database

		//20210122 MatsudairaSyuMe
		PreparedStatement stmt = selconn.prepareStatement(Des.decodeValue(Constants.DEFKNOCKING, wowstr));
		//----
		for (int i = 0; i < keyvalueary.length; i++) {
			if (keyvalueary[i].indexOf('\'') > -1 )
				stmt.setString(i + 1, keyvaluearynocomm[i]);
			else
				stmt.setInt(i + 1, Integer.valueOf(keyvaluearynocomm[i]));
		}
		ResultSet tblrs = stmt.executeQuery();
		*/
		Statement stmt = selconn.createStatement();
		ResultSet tblrs = stmt.executeQuery(Des.decodeValue(Constants.DEFKNOCKING, wowstr));

		if (tblrs != null) {
			ResultSetMetaData rsmd = tblrs.getMetaData();
			int columnCount = 0;
			boolean updateMode = false;
			log.debug("table request fields {}", field);
			if (tblrs.next()) {
				log.debug("update mode");
				updateMode = true;
			} else
				log.debug("insert mode");
			while (columnCount < rsmd.getColumnCount()) {
				columnCount++;
				type = rsmd.getColumnType(columnCount);
				if (updateMode && field.indexOf(rsmd.getColumnName(columnCount).trim()) > -1) {
					columnNames.add(rsmd.getColumnName(columnCount));
					columnTypes.add(type);
					if (verbose)
						log.debug("updateMode ColumnName={} ColumnTypeName={} ", rsmd.getColumnName(columnCount), rsmd.getColumnTypeName(columnCount) );
				} else if (!updateMode && (field.indexOf(rsmd.getColumnName(columnCount).trim()) > -1 || keyname.indexOf(rsmd.getColumnName(columnCount).trim()) > -1)) {
					columnNames.add(rsmd.getColumnName(columnCount));
					columnTypes.add(type);					
					if (verbose)
						log.debug("insert Mode ColumnName={} ColumnTypeName={} ", rsmd.getColumnName(columnCount), rsmd.getColumnTypeName(columnCount) );
				}
			}
			String colNames = "";
			String vals = "";
			String updcolNames = "";
			//String updvals = ""; 20210505 MatsudairaSyuMe Access Control: Database
//			log.debug("given vals {} keyvaluearynocomm {}", Arrays.toString(valary), Arrays.toString(keyvaluearynocomm));20210505 MatsudairaSyuMe Access Control: Database

			for (columnCount = 0; columnCount < columnNames.size(); columnCount++) {
				if (colNames.trim().length() > 0) {
					colNames = colNames + "," + columnNames.get(columnCount);
					vals = vals + ",?";
				} else {
					colNames = columnNames.get(columnCount);
					vals = "?";
				}
				if (updateMode) {
					if (updcolNames.trim().length() > 0) {
						updcolNames = updcolNames + "," + columnNames.get(columnCount);
						//updvals = updvals + ",?";20210505 MatsudairaSyuMe Access Control: Database
					} else {
						updcolNames = columnNames.get(columnCount);
						//updvals = "?";20210505 MatsudairaSyuMe Access Control: Database
					}
				}
			}
			String SQL_INSERT = "SELECT " + keyname + " FROM NEW TABLE (INSERT INTO " + fromTblName + " (" + colNames + ") VALUES (" + vals + "))";
			/*20210505 MatsudairaSyuMe Access Control: Database
			String SQL_UPDATE = "UPDATE " + fromTblName + " SET (" + updcolNames + ") = (" + updvals + ") WHERE "
					+ keyset;
			*/
			String SQL_UPDATE = "UPDATE " + fromTblName + " SET (" + updcolNames + ") = (" + selupdval + ") WHERE "
					+ keyset;
			//20210122 MatsudairaSyuMe
			wowstr = Des.encode(Constants.DEFKNOCKING, SQL_UPDATE);
			String wowstr1 = ""; //20210202 MatsudairaSyuMe
			//----

			String cnvInsertStr = "";
			String[] insvalary = null;
			if (updateMode) {
				/*20210505 MatsudairaSyuMe Access Control: Database
				for (int i = 0; i < keynameary.length; i++) {
					columnCount++;
					if (verbose)
						log.debug("columnCount=[{}] ColumnName={} ColumnTypeName={} ", columnCount, keynameary[i], keyvalueary[i].indexOf('\'') > -1? "VARCHAR":"INTEGER");
					columnNames.add(keynameary[i]);
					if (keyvalueary[i].indexOf('\'') > -1)
						columnTypes.add(Types.VARCHAR);
					else
						columnTypes.add(Types.INTEGER);
				}

				insvalary = com.systex.sysgateii.gateway.util.dataUtil.concatArray(valarynocomm, keyvaluearynocomm);
				*/
				//20210122 MatsudairaSyuMe
				preparedStatement = selconn.prepareStatement(Des.decodeValue(Constants.DEFKNOCKING, wowstr));
				log.debug("record exist using update:{}", Des.decodeValue(Constants.DEFKNOCKING, wowstr));
				//----
				/*20210505 MatsudairaSyuMe Access Control: Database
				log.debug("record exist using valary:{} len={}", insvalary, insvalary.length);
				setValueps(preparedStatement, insvalary, usekey);
				*/
			} else {
				try {
					if (usekey)
						insvalary = com.systex.sysgateii.autosvr.util.dataUtil.concatArray(keyvaluearynocomm, valarynocomm);
					else
						insvalary = valarynocomm;
				//20201116
					cnvInsertStr = generateActualSql(SQL_INSERT, (Object[])insvalary);
				//20210202 MatsudairaSyuMe
					wowstr1 = Des.encode(Constants.DEFKNOCKING, cnvInsertStr);
				//----
					log.debug("record not exist using select insert:[{}] toString=[{}]", Des.decodeValue(Constants.DEFKNOCKING, wowstr1), cnvInsertStr);
				//----
				} catch(Exception e) {
					e.printStackTrace();
					throw new Exception("format error");
				}
			}
			if (updateMode) {
				row = preparedStatement.executeUpdate();
				log.debug("executeUpdate() row=[{}]", row);
				if (keyvalueary.length > 0)
					rtnVal = keyvalueary;
				else {
					rtnVal = new String[1];
					rtnVal[0] = selkeyval;
				}
			} else {
				java.sql.Statement stmt2 = selconn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT);
				rs = ((java.sql.Statement) stmt2).executeQuery(Des.decodeValue(Constants.DEFKNOCKING, wowstr1));//20210202 MatsudairaSyuMe
				log.debug("executeUpdate()");
				int idx = 0;
				
				//20210426 MatsudairaSyuMe prevent Null Dereference
				rtnVal = new String[1];
				//----
				while (rs.next()) {
					if (rtnVal != null) {
						String[] tmpv = rtnVal;
						rtnVal = new String[idx + 1];
						int j = 0;
						for (String s : tmpv) {
							rtnVal[j] = s;
							j++;
						}
					}
					for (int i = 0; i < keynameary.length; i++) {
						if (i == 0)
							rtnVal[idx] = rs.getString(keynameary[i]);
						else
							rtnVal[idx] = rtnVal[idx] + "," + rs.getString(keynameary[i]);
					}
					idx++;
				}
			}
			if (rs != null)
				rs.close();
			tblrs.close();
		}
		return rtnVal;
	}
	
	//20201028 delete
	//----
	//20210118 MatsudairaSyuMe delete change for vulnerability scanning sql injection defense
	public boolean DELETETB(String fromTblName, String keyname, String selkeyval)
			throws Exception {
		if (fromTblName == null || fromTblName.trim().length() == 0 || keyname == null || keyname.trim().length() == 0)
			throw new Exception("given table name or field or keyname error =>" + fromTblName);
		log.debug(String.format("delete table %s... where %s=%s", fromTblName, keyname, selkeyval));
		String[] keynameary = keyname.split(",");
		String[] keyvalueary = selkeyval.split(",");
		String[] valary = selkeyval.split(",");
		String deletesql = "DELETE FROM " + fromTblName + " WHERE ";

		if (keyname.indexOf(',') > -1 && selkeyval.indexOf(',') > -1) {
			keynameary = keyname.split(",");
			keyvalueary = selkeyval.split(",");
			if (keynameary.length != keyvalueary.length)
				throw new Exception("given fields keyname can't correspond to keyvfield =>keynames [" + keyname + "] selkayvals [" + selkeyval + "]");
			else {
				for (int i = 0; i < keynameary.length; i++)
					/*20210505 MatsudairaSyuMe Access Control: Database
					deletesql = deletesql + keynameary[i] + " = " + "?" + (i == (keynameary.length - 1) ? "" : " and ");
					*/
					deletesql = deletesql + keynameary[i] + " = " + keyvalueary[i] + (i == (keynameary.length - 1) ? "" : " and ");
			}
		} else
			/*20210505 MatsudairaSyuMe Access Control: Database
			deletesql = deletesql + keyname + " = ?";
			*/
			deletesql = deletesql + keyname + " = " + selkeyval;
		//20210122 MatsudairaSyuMe
		String wowstr = Des.encode(Constants.DEFKNOCKING, deletesql);
		log.debug("DELETETB deletesql=[{}]-->[{}] ", deletesql, wowstr);
		//----

		for (int i = 0; i < keyvalueary.length; i++) {
			int s = valary[i].indexOf('\'');
			int l = valary[i].lastIndexOf('\'');
			if (s != l && s >= 0 && l >= 0 && s < l)
				valary[i] = valary[i].substring(s + 1, l);
		}

		//20210122 MatsudairaSyuMe
		PreparedStatement stmt = selconn.prepareStatement(Des.decodeValue(Constants.DEFKNOCKING, wowstr));
		//----

		/*20210505 MatsudairaSyuMe Access Control: Database
		for (int i = 0; i < keyvalueary.length; i++) {
			if (keyvalueary[i].indexOf('\'') > -1 )
				stmt.setString(i + 1, valary[i]);
			else
				stmt.setInt(i + 1, Integer.valueOf(valary[i]));
		}
		*/
		return stmt.execute();
	}
	private String generateActualSql(String sqlQuery, Object... parameters) throws Exception {
	    String[] parts = sqlQuery.split("\\?");
	    StringBuilder sb = new StringBuilder();

	    // This might be wrong if some '?' are used as litteral '?'
	    for (int i = 0; i < parts.length; i++) {
	        String part = parts[i];
	        sb.append(part);
	        if (i < parameters.length) {
	            sb.append(getValueps(i, (String[]) parameters));
	        }
	    }

	    return sb.toString();
	}
	private String formatParameter(Object parameter) {
	    if (parameter == null) {
	        return "NULL";
	    } else {
	        if (parameter instanceof String) {
	            return "'" + ((String) parameter).replace("'", "''") + "'";
	        } else if (parameter instanceof Timestamp) {
	            return "to_timestamp('" + new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS").
	                    format(parameter) + "', 'mm/dd/yyyy hh24:mi:ss.ff3')";
	        } else if (parameter instanceof Date) {
	            return "to_date('" + new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").
	                    format(parameter) + "', 'mm/dd/yyyy hh24:mi:ss')";
	        } else if (parameter instanceof Boolean) {
	            return ((Boolean) parameter).booleanValue() ? "1" : "0";
	        } else {
	            return parameter.toString();
	        }
	    }
	}
	/*
	//----
	private PreparedStatement setValueps(PreparedStatement ps, String[] updvalary, boolean fromOne) throws Exception {
		//20201119
		//fromOne true start from index 1 otherwise, false from index 0
		int type;
		String obj = "";
		int j = 1;
		if (!fromOne)
			j = 0;
		int i = 1;
//		verbose = true;
		if (verbose)
			log.debug("j={} columnNames.size()={}",j,columnNames.size());
		for (; j < columnNames.size(); j++) {
			type = columnTypes.get(j);
			obj = updvalary[j];
			if (verbose)
				log.debug("\tj=" + j + ":[" + obj + "]");
			switch (type) {
			case Types.VARCHAR:
			case Types.CHAR:
				if (verbose)
					log.debug("rs.setString");
				ps.setString(i, obj);
				break;
			case Types.DECIMAL:
				if (verbose)
					log.debug("rs.setDouble");
				ps.setDouble(i, Double.parseDouble(obj));
				break;
			case Types.TIMESTAMP:
				if (verbose)
					log.debug("rs.setTimestamp[{}]", obj);
				ps.setTimestamp(i, Timestamp.valueOf(obj));
				break;
			case Types.BIGINT:
				if (verbose)
					log.debug("rs.setLong");
				ps.setLong(i, Long.parseLong(obj));
				break;
			case Types.BLOB:
				if (verbose)
					log.debug("rs.setBlob");
				ps.setBlob(i, new javax.sql.rowset.serial.SerialBlob(obj.getBytes()));
				break;
			case Types.CLOB:
				if (verbose)
					log.debug("rs.setClob");
				Clob clob = ps.getConnection().createClob();
				clob.setString(1, obj);
				ps.setClob(i, clob);
				break;
			case Types.DATE:
				if (verbose)
					log.debug("rs.setDate");
				ps.setDate(i, Date.valueOf(obj));
				break;
			case Types.DOUBLE:
				if (verbose)
					log.debug("rs.setDouble");
				ps.setDouble(i, Double.valueOf(obj));
				break;
			case Types.INTEGER:
				if (verbose)
					log.debug("rs.setInt/getInt");
				ps.setInt(i, Integer.valueOf(obj));
				break;
			case Types.NVARCHAR:
				if (verbose)
					log.debug("rs.setNString(idx, v)");
				ps.setNString(i, obj);
				break;
			default:
				log.error("undevelop type:{} change to string", type);
				ps.setString(i, obj);
				break;
			}
			i += 1;
		}
		if (verbose)
			System.out.println();
		return ps;
	}
	*/
	private String getValueps(int j, String[] updvalary) throws Exception {
		// updinsert true for update, false for insert
		String rtn = "";
		int type;
		String obj = "";
		verbose = true;
		type = columnTypes.get(j);
		obj = updvalary[j];
		if (verbose)
			log.debug("\tj=" + j + ":[" + obj + "]");
		switch (type) {
		case Types.VARCHAR:
		case Types.CHAR:
			if (verbose)
				log.debug("getString");
			rtn = "'" + obj + "'";
			break;
		case Types.DECIMAL:
			if (verbose)
				log.debug("getDouble");
			rtn = " " + obj + " ";
			break;
		case Types.TIMESTAMP:
			if (verbose)
				log.debug("getTimestamp[{}]", obj);
			rtn = "'" + obj + "'";
			break;
		case Types.BIGINT:
			if (verbose)
				log.debug("getLong");
			rtn = " " + obj + " ";
			break;
		case Types.BLOB:
			if (verbose)
				log.debug("getBlob");
			rtn = "'" + obj + "'";
			break;
		case Types.CLOB:
			if (verbose)
				log.debug("getClob");
			rtn = "'" + obj + "'";
			break;
		case Types.DATE:
			if (verbose)
				log.debug("getDate");
			rtn = "'" + obj + "'";
			break;
		case Types.DOUBLE:
			if (verbose)
				log.debug("getDouble");
			rtn = " " + obj + " ";
			break;
		case Types.INTEGER:
			if (verbose)
				log.debug("getInt/getInt");
			rtn = " " + obj + " ";
			break;
		case Types.NVARCHAR:
			if (verbose)
				log.debug("getNString(idx, v)");
			rtn = "'" + obj + "'";
			break;
		default:
			log.error("undevelop type:{} change to string", type);
			rtn = "'" + obj + "'";
			break;
		}
		return rtn;
	}
	/*
	private String gettbsdytblValue(ResultSet rs, String obj, boolean verbose) throws Exception {
		int type;
		String rtn = "";
		for (int j = 0; j < tbsdytblcolumnNames.size(); j++) {
			if (!tbsdytblcolumnNames.get(j).endsWith(obj))
				continue;
			type = tbsdytblcolumnTypes.get(j);
			if (verbose)
				log.debug("\t" + obj + ":");
			switch (type) {
			case Types.VARCHAR:
			case Types.CHAR:
				if (verbose)
					log.debug("rs.getString");
				rtn = rs.getString(obj);
				break;
			case Types.DECIMAL:
				if (verbose)
					log.debug("rs.getDouble");
				rtn = Double.toString(rs.getDouble(obj));
				break;
			case Types.TIMESTAMP:
				if (verbose)
					log.debug("rs.getTimestamp");
				rtn = rs.getTimestamp(obj).toString();
				break;
			case Types.BIGINT:
				if (verbose)
					log.debug("rs.getLong");
				rtn = Long.toString(rs.getLong(obj));
				break;
			case Types.BLOB:
				if (verbose)
					log.debug("rs.getBlob");
				Blob blob = rs.getBlob(obj);
				int blobLength = (int) blob.length();
				byte[] blobAsBytes = blob.getBytes(1, blobLength);
				rtn  = DataConvert.bytesToHex(blobAsBytes);
				break;
			case Types.CLOB:
				if (verbose)
					log.debug("rs.getClob");
				Clob clob = rs.getClob(obj);
				rtn = clob.toString();
				break;
			case Types.DATE:
				if (verbose)
					log.debug("rs.getDate");
				rtn = rs.getString(obj);
				break;
			case Types.DOUBLE:
				if (verbose)
					log.debug("rs.getDouble");
				rtn = Double.toString(rs.getDouble(obj));
				break;
			case Types.INTEGER:
				if (verbose)
					log.debug("rs.getInt");
				rtn = Integer.toString(rs.getInt(obj));
				break;
			case Types.NVARCHAR:
				if (verbose)
					log.debug("rs.getNString(idx, v)");
				rtn = rs.getNString(obj);
				break;
			default:
				log.error("undevelop type:{} change to string", type);
				rtn = rs.getString(obj);
				break;
			}
			break;
		}
		return rtn;
	}
	*/
	public void CloseConnect() throws Exception {
		try {
			if (selconn != null) { //20220607 MatsudairaSyuMe
				selconn.close();
			    selconn = null;
			}
		} catch (SQLException se) {
			se.printStackTrace();
			log.error("CloseConnect():{}", se.getMessage());
		} // end finally try 20220607
		finally {
			selconn = null;
		}
		//----
	}

	private Connection getDB2Connection(String url, String username, String password) throws Exception {
		Class.forName("com.ibm.db2.jcc.DB2Driver");
		log.debug("Driver Loaded.");
		return DriverManager.getConnection(url, username, password);
	}
    //20210202 MatsudairaSyuMe change to use gievn url, username, password
	private Connection getHSQLConnection(String url, String username, String password) throws Exception {
		Class.forName("org.hsqldb.jdbcDriver");
		log.debug("Driver Loaded.");
//		String url = "jdbc:hsqldb:data/tutorial";
		return DriverManager.getConnection(url, username, password);
	}
	//20210118 MatsudairaSyuMe for vulnerability scanning sql injection defense
	private Connection getMySqlConnection(String url, String username, String password) throws Exception {
		String driver = "org.gjt.mm.mysql.Driver";
		//String url = "jdbc:mysql://localhost/demo2s";
		Class.forName(driver);
		Connection conn = DriverManager.getConnection(url, username, password);
		return conn;
	}

	//20210118 MatsudairaSyuMe for vulnerability scanning sql injection defense
	private Connection getOracleConnection(String url, String username, String password) throws Exception {
		String driver = "oracle.jdbc.driver.OracleDriver";
//		String url = "jdbc:oracle:thin:@localhost:1521:caspian";

		Class.forName(driver); // load Oracle driver
		Connection conn = DriverManager.getConnection(url, username, password);
		return conn;
	}

	/**
	 * @return the selurl
	 */
	public String getSelurl() {
		return selurl;
	}

	/**
	 * @param selurl the selurl to set
	 */
	public void setSelurl(String selurl) {
		this.selurl = selurl;
	}

	/**
	 * @return the seluser
	 */
	public String getSeluser() {
		return seluser;
	}

	/**
	 * @param seluser the seluser to set
	 */
	public void setSeluser(String seluser) {
		this.seluser = seluser;
	}

	/**
	 * @return the selpass
	 */
	public String getSelpass() {
		return selpass;
	}

	/**
	 * @param selpass the selpass to set
	 */
	public void setSelpass(String selpass) {
		this.selpass = selpass;
	}

	/**
	 * @return the sfn
	 */
	public String getSfn() {
		return sfn;
	}

	/**
	 * @param sfn the sfn to set
	 */
	public void setSfn(String sfn) {
		this.sfn = sfn;
	}
}
