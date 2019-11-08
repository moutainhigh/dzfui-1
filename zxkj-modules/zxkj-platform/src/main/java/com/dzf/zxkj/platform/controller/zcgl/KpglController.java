package com.dzf.zxkj.platform.controller.zcgl;

import com.dzf.zxkj.base.exception.DZFWarpException;
import com.dzf.zxkj.base.exception.WiseRunException;
import com.dzf.zxkj.common.base.BaseController;
import com.dzf.zxkj.common.constant.ISysConstants;
import com.dzf.zxkj.common.entity.Grid;
import com.dzf.zxkj.common.entity.Json;
import com.dzf.zxkj.common.entity.Page;
import com.dzf.zxkj.common.entity.ReturnData;
import com.dzf.zxkj.common.enums.LogRecordEnum;
import com.dzf.zxkj.common.exception.BusinessException;
import com.dzf.zxkj.common.lang.DZFBoolean;
import com.dzf.zxkj.common.lang.DZFDate;
import com.dzf.zxkj.common.lang.DZFDouble;
import com.dzf.zxkj.common.query.QueryParamVO;
import com.dzf.zxkj.common.utils.StringUtil;
import com.dzf.zxkj.jackson.annotation.MultiRequestBody;
import com.dzf.zxkj.platform.model.sys.BdTradeAssetCheckVO;
import com.dzf.zxkj.platform.model.sys.CorpVO;
import com.dzf.zxkj.platform.model.tax.TaxitemVO;
import com.dzf.zxkj.platform.model.zcgl.AssetcardDisplayColumnVO;
import com.dzf.zxkj.platform.model.zcgl.AssetcardVO;
import com.dzf.zxkj.platform.service.sys.ICorpService;
import com.dzf.zxkj.platform.service.zcgl.IKpglService;
import com.dzf.zxkj.platform.service.zcgl.IworkloadManagement;
import com.dzf.zxkj.platform.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("am_kpglact")
@SuppressWarnings("all")
public class KpglController extends BaseController {

    @Autowired
    private IKpglService am_kpglserv;

    @Autowired
    private IworkloadManagement am_workloadmagserv;
    @Autowired
    private ICorpService corpService;

    @PostMapping("doAdd")
    public ReturnData doAdd() throws DZFWarpException {//新增默认值
        Grid json = new Grid();
        try {
            String newcode = am_kpglserv.buildAssetcardCode(SystemUtil.getLoginCorpId());
            json.setSuccess(true);
            json.setMsg("获取成功");
            json.setRows(newcode);
        } catch (Exception e) {
            printErrorLog(json, e, "获取新编码失败");
            log.error("获取新编码失败", e);
        }
        return ReturnData.ok().data(json);
    }

    /**
     * 赋默认值
     *
     * @param vo
     */
    private void setDefaultValue(AssetcardVO vo) {
        String loginDate = SystemUtil.getLoginDate();
        vo.setPk_corp(SystemUtil.getLoginCorpId());
        vo.setCoperatorid(SystemUtil.getLoginUserId());
        vo.setDoperatedate(new DZFDate(loginDate));
        vo.setAccountmny(vo.getAssetmny());
        if (vo.getIsperiodbegin() == null
                || !vo.getIsperiodbegin().booleanValue()) {
            vo.setAccountusedperiod(1);
            vo.setUsedperiod(1);
        }
    }

    private ReturnData save1(AssetcardVO data) {
        Json json = new Json();
        if (data != null) {
            try {
                if (!StringUtil.isEmpty(data.getPrimaryKey())) {
                    checkCorp(SystemUtil.getLoginCorpId(), data);
                }
                CorpVO corpvo = corpService.queryByPk(SystemUtil.getLoginCorpId());
                setDefaultValue(data);
                data = am_kpglserv.save(corpvo, data);
                json.setSuccess(true);
                json.setRows(data);
                json.setMsg("保存成功");
                json.setHead(data);
                writeLogRecord(LogRecordEnum.OPE_KJ_ZCGL, "新增资产卡片:" + data.getPeriod() + "，卡片编码:" + data.getAssetcode(), ISysConstants.SYS_2);
            } catch (Exception e) {
                printErrorLog(json, e, "保存失败");
                log.error("保存失败", e);
            }
        } else {
            json.setSuccess(false);
            json.setMsg("保存失败:数据为空。");
        }
        return ReturnData.ok().data(json);
    }

    // 保存
    @PostMapping("save")
    public ReturnData save(@MultiRequestBody AssetcardVO data) {
        if (StringUtil.isEmpty(data.getPrimaryKey())) {
            return save1(data);
        } else {
            return update(data);
        }
    }

