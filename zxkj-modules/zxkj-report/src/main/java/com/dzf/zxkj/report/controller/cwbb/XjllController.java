package com.dzf.zxkj.report.controller.cwbb;

import com.alibaba.fastjson.JSON;
import com.dzf.zxkj.base.query.KmReoprtQueryParamVO;
import com.dzf.zxkj.base.utils.DzfTypeUtils;
import com.dzf.zxkj.base.utils.FieldMapping;
import com.dzf.zxkj.common.constant.ISysConstants;
import com.dzf.zxkj.common.entity.Grid;
import com.dzf.zxkj.common.entity.Json;
import com.dzf.zxkj.common.entity.ReturnData;
import com.dzf.zxkj.common.enums.LogRecordEnum;
import com.dzf.zxkj.common.lang.DZFDate;
import com.dzf.zxkj.common.lang.DZFDouble;
import com.dzf.zxkj.common.query.QueryPageVO;
import com.dzf.zxkj.common.query.QueryParamVO;
import com.dzf.zxkj.common.utils.CodeUtils1;
import com.dzf.zxkj.common.utils.DateUtils;
import com.dzf.zxkj.common.utils.StringUtil;
import com.dzf.zxkj.excel.util.Excelexport2003;
import com.dzf.zxkj.jackson.annotation.MultiRequestBody;
import com.dzf.zxkj.jackson.utils.JsonUtils;
import com.dzf.zxkj.platform.model.bdset.PzmbbVO;
import com.dzf.zxkj.platform.model.pzgl.TzpzHVO;
import com.dzf.zxkj.platform.model.pzgl.VoucherParamVO;
import com.dzf.zxkj.platform.model.report.ReportDataGrid;
import com.dzf.zxkj.platform.model.report.ReportDataGrid.XjllMsgVo;
import com.dzf.zxkj.platform.model.report.XjllMxvo;
import com.dzf.zxkj.platform.model.report.XjllbVO;
import com.dzf.zxkj.platform.model.sys.CorpVO;
import com.dzf.zxkj.platform.model.sys.UserVO;
import com.dzf.zxkj.platform.service.IZxkjPlatformService;
import com.dzf.zxkj.report.controller.ReportBaseController;
import com.dzf.zxkj.report.entity.ReportExcelExportVO;
import com.dzf.zxkj.report.excel.cwbb.XjllMXbExcelField;
import com.dzf.zxkj.report.excel.cwbb.XjllbExcelField;
import com.dzf.zxkj.report.service.cwbb.IXjllbReport;
import com.dzf.zxkj.report.utils.ReportUtil;
import com.dzf.zxkj.report.utils.VoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("gl_rep_xjlybact")
@Slf4j
public class XjllController extends ReportBaseController {

    @Autowired
    private IXjllbReport gl_rep_xjlybserv;

    @Autowired
    private IZxkjPlatformService zxkjPlatformService;

    /**
     *
     */
    @PostMapping("/queryAction")
    public ReturnData<ReportDataGrid> queryAction(@MultiRequestBody QueryParamVO queryvo, @MultiRequestBody CorpVO corpVO) {
        ReportDataGrid grid = new ReportDataGrid();
        QueryParamVO vo = getQueryParamVO(queryvo,corpVO);
        try {
            if (vo != null) {
                checkPowerDate(vo,corpVO);
                XjllbVO[] xjllbvos = gl_rep_xjlybserv.query(vo);
                if (xjllbvos != null && xjllbvos.length > 0) {
                    grid.setTotal((long) xjllbvos.length);
                    grid.setRows(Arrays.asList(xjllbvos));
                }
                //赋值不平衡信息
                putBlanceMsg(grid,xjllbvos);
            }
            grid.setMsg("查询成功");
            grid.setSuccess(true);
        } catch (Exception e) {
            grid.setRows(new ArrayList<XjllbVO>());
            printErrorLog(grid, e, "查询失败！");
        }

        // 日志记录接口
//        writeLogRecord(LogRecordEnum.OPE_KJ_CWREPORT.getValue(), "现金流量表查询:" + vo.getQjq(), ISysConstants.SYS_2);

        return ReturnData.ok().data(grid);
    }


