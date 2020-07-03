package com.dzf.zxkj.report.controller.cwbb;

import com.dzf.zxkj.base.exception.BusinessException;
import com.dzf.zxkj.base.utils.SpringUtils;
import com.dzf.zxkj.common.entity.Grid;
import com.dzf.zxkj.common.entity.ReturnData;
import com.dzf.zxkj.common.enums.LogRecordEnum;
import com.dzf.zxkj.common.model.SuperVO;
import com.dzf.zxkj.common.query.KmReoprtQueryParamVO;
import com.dzf.zxkj.common.query.PrintParamVO;
import com.dzf.zxkj.common.query.QueryParamVO;
import com.dzf.zxkj.common.utils.StringUtil;
import com.dzf.zxkj.excel.util.Excelexport2003;
import com.dzf.zxkj.jackson.annotation.MultiRequestBody;
import com.dzf.zxkj.jackson.utils.JsonUtils;
import com.dzf.zxkj.pdf.PrintReporUtil;
import com.dzf.zxkj.platform.model.report.LrbVO;
import com.dzf.zxkj.platform.model.report.LrbquarterlyVO;
import com.dzf.zxkj.platform.model.report.ZcFzBVO;
import com.dzf.zxkj.platform.model.sys.CorpTaxVo;
import com.dzf.zxkj.platform.model.sys.CorpVO;
import com.dzf.zxkj.platform.model.sys.UserVO;
import com.dzf.zxkj.platform.service.IZxkjPlatformService;
import com.dzf.zxkj.report.controller.ReportBaseController;
import com.dzf.zxkj.report.entity.ReportExcelExportVO;
import com.dzf.zxkj.report.excel.cwbb.LrbQuarterlyExcelField;
import com.dzf.zxkj.report.service.cwbb.ILrbQuarterlyReport;
import com.dzf.zxkj.report.service.cwbb.IZcFzBReport;
import com.dzf.zxkj.report.service.pub.IReportPubService;
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
@RequestMapping("gl_rep_lrbquarteract")
@Slf4j
public class LrbQuarterlyController extends ReportBaseController {
    @Autowired
    private ILrbQuarterlyReport gl_rep_lrbquarterlyserv;

    @Autowired
    private IZxkjPlatformService zxkjPlatformService;

    @Autowired
    private IReportPubService gl_rep_pubser;
    /**
     * 查询科目明细数据
     */
    @PostMapping("/queryAction")
    public ReturnData<Grid> queryAction(@MultiRequestBody QueryParamVO queryvo, @MultiRequestBody CorpVO corpVO) {
        Grid grid = new Grid();
        QueryParamVO queryParamvo = getQueryParamVO(queryvo, corpVO);
        try {
            // 校验
            checkSecurityData(null, new String[]{queryParamvo.getPk_corp()},null);
            queryParamvo.setRptsource("lrb");
            queryParamvo.setQjq(queryParamvo.getBegindate1().toString().substring(0, 7));
            queryParamvo.setQjz(queryParamvo.getBegindate1().toString().substring(0, 7));
            queryParamvo.setEnddate(queryParamvo.getBegindate1());
            /** 开始日期应该在建账日期前,检查权限 */
            checkPowerDate(queryParamvo, corpVO);
            LrbquarterlyVO[] fsejyevos = null;
            fsejyevos = gl_rep_lrbquarterlyserv.getLRBquarterlyVOs(queryParamvo);
            grid.setTotal(fsejyevos == null ? 0 : (long) Arrays.asList(fsejyevos).size());
            grid.setRows(fsejyevos == null ? null : Arrays.asList(fsejyevos));
            grid.setSuccess(true);
        } catch (Exception e) {
            grid.setRows(new ArrayList<LrbVO>());
            printErrorLog(grid, e, "查询失败！");
        }

        //日志记录接口
        writeLogRecord(LogRecordEnum.OPE_KJ_CWREPORT,
                "利润表季报查询:" + queryParamvo.getBegindate1().toString(), 2);

        return ReturnData.ok().data(grid);
    }


    @Override
    public String getPrintTitleName() {
        return "利 润 表 季 报";
    }


    private LrbVO[] convertLrb(LrbquarterlyVO[] listVo, String qj) {
        if (listVo == null || listVo.length == 0) {
            throw new BusinessException("数据为空");
        }

        String month = qj.substring(5, 7);

        List<LrbVO> reslist = new ArrayList<LrbVO>();

        LrbVO tlrbvo = null;
        for (LrbquarterlyVO quaryvo : listVo) {
            tlrbvo = new LrbVO();

            tlrbvo.setXm(quaryvo.getXm());

            tlrbvo.setBnljje(quaryvo.getBnljje());

            if ("03".equals(month)) {
                tlrbvo.setByje(quaryvo.getQuarterFirst());
            } else if ("06".equals(month)) {
                tlrbvo.setByje(quaryvo.getQuarterSecond());
            } else if ("09".equals(month)) {
                tlrbvo.setByje(quaryvo.getQuarterThird());
            } else {
                tlrbvo.setByje(quaryvo.getQuarterFourth());
            }
            reslist.add(tlrbvo);
        }

        return reslist.toArray(new LrbVO[0]);
    }

