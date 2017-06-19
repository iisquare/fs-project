package com.iisquare.etl.spark.utils;

import java.io.IOException;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.protobuf.ProtobufUtil;
import org.apache.hadoop.hbase.protobuf.generated.ClientProtos;
import org.apache.hadoop.hbase.util.Base64;

public class HBaseUtil {

	public static String convertScanToString(Scan scan) {
		ClientProtos.Scan proto = null;
		try {
			proto = ProtobufUtil.toScan(scan);
		} catch (IOException e) {
			return null;
		}
		return Base64.encodeBytes(proto.toByteArray());
	}
	
	public static String cloneValue(Cell cell){
		if(null == cell) return null;
		return new String(CellUtil.cloneValue(cell));
	}
	
	public static String cloneValue(Result result, String family, String qualifier){
		return cloneValue(result.getColumnLatestCell(
			null == family ? null : family.getBytes(), null == qualifier ? null : qualifier.getBytes()));
	}
	
}
