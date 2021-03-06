package com.dzf.zxkj.platform.service.zncs.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.dzf.zxkj.base.dao.SingleObjectBO;
import com.dzf.zxkj.base.exception.BusinessException;
import com.dzf.zxkj.base.exception.DZFWarpException;
import com.dzf.zxkj.common.constant.IBillManageConstants;
import com.dzf.zxkj.common.lang.DZFBoolean;
import com.dzf.zxkj.common.lang.DZFDate;
import com.dzf.zxkj.common.lang.DZFDouble;
import com.dzf.zxkj.common.utils.DateUtils;
import com.dzf.zxkj.common.utils.IDefaultValue;
import com.dzf.zxkj.common.utils.StringUtil;
import com.dzf.zxkj.platform.model.image.DcModelHVO;
import com.dzf.zxkj.platform.model.piaotong.CaiFangTongBVO;
import com.dzf.zxkj.platform.model.piaotong.CaiFangTongHVO;
import com.dzf.zxkj.platform.model.piaotong.PiaoTongJinXiangBVO;
import com.dzf.zxkj.platform.model.piaotong.PiaoTongJinXiangHVO;
import com.dzf.zxkj.platform.model.pjgl.VATInComInvoiceVO;
import com.dzf.zxkj.platform.model.pzgl.TzpzHVO;
import com.dzf.zxkj.platform.model.pzgl.VoucherParamVO;
import com.dzf.zxkj.platform.model.sys.CorpTaxVo;
import com.dzf.zxkj.platform.model.sys.CorpVO;
import com.dzf.zxkj.platform.model.zncs.VATInComInvoiceBVO;
import com.dzf.zxkj.platform.service.pzgl.IVoucherService;
import com.dzf.zxkj.platform.service.sys.IBDCorpTaxService;
import com.dzf.zxkj.platform.service.sys.IDcpzService;
import com.dzf.zxkj.platform.service.zncs.IPiaoTongJinXiangService;
import com.dzf.zxkj.platform.service.zncs.IVATInComInvoiceService;
import com.dzf.zxkj.platform.util.zncs.ICaiFangTongConstant;
import com.dzf.zxkj.platform.util.zncs.PiaoTongJinXiang;
import com.dzf.zxkj.platform.util.zncs.VatUtil;
import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service("piaotongjinxiangservice")
public class PiaoTongJinXiangServiceImpl implements IPiaoTongJinXiangService {
	private Logger log = Logger.getLogger(this.getClass());
	@Autowired
	private SingleObjectBO singleObjectBO;
	@Autowired
	private IDcpzService dcpzjmbserv;
	@Autowired
	private IVATInComInvoiceService gl_vatincinvact;
	@Autowired
	private IVoucherService gl_tzpzserv;
	@Autowired
	private IBDCorpTaxService sys_corp_tax_serv;

//	private static final String CHARGEDEPTNAME_YBR = "一般纳税人";

	@Override
	public Map<String, VATInComInvoiceVO> savePt(CorpVO corpvo, String f2, String userid, DZFDate loginDate) throws DZFWarpException {

		if(corpvo == null){
			throw new BusinessException("公司参数不完整，请检查");
		}

		PiaoTongJinXiang pt = getPtInstance(corpvo, f2, loginDate);//获取实例

		//1身份认证接口
		String token = pt.getToken();

		if(StringUtil.isEmpty(token)){
			throw new BusinessException("获取token信息不完整，请检查");
		}

		CaiFangTongHVO[] cfthvos = getBillInfo(pt, token, corpvo, userid);

		Map<String, VATInComInvoiceVO> repmap = updateDatas(cfthvos, corpvo);

		return repmap;
	}

