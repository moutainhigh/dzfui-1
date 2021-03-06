package com.dzf.zxkj.report.excel.rptexp;

import com.dzf.zxkj.platform.model.report.LrbVO;
import com.dzf.zxkj.platform.model.report.XjllbVO;
import com.dzf.zxkj.platform.model.report.ZcFzBVO;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.Map;

public interface MoreWorkBookKj2007Excel {
    Workbook createWorkBookZcfzKj2007(Map<String, ZcFzBVO> vOMap, Map<String, String> taxaxVoMap) throws Exception;
    Workbook createWorkBookLrbKj2007(Map<String, LrbVO> vOMap, Map<String, String> taxaxVoMap) throws Exception;
    Workbook createWorkBookXjllKj2007(Map<String, XjllbVO> vOMap, Map<String, String> taxaxVoMap) throws Exception;
}