    // 查询
    @PostMapping("query")
    public ReturnData query(@MultiRequestBody QueryParamVO paramvo, @MultiRequestBody Page page, @MultiRequestBody CorpVO corpVO) {
        Grid grid = new Grid();
        try {
            if (paramvo != null) {
                paramvo = getQueryParamVO(paramvo, corpVO);
                List<AssetcardVO> list = am_kpglserv.query(paramvo);
                if (list != null && list.size() > 0) {
                    grid.setRows(getPagedCardVOs(list, page.getPage(), page.getRows(), grid));
                    grid.setMsg("查询成功！");
                }
            }
        } catch (Exception e) {
            printErrorLog(grid, e, "查询失败");
            log.error("查询失败", e);
        }
        return ReturnData.ok().data(grid);
    }

    /**
     * 将查询后的结果分页
     *
     * @param kmmxvos
     * @param page
     * @param rows
     * @return
     */
    private List<AssetcardVO> getPagedCardVOs(List<AssetcardVO> cardlist, int page, int rows, Grid grid) throws DZFWarpException {
        if (cardlist == null || cardlist.size() == 0) {
            grid.setTotal((long) 0);
            return cardlist;
        }
        List<AssetcardVO> listresmxvo = new ArrayList<AssetcardVO>();//需要返回的结果集

        //分页
        if (cardlist != null && cardlist.size() > 0) {
            int start = (page - 1) * rows;
            for (int i = start; i < page * rows && i < cardlist.size(); i++) {
                listresmxvo.add(cardlist.get(i));
            }
            grid.setTotal((long) cardlist.size());
        } else {
            grid.setTotal((long) 0);
        }
        return listresmxvo;
    }

    private ReturnData update(AssetcardVO data) {
        Json json = new Json();
        if (data != null) {
            try {
                CorpVO corp = corpService.queryByPk(SystemUtil.getLoginCorpId());
                checkCorp(corp.getPk_corp(), data);
                am_kpglserv.update(corp, data);
                json.setSuccess(true);
                json.setRows(data);
                json.setMsg("更新成功");
                json.setHead(data);
                writeLogRecord(LogRecordEnum.OPE_KJ_ZCGL, "修改资产卡片:" + data.getPeriod() + "，卡片编码:" + data.getAssetcode(), ISysConstants.SYS_2);
            } catch (Exception e) {
                printErrorLog(json, e, "更新失败");
                log.error("更新失败", e);
            }
        } else {
            json.setSuccess(false);
            json.setMsg("更新失败:数据为空。");
        }
        return ReturnData.ok().data(json);
    }

    /**
     * 保存前校验是否重复
     */
    @PostMapping("checkBefSave")
    public ReturnData checkBefSave(@MultiRequestBody AssetcardVO data) {
        Json json = new Json();
        json.setSuccess(true);
        try {
            String tips = am_kpglserv.brepeat_sig(data, SystemUtil.getLoginCorpId());
            if (!StringUtil.isEmpty(tips)) {
                throw new BusinessException(tips);
            }
        } catch (Exception e) {
            printErrorLog(json, e, "操作失败<br>");
            log.error("操作失败<br>", e);
        }
        return ReturnData.ok().data(json);
    }

    @PostMapping("checkEdit")
    public ReturnData checkEdit(@MultiRequestBody AssetcardVO data) {
        Json json = new Json();
        json.setSuccess(true);
        try {
            String edit = am_kpglserv.checkEdit(SystemUtil.getLoginCorpId(), data);
            if (!StringUtil.isEmpty(edit)) {
                json.setSuccess(false);
                json.setMsg(edit);
            }
        } catch (Exception e) {
            log.error("操作失败", e);
        }
        return ReturnData.ok().data(json);
    }

    // 查询一条
    @PostMapping("queryOne")
    public ReturnData queryOne(@MultiRequestBody AssetcardVO data, @MultiRequestBody CorpVO corpVO) {
        Json json = new Json();
        try {
            QueryParamVO paramvo = new QueryParamVO();
            if (!StringUtil.isEmpty(data.getPk_assetcard())
                    && data.getPk_assetcard().indexOf(",") >= 0) {
                paramvo.setPk_assetcard(data.getPk_assetcard().split(",")[0]);
            } else {
                paramvo.setPk_assetcard(data.getPk_assetcard());
            }
            paramvo.setPk_corp(SystemUtil.getLoginCorpId());
            if (paramvo != null) {
                List<AssetcardVO> list = am_kpglserv.query(paramvo);
                if (list != null && list.size() > 0) {
                    list.get(0).setChargedeptname(corpVO.getChargedeptname());
                    json.setSuccess(true);
                    json.setMsg("查询成功");
                    json.setRows(list);
                }
            }
        } catch (Exception e) {
            printErrorLog(json, e, "查询失败");
            log.error("查询失败", e);
        }
        return ReturnData.ok().data(json);
    }