    private ZcFzBVO[] getZcfzbData(String qj, String corpIds) {
        IZcFzBReport gl_rep_zcfzserv = (IZcFzBReport) SpringUtils.getBean("gl_rep_zcfzserv");

        ZcFzBVO[] zcfzbvos = gl_rep_zcfzserv.getZCFZBVOs(qj.substring(0, 7), corpIds, "N",
                new String[]{"N", "N", "N", "N"});

        return zcfzbvos;
    }


    private void formaterColumn(LrbquarterlyVO[] listVo, String qj ) {
        if (listVo != null && listVo.length > 0) {
            if (!StringUtil.isEmpty(qj)) {
                for (LrbquarterlyVO vo: listVo) {
                    if (qj.indexOf("第一季度") >= 0) {
                        vo.setSntqs(vo.getLastquarterFirst());
                    } else if (qj.indexOf("第二季度") >= 0) {
                        vo.setSntqs(vo.getLastquarterSecond());
                    } else if (qj.indexOf("第三季度") >= 0) {
                        vo.setSntqs(vo.getLastquarterThird());
                    }else if (qj.indexOf("第四季度") >= 0) {
                        vo.setSntqs(vo.getLastquarterFourth());
                    }
                }
            }

        }
    }

    //导出Excel
    @PostMapping("export/excel")
    public void excelReport(@MultiRequestBody ReportExcelExportVO excelExportVO, @MultiRequestBody KmReoprtQueryParamVO queryparamvo, @MultiRequestBody CorpVO corpVO, @MultiRequestBody UserVO userVO, HttpServletResponse response){
        // 校验
        checkSecurityData(null, new String[]{queryparamvo.getPk_corp()},null);
        LrbquarterlyVO[] listVo = JsonUtils.deserialize(excelExportVO.getList(), LrbquarterlyVO[].class);
        String gs=  excelExportVO.getCorpName();
        String qj=  excelExportVO.getTitleperiod();
        formaterColumn(listVo, qj);
        Excelexport2003<LrbquarterlyVO> lxs = new Excelexport2003<LrbquarterlyVO>();
        LrbQuarterlyExcelField lrb = new LrbQuarterlyExcelField(excelExportVO.getCurrjd());
        lrb.setZeroshownull(!queryparamvo.getBshowzero().booleanValue());
        lrb.setLrbvos(listVo);
        lrb.setQj(qj);
        lrb.setCreator(userVO.getUser_name());
        lrb.setCorpName(gs);

        baseExcelExport(response,lxs,lrb);

        //日志记录接口
        writeLogRecord(LogRecordEnum.OPE_KJ_CWREPORT,
                "利润表季报导出:"+qj,2);
    }