    private void putBlanceMsg(ReportDataGrid grid, XjllbVO[] xjllbvos) {
        grid.setBlancemsg(true);
        grid.setBlancetitle("");
        DZFDouble xjlltotal = DZFDouble.ZERO_DBL;
        DZFDouble kmqcvalue = DZFDouble.ZERO_DBL;
        DZFDouble kmqmvalue = DZFDouble.ZERO_DBL;
        if (xjllbvos != null && xjllbvos.length > 0) {
            XjllMsgVo xjllmsgvo = grid.new XjllMsgVo();
            for (XjllbVO bvo : xjllbvos) {
                if (bvo.getBxjlltotal() != null && bvo.getBxjlltotal().booleanValue()) {
                    xjlltotal = VoUtils.getDZFDouble(bvo.getBqje()).setScale(2, DZFDouble.ROUND_HALF_UP);
                }
                if (bvo.getBkmqc() != null && bvo.getBkmqc().booleanValue()) {
                    kmqcvalue = VoUtils.getDZFDouble(bvo.getBqje()).setScale(2, DZFDouble.ROUND_HALF_UP);
                }
                if (bvo.getBkmqm() != null && bvo.getBkmqm().booleanValue()) {
                    kmqmvalue = VoUtils.getDZFDouble(bvo.getBqje()).setScale(2, DZFDouble.ROUND_HALF_UP);
                }
            }
            DZFDouble ce = xjlltotal.sub(kmqmvalue.sub(kmqcvalue));
            xjllmsgvo.setXjlltotal(xjlltotal);
            xjllmsgvo.setKmqcvalue(kmqcvalue);
            xjllmsgvo.setKmqmvalue(kmqmvalue);
            xjllmsgvo.setCe(ce);
            if (ce.doubleValue()!=0) {
                grid.setBlancemsg(false);
                grid.setBlancetitle("不平衡");
            }
            grid.setXjll_jyx(xjllmsgvo);
        }
    }

    /**
     * 联查现金流量明细账
     */
    @PostMapping("/queryMxAction")
    public ReturnData<Grid> queryMxAction(@MultiRequestBody QueryParamVO queryvo, @MultiRequestBody CorpVO corpVO){
        Grid grid = new Grid();
        try {
            QueryParamVO vo = getQueryParamVO(queryvo,corpVO);
            if(vo != null){
                XjllMxvo[] xjllMxvo = gl_rep_xjlybserv.getXJllMX(vo.getQjq(), vo.getPk_corp(), vo.getHc());
                if(xjllMxvo != null && xjllMxvo.length > 0){
                    grid.setTotal((long)xjllMxvo.length);
                    grid.setRows(Arrays.asList(xjllMxvo));
                }
            }
            grid.setSuccess(true);
        } catch (Exception e) {
            grid.setRows(new ArrayList<XjllMxvo>());
            printErrorLog(grid, e, "查询失败！");
        }
        return ReturnData.ok().data(grid);
    }


    private String getPubParam(CorpVO cpvo) {
        return "corpIds="+cpvo.getPk_corp()+"&gsname="+ CodeUtils1.deCode(cpvo.getUnitname());
    }

    @PostMapping("/linkPz")
    public  ReturnData<Json> linkPz(@MultiRequestBody QueryParamVO queryvo, @MultiRequestBody CorpVO corpVO){
        Json json = new Json();
        try {
            QueryParamVO vo = getQueryParamVO(queryvo,corpVO);
            //凭证查询vo
            VoucherParamVO pzparamvo = new VoucherParamVO();
            pzparamvo.setPk_corp(vo.getPk_corp());
            pzparamvo.setPage(1);
            pzparamvo.setBegindate(DateUtils.getPeriodStartDate(vo.getQjq()));
            pzparamvo.setEnddate(DateUtils.getPeriodEndDate(vo.getQjq()));
            pzparamvo.setSerdate("serMon");
            pzparamvo.setStartYear(vo.getQjq().substring(0, 4));
            pzparamvo.setStartMonth(pzparamvo.getBegindate().getStrMonth());
            pzparamvo.setEndYear(pzparamvo.getEnddate().getYear()+"");
            pzparamvo.setEndMonth(pzparamvo.getEnddate().getStrMonth());
            pzparamvo.setPz_status(0);
            pzparamvo.setRows(50);
            pzparamvo.setIs_error_cash(Boolean.TRUE);
            QueryPageVO pagevo = zxkjPlatformService.processQueryVoucherPaged(pzparamvo);
            TzpzHVO[] vos = (TzpzHVO[]) pagevo.getPagevos();
            String url = "";
            if(vos!=null && vos.length ==1){
                //填制凭证界面
                url = vos[0].getPk_tzpz_h();
            }else{
                url = "gl/gl_pzgl/gl_pzgl.jsp?"+getPubParam(corpVO)+"&is_error_cash=true&source=xjll&pzbegdate="+DateUtils.getPeriodStartDate(vo.getQjq())+"&pzenddate="+DateUtils.getPeriodEndDate(vo.getQjq());
            }
            json.setSuccess(true);
            json.setMsg("成功");
            json.setRows(url);
        } catch (Exception e) {
            printErrorLog(json, e, "查询失败");
        }
        return  ReturnData.ok().data(json);
    }

