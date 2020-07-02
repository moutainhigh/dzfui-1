package com.dzf.zxkj.report.controller.cwbb;

import com.dzf.zxkj.common.constant.ISysConstants;
import com.dzf.zxkj.common.entity.Grid;
import com.dzf.zxkj.common.entity.Json;
import com.dzf.zxkj.common.entity.ReturnData;
import com.dzf.zxkj.common.enums.LogRecordEnum;
import com.dzf.zxkj.common.lang.DZFDate;
import com.dzf.zxkj.common.lang.DZFDouble;
import com.dzf.zxkj.common.model.SuperVO;
import com.dzf.zxkj.common.query.KmReoprtQueryParamVO;
import com.dzf.zxkj.common.query.PrintParamVO;
import com.dzf.zxkj.common.query.QueryParamVO;
import com.dzf.zxkj.common.utils.DateUtils;
import com.dzf.zxkj.common.utils.StringUtil;
import com.dzf.zxkj.excel.util.Excelexport2003;
import com.dzf.zxkj.jackson.annotation.MultiRequestBody;
import com.dzf.zxkj.jackson.utils.JsonUtils;
import com.dzf.zxkj.pdf.PrintReporUtil;
import com.dzf.zxkj.platform.model.report.LrbVO;
import com.dzf.zxkj.platform.model.sys.CorpTaxVo;
import com.dzf.zxkj.platform.model.sys.CorpVO;
import com.dzf.zxkj.platform.model.sys.UserVO;
import com.dzf.zxkj.platform.service.IZxkjPlatformService;
import com.dzf.zxkj.report.controller.ReportBaseController;
import com.dzf.zxkj.report.entity.ReportExcelExportVO;
import com.dzf.zxkj.report.excel.cwbb.LrbCenterExcelField;
import com.dzf.zxkj.report.excel.cwbb.LrbExcelField;
import com.dzf.zxkj.report.service.cwbb.ILrbReport;
import com.dzf.zxkj.report.service.pub.IReportPubService;
import com.dzf.zxkj.report.utils.ReportUtil;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("gl_rep_lrbact")
@Slf4j
public class LrbController extends ReportBaseController {

    @Autowired
    private ILrbReport gl_rep_lrbserv;

    @Autowired
    private IZxkjPlatformService zxkjPlatformService;

    @Autowired
    private IReportPubService gl_rep_pubser;

    /**
     * 查询科目明细数据
     */
    @PostMapping("/queryAction")
    public ReturnData queryAction(@MultiRequestBody QueryParamVO queryvo, @MultiRequestBody CorpVO corpVO) {
        Grid grid = new Grid();
        QueryParamVO queryParamvo = getQueryParamVO(queryvo, corpVO);
        try {
            // 校验
            checkSecurityData(null, new String[]{queryParamvo.getPk_corp()},null);
            /** 来源利润表 */
            queryParamvo.setRptsource("lrb");
            queryParamvo.setQjz(queryParamvo.getQjq());
            queryParamvo.setBegindate1(DateUtils.getPeriodStartDate(queryParamvo.getQjq()));
            queryParamvo.setEnddate(queryParamvo.getBegindate1());
            LrbVO[] fsejyevos = null;
            /** 开始日期应该在建账日期前,检查权限 */
            corpVO = zxkjPlatformService.queryCorpByPk(queryParamvo.getPk_corp());
            checkPowerDate(queryParamvo, corpVO);
            fsejyevos = gl_rep_lrbserv.getLRBVOs(queryParamvo);

            grid.setTotal(fsejyevos == null ? 0 : (long) Arrays.asList(fsejyevos).size());
            grid.setRows(fsejyevos == null ? null : Arrays.asList(fsejyevos));
            grid.setSuccess(true);
            grid.setMsg("查询成功");

        } catch (Exception e) {
            grid.setRows(new ArrayList<LrbVO>());
            printErrorLog(grid, e, "查询失败！");
        }

        /** 日志记录接口 */
        writeLogRecord(LogRecordEnum.OPE_KJ_CWREPORT,
                "利润表查询:" + DateUtils.getPeriod(queryParamvo.getEnddate()), ISysConstants.SYS_2);

        return ReturnData.ok().data(grid);
    }