    @PostMapping("queryDisplayColumn")
    public ReturnData queryDisplayColumn() {
        Grid grid = new Grid();
        try {
            List<AssetcardDisplayColumnVO> list = am_kpglserv.qryDisplayColumns(SystemUtil.getLoginCorpId());
            grid.setRows(list);
            grid.setSuccess(true);
            grid.setTotal((long) list.size());
        } catch (Exception e) {
            printErrorLog(grid, e, "查询失败");
            log.error("查询失败", e);
        }
        return ReturnData.ok().data(grid);
    }

    @PostMapping("querySmSl")
    public ReturnData querySmSl() {
        Grid grid = new Grid();
        try {
            List<TaxitemVO> itemsvo = am_kpglserv.qrytaxitems(SystemUtil.getLoginCorpId());
            grid.setRows(itemsvo);
            grid.setSuccess(true);
            grid.setMsg("查询成功");
        } catch (Exception e) {
            printErrorLog(grid, e, "查询失败");
            log.error("查询失败", e);
        }
        return ReturnData.ok().data(grid);
    }

    @PostMapping("saveDisplayColumn")
    public ReturnData saveDisplayColumn(@MultiRequestBody AssetcardDisplayColumnVO displayvo) {
        Json json = new Json();

        try {
            am_kpglserv.saveDisplayColumn(displayvo, SystemUtil.getLoginCorpId());

            json.setSuccess(true);
            json.setMsg("操作成功");
        } catch (Exception e) {
            printErrorLog(json, e, "操作失败");
            log.error("操作失败", e);
        }
        return ReturnData.ok().data(json);
    }

    /**
     * 用于参照数据查询
     */
    @PostMapping("queryRefData")
    public ReturnData queryRefData(String isclear) {
        Grid grid = new Grid();
        try {
            String pk_corp = SystemUtil.getLoginCorpId();
            String loginDate = SystemUtil.getLoginDate();
            List<AssetcardVO> list = am_kpglserv.queryByPkcorp(loginDate,
                    pk_corp, isclear);
            if (list != null && list.size() > 0) {
                grid.setTotal((long) list.size());
                grid.setRows(list);
                grid.setMsg("查询成功！");
            }
        } catch (Exception e) {
            printErrorLog(grid, e, "查询失败");
            log.error("查询失败", e);
        }
        return ReturnData.ok().data(grid);
    }

    /**
     * 查询条件
     *
     * @return
     */
    private QueryParamVO getQueryParamVO(QueryParamVO paramvo, CorpVO corpVO) {
        DZFDate busibegindata = corpVO.getBusibegindate();
        if (paramvo.getBegindate1() != null && paramvo.getBegindate1().before(busibegindata)) {
            paramvo.setBegindate1(busibegindata);
        }

        paramvo.setPk_corp(SystemUtil.getLoginCorpId());

        return paramvo;
    }

    /**
     * 资产清理（不支持批量，如果批量的话，需要修改接口）
     *
     * @throws Exception
     */
    @PostMapping("updateAssetClear")
    public ReturnData updateAssetClear(@MultiRequestBody AssetcardVO data, @MultiRequestBody CorpVO corp) {
        Json json = new Json();
        if (data != null) {
            try {
                String loginDate = SystemUtil.getLoginDate();
                checkCorp(corp.getPk_corp(), data);
                am_kpglserv.updateAssetClear(loginDate, corp,
                        new AssetcardVO[]{data}, SystemUtil.getLoginUserId());
                json.setSuccess(true);
                json.setRows(data);
                json.setMsg("资产清理成功");
                json.setHead(data);
            } catch (Exception e) {
                printErrorLog(json, e, "资产清理失败");
                log.error("资产清理失败", e);
            }
        } else {
            json.setSuccess(false);
            json.setMsg("资产清理失败:数据为空。");
        }
        return ReturnData.ok().data(json);
    }
@PostMapping("updateMultAssetClear")
    public ReturnData updateMultAssetClear(@MultiRequestBody List<AssetcardVO> list, @MultiRequestBody CorpVO corp) {
        Json json = new Json();
        if (list != null && list.size() > 0) {
            AssetcardVO[] vos = list.toArray(new AssetcardVO[0]);
            try {
                for (AssetcardVO vo1 : vos) {
                    checkCorp(corp.getPk_corp(), vo1);
                }
                String loginDate = SystemUtil.getLoginDate();
                am_kpglserv.updateAssetClear(loginDate, corp, vos, SystemUtil.getLoginUserId());
                json.setSuccess(true);
                json.setRows(list);
                json.setMsg("资产清理成功");
                json.setHead(list);
            } catch (Exception e) {
                printErrorLog(json, e, "资产清理失败");
                log.error("资产清理失败",e);
            }
        } else {
            json.setSuccess(false);
            json.setMsg("资产清理失败:数据为空。");
        }
        return ReturnData.ok().data(json);
    }

//    批量转总账
    @PostMapping("batchToVoucher")
    public ReturnData batchToVoucher(String ids, String merge) {
        Json json = new Json();
        try {
            String[] assetids = ids.split(",");
            DZFBoolean bmerge = new DZFBoolean(merge);
            DZFDate date = new DZFDate(SystemUtil.getLoginDate());
            String tips = am_kpglserv.saveVoucherFromZc(assetids, SystemUtil.getLoginCorpId(), SystemUtil.getLoginUserId(), date, bmerge);
            if (!StringUtil.isEmpty(tips)) {
                throw new BusinessException(tips);
            }
            json.setSuccess(true);
            json.setMsg("操作成功");
        } catch (Exception e) {
            printErrorLog(json, e, "操作失败");
            log.error("操作失败", e);
        }
        return ReturnData.ok().data(json);
    }

