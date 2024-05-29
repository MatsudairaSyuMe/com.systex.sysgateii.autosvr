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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.systex.sysgateii.autosvr.comm.Constants;
import com.systex.sysgateii.autosvr.conf.DynamicProps;  //20240508
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
		this.selconn = getDB2Connection(selurl, seluser, selpass); //20220609 private
		log.debug("Connected to database successfully...");
	}

	public GwDao(String selurl, String seluser, String selpass, boolean v) throws Exception {
		super();
		this.selurl = selurl;
		this.seluser = seluser;
		this.selpass = selpass;
//		log.debug("Connecting to a selected database...");
		this.selconn = getDB2Connection(selurl, seluser, selpass); //20220609 private
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
		log.atDebug().setMessage(String.format("Select from table %s... where %s=%s", fromTblName, keyname, selkeyval)).log();//20240517 change for Log Forging(debug)
		String keyset = "", keyset2 = "";
		String[] keynameary = keyname.split(",");
		String[] keyvalueary = selkeyval.split(",");
//		String[] keyvaluearynocomm = selkeyval.split(",");20210505 MatsudairaSyuMe Access Control: Database
		if (keynameary.length != keyvalueary.length)
			throw new Exception("given fields keyname can't correspond to keyvfield =>keynames [" + keyname + "] selkeyval [" + selkeyval + "]");
		else {
			for (int i = 0; i < keynameary.length; i++) {
				keyset = keyset + keynameary[i] + " = " + keyvalueary[i] + (i == (keynameary.length - 1) ? "" : " and ");
				keyset2 = keyset2 + keynameary[i] + " = " + "?"+ (i == (keynameary.length - 1) ? "" : " and ");
			}
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
		log.atDebug().setMessage("UPSERT selstr [{}]-->[{}]").addArgument(selstr).addArgument(wowstr).log();//20240517 change for Log Forging(debug)


		//----
		//20240503 mark up updval log.debug("update value [{}]", updval);
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
		log.atDebug().setMessage(String.format("Select from table %s... where %s=%s", fromTblName, keyname, selkeyval)).log();//20240517 change for Log Forging(debug)
		
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
			log.atDebug().setMessage("keyname = keyvalue=[{}]").addArgument(keyname + "=" + keyvalue).log();//20240517 change for Log Forging(debug)
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
			log.atDebug().setMessage("SELONEFLD selstr [{}]-->[{}]").addArgument(selstr).addArgument(wowstr).log();//20240517 change for Log Forging(debug)
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
			//20240503 MatsudairaMe mark for System Information Leak e.printStackTrace();
			log.error("error : exception");//20240503 change log message
		}
		log.atDebug().setMessage("return SELONEFLD=[{}]").addArgument(rtnVal).log();//20240517 change for Log Forging(debug)
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
			log.atDebug().setMessage("SELMFLD selstr [{}]-->[{}]").addArgument(selstr).addArgument(wowstr).log();//20240517 change for Log Forging(debug)
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
					//20240529 Dead Code: Expression is Always false never used in "autosvr" if (verbose)
					//	log.debug("ColumnName={} ColumnTypeName={} ", rsmd.getColumnName(columnCount), rsmd.getColumnTypeName(columnCount) );
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
			//20240503 MatsudairaSyuMe mark for System Information Leak e.printStackTrace();
			log.error("error : exception");//20240503 change log message
		}
		//20240529 Dead Code: Expression is Always false never used in "autosvr" if (verbose)
		//	log.debug("return SELMFLD length=[{}]", rtnVal.length);
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
					//20240529 Dead Code: Expression is Always false never use in "autosvr" if (verbose)
					//	log.debug("ColumnName={} ColumnTypeName={} ", rsmd.getColumnName(columnCount), rsmd.getColumnTypeName(columnCount) );
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
			//20240503 MAtsudairaSyuuMe mark for System Information Leak e.printStackTrace();
			log.error("error : exceoiption");//20240503 change log message
		}
		log.debug("return SELMFLDNOIDX length=[{}]", rtnVal.length);
		return rtnVal;
	}

	//20210118 MatsudairaSyuMe delete change for vulnerability scanning sql injection defense
	public String[] INSSELChoiceKey(String fromTblName, String field, String selupdval, String keyname, String selkeyval, boolean usekey, boolean verbose) throws Exception {
		String[] rtnVal = {};//20240523 prevent Redundant Null Check
		columnNames = new Vector<String>();
		columnTypes = new Vector<Integer>();
		if (fromTblName == null || fromTblName.trim().length() == 0 || field == null || field.trim().length() == 0
				|| keyname == null || keyname.trim().length() == 0)
			throw new Exception("given table name or field or keyname error =>" + fromTblName);
		log.atDebug().setMessage(String.format("first test Select from table %s... where %s=%s", fromTblName, keyname, selkeyval)).log();//20240517 change for Log Forging(debug)
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

		////20240529 Dead Code: Expression is Always false for "autosvr" if (usekey) {
		//	selstr = "SELECT " + keyname + "," + field + " FROM " + fromTblName + " where " + keyset;
		//} else {
			selstr = "SELECT " + field + " FROM " + fromTblName + " where " + keyset;
		//}
		//20210122 MatsudairaSyuMe
		String wowstr = Des.encode(Constants.DEFKNOCKING, selstr);
		log.atDebug().setMessage("sqlstr=[{}]-->[{}] selupdval value [{}] selkeyval [{}]").addArgument(selstr).addArgument(wowstr).addArgument(selupdval).addArgument(selkeyval).log();//20240517 change for Log Forging(debug)
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
					////20240529 Dead Code: Expression is Always false for "autosvr" if (usekey)
					//	insvalary = com.systex.sysgateii.autosvr.util.dataUtil.concatArray(keyvaluearynocomm, valarynocomm);
					//else
						insvalary = valarynocomm;
				//20201116
					cnvInsertStr = generateActualSql(SQL_INSERT, (Object[])insvalary);
				//20210202 MatsudairaSyuMe
					wowstr1 = Des.encode(Constants.DEFKNOCKING, cnvInsertStr);
				//----
					log.debug("record not exist using select insert:[{}] toString=[{}]", Des.decodeValue(Constants.DEFKNOCKING, wowstr1), cnvInsertStr);
				//----
				} catch(Exception e) {
					//20240503 MatsuDairaSyuMe mark for System Information Leak e.printStackTrace();
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
				log.atDebug().setMessage("executeUpdate()").log();//20240517 change for Log Forging(debug)
				int idx = 0;
				
				//20210426 MatsudairaSyuMe prevent Null Dereference
				rtnVal = new String[1];
				//----
				while (rs.next()) {
					//if (rtnVal != null) {
						String[] tmpv = rtnVal;
						rtnVal = new String[idx + 1];
						int j = 0;
						for (String s : tmpv) {
							rtnVal[j] = s;
							j++;
						}
					//}
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
		log.atDebug().setMessage(String.format("delete table %s... where %s=%s", fromTblName, keyname, selkeyval)).log();//20240517 change for Log Forging(debug)
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
		log.atDebug().setMessage("DELETETB deletesql=[{}]-->[{}] ").addArgument(deletesql).addArgument(wowstr).log();//20240517 change for Log Forging(debug)
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
		//20240503 mark if (verbose)
		//20240503 mark	log.debug("\tj=" + j + ":[" + obj + "]");
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
			//20240503 mark up if (verbose)
			//20240503 mark up 	log.debug("getTimestamp[{}]", obj);
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
			//20220613 MatsudairaSyuMe
			if (this.reusedpreparedStatement != null) {
				this.reusedpreparedStatement.close();
				this.reusedpreparedStatement = null;
			}
			if (this.reusedDeletepreparedStatement != null) {
				this.reusedDeletepreparedStatement.close();
				this.reusedDeletepreparedStatement = null;
			}
			if (this.reusedDevStspreparedStatement != null) {
				this.reusedDevStspreparedStatement.close();
				this.reusedDevStspreparedStatement = null;
			}
			if (this.reusedDevInspreparedStatement != null) {
				this.reusedDevInspreparedStatement.close();
				this.reusedDevInspreparedStatement = null;
			}
			if (this.reusedDevUpdpreparedStatement != null) {
				this.reusedDevUpdpreparedStatement.close();
				this.reusedDevUpdpreparedStatement = null;
			}
			if (this.reusedTBSDYpreparedStatement != null) {
				this.reusedTBSDYpreparedStatement.close();
				this.reusedTBSDYpreparedStatement = null;
			}
			//----
			if (selconn != null) { //20220607 MatsudairaSyuMe
				selconn.close();
			    selconn = null;
			}
		} catch (SQLException se) {
			//20240503 MatsudairaSyuMe mark for System Information Leak se.printStackTrace();
			log.error("CloseConnect():exception");//20240503 change log message
		} // end finally try 20220607
		finally {
			//20220613 MatsudairaSyuMe
			try {if(this.reusedpreparedStatement != null) this.reusedpreparedStatement.close();} catch(Exception e) {};
			this.reusedpreparedStatement = null;
			try {if(this.reusedDeletepreparedStatement != null) this.reusedDeletepreparedStatement.close();} catch(Exception e) {};
			this.reusedDeletepreparedStatement = null;
			try {if(this.reusedDevStspreparedStatement != null) this.reusedDevStspreparedStatement.close();} catch(Exception e) {};
			this.reusedDevStspreparedStatement = null;
			try {if(this.reusedDevInspreparedStatement != null) this.reusedDevInspreparedStatement.close();} catch(Exception e) {};
			this.reusedDevInspreparedStatement = null;
			try {if(this.reusedDevUpdpreparedStatement != null) this.reusedDevUpdpreparedStatement.close();} catch(Exception e) {};
			this.reusedDevUpdpreparedStatement = null;
			try {if(this.reusedTBSDYpreparedStatement != null) this.reusedTBSDYpreparedStatement.close();} catch(Exception e) {};
			this.reusedTBSDYpreparedStatement = null;
			try {if(this.selconn != null) this.selconn.close();} catch(Exception e) {};
			this.selconn = null;
		}
		//----
	}

	private Connection getDB2Connection(String url, String uid, String using) throws Exception {
		Class.forName("com.ibm.db2.jcc.DB2Driver");
		log.debug("Driver Loaded.");
		return DriverManager.getConnection(url, uid, DynamicProps.getDecryptedPd());//20240508
	}
    //20240503 MatsudairaSyuMe change to use given url,...
	private Connection getHSQLConnection(String url, String uid, String using) throws Exception {
		Class.forName("org.hsqldb.jdbcDriver");
		log.debug("Driver Loaded.");
//		String url = "jdbc:hsqldb:data/tutorial";
		return DriverManager.getConnection(url, uid, using);
	}
	//20210118 MatsudairaSyuMe for vulnerability scanning sql injection defense
	private Connection getMySqlConnection(String url, String uid, String using) throws Exception {
		String driver = "org.gjt.mm.mysql.Driver";
		//String url = "jdbc:mysql://localhost/demo2s";
		Class.forName(driver);
		Connection conn = DriverManager.getConnection(url, uid, using);
		return conn;
	}

	//20210118 MatsudairaSyuMe for vulnerability scanning sql injection defense
	private Connection getOracleConnection(String url, String uid, String using) throws Exception {
		String driver = "oracle.jdbc.driver.OracleDriver";
//		String url = "jdbc:oracle:thin:@localhost:1521:caspian";

		Class.forName(driver); // load Oracle driver
		Connection conn = DriverManager.getConnection(url, uid, using);
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
	//20220613 MatsudairaSyuMe create reused PreparedStatement
	private PreparedStatement reusedDevStspreparedStatement = null;
	private String preparedDevSelSqlStr = "";
	private PreparedStatement reusedDevInspreparedStatement = null;
	private String preparedDevInsSqlStr = "";
	private PreparedStatement reusedDevUpdpreparedStatement = null;
	private String preparedDevUpdSqlStr = "";

	public int UPSERT_R(String fromTblName, String field, String updval, String keyname, String selkeyval, boolean initType)
			throws Exception {
		if (fromTblName == null || fromTblName.trim().length() == 0 || field == null || field.trim().length() == 0
				|| keyname == null || keyname.trim().length() == 0)
			throw new Exception("given table name or field or keyname error =>" + fromTblName);
		log.atDebug().setMessage(String.format("Select from table %s... where %s=%s", fromTblName, keyname, selkeyval)).log();//20240517 change for Log Forging(debug)
		String keyset = "";
		int row = 0;
		boolean updateMode = false;
		String[] keynameary = keyname.split(",");
		String[] keyvalueary = selkeyval.split(",");
		String[] updvalary = updval.split(",");
		if (keynameary.length != keyvalueary.length)
			throw new Exception("given fields keyname can't correspond to keyvfield =>keynames [" + keyname + "] selkeyval [" + selkeyval + "]");
		else {
			for (int i = 0; i < keynameary.length; i++)
				keyset = keyset + keynameary[i] + " = " + keyvalueary[i] + (i == (keynameary.length - 1) ? "" : " and ");
		}
		try {
			if (initType) {
				String initkeyset = "";
				for (int i = 0; i < keynameary.length; i++)
					initkeyset = initkeyset + keynameary[i] + " = " + "?" + (i == (keynameary.length - 1) ? "" : " and ");
				String initkeyval = "";
				for (int i = 0; i < keynameary.length; i++)
					initkeyval = initkeyval + "?" + ((i == (keynameary.length - 1)) ? "" : ",");
				String initupdval = "";
				for (int i = 0; i < updvalary.length; i++)
					initupdval = initupdval + "?" + ((i == (updvalary.length - 1)) ? "" : ",");
				this.preparedDevSelSqlStr = "SELECT " + keyname + "," + field + " FROM " + fromTblName + " where " + initkeyset;
				this.preparedDevInsSqlStr = "INSERT INTO " + fromTblName + " (" + keyname + "," + field + ") VALUES (" + initkeyval + "," +  initupdval + ")";
				this.preparedDevUpdSqlStr = "UPDATE " + fromTblName + " SET (" + field  + ") = (" + initupdval + ") WHERE "
						+ keyset;
			}
			String wowstr = Des.encode(Constants.DEFKNOCKING, this.preparedDevSelSqlStr);
			log.atDebug().setMessage("UPSERT_R selstr [{}]-->[{}]").addArgument(this.preparedDevSelSqlStr).addArgument(wowstr).log();//20240517 change for Log Forging(debug)
			//20240503 mark up updval log.debug("UPSERT_R update value [{}]", updval);
			wowstr = Des.encode(Constants.DEFKNOCKING, this.preparedDevInsSqlStr);
			log.atDebug().setMessage("UPSERT_R insstr [{}]-->[{}]").addArgument(this.preparedDevInsSqlStr).addArgument(wowstr).log();//20240517 change for Log Forging(debug)
			wowstr = Des.encode(Constants.DEFKNOCKING, this.preparedDevUpdSqlStr);
			log.atDebug().setMessage("UPSERT_R updstr [{}]-->[{}]").addArgument(this.preparedDevUpdSqlStr).addArgument(wowstr).log();//20240517 change for Log Forging(debug)

			//20240529 Dead Code: Expression is Always true in "autosvr" if (initType) {
				try {
					if (this.reusedDevStspreparedStatement != null)
						this.reusedDevStspreparedStatement.close();
					this.reusedDevStspreparedStatement = null;
					if (this.reusedDevInspreparedStatement != null)
						this.reusedDevInspreparedStatement.close();
					this.reusedDevInspreparedStatement = null;
					if (this.reusedDevUpdpreparedStatement != null)
						this.reusedDevUpdpreparedStatement.close();
					this.reusedDevUpdpreparedStatement = null;

				} catch (Exception e) {
					//20240503 MatsudairaSyuMe mark for System Information Leak e.printStackTrace();
					log.error("ERROR !! this.reusedDevStspreparedStatement.close() or this.reusedDevInspreparedStatement.close() or this.reusedDevUpdpreparedStatement.close();: exception");//20240503 change log message
				} finally {
					if (this.reusedDevStspreparedStatement != null) {
						try {this.reusedDevStspreparedStatement.close();} catch (Exception ie) {log.error("ERROR !! reusedDevStspreparedStatement close anyaway");}
						this.reusedDevStspreparedStatement = null;  //close any away
					}
					if (this.reusedDevInspreparedStatement != null) {
						try {this.reusedDevInspreparedStatement.close();} catch (Exception ie) {log.error("ERROR !! reusedDevStspreparedStatement close anyaway");}
						this.reusedDevInspreparedStatement = null;  //close any away
					}
					if (this.reusedDevUpdpreparedStatement != null) {
						try {this.reusedDevUpdpreparedStatement.close();} catch (Exception ie) {log.error("ERROR !! reusedDevStspreparedStatement close anyaway");}
						this.reusedDevUpdpreparedStatement = null;  //close any away
					}
				}
				wowstr = Des.encode(Constants.DEFKNOCKING, this.preparedDevSelSqlStr);
				this.reusedDevStspreparedStatement = selconn.prepareStatement(Des.decodeValue(Constants.DEFKNOCKING, wowstr));
				wowstr = Des.encode(Constants.DEFKNOCKING, this.preparedDevInsSqlStr);
				this.reusedDevInspreparedStatement = selconn.prepareStatement(Des.decodeValue(Constants.DEFKNOCKING, wowstr));
				wowstr = Des.encode(Constants.DEFKNOCKING, this.preparedDevUpdSqlStr);
				this.reusedDevUpdpreparedStatement = selconn.prepareStatement(Des.decodeValue(Constants.DEFKNOCKING, wowstr));
			/*} else {
				int startidx = 1;
				for (String s: keyvalueary) {
					s = s.trim();
					if (s.startsWith("'") && s.endsWith("'")) {
						s = s.replaceAll("^\'|\'$", "");
						this.reusedDevStspreparedStatement.setString(startidx, s);
					} else
						this.reusedDevStspreparedStatement.setInt(startidx, Integer.valueOf(s));
					startidx +=1;
				}
				ResultSet tblrs = this.reusedDevStspreparedStatement.executeQuery();
				int idx = 0;
				if (tblrs != null) {
					while (tblrs.next()) {
						idx++;
					}
					tblrs.close();
				}
				this.reusedDevStspreparedStatement.clearParameters();
				if (idx > 0) {
					log.debug("update mode");
					updateMode = true;
				} else {
					log.debug("insert mode");
					updateMode = false;					
				}
				if (updateMode) {
					startidx = 1;
					for (String s: updvalary) {
						s = s.trim();
						if (s.startsWith("'") && s.endsWith("'")) {
							s = s.replaceAll("^\'|\'$", "");
							this.reusedDevUpdpreparedStatement.setString(startidx, s);
						} else
							this.reusedDevUpdpreparedStatement.setInt(startidx, Integer.valueOf(s));
						startidx +=1;
					}
					for (String s: keyvalueary) {
						s = s.trim();
						if (s.startsWith("'") && s.endsWith("'")) {
							s = s.replaceAll("^\'|\'$", "");
							this.reusedDevInspreparedStatement.setString(startidx, s);
						} else
							this.reusedDevInspreparedStatement.setInt(startidx, Integer.valueOf(s));
						startidx +=1;
					}
					row = this.reusedDevUpdpreparedStatement.executeUpdate();
					log.atDebug().setMessage("record exist using update:{} result=[{}]").addArgument(this.preparedDevUpdSqlStr).addArgument(row).log();//20240517 change for Log Forging(debug)
					this.reusedDevUpdpreparedStatement.clearParameters();
				} else {
					startidx = 1;
					for (String s: keyvalueary) {
						s = s.trim();
						if (s.startsWith("'") && s.endsWith("'")) {
							s = s.replaceAll("^\'|\'$", "");
							this.reusedDevInspreparedStatement.setString(startidx, s);
						} else
							this.reusedDevInspreparedStatement.setInt(startidx, Integer.valueOf(s));
						startidx +=1;
					}
					for (String s: updvalary) {
						s = s.trim();
						if (s.startsWith("'") && s.endsWith("'")) {
							s = s.replaceAll("^\'|\'$", "");
							this.reusedDevInspreparedStatement.setString(startidx, s);
						} else
							this.reusedDevInspreparedStatement.setInt(startidx, Integer.valueOf(s));
						startidx +=1;
					}
					row = this.reusedDevInspreparedStatement.executeUpdate();
					log.debug("record not exist using insert:{} result=[{}]", this.preparedDevInsSqlStr, row);
					this.reusedDevInspreparedStatement.clearParameters();
				}
			}*/
		} catch (Exception e) {
			//20240503 MatsudairaSyuMe mark for System Information Leak e.printStackTrace();
			log.error("error UPSERT_R: exception");//20240503 change log message
		}
		//20240529 Dead Code: Expression is Always false in "autosvr"if (!initType)
		//	log.debug("return UPSERT_R length=[{}]", row);
		return row;
	}
	private PreparedStatement reusedpreparedStatement = null;
	private String preparedSqlStr = "";
	public String[] SELMFLD_R(String fromTblName, String fieldsn, String keyname, String keyvalue, boolean initType)
			throws Exception {
		String[] rtnVal = {};
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
			if (keynameary.length != keyvalueary.length)
				throw new Exception("given fields keyname can't correspond to keyvfield =>keynames [" + keyname + "] keyvalues [" + keyvalue + "]");
			else {
				for (int i = 0; i < keynameary.length; i++)
					keyset = keyset + keynameary[i] + " = " + keyvalueary[i] + (i == (keynameary.length - 1) ? "" : " and ");
			}

			if ((keyname.indexOf(',') > -1) && (keyvalue.indexOf(',') > -1)
					&& (keynameary.length != keyvalueary.length))
				return rtnVal;

			if (initType)
				this.preparedSqlStr = "SELECT " + fieldsn + " FROM " + fromTblName + " where " + keyset;
			String wowstr = Des.encode(Constants.DEFKNOCKING, this.preparedSqlStr);
			log.debug("SELMFLD selstr [{}]-->[{}]", this.preparedSqlStr, wowstr);
			//20240529 Dead Code: Expression is Always true in "autosvr" always true if (initType) {
				try {
					if (this.reusedpreparedStatement != null)
						this.reusedpreparedStatement.close();
					this.reusedpreparedStatement = null;
				} catch (Exception e) {
					//20240503 MAtsudairaSyuMe mark for System Information Leak e.printStackTrace();
					log.error("ERROR !! this.reusedpreparedStatement.close(): exception");//20240503 change log message
				} finally {
					if (this.reusedpreparedStatement != null) {
						try {this.reusedpreparedStatement.close();} catch (Exception ie) {log.error("ERROR !! reusedpreparedStatement close anyaway");}
						this.reusedpreparedStatement = null;  //close any away
					}
				}
				this.reusedpreparedStatement = selconn.prepareStatement(Des.decodeValue(Constants.DEFKNOCKING, wowstr));
			/*} else {
				int startidx = 1;
				for (String s: keyvalueary) {
					s = s.trim();
					if (s.startsWith("'") && s.endsWith("'")) {
						s = s.replaceAll("^\'|\'$", "");
						this.reusedpreparedStatement.setString(startidx, s);
					} else
						this.reusedpreparedStatement.setInt(startidx, Integer.valueOf(s));
					startidx +=1;
				}
				ResultSet tblrs = this.reusedpreparedStatement.executeQuery();

				if (tblrs != null) {
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
				this.reusedpreparedStatement.clearParameters();
			}*/
		} catch (Exception e) {
			//20240503 MatsudairaSyuMe mark for System Information Leak e.printStackTrace();
			log.error("error : exception");//20240503 change log message
		}
		//20240529 Dead Code: Expression is Always false never used in "autosvr" if (!initType)
		//	log.debug("return SELMFLD_R length=[{}]", rtnVal.length);
		return rtnVal;
	}
	private PreparedStatement reusedTBSDYpreparedStatement = null;
	private String preparedTBSDYSqlStr = "";
	public String SELTBSDY_R(String fromTblName, String fieldn, String keyname, String keyvalue, boolean initType)
			throws Exception {
		String rtnVal = "";
		if (fromTblName == null || fromTblName.trim().length() == 0 || fieldn == null || fieldn.trim().length() == 0
				|| keyname == null || keyname.trim().length() == 0)
			return rtnVal;
		try {
			log.debug("keyname = keyvalue=[{}]",  keyname + "=" + keyvalue);
			String keyset = "";
			String[] keynameary = keyname.split(",");
			String[] keyvalueary = keyvalue.split(",");
			if (keynameary.length != keyvalueary.length)
				throw new Exception("given fields keyname can't correspond to keyvfield =>keynames [" + keyname + "] keyvalues [" + keyvalue + "]");
			else {
				for (int i = 0; i < keynameary.length; i++)
					keyset = keyset + keynameary[i] + " = " + keyvalueary[i] + (i == (keynameary.length - 1) ? "" : " and ");
			}

			if ((keyname.indexOf(',') > -1) && (keyvalue.indexOf(',') > -1)
					&& (keynameary.length != keyvalueary.length))
				return rtnVal;
			if (initType)
				this.preparedTBSDYSqlStr = "SELECT " + fieldn + " FROM " + fromTblName + " where " + keyset;
			String wowstr = Des.encode(Constants.DEFKNOCKING, this.preparedTBSDYSqlStr);
			log.debug("SELTBSDY_R selstr [{}]-->[{}]",this.preparedTBSDYSqlStr, wowstr);
			if (initType) {
				try {
					if (this.reusedTBSDYpreparedStatement != null)
						this.reusedTBSDYpreparedStatement.close();
					this.reusedTBSDYpreparedStatement = null;
				} catch (Exception e) {
					//20240503 MAtsudairaSyuMe mark for System Information Leak e.printStackTrace();
					log.error("ERROR !! this.reusedTBSDYpreparedStatement.close(): exception");//20240503 change log message
				} finally {
					if (this.reusedTBSDYpreparedStatement != null) {
						try {this.reusedTBSDYpreparedStatement.close();} catch (Exception ie) {log.error("ERROR !! reusedTBSDYpreparedStatement close anyaway");}
						this.reusedTBSDYpreparedStatement = null;  //close any away
					}
				}
				this.reusedTBSDYpreparedStatement = selconn.prepareStatement(Des.decodeValue(Constants.DEFKNOCKING, wowstr));
			} else {
				int startidx = 1;
				for (String s: keyvalueary) {
					s = s.trim();
					if (s.startsWith("'") && s.endsWith("'")) {
						s = s.replaceAll("^\'|\'$", "");
						this.reusedTBSDYpreparedStatement.setString(startidx, s);
					} else
						this.reusedTBSDYpreparedStatement.setInt(startidx, Integer.valueOf(s));
					startidx +=1;
				}
				ResultSet tblrs = this.reusedTBSDYpreparedStatement.executeQuery();
				int idx = 0;
				while (tblrs.next()) {
					if (idx == 0)
						rtnVal = tblrs.getString(fieldn);
					else
						rtnVal = rtnVal + "," + tblrs.getString(fieldn);
					idx++;
				}
				tblrs.close();
				this.reusedTBSDYpreparedStatement.clearParameters();
			}
		} catch (Exception e) {
			//20240503 MatsudairaSyuMe mark for System Information Leak e.printStackTrace();
			log.error("error SELTBSDY_R : exception");//20240503 change log message
		}
		log.atDebug().setMessage("return SELTBSDY_R=[{}]").addArgument(rtnVal).log();//20240517 change for Log Forging(debug)
		return rtnVal;
	}
	private PreparedStatement reusedDeletepreparedStatement = null;
	private String preparedDeleteSqlStr = "";
	public boolean DELETETB_R(String fromTblName, String keyname, String selkeyval, boolean initType)
			throws Exception {
		if (fromTblName == null || fromTblName.trim().length() == 0 || keyname == null || keyname.trim().length() == 0)
			throw new Exception("given table name or field or keyname error =>" + fromTblName);
		log.debug(String.format("delete table %s... where %s=%s", fromTblName, keyname, selkeyval));
		String[] keynameary = keyname.split(",");
		String[] keyvalueary = selkeyval.split(",");
		String deletesql = "DELETE FROM " + fromTblName + " WHERE ";

		if (keyname.indexOf(',') > -1 && selkeyval.indexOf(',') > -1) {
			keynameary = keyname.split(",");
			keyvalueary = selkeyval.split(",");
			if (keynameary.length != keyvalueary.length)
				throw new Exception("given fields keyname can't correspond to keyvfield =>keynames [" + keyname + "] selkayvals [" + selkeyval + "]");
			else {
				for (int i = 0; i < keynameary.length; i++)
					deletesql = deletesql + keynameary[i] + " = " + keyvalueary[i] + (i == (keynameary.length - 1) ? "" : " and ");
			}
		} else
			deletesql = deletesql + keyname + " = " + selkeyval;
		if (initType)
			this.preparedDeleteSqlStr = deletesql;
		String wowstr = Des.encode(Constants.DEFKNOCKING, this.preparedDeleteSqlStr);
		log.debug("DELETETB_R deletesql=[{}]-->[{}] ", this.preparedDeleteSqlStr, wowstr);
		//----
		//20240529 Dead Code: Expression is Always true always true in "autosvr" if (initType) {
			try {
				if (this.reusedDeletepreparedStatement != null)
					this.reusedDeletepreparedStatement.close();
				this.reusedDeletepreparedStatement = null;
			} catch (Exception e) {
				//20240503 MatsudairaSyuMe mark for System Information Leak e.printStackTrace();
				log.error("ERROR !! this.reusedDeletepreparedStatement.close(): exception");//20240503 change log messge
			} finally {
				if (this.reusedDeletepreparedStatement != null) {
					try {this.reusedDeletepreparedStatement.close();} catch (Exception ie) {log.error("ERROR !! reusedDeletepreparedStatement close anyaway");}
					this.reusedDeletepreparedStatement = null;  //close any away
				}
			}
			this.reusedDeletepreparedStatement = selconn.prepareStatement(Des.decodeValue(Constants.DEFKNOCKING, wowstr));
			return true;
		/*} else {
			int startidx = 1;
			for (String s: keyvalueary) {
				s = s.trim();
				if (s.startsWith("'") && s.endsWith("'")) {
					s = s.replaceAll("^\'|\'$", "");
					this.reusedDeletepreparedStatement.setString(startidx, s);
					log.debug("DELETETB_R deletesql idx[{}] setString [{}] ", startidx, s);
				} else {
					this.reusedDeletepreparedStatement.setInt(startidx, Integer.valueOf(s));
					log.debug("DELETETB_R deletesql idx[{}] setInt [{}] ", startidx, Integer.valueOf(s));
				}
				startidx +=1;
			}
			boolean rtn = this.reusedDeletepreparedStatement.execute();
			this.reusedpreparedStatement.clearParameters();
			return rtn;
		}*/
	}
	public Connection getConn() {
		return this.selconn;
	}

	//20230316 MatsudairaSuMe get one field byte array
	public byte[] SELONEFLDBary(String fromTblName, String fieldn, String keyname, String keyvalue, boolean verbose)
			throws Exception {
		byte[] rtnVal = null;
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
			if (keynameary.length != keyvalueary.length)
				throw new Exception("given fields keyname can't correspond to keyvfield =>keynames [" + keyname + "] keyvalues [" + keyvalue + "]");
			else {
				for (int i = 0; i < keynameary.length; i++)
					keyset = keyset + keynameary[i] + " = " + keyvalueary[i] + (i == (keynameary.length - 1) ? "" : " and ");
			}

			if ((keyname.indexOf(',') > -1) && (keyvalue.indexOf(',') > -1)
					&& (keynameary.length != keyvalueary.length))
				return rtnVal;
			String selstr = "SELECT " + fieldn + " FROM " + fromTblName + " where " + keyset;
			String wowstr = Des.encode(Constants.DEFKNOCKING, selstr);
			log.debug("SELONEFLDBary selstr [{}]-->[{}]",selstr, wowstr);
			Statement stmt = selconn.createStatement();
			ResultSet tblrs = stmt.executeQuery(Des.decodeValue(Constants.DEFKNOCKING, wowstr));
			
			int type = -1;
			if (tblrs != null) {
				ResultSetMetaData rsmd = tblrs.getMetaData();
				int columnCount = 0;
				while (columnCount < rsmd.getColumnCount()) {
					columnCount++;
					type = rsmd.getColumnType(columnCount);
					//20240529 Dead Code: Expression is Always false never use in "autosvr" if (verbose)
					//	log.debug("ColumnName={} ColumnTypeName={} ", rsmd.getColumnName(columnCount), rsmd.getColumnTypeName(columnCount) );
					tbsdytblcolumnNames.add(rsmd.getColumnName(columnCount));
					tbsdytblcolumnTypes.add(type);
				}
				while (tblrs.next()) {
					rtnVal = tblrs.getBytes(fieldn);
				}
				tblrs.close();
			}
		} catch (Exception e) {
			//20240503 MatsudairaSyuMe mark for System Information Leak e.printStackTrace();
			log.error("error : exception");//20240503 change log message
		}
		log.atDebug().setMessage("return SELONEFLDBary={}").addArgument(rtnVal).log();//20240517 change for Log Forging(debug)
		return rtnVal;
	}

	//----
	//20240412 add TW passbook's category from DB
	public String[] selectTWPBCategory(String paramType) throws SQLException {
		String selstr = "SELECT PARAM_CODE FROM TBSYSPARAMETER WHERE PARAM_TYPE = ?";
		PreparedStatement pstmt = selconn.prepareStatement(selstr);
		pstmt.setString(1, paramType);
		ResultSet rs = pstmt.executeQuery();
		List<String> paramCodes = new ArrayList<>();
		while(rs.next()) {
			paramCodes.add(rs.getString("PARAM_CODE"));
		}
		return paramCodes.toArray(new String[0]);
	}
	/*
	public int UPSERT(String fromTblName, String field, String updval, String keyname, String selkeyval) {
		int rtn = -1;
		if (fromTblName == null || fromTblName.trim().length() == 0 || field == null || field.trim().length() == 0
				|| keyname == null || keyname.trim().length() == 0) {
			log.error("given table name or field or keyname error =>{}", fromTblName);
			return -1;
		}
		log.debug(String.format("Select from table {%s}... where {%s}={%s} updval={%s}", fromTblName, keyname, selkeyval, updval));
		String keyset = "";
		String[] keynameary = keyname.split(",");
		String[] keyvalueary = selkeyval.split(",");
		String[] updvalary = updval.split(",");
		if (keynameary.length != keyvalueary.length || updvalary == null || (updvalary != null && updvalary.length == 0)) {
			log.error("given fields keyname can't correspond to keyvfield =>keynames [{}] selkeyval [{}]", keyname,  selkeyval);
			return -1;
		} else {
			for (int i = 0; i < keynameary.length; i++) {
				keyset = keyset + keynameary[i] + " = " + "?" + (i == (keynameary.length - 1) ? "" : " and ");
				keyvalueary[i] = keyvalueary[i].trim();
			}
			for (int i = 0; i < updvalary.length; i++)
				updvalary[i] = updvalary[i].trim();
		}
		String sqlstr;
		int type = -1;
		if (fromTblName.toUpperCase().indexOf("TB_AUDEVSTS") != -1) {
			type = 0;
			sqlstr = "SELECT IP,PORT,SYSIP,SYSPORT,ACTPAS,DEVTPE,CURSTUS,VERSION,CREATOR,MODIFIER FROM TB_AUDEVSTS WHERE BRWS = ? AND SVRID = ?";
		} else if (fromTblName.toUpperCase().indexOf("TB_AUSVRSTS") != -1) {
			type = 1;
			sqlstr = "SELECT AUID,IP,CURSTUS,PID,CREATOR,MODIFIER,LASTUPDATE FROM TB_AUSVRSTS WHERE SVRID = ?";
		} else if (fromTblName.toUpperCase().indexOf("TB_AUGEN") != -1) {
			type = 2;
			sqlstr = "SELECT TBSDY FROM TB_AUGEN WHERE BKNO=?";
		} else if (fromTblName.toUpperCase().indexOf("TB_AUDEVCMD") != -1) {
			type = 3;
			sqlstr = "SELECT SVRID,BRWS,CMD,AUID,CMDCREATETIME,EMPNO FROM TB_AUDEVCMD WHERE SVRID=? AND BRWS=?";
		} else {  //TB_AUSVRCMD
			type = 4;
			sqlstr = "SELECT SVRID,IP,CMD,CMDCREATETIME,CMDRESULT,CMDRESULTTIME,EMPNO FROM TB_AUSVRCMD WHERE SVRID=?";
		}
		log.debug("UPSERT2 selstr [{}]", sqlstr);
		try {
			PreparedStatement pstmt = selconn.prepareStatement(sqlstr);
			for (int i = 0; i < keyvalueary.length; i++) {
				if (keyvalueary[i].startsWith("'") && keyvalueary[i].endsWith("'")) {
					pstmt.setString(i + 1, keyvalueary[i].substring(1, keyvalueary[i].length() - 1));
				} else {
					pstmt.setInt(i + 1,Integer.decode(keyvalueary[i]));
				}
			}
			ResultSet selectresult = pstmt.executeQuery();
			if (selectresult.next()) {
				//update mode;
				switch (type) {
					case 0:
						sqlstr = "UPDATE TB_AUDEVSTS SET IP=?,PORT=?,SYSIP=?,SYSPORT=?,ACTPAS=?,DEVTPE=?,CURSTUS=?,VERSION=?,CREATOR=?,MODIFIER=? WHERE BRWS=? AND SVRID=?";
						break;
					case 1:
						sqlstr = "UPDATE TB_AUSVRSTS SET AUID=?,IP=?,CURSTUS=?,PID=?,CREATOR=?,MODIFIER=?,LASTUPDATE=? WHERE SVRID=?";
						break;
					case 2:
						sqlstr = "UPDATE TB_AUGEN SET TBSDY=? WHERE BKNO=?";
						break;
					case 3:
						sqlstr = "UPDATE TB_AUDEVCMD SET AUID=?,CMD=?,CMDCREATETIME=?,CMDRESULT=?,CMDRESULTTIME=?,EMPNO=? WHERE SVRID=? AND BRWS=?";
						break;
					case 4:
						sqlstr = "UPDATE TB_AUSVRCMD SET IP=?, CMD=?,CMDCREATETIME=?,CMDRESULT=?,CMDRESULTTIME=?,EMPNO=? WHERE SVRID=?";
						break;
				}
				log.debug("UPSERT2 pstmt.executeQuery() update mode {}", sqlstr);
				pstmt = selconn.prepareStatement(sqlstr);
				int j = 1;
				for (int i = 0; i < updvalary.length; i++) {
					if (updvalary[i].startsWith("'") && updvalary[i].endsWith("'")) {
						pstmt.setString(j, updvalary[i].substring(1,updvalary[i].length() - 1));
					} else {
						pstmt.setInt(j,Integer.decode( updvalary[i]));
					}
					j += 1;
				}
				for (int i = 0; i < keyvalueary.length; i++) {
					if (keyvalueary[i].startsWith("'") && keyvalueary[i].endsWith("'")) {
						pstmt.setString(j, keyvalueary[i].substring(1, keyvalueary[i].length() - 1));
					}else {
						pstmt.setInt(j,Integer.decode(keyvalueary[i]));
					}
					j += 1;
				}
				rtn = pstmt.executeUpdate();
			} else {
				//insert mode;
				switch (type) {
					case 0:
						sqlstr = "INSERT INTO TB_AUDEVSTS (BRWS,SVRID,IP,PORT,SYSIP,SYSPORT,ACTPAS,DEVTPE,CURSTUS,VERSION,CREATOR,MODIFIER) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
						break;
					case 1:
						sqlstr = "INSERT INTO TB_AUSVRSTS (SVRID,AUID,IP,CURSTUS,PID,CREATOR,MODIFIER,LASTUPDATE) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
						break;
					case 2:
						sqlstr = "INSERT INTO TB_AUGEN (BKNO, TBSDY) VALUES (?,?)";
						break;
					case 3:
						sqlstr = "INSERT INTO TB_AUDEVCMD (SVRID,BRWS,AUID,CMD,CMDCREATETIME,CMDRESULT,CMDRESULTTIME,EMPNO) VALUES (?,?,?,?,?,?,?,?)";
						break;
					case 4:
						sqlstr = "INSERT INTO TB_AUSVRCMD (SVRID,IP,CMD,CMDCREATETIME,CMDRESULT,CMDRESULTTIME,EMPNO) VALUES (?,?,?,?,?,?,?)";
						break;
				}
				log.debug("UPSERT2 pstmt.executeQuery() insert mode {}", sqlstr);
				pstmt = selconn.prepareStatement(sqlstr);
				int j = 1;
				for (int i = 0; i < keyvalueary.length; i++) {
					if (keyvalueary[i].startsWith("'") && keyvalueary[i].endsWith("'")) {
						pstmt.setString(j, keyvalueary[i].substring(1, keyvalueary[i].length() - 1));
					} else {
						pstmt.setInt(j,Integer.decode(keyvalueary[i]));
					}
					j += 1;
				}
				for (int i = 0; i < updvalary.length; i++) {
					if (updvalary[i].startsWith("'") && updvalary[i].endsWith("'")) {
						pstmt.setString(j, updvalary[i].substring(1,updvalary[i].length() - 1));
					}else {
						pstmt.setInt(j,Integer.decode( updvalary[i]));
					}
					j += 1;
				}
				rtn = pstmt.executeUpdate();
			}
		} catch(Exception e) {
			return -1;
		}
		return rtn;
	}
	*/
}