    @PostMapping("/queryCenterAction")
    public ReturnData<Grid> queryCenterAction(@MultiRequestBody QueryParamVO queryvo, @MultiRequestBody CorpVO corpVO) {
        Grid grid = new Grid();
        QueryParamVO queryParamvo = getQueryParamVO(queryvo, corpVO);
        try {
            // 校验
            checkSecurityData(null, new String[]{queryParamvo.getPk_corp()},null);
            queryParamvo.setRptsource("lrb");
            queryParamvo.setQjq(DateUtils.getPeriod(queryParamvo.getBegindate1()));
            queryParamvo.setQjz(DateUtils.getPeriod(queryParamvo.getBegindate1()));
            queryParamvo.setEnddate(queryParamvo.getBegindate1());
            LrbVO[] fsejyevos = null;

            /** 开始日期应该在建账日期前,检查权限 */
            checkPowerDate(queryParamvo, corpVO);
            fsejyevos = gl_rep_lrbserv.getLRBVOs(queryParamvo);

            grid.setTotal(fsejyevos == null ? 0 : (long) Arrays.asList(fsejyevos).size());
            grid.setRows(fsejyevos == null ? null : Arrays.asList(fsejyevos));
            grid.setSuccess(true);
        } catch (Exception e) {
            grid.setRows(new ArrayList<LrbVO>());
            printErrorLog(grid, e, "查询失败！");
        }

        /** 日志记录接口 */
        writeLogRecord(LogRecordEnum.OPE_KJ_CWREPORT,
                "分部利润表查询:"+DateUtils.getPeriod(queryParamvo.getEnddate()), ISysConstants.SYS_2);
        return ReturnData.ok().data(grid);

    }


    /**
     * 查询一年的数据
     */
    @PostMapping("/queryYearAction")
    public ReturnData<Grid> queryYearAction(@MultiRequestBody QueryParamVO queryvo, @MultiRequestBody CorpVO corpVO) {
        Grid grid = new Grid();
        try {
            // 校验
            checkSecurityData(null, new String[]{queryvo.getPk_corp()},null);
            QueryParamVO queryParamvo = getQueryParamVO(queryvo, corpVO);
            int year = queryParamvo.getBegindate1().getYear();
            Map<String, List<LrbVO>> maplist = gl_rep_lrbserv.getYearLrbMap(String.valueOf(year), queryParamvo.getPk_corp(), "", null, queryParamvo.getIshasjz());
            /** map转换成list */
            List<LrbVO> reslrb = new ArrayList<LrbVO>();
            for (Map.Entry<String, List<LrbVO>> entry : maplist.entrySet()) {
                for (LrbVO lrbvo : entry.getValue()) {
                    if (lrbvo.getXm() != null &&
                            ("一、营业收入".equals(lrbvo.getXm())
                                    || "三、利润总额（亏损总额以“-”填列）".equals(lrbvo.getXm())
                                    || "三、利润总额（亏损总额以“-”号填列）".equals(lrbvo.getXm())
                                    || "减：所得税费用".equals(lrbvo.getXm()))
                            || "减：所得税".equals(lrbvo.getXm())
                    ) {
                        reslrb.add(lrbvo);
                    }
                }
            }
            grid.setRows(reslrb);
            grid.setSuccess(true);
        } catch (Exception e) {
            printErrorLog(grid, e, "查询失败！");
        }
        return ReturnData.ok().data(grid);
    }


    @Override
    public String getPrintTitleName() {
        return "利 润 表";
    }