    @PostMapping("queryKmFromZclb")
    public ReturnData queryKmFromZclb(String zclbid) {
        Json json = new Json();
        try {
            List<BdTradeAssetCheckVO> listvos = am_kpglserv.queryDefaultFromZclb(SystemUtil.getLoginCorpId(), zclbid);
            json.setSuccess(true);
            json.setData(listvos);
            json.setMsg("查询成功");
        } catch (Exception e) {
            printErrorLog(json, e, "查询类别出错");
            log.error("查询类别出错", e);
        }
        return ReturnData.ok().data(json);
    }

    private CellStyle getDecimalFormatStyle(Workbook workbook) {
        CellStyle rightstyle = getCellStyle(workbook);
        Font font = workbook.createFont();
        font.setColor(HSSFColor.BLUE.index);
        rightstyle.setFont(font);
        String style = "#,##0";
        for (int i = 0; i < 2; i++) {
            if (i == 0)
                style = style + ".";
            style = style + "0";
        }
        rightstyle.setAlignment(HorizontalAlignment.CENTER);
        DataFormat fmt = workbook.createDataFormat();
        rightstyle.setDataFormat(fmt.getFormat(style));
        return rightstyle;
    }

    private CellStyle getCellStyle(Workbook workbook) {
        CellStyle style2 = workbook.createCellStyle();
        style2.setFillForegroundColor(HSSFColor.LIGHT_YELLOW.index);
        style2.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style2.setBorderBottom(BorderStyle.THIN);
        style2.setBorderLeft(BorderStyle.THIN);
        style2.setBorderRight(BorderStyle.THIN);
        style2.setBorderTop(BorderStyle.THIN);
        style2.setAlignment(HorizontalAlignment.CENTER);
        style2.setVerticalAlignment(VerticalAlignment.CENTER);
        return style2;
    }

    private Workbook createWorkBook(String[] fields, List<AssetcardVO> assetCardVOList) throws IOException, InvalidFormatException {

        Resource resource = new ClassPathResource("template/report/taxdeclaration/gdzcModel.xls");

        Workbook workbook = WorkbookFactory.create(resource.getInputStream());

        Sheet sheet = workbook.getSheetAt(0);
        for (int i = 0; i < assetCardVOList.size(); i++) {
            Row row = sheet.createRow(i + 1);
            for (int j = 0; j < fields.length; j++) {
                Object value = assetCardVOList.get(i).getAttributeValue(fields[j]);
                Cell cell = row.createCell(j);
                cell.setCellStyle(getCellStyle(workbook));
                String textValue;
                if (value != null) {

                    boolean isDouble = true;
                    if (value instanceof DZFBoolean) {
                        textValue = ((DZFBoolean) value).booleanValue() ? "是" : "否";
                        cell.setCellValue(getOtherHssfRichTextString(workbook, textValue));
                    } else if (value instanceof DZFDouble) {
                        textValue = ((DZFDouble) value).setScale(2, DZFDouble.ROUND_HALF_UP).toString();
                        cell.setCellValue(Double.parseDouble(textValue));
                        cell.setCellStyle(getDecimalFormatStyle(workbook));
                    } else if (value instanceof Date) {
                        textValue = new SimpleDateFormat("yyy-MM-dd").format((Date) value);
                        cell.setCellValue(getOtherHssfRichTextString(workbook, textValue));
                    } else {
                        // 其它数据类型都当作字符串简单处理
                        textValue = value.toString();
                        cell.setCellValue(getOtherHssfRichTextString(workbook, textValue));
                    }
                }
            }
        }

        return workbook;
    }

