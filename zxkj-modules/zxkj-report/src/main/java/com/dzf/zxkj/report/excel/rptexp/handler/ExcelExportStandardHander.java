package com.dzf.zxkj.report.excel.rptexp.handler;

import com.dzf.model.gl.gl_cwreport.LrbVO;
import com.dzf.model.gl.gl_cwreport.XjllbVO;
import com.dzf.model.gl.gl_cwreport.ZcFzBVO;
import com.dzf.report.enums.ExportTemplateEnum;
import com.dzf.service.gl.gl_cwreport.rptexp.ExcelExportHander;
import com.dzf.service.gl.gl_cwreport.rptexp.MoreWorkBookKj2007Excel;
import com.dzf.service.gl.gl_cwreport.rptexp.MoreWorkBookKj2013Excel;
import com.dzf.service.gl.gl_cwreport.rptexp.ResourceUtil;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.core.io.Resource;

import java.util.Map;

public class ExcelExportStandardHander extends ExcelExportHander implements MoreWorkBookKj2007Excel, MoreWorkBookKj2013Excel {
    @Override
    public Workbook createWorkBookZcfzKj2007(Map<String, ZcFzBVO> vOMap, Map<String, String> taxaxVoMap) throws Exception {
        Resource resource = ResourceUtil.get(ExportTemplateEnum.STANDARD, ResourceUtil.ResourceEnum.KJ2007ZCFZ);
        Workbook workbook = WorkbookFactory.create(resource.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        handleZcfzbSheet(sheet, taxaxVoMap, vOMap, 5, new Integer[]{0,2,3,4,6,7},new String[]{"qmye1","ncye1","qmye2","ncye2"});
        return workbook;
    }

    @Override
    public Workbook createWorkBookLrbKj2007(Map<String, LrbVO> vOMap, Map<String, String> taxaxVoMap) throws Exception {
        Resource resource = ResourceUtil.get(ExportTemplateEnum.STANDARD, ResourceUtil.ResourceEnum.KJ2007LR);
        Workbook workbook = WorkbookFactory.create(resource.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        handleLrbSheet(sheet, taxaxVoMap, vOMap, 4, new Integer[]{0,2,3}, new String[]{"byje","bnljje"});
        return workbook;
    }

    @Override
    public Workbook createWorkBookXjllKj2007(Map<String, XjllbVO> vOMap, Map<String, String> taxaxVoMap) throws Exception {
        Resource resource = ResourceUtil.get(ExportTemplateEnum.STANDARD, ResourceUtil.ResourceEnum.KJ2007XJLL);
        Workbook workbook = WorkbookFactory.create(resource.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        handleXjllSheet(sheet, taxaxVoMap, vOMap, 4, new Integer[]{0,2,3}, new String[]{"bqje","sqje"});
        return workbook;
    }

    @Override
    public Workbook createWorkBookZcfzKj2013(Map<String, ZcFzBVO> vOMap, Map<String, String> taxaxVoMap) throws Exception {
        Resource resource = ResourceUtil.get(ExportTemplateEnum.STANDARD, ResourceUtil.ResourceEnum.KJ2013ZCFZ);
        Workbook workbook = WorkbookFactory.create(resource.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        handleZcfzbSheet(sheet, taxaxVoMap, vOMap, 5, new Integer[]{0,2,3,4,6,7},new String[]{"qmye1","ncye1","qmye2","ncye2"});
        return workbook;
    }

    @Override
    public Workbook createWorkBookLrbKj2013(Map<String, LrbVO> vOMap, Map<String, String> taxaxVoMap) throws Exception {
        Resource resource = ResourceUtil.get(ExportTemplateEnum.STANDARD, ResourceUtil.ResourceEnum.KJ2013LR);
        Workbook workbook = WorkbookFactory.create(resource.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        handleLrbSheet(sheet, taxaxVoMap, vOMap, 4, new Integer[]{0,2,3}, new String[]{"bnljje","byje"});
        return workbook;
    }

    @Override
    public Workbook createWorkBookXjllKj2013(Map<String, XjllbVO> vOMap, Map<String, String> taxaxVoMap) throws Exception {
        Resource resource = ResourceUtil.get(ExportTemplateEnum.STANDARD, ResourceUtil.ResourceEnum.KJ2013XJLL);
        Workbook workbook = WorkbookFactory.create(resource.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        handleXjllSheet(sheet, taxaxVoMap, vOMap, 5, new Integer[]{0,2,3}, new String[]{"sqje","bqje"});
        return workbook;
    }
}
