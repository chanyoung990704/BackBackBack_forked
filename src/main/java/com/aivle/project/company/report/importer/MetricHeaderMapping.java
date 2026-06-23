package com.aivle.project.company.report.importer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 엑셀 헤더(지표명) → metric_code 매핑.
 */
public final class MetricHeaderMapping {

	private static final Map<String, String> NAME_TO_CODE;

	static {
		Map<String, String> mapping = new HashMap<>();
		mapping.put("ROA", "ROA");
		mapping.put("ROE", "ROE");
		mapping.put("매출액영업이익률", "OpMargin");
		mapping.put("부채비율", "DbRatio");
		mapping.put("자기자본비율", "EqRatio");
		mapping.put("자본잠식률", "CapImpRatio");
		mapping.put("단기차입금비율", "STDebtRatio");
		mapping.put("유동비율", "CurRatio");
		mapping.put("당좌비율", "QkRatio");
		mapping.put("유동부채비율", "CurLibRatio");
		mapping.put("CFO_자산비율", "CFO_AsRatio");
		mapping.put("CFO_매출액비율", "CFO_Sale");
		mapping.put("CFO증감률", "CFO_GR");
		NAME_TO_CODE = Collections.unmodifiableMap(mapping);
	}

	private MetricHeaderMapping() {
	}

	public static String toMetricCode(String headerName) {
		if (headerName == null) {
			return null;
		}
		return NAME_TO_CODE.get(headerName.trim());
	}
}