    private HSSFRichTextString getOtherHssfRichTextString(Workbook workbook, String textValue) {
        HSSFRichTextString richString = new HSSFRichTextString(
                textValue);
        Font font3 = workbook.createFont();
        font3.setColor(HSSFColor.BLUE.index);
        richString.applyFont(font3);
        return richString;
    }


//    public void expExcel() {
//        String ids = getRequest().getParameter("id");
//        String lx = getRequest().getParameter("lx");//模板导出类型0 是默认导出，1按照导入格式导出
//        List<AssetcardVO> listVo = am_kpglserv.queryByIds(ids);
//        HttpServletResponse response = getResponse();
//
//        ExportExcel<AssetcardVO> ex = new ExportExcel<AssetcardVO>();
//        Map<String, String> map = getExpFieldMap(lx);
//        String[] enFields = new String[map.size()];
//        String[] cnFields = new String[map.size()];
//        // 填充普通字段数组
//        int count = 0;
//        for (Map.Entry<String, String> entry : map.entrySet()) {
//            enFields[count] = entry.getKey();
//            cnFields[count] = entry.getValue();
//            count++;
//        }
//        OutputStream toClient = null;
//        try {
//            for (AssetcardVO vo : listVo) {
//                if (vo.getZjtype() != null && vo.getZjtype().intValue() == 0) {
//                    vo.setZjtypestr("平均年限法");
//                } else if (vo.getZjtype() != null
//                        && vo.getZjtype().intValue() == 1) {
//                    vo.setZjtypestr("工作量法");
//                } else if (vo.getZjtype() != null
//                        && vo.getZjtype().intValue() == 2) {
//                    vo.setZjtypestr("双倍余额递减法");
//                } else if (vo.getZjtype() != null
//                        && vo.getZjtype().intValue() == 3) {
//                    vo.setZjtypestr("年数总和法");
//                }
//                checkCorp(getLogincorppk(), vo);
//            }
//            // OutputStream out = new FileOutputStream(path);
//            response.reset();
//            // 设置response的Header
//            response.setCharacterEncoding("UTF-8");
//            response.setContentType("application/ms-excel; charset=UTF-8");
//            String date = DateUtils.getDate(new Date());
//            String fileName = "资产卡片" + date + ".xls";
//            String formattedName = URLEncoder.encode(fileName, "UTF-8");
//            response.addHeader("Content-Disposition", "attachment;filename=" + fileName + ";filename*=UTF-8''" + formattedName);
//            toClient = new BufferedOutputStream(response.getOutputStream());
//            if ("1".equals(lx)) {
//                Workbook workBook = createWorkBook(enFields, listVo);
//                workBook.write(toClient);
//            } else {
//                ex.exportExcel("资产卡片", cnFields, enFields, listVo,
//                        toClient);
//            }
//            toClient.flush();
//            response.getOutputStream().flush();
//            writeLogRecord(LogRecordEnum.OPE_KJ_ZCGL.getValue(), "导出资产卡片", ISysConstants.SYS_2);
//        } catch (Exception e) {
//            log.error("资产卡片EXCEL导出错误", e);
//        } finally {
//            try {
//                if (toClient != null) {
//                    toClient.close();
//                }
//            } catch (IOException e) {
//                log.error("资产卡片EXCEL导出错误", e);
//            }
//            try {
//                if (response.getOutputStream() != null) {
//                    response.getOutputStream().close();
//                }
//            } catch (IOException e) {
//                log.error("资产卡片EXCEL导出错误", e);
//            }
//        }
//    }