	private Map<String, VATInComInvoiceVO> updateDatas(CaiFangTongHVO[] cfthvos, CorpVO corpvo){
		String pk_corp = corpvo.getPk_corp();
		String nsrsbh = corpvo.getVsoccrecode();//公司的纳税人识别号
		Map<String, VATInComInvoiceVO> map = new LinkedHashMap<String, VATInComInvoiceVO>();

		if(cfthvos == null || cfthvos.length == 0){
			return map;
		}

		List<DcModelHVO> dcList = dcpzjmbserv.query(pk_corp);
		Map<String, DcModelHVO> dcMap = new HashMap<String, DcModelHVO>();
//		Map<String, DcModelHVO> dcMap1 = new HashMap<String, DcModelHVO>();
		hashlizeDcModel(dcList, dcMap);

		String hm = null;
		String kplx = null;
		String demo = null;

		VATInComInvoiceVO oldvo = null;
		VATInComInvoiceVO incomvo = null;

		Map<String, String> hmap = getVATHMapping();
		Map<String, String> bmap = getVATBMapping();
//		String gmfsbh = null;
//		int count = 0;
		for(CaiFangTongHVO hvo : cfthvos){
//			if(count++ > 10)//先这么控制下 上线时在拿掉
//				continue;

			hm = hvo.getFphm();

			kplx = hvo.getKplx();

//			gmfsbh = hvo.getGmf_nsrsbh();//购买方识别号

			oldvo = gl_vatincinvact.queryByCGTId(hm, hvo.getFpdm(), pk_corp);//查询库里是否存在此票

			if(!StringUtil.isEmpty(kplx)
					&& (ICaiFangTongConstant.FPLX_PT_0.equals(kplx)
							|| ICaiFangTongConstant.FPLX_PT_3.equals(kplx))){
				int dr = 0;
				if(oldvo != null){
					dr = 1;
					setCFTDelVO(hvo, dr, "新增前校验到该号码代码数据库中已有数据");
				}
//				else if(!nsrsbh.equals(gmfsbh)){
//					dr = 1;
//					setCFTDelVO(hvo, dr, "新增前校验到该号码代码的购买方识别号不匹配");
//				}

				if(dr == 0 && (hvo.getChildren() == null
								|| hvo.getChildren().length == 0)){
					hvo = buildInComVO(hvo);
				}

				hvo = saveCftVO(pk_corp, hvo);


				incomvo = tranVATInComData(hvo, dr, hmap, bmap, dcMap, null);

				incomvo = saveIncomVO(pk_corp, incomvo);

				if(dr == 0){
					map.put(hm, incomvo);
				}

			}else if(ICaiFangTongConstant.FPLX_PT_2.equals(kplx)){
				int dr = 0;

//				if(!nsrsbh.equals(gmfsbh)){
//					dr = 1;
//					demo = "新增前校验到该号码代码的购买方识别号不匹配";
//				}
				if(oldvo == null){
					demo = "新增前校验到该号码代码(废票)数据库中没有数据";
				}else{
					updateInComVOStatus(oldvo, hvo, kplx, pk_corp);
					dr = 1;
					demo = "新增前校验到该号码代码(废票)数据库中有数据";

				}

				setCFTDelVO(hvo, dr, demo);

				if(dr == 0 && (hvo.getChildren() == null
						|| hvo.getChildren().length == 0)){
					hvo = buildInComVO(hvo);
				}


				hvo = saveCftVO(pk_corp, hvo);
				incomvo = tranVATInComData(hvo, dr, hmap, bmap, dcMap, null);

				incomvo = saveIncomVO(pk_corp, incomvo);

				if(dr == 0){
					map.put(hm, incomvo);
				}
			}

		}

		return map;
	}

