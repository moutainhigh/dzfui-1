package com.dzf.zxkj.platform.service.pjgl;

import com.dzf.zxkj.base.exception.DZFWarpException;
import com.dzf.zxkj.common.entity.Grid;
import com.dzf.zxkj.common.lang.DZFBoolean;
import com.dzf.zxkj.platform.model.bdset.YntCpaccountVO;
import com.dzf.zxkj.platform.model.glic.InventoryAliasVO;
import com.dzf.zxkj.platform.model.glic.InventorySetVO;
import com.dzf.zxkj.platform.model.icset.IntradeHVO;
import com.dzf.zxkj.platform.model.image.DcModelHVO;
import com.dzf.zxkj.platform.model.pjgl.InvoiceParamVO;
import com.dzf.zxkj.platform.model.pjgl.VATInComInvoiceVO;
import com.dzf.zxkj.platform.model.pjgl.VatBusinessTypeVO;
import com.dzf.zxkj.platform.model.pjgl.VatInvoiceSetVO;
import com.dzf.zxkj.platform.model.pzgl.TzpzHVO;
import com.dzf.zxkj.platform.model.sys.CorpVO;
import com.dzf.zxkj.platform.model.tax.TaxitemVO;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface IVATInComInvoiceService {

	/**
	 * 查询
	 * @param paramvo
	 * @param sort
	 * @param order
	 * @return
	 * @throws DZFWarpException
	 */
	public List<VATInComInvoiceVO> quyerByPkcorp(InvoiceParamVO paramvo, String sort, String order) throws DZFWarpException;

	public VATInComInvoiceVO queryByID(String pk) throws DZFWarpException;
	/**
	 * 更新操作
	 * @param isAddNew
	 * @param pk_corp
	 * @param cuserid
	 * @param sort
	 * @param order
	 * @param list
	 * @throws DZFWarpException
	 */
//	public void updateVOArr(DZFBoolean isAddNew, String pk_corp, String cuserid, String sort, String order, List<VATInComInvoiceVO> list) throws DZFWarpException;
	
	
	public VATInComInvoiceVO[] updateVOArr(String pk_corp, Map<String, VATInComInvoiceVO[]> map) throws DZFWarpException;
	
	/**
	 * 删除
	 * @param vo
	 * @param pk_corp
	 * @throws DZFWarpException
	 */
	void delete(VATInComInvoiceVO vo, String pk_corp) throws DZFWarpException;
	
	/**
	 * 导入
	 * @param file
	 * @param pk_corp
	 * @param fileType
	 * @param userid
	 * @throws DZFWarpException
	 */
	public void saveImp(File file, VATInComInvoiceVO paramvo, String pk_corp, String fileType, String userid, StringBuffer msg) throws DZFWarpException;
	
	/**
	 * 生成凭证
	 * @throws DZFWarpException
	 */
	public void createPZ(VATInComInvoiceVO vo, String pk_corp, String userid, Map<String, DcModelHVO> map, VatInvoiceSetVO setvo, DZFBoolean lwflag, boolean accway, boolean isT) throws DZFWarpException;
	
//	public Map<String, DcModelHVO> queryDcModelVO(String pk_corp) throws DZFWarpException;
	/**
	 * 构造进项vo
	 * @param vos
	 * @param pk_corp
	 * @return
	 * @throws DZFWarpException
	 */
	public List<VATInComInvoiceVO> construcComInvoice(VATInComInvoiceVO[] vos, String pk_corp) throws DZFWarpException;
	
	/**
	 * 设置业务类型
	 * @param vos
	 * @param busiid
	 * @param businame
	 * @param selvalue
	 * @param userid
	 * @param pk_corp
	 * @return
	 * @throws DZFWarpException
	 */
	public String saveBusiType(VATInComInvoiceVO[] vos, String busiid, String businame, String selvalue, String userid, String pk_corp) throws DZFWarpException;
	
	/**
	 * 设置入账期间
	 * @param vos
	 * @param pk_corp
	 * @param period
	 * @return
	 * @throws DZFWarpException
	 */
	public String saveBusiPeriod(VATInComInvoiceVO[] vos, String pk_corp, String[] arguments) throws DZFWarpException;
	
    /**
     * 构造凭证vo
     * @param vos
     * @param pk_corp
     * @return
     * @throws DZFWarpException
     */
	public TzpzHVO getTzpzHVOByID(VATInComInvoiceVO[] vos, String pk_corp, String userid, VatInvoiceSetVO setvo, boolean accway) throws DZFWarpException;
	
	/**
	 * 合并生单前校验
	 * @param vos
	 * @throws DZFWarpException
	 */
	public void checkBeforeCombine(VATInComInvoiceVO[] vos) throws DZFWarpException;
	
	/**
	 * 合并生单
	 * @param vos
	 * @param pk_corp
	 * @param userid
	 * @param modelvo
	 * @param isT
	 * @throws DZFWarpException
	 */
	public void saveCombinePZ(List<VATInComInvoiceVO> list, String pk_corp, String userid, Map<String, DcModelHVO> dcmap, VatInvoiceSetVO setvo, DZFBoolean lwflag, boolean accway, boolean isT) throws DZFWarpException;
	
	/**
	 * 构造需要的业务类型
	 * @param pk_corp
	 * @return
	 * @throws DZFWarpException
	 */
	public List<String> getBusiTypes(String pk_corp) throws DZFWarpException;
	
	public void scanMatchBusiName(VATInComInvoiceVO incomvo, Map<String, DcModelHVO> dcmap) throws DZFWarpException;
	
	public Map<String, VATInComInvoiceVO> savePt(String pk_corp, String userid, String ccrecode, String jspbh, VATInComInvoiceVO paramvo) throws DZFWarpException;
	
	public VATInComInvoiceVO queryByCGTId(String fphm, String fpdm, String pk_corp) throws DZFWarpException;

	/**
	 * 根据pks查询vo记录
	 * @param pks
	 * @param pk_corp
	 * @return
	 * @throws DZFWarpException
	 */
	public List<VATInComInvoiceVO> queryByPks(String[] pks, String pk_corp) throws DZFWarpException;

	public List<TaxitemVO> queryTaxItems(String pk_corp) throws DZFWarpException;
	
//	public void dealBodyTaxItem(List<TaxitemVO> taxvos, VATInComInvoiceBVO bvo) throws DZFWarpException;
	
	/**
	 * 生成入库单
	 * @param vo
	 * @param accounts
	 * @param corpvo
	 * @param userid
	 * @return
	 * @throws DZFWarpException
	 */
	public IntradeHVO createIC(VATInComInvoiceVO vo, YntCpaccountVO[] accounts, CorpVO corpvo, String userid) throws DZFWarpException;

	public void saveGL(IntradeHVO hvo, String pk_corp, String userid) throws DZFWarpException;
	
	public void saveTotalGL(IntradeHVO[] vos, String pk_corp, String userid) throws DZFWarpException;
	
	public void deletePZH(String pk_corp, String pk_tzpz_h) throws DZFWarpException;
	
	public void updatePZH(TzpzHVO headvo) throws DZFWarpException;
	
	/**
	 * 更新单据入库状态
	 * @param pk_vatsaleinvoice
	 * @param pk_corp
	 * @param pk_ictrade_h
	 * @throws DZFWarpException
	 */
	public void updateICStatus(String pk_vatsaleinvoice, String pk_corp, String pk_ictrade_h) throws DZFWarpException;
	
	public List<InventoryAliasVO> matchInventoryData(String pk_corp, VATInComInvoiceVO[] vos, InventorySetVO invsetvo)throws DZFWarpException;

	public InventoryAliasVO[] saveInventoryData(String pk_corp, InventoryAliasVO[] vos, List<Grid> logList)throws DZFWarpException;

	/**
	 * 生成凭证
	 * @throws DZFWarpException
	 */
	public void createPZ(VATInComInvoiceVO vo, String pk_corp, String userid, boolean accway, boolean isT, InventorySetVO invsetvo, VatInvoiceSetVO setvo, String jsfs) throws DZFWarpException;

	/**
	 * 合并生单
	 * @param vos
	 * @param pk_corp
	 * @param userid
	 * @param modelvo
	 * @param isT
	 * @throws DZFWarpException
	 */
	public void saveCombinePZ(List<VATInComInvoiceVO> list, String pk_corp, String userid, VatInvoiceSetVO setvo, boolean accway, boolean isT, InventorySetVO invsetvo, String jsfs) throws DZFWarpException;
	
	public CorpVO chooseTicketWay(String pk_corp) throws DZFWarpException;

	public List<VatBusinessTypeVO> getBusiType(String pk_corp) throws DZFWarpException;
}