    private Map<String, String> getExpFieldMap(String lx) {
        if ("1".equals(lx)) {//按照导入格式导出
            Map<String, String> map = new LinkedHashMap<String, String>(40);
            map.put("zccode", "资产编码");
            map.put("assetname", "资产名称");
            map.put("assetcateall", "资产类别");
//			map.put("accountdate", "开始使用日期");
            map.put("period", "入账日期");
            map.put("zjtypestr", "折旧方式");
            map.put("uselimit", "预计使用年限");
            map.put("gzzl", "总工作量");
            map.put("gzldw", "工作量单位");
            map.put("assetmny", "原值");
            map.put("njxsf", "进项税额");
            map.put("depreciation", "总累计折旧");
            map.put("salvageratio", "残值率");
//			map.put("depreciationperiod", "已计提折旧期间");
            map.put("isperiodbegin", "录入前已生成凭证");
            map.put("initdepreciation", "录入前已提折旧");
            map.put("qcljgzl", "录入前累计工作量");
            map.put("initdepreciationperiod", "录入前已提期间(月)");
            map.put("zckmcode", "固定（无形）资产科目");
            map.put("jskmcode", "结算科目");
            map.put("jtzjkmcode", "折旧（摊销）科目");
            map.put("zjfykmcode", "折旧（摊销）费用科目");
            return map;
        } else {//默认是0
            Map<String, String> map = new LinkedHashMap<String, String>(40);
            map.put("assetcode", "卡片编码");
            map.put("period", "入账日期");
            map.put("zckm", "原值入账科目");
            map.put("zccode", "资产编码");
            map.put("assetname", "资产名称");
            map.put("jtzjkm", "折旧(摊销)科目");
            map.put("assetcate", "资产类别");
            map.put("assetproperty", "资产属性");
            map.put("zjfykm", "费用科目");
//			map.put("accountdate", "开始使用日期");
            map.put("zjtypestr", "折旧方式");
            map.put("uselimit", "预计使用年限");
            map.put("monthzj", "月折旧额");
            map.put("assetmny", "原值");
            map.put("depreciation", "总累计折旧");
            map.put("salvageratio", "残值率");
            map.put("plansalvage", "预计残值");
            map.put("assetnetvalue", "资产净值");
            map.put("depreciationperiod", "已计提折旧期间");
//			map.put("qcnetvalue", "期初净值");
//			map.put("usedperiod", "总累计使用期间月(数)");
            map.put("isperiodbegin", "录入前已生成凭证");
            map.put("initdepreciation", "录入前已提折旧");
            map.put("initdepreciationperiod", "录入前已提期间(月)");
//			map.put("initusedperiod", "期初已使用期间数(月)");
            map.put("istogl", "已转总账");
            map.put("isclear", "已清理");
//			map.put("accountdepreciation", "建账累计折旧");
//			map.put("accountdepreciationperiod", "建账折旧期间数(月)");
//			map.put("accountusedperiod", "建账已使用期间数(月)");
            map.put("voucherno", "凭证号");
            map.put("depreciationdate", "最后折旧月");
            return map;
        }

    }

    private void checkCorp(String loginCorp, AssetcardVO card)
            throws DZFWarpException {

        AssetcardVO vo = am_kpglserv.queryById(card.getPrimaryKey());
        if (vo == null)
            throw new BusinessException("该数据不存在，或已被删除！");
        if (!loginCorp.equals(vo.getPk_corp()))
            throw new BusinessException("只能操作当前登录公司权限内的数据！");
    }

    @PostMapping("doOrder")
    public ReturnData doOrder() {
        Json json = new Json();
        try {
            am_kpglserv.updateOrder(SystemUtil.getLoginCorpId());
            json.setSuccess(true);
            json.setMsg("整理成功");
            writeLogRecord(LogRecordEnum.OPE_KJ_ZCGL, "资产卡片整理:" + SystemUtil.getLoginDate(), ISysConstants.SYS_2);
        } catch (Exception e) {
            // json.setSuccess(false);
            // json.setMsg("整理失败！");
            printErrorLog(json,  e, "整理失败");
            log.error("整理失败", e);
        }
        return ReturnData.ok().data(json);
    }