    //导出Excel
    @PostMapping("export/excel")
    public void excelReport(ReportExcelExportVO excelExportVO, KmReoprtQueryParamVO queryparamvo, @MultiRequestBody CorpVO corpVO, @MultiRequestBody UserVO userVO, HttpServletResponse response) {
        XjllbVO[] listVo= JsonUtils.deserialize(excelExportVO.getList(),XjllbVO[].class);

        String gs = listVo[0].getGs();
        String qj = listVo[0].getTitlePeriod();
        String corpIds = queryparamvo.getPk_corp();
        if(StringUtil.isEmpty(corpIds)){
            corpIds = corpVO.getPk_corp();
        }
        Excelexport2003<XjllbVO> lxs = new Excelexport2003<XjllbVO>();
        XjllbExcelField xjll = new XjllbExcelField();
        xjll.setColumnOrder(excelExportVO.getColumnOrder());
        CorpVO cpvo = zxkjPlatformService.queryCorpByPk(corpIds);

        if(cpvo!=null){
            xjll.setCorptype(cpvo.getCorptype());
        }
        getXjllExcel(excelExportVO,queryparamvo,userVO,listVo, gs, qj, xjll);//获取现金流量的数据

        baseExcelExport(response,lxs,xjll);

//        String excelsel = getRequest().getParameter("excelsel");
//        if(!StringUtil.isEmpty(excelsel) && "1".equals(excelsel)){
//            qj  = qj.substring(0, 4);
//        }
//        // 日志记录接口
//        writeLogRecord(LogRecordEnum.OPE_KJ_CWREPORT.getValue(), "现金流量表导出:" +  qj, ISysConstants.SYS_2);
    }

    //导出Excel
    @PostMapping("export/excelmx")
    public void excelReportMx(ReportExcelExportVO excelExportVO, KmReoprtQueryParamVO queryparamvo, @MultiRequestBody CorpVO corpVO, @MultiRequestBody UserVO userVO, HttpServletResponse response) {
        XjllMxvo[] listVo = JsonUtils.deserialize(excelExportVO.getList(), XjllMxvo[].class);
        String gs = listVo[0].getGs();
        String qj = listVo[0].getTitlePeriod();
        String corpIds = queryparamvo.getPk_corp();
        if (StringUtil.isEmpty(corpIds)) {
            corpIds =  corpVO.getPk_corp();
        }
        Excelexport2003<XjllMxvo> lxs = new Excelexport2003<XjllMxvo>();
        XjllMXbExcelField xjll = new XjllMXbExcelField();
        xjll.setKmmxvos(listVo);
        xjll.setCorpName(gs);
        xjll.setQj(qj);

        baseExcelExport(response,lxs,xjll);

//        String excelsel = getRequest().getParameter("excelsel");
//        if (!StringUtil.isEmpty(excelsel) && "1".equals(excelsel)) {
//            qj = qj.substring(0, 4);
//        }
//        // 日志记录接口
//        writeLogRecord(LogRecordEnum.OPE_KJ_CWREPORT.getValue(), "现金流量表明细导出:" + qj, ISysConstants.SYS_2);
    }

    private void getXjllExcel(ReportExcelExportVO excelExportVO, KmReoprtQueryParamVO queryParamvo,UserVO userVO , XjllbVO[] listVo, String gs, String qj, XjllbExcelField xjllb) {

        List<XjllbVO[]> lrbvos = new ArrayList<XjllbVO[]>();

        String excelsel = excelExportVO.getExcelsel();

        String[] strs = new String[] { "一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月" };

        List<String> periods = new ArrayList<String>();

        List<String> titlename = new ArrayList<String>();
        if (!StringUtil.isEmpty(excelsel) && "1".equals(excelsel)) {// 按照年来查询

            queryParamvo.setQjq(queryParamvo.getQjq().substring(0, 4)+"-01");

            queryParamvo.setQjz(queryParamvo.getQjq().substring(0, 4)+ "-12");

            Map<String, XjllbVO[]> mapvalues = gl_rep_xjlybserv.queryEveryPeriod(queryParamvo);

            CorpVO cpvo = zxkjPlatformService.queryCorpByPk(queryParamvo.getPk_corp());

            String begstr = null;

            if (cpvo.getBegindate().getYear() == Integer.parseInt(queryParamvo.getQjq().substring(0, 4))) {// 和建账日期对比
                begstr = DateUtils.getPeriod(cpvo.getBegindate()) + "-01";// 从一月份开始查询
            }else{
                begstr =  queryParamvo.getQjq().substring(0, 4)+"-01" + "-01";
            }

            String endstr =  queryParamvo.getQjq().substring(0, 4)+ "-12" + "-01";// 从一月份开始查询

            periods = ReportUtil.getPeriods(new DZFDate(begstr), new DZFDate(endstr));

            for (String period : periods) {
                if (mapvalues.get(period) != null && mapvalues.get(period).length > 0) {
                    lrbvos.add(mapvalues.get(period));
                }
            }

            for (int i = strs.length - lrbvos.size(); i < 12; i++) {
                titlename.add(strs[i]);
            }
        }else{
            lrbvos.add(listVo);
            titlename.add("现金流量表");
            periods.add(qj);
        }

        xjllb.setPeriods(periods.toArray(new String[0]));
        xjllb.setAllsheetxjllvos(lrbvos);
        xjllb.setAllsheetname(titlename.toArray(new String[0]));
        xjllb.setXjllbvos(listVo);
        xjllb.setQj(qj);
        xjllb.setCreator(userVO.getUser_name());
        xjllb.setCorpName(gs);
    }
}