    @PostMapping("/queryQuarterlySdsShui")
    public ReturnData<Json> queryQuarterlySdsShui(@MultiRequestBody QueryParamVO queryvo, @MultiRequestBody CorpVO corpVO) {
        Json grid = new Json();
        try {
            // 校验
            checkSecurityData(null, new String[]{queryvo.getPk_corp()},null);
            QueryParamVO queryParamvo = getQueryParamVO(queryvo, corpVO);
            /** 开始日期应该在建账日期前,检查权限 */
            checkPowerDate(queryParamvo, corpVO);
            DZFDouble sds = zxkjPlatformService.getQuarterlySdsShui(queryParamvo.getPk_corp(),
                    DateUtils.getPeriod(queryParamvo.getBegindate1()));
            grid.setData(sds);
            grid.setSuccess(true);
            grid.setMsg("查询所得税成功！");
        } catch (Exception e) {
            grid.setRows(new ArrayList<LrbVO>());
            printErrorLog(grid, e, "查询所得税失败！");
        }

        /** 日志记录接口 */
        writeLogRecord(LogRecordEnum.OPE_KJ_CWREPORT, "所得税查询", ISysConstants.SYS_2);
        return ReturnData.ok().data(grid);
    }

    /**
     * 导出Excel
     */
    @PostMapping("export/excel")
    public void excelReport(@MultiRequestBody ReportExcelExportVO excelExportVO, @MultiRequestBody KmReoprtQueryParamVO queryparamvo, @MultiRequestBody CorpVO corpVO, @MultiRequestBody UserVO userVO, HttpServletResponse response) {
        // 校验
        checkSecurityData(null, new String[]{queryparamvo.getPk_corp()},null);
        corpVO = zxkjPlatformService.queryCorpByPk(queryparamvo.getPk_corp());
        LrbVO[] listVo = JsonUtils.deserialize(excelExportVO.getList(), LrbVO[].class);
        String gs = excelExportVO.getCorpName();
        String qj = excelExportVO.getTitleperiod();

        String title = "";
        Excelexport2003<LrbVO> lxs = new Excelexport2003<LrbVO>();
        LrbExcelField lrb = null;
        if (!StringUtil.isEmpty(excelExportVO.getXmmcid())) {
            lrb = new LrbCenterExcelField();
            title = "分部";
        } else {
            lrb = new LrbExcelField();
        }
        lrb.setZeroshownull(!queryparamvo.getBshowzero().booleanValue());
        lrb.setCorptype(corpVO.getCorptype());
        lrb.setColumnOrder(excelExportVO.getColumnOrder());
        getLrbExcel(excelExportVO, corpVO, queryparamvo, userVO, listVo, gs, qj, lrb);
        //导出
        baseExcelExport(response, lxs, lrb);

        String excelsel = excelExportVO.getExcelsel();
        if (!StringUtil.isEmpty(excelsel) && "1".equals(excelsel)) {
            qj = qj.substring(0, 4);
        }
        /** 日志记录接口 */
        writeLogRecord(LogRecordEnum.OPE_KJ_CWREPORT,
                title + "利润表导出:" + qj, ISysConstants.SYS_2);
    }