    private static final Object[][] STYLE_1 = new Object[][]{{0, "zccode", "资产编码"}, // "资产编码");
            {1, "assetname", "资产名称"}, // "资产名称");
            {2, "assetcate", "资产类别"}, // "资产类别");
//			{ 3, "accountdate","开始使用日期" }, // "开始使用日期");
            {3, "period", "入账日期"}, // "开始使用日期");
            {4, "zjtype", "折旧方式"}, // "折旧方式");
            {5, "uselimit", "预计使用年限"}, // "预计使用年限");
            {6, "gzzl", "总工作量"},// 工作总量
            {7, "gzldw", "工作量单位"},// 工作单位
            {8, "assetmny", "原值"}, // "原值");{ 6, "monthzj"}, //"月折旧");
            {9, "njxsf", "进项税额"}, // 进项税额
            {10, "depreciation", "总累计折旧"}, // "总累计折旧");
            {11, "salvageratio", "残值率"}, // "残值率");
//			{ 12, "depreciationperiod","已计提折旧期间" }, // "已计提折旧期间");
            {12, "isperiodbegin", "录入前已生成凭证"}, // "录入前已入账");
            {13, "initdepreciation", "录入前已提折旧"}, // "期初累计折旧");
            {14, "qcljgzl", "录入前累计工作量"},// 期初累计工作量
            {15, "initdepreciationperiod", "录入前折旧期间数(月)"}, // "期初折旧期间数(月)");
            {16, "pk_zckm", "固定(无形)资产科目"}, // 固定(无形)资产科目
            {17, "pk_jskm", " 结算科目"}, // 结算科目
            {18, "pk_jtzjkm", "折旧(摊销)科目"}, // 折旧(摊销)科目
            {19, "pk_zjfykm", "折旧(摊销)费用科目"} // 折旧(摊销)费用科目
    };

//    public void impExcel() {
//        Json json = new Json();
//        try {
//            ((MultiPartRequestWrapper) getRequest()).getParameterMap();
//            File[] infiles = ((MultiPartRequestWrapper) getRequest())
//                    .getFiles("file");
//            String[] filenames = ((MultiPartRequestWrapper) getRequest())
//                    .getFileNames("file");
//            if (infiles == null) {
//                throw new Exception("导入文件为空");
//            }
//            File infile = infiles[0];
//            String filename = filenames[0];
//            if (!filename.endsWith(".xls")) {
//                throw new Exception("文件格式不正确");
//            }
//            Object[] vos = onBoImp(infile);
//            json.setRows(vos[0]);
//            json.setSuccess(true);
//            String tips1 = (String) vos[1];
//            String tips2 = (String) vos[2];
//            if (tips1.length() != 0 || tips2.length() != 0) {
//                json.setMsg(tips1 + "<br/>" + tips2);
//            }
//        } catch (Exception e) {
//            printErrorLog(json, log, e, "文件导入失败");
//            if (e instanceof BusinessException) {
//                json.setMsg(e.getMessage());
//            }
//        }
//        writeJson(json);
//    }
//
//    private Object[] onBoImp(File infile) throws DZFWarpException {
//        FileInputStream is = null;
//        try {
//            is = new FileInputStream(infile);
////			XSSFWorkbook rwb = new XSSFWorkbook(is);
//            HSSFWorkbook rwb = new HSSFWorkbook(is);
//            int sheetno = rwb.getNumberOfSheets();
//            if (sheetno == 0) {
//                throw new BusinessException("需要导入的数据为空。");
//            }
////			XSSFSheet sheets = rwb.getSheetAt(0);// 取第2个工作簿
//            HSSFSheet sheets = rwb.getSheetAt(0);
//            return doImport(infile, sheets);
//        } catch (FileNotFoundException e2) {
//            throw new BusinessException("文件未找到");
//        } catch (IOException e2) {
//            throw new BusinessException("文件格式不正确，请选择导入文件");
//        } finally {
//            if (is != null) {
//                try {
//                    is.close();
//                } catch (IOException e) {
//                    log.error("资产卡片导入关闭流失败", e);
//                }
//            }
//        }
//    }
//
//    private Object[] doImport(File excelfile, HSSFSheet sheets)
//            throws DZFWarpException {
//        AssetcardVO[] vos = getDataByExcel(excelfile.getPath(), sheets);
//        if (vos == null || vos.length <= 0) {
//            throw new BusinessException("导入文件数据为空，请检查。");
//        }
//
//        Object[] objs = am_kpglserv.impExcel(getLoginDate(),
//                getLoginUserid(), getLoginCorpInfo(), vos);
//        //导账
//        for (AssetcardVO vo : vos) {
//            writeLogRecord(LogRecordEnum.OPE_KJ_ZCGL.getValue(), "导入资产卡片:" + vo.getPeriod() + "，卡片编码:" + vo.getAssetcode(), ISysConstants.SYS_2);
//        }
//        return objs;
//    }