	private CaiFangTongHVO buildInComVO(CaiFangTongHVO hvo){
		List<CaiFangTongHVO> ll = new ArrayList<CaiFangTongHVO>();
		String dateL = hvo.getKprq();//转换前 开票日期
		try {
			if(!StringUtil.isEmpty(dateL)){
//				hvo.setKprq(new DZFDate(Long.parseLong(dateL)).toString());//开票日期

				ll.add(hvo);
				ll = VatUtil.reGetData(ll);

				if(ll != null && ll.size() > 0){
					hvo = ll.get(0);
				}
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

//		hvo.setKprq(dateL);//重新设置回来
		return hvo;
	}

	private void updateInComVOStatus(VATInComInvoiceVO invo, CaiFangTongHVO hvo, String kplx, String pk_corp){
		DZFDouble je = StringUtil.isEmpty(hvo.getKphjje()) ? new DZFDouble(0) : new DZFDouble(hvo.getKphjje());
		kplx = getKplx(kplx, je);
		invo.setKplx(kplx);
		singleObjectBO.update(invo,
				new String[]{ "kplx" });

		if(!StringUtil.isEmpty(invo.getPk_tzpz_h())){
			VoucherParamVO paramvo = new VoucherParamVO();
			paramvo.setPk_tzpz_h(invo.getPk_tzpz_h());
			TzpzHVO tzpzhvo = gl_tzpzserv.queryHeadVoById(paramvo.getPk_tzpz_h());
			if(tzpzhvo != null){
				gl_tzpzserv.deleteVoucher(tzpzhvo);
			}
		}

	}

	private VATInComInvoiceVO saveIncomVO(String pk_corp, VATInComInvoiceVO vo){
		return (VATInComInvoiceVO) singleObjectBO.saveObject(pk_corp, vo);
	}

//	private Object specialTransDate(String value){
//		if(value == null)
//			return null;
//		Long v = Long.parseLong(value);
//		return new DZFDate(v);
//	}

	private VATInComInvoiceVO tranVATInComData(CaiFangTongHVO hvo,
			int dr,
			Map<String, String> hmap,
			Map<String, String> bmap,
			Map<String, DcModelHVO> dcMap,
			Map<String, DcModelHVO> dcMap1){
		VATInComInvoiceVO incomvo = new VATInComInvoiceVO();

		Object value = null;

		for(Map.Entry<String, String> entry : hmap.entrySet()){

			value = hvo.getAttributeValue(entry.getKey());
//			if("kprq".equals(entry.getKey())//开票日期
//					|| "vdef2".equals(entry.getKey())){//认证日期
//				value = specialTransDate((String)value);
//			}

			if(value != null){
				incomvo.setAttributeValue(entry.getValue(), value);
			}
		}

		setDefaultHeadValue(incomvo, hvo, dr);

		CaiFangTongBVO[] cftbvos = hvo.getChildren();

		if(cftbvos != null && cftbvos.length > 0){
			List<VATInComInvoiceBVO> incombList = new ArrayList<VATInComInvoiceBVO>();
			VATInComInvoiceBVO incombvo = null;
			String spmc = null;
			DZFDouble spsl = null;
			for(CaiFangTongBVO cftbvo : cftbvos){
				incombvo = new VATInComInvoiceBVO();
				for(Map.Entry<String, String> entry1 : bmap.entrySet()){

					value = cftbvo.getAttributeValue(entry1.getKey());
					incombvo.setAttributeValue(entry1.getValue(), value);
				}

				if(StringUtil.isEmpty(spmc)
						&& !StringUtil.isEmpty(incombvo.getBspmc())){
					spmc = incombvo.getBspmc();
				}

				if(spsl == null && incombvo.getBspsl() != null){
					spsl = incombvo.getBspsl();
				}

				incombvo.setDr(dr);

				incombList.add(incombvo);
			}

			incomvo.setChildren(incombList.toArray(
					new VATInComInvoiceBVO[0]));

			incomvo.setSpsl(spsl);//设置税率
			if(!StringUtil.isEmpty(spmc)){
				incomvo.setSpmc(spmc);
				gl_vatincinvact.scanMatchBusiName(incomvo, dcMap);
			}
		}

		return incomvo;
	}

//	public void scanMatchBusiName(VATInComInvoiceVO incomvo, Map<String, DcModelHVO> dcmap) throws DZFWarpException{
//		if(dcmap == null || dcmap.size() == 0){
//			return;
//		}
//		String pk_corp = incomvo.getPk_corp();//公司pk
//		CorpVO corpvo = CorpCache.getInstance().get(null, pk_corp);
//		String chargedeptname = corpvo.getChargedeptname();
//		DZFBoolean iszh = incomvo.getIsZhuan();
//		iszh = iszh != null && iszh.booleanValue() ? iszh : DZFBoolean.FALSE;
//
//		String spmc = incomvo.getSpmc();
//
//		String key = null;
//
//		if(CHARGEDEPTNAME_YBR.equals(chargedeptname)){
//			boolean ismatch = isMatchFei(spmc);
//			if(iszh.booleanValue()){
//				if(ismatch){
//					key = "购买其他（一般人）"
//							+ "_" + FieldConstant.FPSTYLE_01
//							+ "_" + FieldConstant.SZSTYLE_06;
//				}else{
//					key = FieldConstant.YWSTYLE_24_02
//							+ "_" + FieldConstant.FPSTYLE_01
//							+ "_" + FieldConstant.SZSTYLE_06;
//				}
//
//			}else{
//
//				if(ismatch){
//					key = FieldConstant.YWSTYLE_21
//							+ "_" + FieldConstant.FPSTYLE_21
//							+ "_" + FieldConstant.SZSTYLE_06;
//				}else{
//					key = FieldConstant.YWSTYLE_24_02
//							+ "_" + FieldConstant.FPSTYLE_02
//							+ "_" + FieldConstant.SZSTYLE_06;
//				}
//			}
//
//		}else{
//			boolean ismatch = isMatchFei(spmc);
//			if(ismatch){
//				key = FieldConstant.YWSTYLE_21
//						+ "_" + FieldConstant.FPSTYLE_21
//						+ "_" + FieldConstant.SZSTYLE_06;
//			}else{
//				key = FieldConstant.YWSTYLE_24_01
//						+ "_" + FieldConstant.FPSTYLE_02
//						+ "_" + FieldConstant.SZSTYLE_06;
//			}
//		}
//
//
////		String zp = iszh != null && iszh.booleanValue()
////				? FieldConstant.FPSTYLE_01 : FieldConstant.FPSTYLE_02;
//
//		DcModelHVO hvo = dcmap.get(key);
//		if(hvo != null && !StringUtil.isEmpty(hvo.getPk_model_h())){
//			incomvo.setPk_model_h(hvo.getPk_model_h());
//			incomvo.setBusitypetempname(hvo.getBusitypetempname());
//		}
//
//	}
//
//	private boolean isMatchFei(String name){
//		if(name.contains("费")
//				|| name.contains("劳务")){
//			return true;
//		}else{
//			return false;
//		}
//	}

	private void setDefaultHeadValue(VATInComInvoiceVO incomvo,
			CaiFangTongHVO hvo,
			int dr){
		//单据来源
		incomvo.setSourcetype(IBillManageConstants.PIAOTONGJX);
		incomvo.setSourcebilltype(ICaiFangTongConstant.LYDJLX_PT);
		incomvo.setSourcebillid(hvo.getPrimaryKey());
		incomvo.setPeriod(DateUtils.getPeriod(new DZFDate()));//期间

		//设置期间
		String period = null;
		if(incomvo.getRzjg() != null && incomvo.getRzjg() == 1 && incomvo.getRzrj() != null){
			period = DateUtils.getPeriod(incomvo.getRzrj());
		}else if(incomvo.getKprj() != null){//因为一键取票的时间是在建账日期后的，所以不调整
			period = DateUtils.getPeriod(incomvo.getKprj());
		}
		incomvo.setInperiod(period);

		//计算税率
//		incomvo.setSpsl(calSl(incomvo.getSpse(), incomvo.getHjje()));

		DZFBoolean iszh = getZhuan(hvo);
		incomvo.setIszhuan(iszh);
		incomvo.setDr(dr);

		//重新设置开票类型
		String kplx = incomvo.getKplx();
		DZFDouble je = incomvo.getHjje();
		kplx = getKplx(kplx, je);
		incomvo.setKplx(kplx);
	}

//	private DZFDouble calSl(DZFDouble se, DZFDouble je){
//		DZFDouble sl = SafeCompute.multiply(SafeCompute.div(se, je), new DZFDouble(100));
//
//		sl = sl.setScale(0, DZFDouble.ROUND_HALF_UP);
//		return sl;
//	}

	private DZFBoolean getZhuan(CaiFangTongHVO cftvo){
		DZFBoolean iszh = null;
		if(!StringUtil.isEmpty(cftvo.getFp_zldm())
				&& ICaiFangTongConstant.FPZLDM_SM_01.equals(cftvo.getFp_zldm())){
			iszh = DZFBoolean.TRUE;
		}else{
			iszh = DZFBoolean.FALSE;
		}

		return iszh;
	}

	private String getKplx(String kplx, DZFDouble je){
		if(!StringUtil.isEmpty(kplx)){
			if(ICaiFangTongConstant.FPLX_PT_0.equals(kplx)){
				if(je != null && je.doubleValue() >= 0){
					kplx = ICaiFangTongConstant.FPLX_1;
				}else if(je != null && je.doubleValue() < 0){
					kplx = ICaiFangTongConstant.FPLX_2;
				}else{
					kplx = null;
				}

			} else if(ICaiFangTongConstant.FPLX_PT_3.equals(kplx)){
				if(je != null && je.doubleValue() >= 0){
					kplx = ICaiFangTongConstant.FPLX_1;
				}else if(je != null && je.doubleValue() < 0){
					kplx = ICaiFangTongConstant.FPLX_2;
				}else{
					kplx = null;
				}
			}
			else if(ICaiFangTongConstant.FPLX_PT_2.equals(kplx)){
				if(je != null && je.doubleValue() >= 0){
					kplx = ICaiFangTongConstant.FPLX_4;
				}else if(je != null && je.doubleValue() < 0){
					kplx = ICaiFangTongConstant.FPLX_5;
				}else{
					kplx = null;
				}
			}else if(ICaiFangTongConstant.FPLX_PT_N.equals(kplx)){//发票查验的结果
				if(je != null && je.doubleValue() >= 0){
					kplx = ICaiFangTongConstant.FPLX_1;
				}else if(je != null && je.doubleValue() < 0){
					kplx = ICaiFangTongConstant.FPLX_2;
				}else{
					kplx = null;
				}
			}else if(ICaiFangTongConstant.FPLX_PT_Y.equals(kplx)){
				if(je != null && je.doubleValue() >= 0){
					kplx = ICaiFangTongConstant.FPLX_4;
				}else if(je != null && je.doubleValue() < 0){
					kplx = ICaiFangTongConstant.FPLX_5;
				}else{
					kplx = null;
				}
			}else{//2失控、4异常暂不处理
				kplx = null;
			}
		}

		return kplx;
	}

//	private void setBodyValue(VATInComInvoiceVO invo, String fpzl){
//		if(ICaiFangTongConstant.FPZLDM_SM_01.equals(fpzl)){
//			VATInComInvoiceBVO bvo = new VATInComInvoiceBVO();
//			bvo.setRowno(1);
//			bvo.setBhjje(invo.getHjje());
//			bvo.setBspse(invo.getSpse());
//			bvo.setBspsl(invo.getSpsl());
//			bvo.setPk_corp(invo.getPk_corp());
//
//			invo.setChildren(new VATInComInvoiceBVO[]{ bvo });
//		}
//	}

	private CaiFangTongHVO saveCftVO(String pk_corp, CaiFangTongHVO hvo){
		return (CaiFangTongHVO) singleObjectBO.saveObject(pk_corp, hvo);
	}

	private void setCFTDelVO(CaiFangTongHVO hvo, int dr, String demo){
		hvo.setDr(dr);//删除标识
		hvo.setDemo1(demo);
		CaiFangTongBVO[] cbvos = hvo.getChildren();
		if(cbvos != null && cbvos.length > 0){
			for(CaiFangTongBVO bvo : cbvos){
				bvo.setDr(dr);
			}
		}
	}

	private Map<String, DcModelHVO> hashlizeDcModel(List<DcModelHVO> list,
			Map<String, DcModelHVO> map){
		if(list == null || list.size() == 0)
			return null;

//		String key = null;
		String key1 = null;
//		String szcode = null;
		String vscode = null;
		String businame = null;
		String pk_corp;
		for(DcModelHVO hvo : list){
//			szcode = hvo.getSzstylecode();
			vscode = hvo.getVspstylecode();
			businame = hvo.getBusitypetempname();

//			key = businame
//					+ "_" + vscode
//					+ "_" + szcode;
//
//			if(!map.containsKey(key)){
//				map.put(key, hvo);
//			}
//
			key1 = businame
					+ "_" + vscode;
			pk_corp = hvo.getPk_corp();
			if(!StringUtil.isEmpty(pk_corp)
					&& !IDefaultValue.DefaultGroup.equals(pk_corp)){
				map.put(key1, hvo);
			}else if(!map.containsKey(key1)){
				map.put(key1, hvo);
			}

		}

		return map;
	}



	private PiaoTongJinXiang getPtInstance(CorpVO corpvo, String f2, DZFDate loginDate){
		String pk_corp = corpvo.getPk_corp();
		String nsrsbh = corpvo.getVsoccrecode();//税号

		CorpTaxVo taxvo = sys_corp_tax_serv.queryCorpTaxVO(pk_corp);
		String golddiskno = taxvo.getGolddiskno();//金税盘编号
//		String lastTime = getLastTime(pk_corp, ICaiFangTongConstant.LYLX_JX);

		String period = DateUtils.getPeriod(loginDate);

		PiaoTongJinXiang stance = new PiaoTongJinXiang(nsrsbh, f2, golddiskno, period, 1);

		return stance;
	}

	private CaiFangTongHVO[] getBillInfo(PiaoTongJinXiang piaotong, String token, CorpVO corpvo, String userid){
		List<PiaoTongJinXiangHVO> cftList = new ArrayList<PiaoTongJinXiangHVO>();

		List<PiaoTongJinXiangHVO> ptvos = piaotong.getVOs(token);

		Integer totalCount = piaotong.getTotalCount();
		Integer totalPageNum = piaotong.getTotalPageNum();
		Integer currentPageNum = piaotong.getCurrentPageNum();

		if(totalCount == null
				|| totalPageNum == null
				|| ptvos == null){
			return null;
		}

		cftList.addAll(ptvos);

		while(totalPageNum > currentPageNum){
			currentPageNum++;
			piaotong.setCurrentPageNum(currentPageNum);
			ptvos = piaotong.getVOs(token);

			cftList.addAll(ptvos);
		}

		CaiFangTongHVO[] hvos = transCFTData(cftList, corpvo, userid);

		return hvos;
	}

	private Map<String, String> getVATHMapping(){
		Map<String, String> map = new HashMap<String, String>();

		map.put("doperatedate", "doperatedate");
		map.put("coperatorid", "coperatorid");
		map.put("pk_corp", "pk_corp");
		map.put("xsf_dz", "xhfdzdh");//销方地址电话
		map.put("xsf_yh", "xhfyhzh");//销方开户行及账号
		map.put("gmf_dz", "ghfdzdh");//购方地址电话
		map.put("gmf_yh", "ghfyhzh");//购方开户行及账号
		map.put("fpdm", "fp_dm");//发票代码
		map.put("fphm", "fp_hm");//发票号码
		map.put("kprq", "kprj");//开票日期
		map.put("gmf_nsrmc", "ghfmc");//购方名称
		map.put("gmf_nsrsbh", "ghfsbh");//购方纳税人识别号
		map.put("xsf_nsrmc", "xhfmc");//销方名称
		map.put("xsf_nsrsbh", "xhfsbh");//销方纳税人识别号
		map.put("kphjse", "spse");//合计税额
		map.put("kphjje", "jshj");//价税合计
		map.put("hjbhsje", "hjje");//合计金额
		map.put("fp_zldm", "fpzl");//票种代码
		map.put("kplx", "kplx");//票据类型
		map.put("bz", "demo");////备注
		map.put("vdef1", "rzjg");//认证状态
		map.put("vdef2", "rzrj");//认证日期
		map.put("jym", "jym");//校验码

		map.put("vdef4", "imgpath");//图片路径

		return map;
	}

	private Map<String, String> getVATBMapping(){
		Map<String, String> map = new HashMap<String, String>();

		map.put("spmc", "bspmc");//商品名称
		map.put("ggxh", "invspec");//规格型号
		map.put("dw", "measurename");//单位
		map.put("spsl", "bnum");//数量
		map.put("spdj", "bprice");//单价
		map.put("spje", "bhjje");//金额
		map.put("sl", "bspsl");//税率
		map.put("se", "bspse");//税额
		map.put("pk_corp", "pk_corp");//公司

		return map;
	}

	private CaiFangTongHVO[] transCFTData(List<PiaoTongJinXiangHVO> ptvos,
			CorpVO corpvo, String userid){
		if(ptvos == null || ptvos.size() == 0)
			return null;

		String pk_corp = corpvo.getPk_corp();
		List<CaiFangTongHVO> ctfList = new ArrayList<CaiFangTongHVO>();
		Map<String, String> hmap = getCFTHMapping();
		Map<String, String> bmap = getCFTBMapping();

		CaiFangTongHVO cftvo = null;
		CaiFangTongBVO cftbvo = null;
		List<CaiFangTongBVO> cftbvos = null;
		List <PiaoTongJinXiangBVO> ptbvos = null;
		Object value = null;
		for(PiaoTongJinXiangHVO hvo : ptvos){

			cftvo = new CaiFangTongHVO();
			for(Map.Entry<String, String> entry : hmap.entrySet()){
				value = hvo.getAttributeValue(entry.getKey());

				if(value != null){
					cftvo.setAttributeValue(entry.getValue(), value);
				}
			}

			cftvo.setLy(ICaiFangTongConstant.LYLX_JX);
			cftvo.setDoperatedate(new DZFDate());
			cftvo.setPk_corp(pk_corp);
			cftvo.setCoperatorid(userid);

			ptbvos = hvo.getInvoiceVatDetailsList();
			cftbvos = new ArrayList<CaiFangTongBVO>();
			if(ptbvos != null && ptbvos.size() > 0){
				for(PiaoTongJinXiangBVO ptbvo : ptbvos){
					cftbvo = new CaiFangTongBVO();
					for(Map.Entry<String, String> entry1 : bmap.entrySet()){
						value = ptbvo.getAttributeValue(entry1.getKey());

						if(value != null){
							cftbvo.setAttributeValue(entry1.getValue(), value);
						}
					}

					cftbvo.setPk_corp(pk_corp);

					cftbvos.add(cftbvo);
				}


			}else{
				cftbvo = new CaiFangTongBVO();
				cftbvo.setPk_corp(pk_corp);;
				cftbvo.setSpje(cftvo.getHjbhsje());
				cftbvo.setSe(cftvo.getKphjse());
			}

			cftvo.setChildren(cftbvos.toArray(
					new CaiFangTongBVO[0]));

			ctfList.add(cftvo);
		}

		return ctfList.toArray(new CaiFangTongHVO[0]);
	}

	private Map<String, String> getCFTBMapping(){
		Map<String, String> map = new HashMap<String, String>();

		map.put("name", "spmc");//货物或应税劳务名称
		map.put("specification", "ggxh");//规格型号
		map.put("unit", "dw");//单位
		map.put("quantity", "spsl");//数量
		map.put("unitPrice", "spdj");//单价
		map.put("money", "spje");//金额
		map.put("taxRate", "sl");//税率
		map.put("taxAmount", "se");//税额

		return map;
	}

	private Map<String, String> getCFTHMapping(){
		Map<String, String> map = new HashMap<String, String>();

		map.put("salesContactWay", "xsf_dz");//销方地址电话
		map.put("salesBankAccount", "xsf_yh");//销方开户行及账号
		map.put("buyerContactWay", "gmf_dz");//购方地址电话
		map.put("buyerBankAccount", "gmf_yh");////购方开户行及账号
		map.put("checkCode", "jym");//校验码
		map.put("machineCode", "jqbh");//机器码
		map.put("invoiceCode", "fpdm");//发票代码
		map.put("invoiceNum", "fphm");//发票号码
		map.put("billingDate", "kprq");//开票日期
		map.put("buyerName", "gmf_nsrmc");//购方名称
		map.put("buyerTaxNum", "gmf_nsrsbh");//购方纳税人识别号
		map.put("salesName", "xsf_nsrmc");//销方名称
		map.put("salesTaxNum", "xsf_nsrsbh");//销方纳税人识别号
		map.put("totalTaxAmount", "kphjse");//合计税额
		map.put("totalAmount", "kphjje");//价税合计
		map.put("totalMoney", "hjbhsje");//合计金额
		map.put("invoiceTicketType", "fp_zldm");//票种代码
		map.put("invoiceStatus", "kplx");//发票异常状态  0-正常 1-失控 2-作废 3-冲红 4-异常
		map.put("remarks", "bz");//备注
		map.put("deductibleStatus", "vdef1");//认证状态
		map.put("deductibleDate", "vdef2");//认证日期
		map.put("deductiblePeriod", "vdef3");//认证所属期
		map.put("invoiceStatus", "kplx");
		map.put("nextRequestTime", "maxkprq");

		map.put("imagePath", "vdef4");//认证所属期

		return map;
	}

//	private String getLastTime(String pk_corp, int sjly){
//
//		String sql = "Select max(h.maxkprq) From ynt_caifangtong_h h Where nvl(dr,0) = 0 and pk_corp = ? and ly = ? ";
//
//		SQLParameter sp = new SQLParameter();
//		sp.addParam(pk_corp);
//		sp.addParam(sjly);
//
//		String lastTime = (String) singleObjectBO.executeQuery(sql,
//				sp, new ObjectProcessor());
//
//		return lastTime;
//	}

}