    private void getLrbExcel(ReportExcelExportVO excelExportVO, CorpVO corpVO, KmReoprtQueryParamVO queryParamvo, UserVO userVO, LrbVO[] listVo, String gs, String qj, LrbExcelField lrb) {

        List<LrbVO[]> lrbvos = new ArrayList<LrbVO[]>();

        String excelsel = excelExportVO.getExcelsel();

        String[] strs = new String[]{"一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"};

        List<String> periods = new ArrayList<String>();

        List<String> titlename = new ArrayList<String>();
        /** 按照年来查询 */
        if (!StringUtil.isEmpty(excelsel) && "1".equals(excelsel)) {

            String year = queryParamvo.getBegindate1().getYear() + "";

            Map<String, List<LrbVO>> mapvalues = gl_rep_lrbserv.getYearLrbMap(year, queryParamvo.getPk_corp(),
                    queryParamvo.getXmmcid(), null, queryParamvo.getIshasjz());

            String begstr = null;

            /** 和建账日期对比 */
            if (corpVO.getBegindate().getYear() == queryParamvo.getBegindate1().getYear()) {
                /** 从一月份开始查询 */
                begstr = DateUtils.getPeriod(corpVO.getBegindate()) + "-01";
            } else {
                begstr = queryParamvo.getBegindate1().getYear() + "-01" + "-01";
            }

            /** 从一月份开始查询 */
            String endstr = queryParamvo.getBegindate1().getYear() + "-12" + "-01";

            periods = ReportUtil.getPeriods(new DZFDate(begstr), new DZFDate(endstr));

            for (String period : periods) {
                if (mapvalues.get(period) != null && mapvalues.get(period).size() > 0) {
                    lrbvos.add(mapvalues.get(period).toArray(new LrbVO[0]));
                }
            }

            for (int i = strs.length - lrbvos.size(); i < 12; i++) {
                titlename.add(strs[i]);
            }
        } else {
            lrbvos.add(listVo);
            if (lrb instanceof  LrbCenterExcelField) {
                titlename.add("分部利润表");
            }else {
                titlename.add("利润表");
            }
            periods.add(qj);
        }

        lrb.setPeriods(periods.toArray(new String[0]));
        lrb.setAllsheetlrbvos(lrbvos);
        lrb.setAllsheetname(titlename.toArray(new String[0]));
        lrb.setLrbvos(listVo);
        lrb.setQj(qj);
        lrb.setCreator(userVO.getUser_name());
        lrb.setCorpName(gs);
    }


    /**
     * 打印操作
     */
    @PostMapping("print")
    public void printAction(@RequestParam Map<String, String> pmap1, @MultiRequestBody UserVO userVO, @MultiRequestBody CorpVO corpVO, HttpServletResponse response) {
        try {

            PrintParamVO printParamVO = JsonUtils.deserialize(JsonUtils.serialize(pmap1), PrintParamVO.class);
            QueryParamVO queryparamvo = JsonUtils.deserialize(JsonUtils.serialize(pmap1), QueryParamVO.class);
            // 校验
            checkSecurityData(null, new String[]{queryparamvo.getPk_corp()},null);
            PrintReporUtil printReporUtil = new PrintReporUtil(zxkjPlatformService, corpVO, userVO, response);
            Map<String, String> pmap = printReporUtil.getPrintMap(printParamVO);
            String strlist = printParamVO.getList();
            String xmmcid = printParamVO.getXmmcid();
            String type = printParamVO.getType();
            String font = printParamVO.getFont();
            String columnOrder = printParamVO.getColumnOrder();
            if (strlist == null) {
                return;
            }
            LrbVO[] bodyvos = JsonUtils.deserialize(strlist, LrbVO[].class);
            Map<String, String> tmap = new LinkedHashMap<String, String>();/** 声明一个map用来存前台传来的设置参数 */
            CorpVO cpvo = zxkjPlatformService.queryCorpByPk(queryparamvo.getPk_corp());
            tmap.put("公司", printParamVO.getCorpName());
            tmap.put("期间", printParamVO.getTitleperiod());
            tmap.put("单位", "元");
            QueryParamVO paramvo = new QueryParamVO();
            paramvo.setPk_corp(corpVO.getPk_corp());
            List<CorpTaxVo> listVos = gl_rep_pubser.queryTaxVoByParam(queryparamvo.getPk_corp());
            if (listVos != null && listVos.size() > 0) {
                Optional<CorpTaxVo> optional = listVos.stream().filter(v -> corpVO.getPk_corp().equals(v.getPk_corp())).findFirst();
                optional.ifPresent(corpTaxVo -> {
                    if (!StringUtil.isEmpty(corpTaxVo.getLegalbodycode())) {
                        pmap.put("单位负责人", corpTaxVo.getLegalbodycode());
                    }
                    if (!StringUtil.isEmpty(corpTaxVo.getLinkman1())) {
                        pmap.put("财务负责人", corpTaxVo.getLinkman1());
                    }
                    pmap.put("制表人", userVO.getUser_name());
                });
            }
            if (type.equals("2")) {
                printReporUtil.setLineheight(12f);
            } else {
                printReporUtil.setLineheight(18f);
            }
            printReporUtil.setBshowzero(queryparamvo.getBshowzero());
            printReporUtil.setBf_Bold(printReporUtil.getBf());
            printReporUtil.setBasecolor(new BaseColor(0, 0, 0));//设置单元格线颜色
            printReporUtil.setTableHeadFount(new Font(printReporUtil.getBf(), Float.parseFloat(font), Font.NORMAL));
            String titlename = "利 润 表";
            Map<String, List<SuperVO>> qrymap = getLrbMap(queryparamvo,zxkjPlatformService,gl_rep_lrbserv);
            Object[] obj = getPrintOrder(columnOrder);//根据类型查询
            if (!StringUtil.isEmpty(xmmcid)) {
                titlename = "分 部 利 润 表";
                printReporUtil.printHz(qrymap, null, titlename,
                        (String[]) obj[0], (String[]) obj[1], (int[]) obj[2], (int) obj[3], pmap, tmap);
            } else {
                printReporUtil.printHz(qrymap, null, titlename,
                        (String[]) obj[0], (String[]) obj[1], (int[]) obj[2], (int) obj[3], pmap, tmap);
            }
            /** 日志记录接口 */
            writeLogRecord(LogRecordEnum.OPE_KJ_CWREPORT,
                    titlename.replaceAll(" ","") + "打印:" + printParamVO.getTitleperiod(), ISysConstants.SYS_2);
        } catch (DocumentException e) {
            log.error("打印错误", e);
        } catch (IOException e) {
            log.error("打印错误", e);
        }
    }