    /**
     * 打印操作
     */
    @PostMapping("print")
    public void printAction(@RequestParam Map<String, String> pmap1, @MultiRequestBody UserVO userVO, @MultiRequestBody CorpVO corpVO, HttpServletResponse response){
        try {
            PrintParamVO printParamVO = JsonUtils.deserialize(JsonUtils.serialize(pmap1), PrintParamVO.class);
            QueryParamVO queryparamvo = JsonUtils.deserialize(JsonUtils.serialize(pmap1), QueryParamVO.class);
            // 校验
            checkSecurityData(null, new String[]{queryparamvo.getPk_corp()},null);
            PrintReporUtil printReporUtil = new PrintReporUtil(zxkjPlatformService, corpVO, userVO, response);
            Map<String, String> pmap = printReporUtil.getPrintMap(printParamVO);
            String strlist = printParamVO.getList();
            String type = printParamVO.getType();
            String font = printParamVO.getFont();
            String curr_jd = printParamVO.getCurrjd();
            if(strlist==null){
                return;
            }
            LrbquarterlyVO[] bodyvos = JsonUtils.deserialize(strlist, LrbquarterlyVO[].class);
            formaterColumn(bodyvos, printParamVO.getTitleperiod());
            Map<String,String> tmap=new LinkedHashMap<String,String>();//声明一个map用来存前台传来的设置参数
            tmap.put("公司",  printParamVO.getCorpName());
            tmap.put("期间",  printParamVO.getTitleperiod());
            tmap.put("单位",  "元");
            QueryParamVO paramvo = new QueryParamVO();
            paramvo.setPk_corp(corpVO.getPk_corp());
            List<CorpTaxVo> listVos = gl_rep_pubser.queryTaxVoByParam(queryparamvo.getPk_corp());
            if(listVos != null && listVos.size() > 0){
                Optional<CorpTaxVo> optional = listVos.stream().filter(v->corpVO.getPk_corp().equals(v.getPk_corp())).findFirst();
                optional.ifPresent(corpTaxVo ->{
                    if(!StringUtil.isEmpty(corpTaxVo.getLegalbodycode())){
                        pmap.put("单位负责人", corpTaxVo.getLegalbodycode());
                    }
                    if(!StringUtil.isEmpty(corpTaxVo.getLinkman1())){
                        pmap.put("财务负责人", corpTaxVo.getLinkman1());
                    }
                    pmap.put("制表人", userVO.getUser_name());
                });
            }
            if(type.equals("2")){
                printReporUtil.setLineheight(12f);
            }
            printReporUtil.setBshowzero(queryparamvo.getBshowzero());
            printReporUtil.setBasecolor(new BaseColor(0,0,0));//设置单元格线颜色
            printReporUtil.setTableHeadFount(new Font(printReporUtil.getBf(), Float.parseFloat(font), Font.NORMAL));//设置表头字体
            Object[] obj = getPrintXm(0,curr_jd);
            printReporUtil.printHz(new HashMap<String, List<SuperVO>>(),bodyvos,"利 润 表 季 报",
                    (String[])obj[0], (String[])obj[1], (int[])obj[2],(int)obj[3],pmap,tmap);

            //日志记录接口
            writeLogRecord(LogRecordEnum.OPE_KJ_CWREPORT,
                    "利润表季报打印:"+ printParamVO.getTitleperiod(),2);
        } catch (DocumentException e) {
            log.error("打印失败", e);
        } catch (IOException e) {
            log.error("打印失败", e);
        }
    }

    public Object[] getPrintXm(int type,String currjd){
        if(!StringUtil.isEmpty(currjd)){
            if("03".equals(currjd)){
                return getPrintXmjd1(type);
            }else if("06".equals(currjd)){
                return getPrintXmjd2(type);
            }else if("09".equals(currjd)){
                return getPrintXmjd3(type);
            }else if("12".equals(currjd)){
                return getPrintXmjd4(type);
            }
        }else{
            return getPrintXm(type);
        }
        return null;
    }

    public Object[] getPrintXm(int type){
        Object[] obj = new Object[4];
        switch (type) {
            case 0:
                obj[0] = new String[]{"xm","hs","bnlj","quarterFirst","quarterSecond","quarterThird","quarterFourth","sntqs"};
                obj[1] = new String[]{"项目","行次","本年累计","第一季度","第二季度","第三季度","第四季度","上年同期数"};
                obj[2] = new int[]{6,1,2,2,2,2,2,2};
                obj[3] = 20;
                break;
            default:
                break;
        }
        return obj;
    }

    public Object[] getPrintXmjd1(int type){
        Object[] obj = new Object[4];
        switch (type) {
            case 0:
                obj[0] = new String[]{"xm","hs","bnlj","quarterFirst","sntqs"};
                obj[1] = new String[]{"项目","行次","本年累计","第一季度","上年同期数"};
                obj[2] = new int[]{6,1,2,2,2};
                obj[3] = 20;
                break;
            default:
                break;
        }
        return obj;
    }

    public Object[] getPrintXmjd2(int type){
        Object[] obj = new Object[4];
        switch (type) {
            case 0:
                obj[0] = new String[]{"xm","hs","bnlj","quarterSecond","sntqs"};
                obj[1] = new String[]{"项目","行次","本年累计","第二季度","上年同期数"};
                obj[2] = new int[]{6,1,2,2,2};
                obj[3] = 20;
                break;
            default:
                break;
        }
        return obj;
    }


    public Object[] getPrintXmjd3(int type){
        Object[] obj = new Object[4];
        switch (type) {
            case 0:
                obj[0] = new String[]{"xm","hs","bnlj","quarterThird","sntqs"};
                obj[1] = new String[]{"项目","行次","本年累计","第三季度","上年同期数"};
                obj[2] = new int[]{6,1,2,2,2};
                obj[3] = 20;
                break;
            default:
                break;
        }
        return obj;
    }

    public Object[] getPrintXmjd4(int type){
        Object[] obj = new Object[4];
        switch (type) {
            case 0:
                obj[0] = new String[]{"xm","hs","bnlj","quarterFourth","sntqs"};
                obj[1] = new String[]{"项目","行次","本年累计","第四季度","上年同期数"};
                obj[2] = new int[]{6,1,2,2,2};
                obj[3] = 20;
                break;
            default:
                break;
        }
        return obj;
    }

}