    /**
     * 读取Excel公司数据
     *
     * @param filepath
     * @return
     * @throws BusinessException
     */
    private AssetcardVO[] getDataByExcel(String filepath, HSSFSheet sheets1)
            throws DZFWarpException {
        List<AssetcardVO> clist = null;
        AssetcardVO[] vos = null;
        try {
            clist = new ArrayList<AssetcardVO>();
            AssetcardVO excelvo = null;
            HSSFCell aCell = null;
            String field = null;
            String sTmp = null;
            int len = (sheets1.getLastRowNum() + 1);
            StringBuffer tipsmsg = new StringBuffer();
            Class cls = Class.forName(AssetcardVO.class.getName());
            for (int iBegin = 1; iBegin < len; iBegin++) {
                excelvo = new AssetcardVO();
                for (int j = 0; j < STYLE_1.length; j++) {
                    try {
                        aCell = sheets1.getRow(iBegin).getCell(
                                (Integer.valueOf(STYLE_1[j][0].toString())));
                        field = STYLE_1[j][1].toString();
                        sTmp = getExcelCellValue(aCell, field, cls.getDeclaredField(field));
                        if (!StringUtil.isEmpty(sTmp)) {
                            if (field.equals("zjtype")) {
                                if (sTmp.equals("平均年限法")) {
                                    excelvo.setAttributeValue(
                                            STYLE_1[j][1].toString(), 0);
                                } else if (sTmp.equals("工作量法")) {
                                    excelvo.setAttributeValue(
                                            STYLE_1[j][1].toString(), 1);
                                } else if (sTmp.equals("双倍余额递减法")) {
                                    excelvo.setAttributeValue(
                                            STYLE_1[j][1].toString(), 2);
                                } else if (sTmp.equals("年数总和法")) {
                                    excelvo.setAttributeValue(
                                            STYLE_1[j][1].toString(), 3);
                                }
                            } else {
                                excelvo.setAttributeValue(STYLE_1[j][1].toString(),
                                        sTmp.replaceAll(" ", ""));
                            }
                        }
                    } catch (Exception e) {
                        //日志记录在这里
                        tipsmsg.append("行数:" + iBegin + "字段:\"" + STYLE_1[j][2] + "\"内容格式错误<br/>");
                    }
                }
                if (!allEmpty(excelvo)) {
                    if (excelvo.getZccode() != null && excelvo.getZccode().trim().length() > 0) {
                        clist.add(excelvo);
                    } else {
                        tipsmsg.append("资产编码不存在<br/>");
                    }
                }
            }
            if (tipsmsg.toString().length() > 0) {
                throw new BusinessException(tipsmsg.toString());
            }
            if (clist.size() > 0) {
                vos = clist.toArray(new AssetcardVO[1]);
            }
        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw new BusinessException(e.getMessage());
            } else {
                throw new WiseRunException(e);
            }
        }
        return vos;
    }

    //全部项是否为空
    private boolean allEmpty(AssetcardVO excelvo) {
        if (excelvo == null) {
            return true;
        }
        String key = "";
        Object objvalue = "";
        for (int j = 0; j < STYLE_1.length; j++) {
            key = (String) STYLE_1[j][1];
            objvalue = excelvo.getAttributeValue(key);
            if (objvalue instanceof String) {
                if (!StringUtil.isEmpty((String) objvalue)) {
                    return false;
                }
            } else if (objvalue != null) {
                return false;
            }
        }
        return true;
    }

    private String getExcelCellValue(HSSFCell cell, String field, Field fiel) {
        String ret = "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");// 格式化日期字符串
            if (cell == null) {
                ret = null;
            } else if (cell.getCellType() == XSSFCell.CELL_TYPE_STRING) {
                ret = cell.getRichStringCellValue().getString();
            } else if (cell.getCellType() == XSSFCell.CELL_TYPE_NUMERIC) {
                // 小数不可用这样格式，只为了凭证编码格式
                java.text.DecimalFormat formatter = new java.text.DecimalFormat("#########.##");
                if ("General".equals(cell.getCellStyle().getDataFormatString()) || fiel.getType().getName().equals(DZFDouble.class.getName())) {
                    ret = formatter.format(cell.getNumericCellValue());
                } else {
                    ret = sdf.format(HSSFDateUtil.getJavaDate(cell.getNumericCellValue()));
                }
            } else if (cell.getCellType() == XSSFCell.CELL_TYPE_FORMULA) {
                ret = cell.getCellFormula();
            } else if (cell.getCellType() == XSSFCell.CELL_TYPE_ERROR) {
                ret = "" + cell.getErrorCellValue();
            } else if (cell.getCellType() == XSSFCell.CELL_TYPE_BOOLEAN) {
                ret = "" + cell.getBooleanCellValue();
            } else if (cell.getCellType() == XSSFCell.CELL_TYPE_BLANK) {
                ret = null;
            }
        } catch (Exception ex) {
            log.error("获取EXCEL数据异常", ex);
            ret = null;
        }
        return ret;
    }

    /**
     * 使用年限调整
     */
    @PostMapping("adjustLimit")
    public ReturnData adjustLimit(String id, String assetcode, String newlimit) {
        Json json = new Json();

        try {
            Integer limit = new Integer(newlimit);
            am_kpglserv.updateAdjustLimit(id, limit);
            json.setSuccess(true);
            json.setMsg("调整成功");
        } catch (Exception e) {
            printErrorLog(json, e, "操作失败!");
            log.error("操作失败", e);
        }
        writeLogRecord(LogRecordEnum.OPE_KJ_ZCGL, "使用年限调整:" + newlimit + "，卡片编码:" + assetcode,
                ISysConstants.SYS_2);
        return ReturnData.ok().data(json);
    }
}