    public Object[] getPrintOrder(String columnOrder) {
        Object[] obj = new Object[4];
        if ("on".equalsIgnoreCase(columnOrder)) {
            obj[0] = new String[]{"xm", "hs", "byje", "bnljje"};
            obj[1] = new String[]{"项            目", "行次", "本月金额", "本年累计金额"};
            obj[2] = new int[]{5, 1, 2, 2};
            obj[3] = 20;
        } else {
            obj[0] = new String[]{"xm", "hs", "bnljje", "byje"};
            obj[1] = new String[]{"项            目", "行次", "本年累计金额", "本月金额"};
            obj[2] = new int[]{5, 1, 2, 2};
            obj[3] = 20;
        }
        return obj;
    }

    public Map<String, List<SuperVO>> getLrbMap(QueryParamVO queryParamvo,IZxkjPlatformService zxkjPlatformService,
                                                ILrbReport gl_rep_lrbserv) {
        Map<String, List<SuperVO>> resmap = new LinkedHashMap<String, List<SuperVO>>();

        CorpVO cpvo = zxkjPlatformService.queryCorpByPk(queryParamvo.getPk_corp());

        String begstr = null;

        begstr = DateUtils.getPeriod(queryParamvo.getBegindate1());

        List<LrbVO[]> listvos = gl_rep_lrbserv.getBetweenLrbMap(DateUtils.getPeriodStartDate(begstr),
                queryParamvo.getEnddate(), queryParamvo.getPk_corp(), queryParamvo.getXmmcid(), null, queryParamvo.getIshasjz());

        if (listvos != null && listvos.size() > 0) {
            List<SuperVO> t_list = new ArrayList<SuperVO>();
            for (LrbVO[] tvos : listvos) {
                t_list = new ArrayList<>();
                for (LrbVO tvo : tvos) {
                    t_list.add(tvo);
                }
                resmap.put(tvos[0].getPeriod(), t_list);
            }
        }


        return resmap;
    }

}
