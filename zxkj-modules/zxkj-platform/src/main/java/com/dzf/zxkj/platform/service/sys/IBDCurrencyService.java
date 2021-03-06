package com.dzf.zxkj.platform.service.sys;

import com.dzf.zxkj.base.exception.DZFWarpException;
import com.dzf.zxkj.platform.model.bdset.BdCurrencyVO;

import java.util.List;

/**
 * 币种
 *
 */
public interface IBDCurrencyService {
	
	//查询币种信息
	public BdCurrencyVO[] queryCurrency() throws DZFWarpException;
	//保存币种信息
	public void save(BdCurrencyVO vo) throws DZFWarpException;
	//删除币种信息
	public void delete(BdCurrencyVO vo) throws DZFWarpException;
	
	public BdCurrencyVO queryCurrencyVOByPk(String pk_currency) throws DZFWarpException;


	// 根据公司id获取币种信息
	public BdCurrencyVO[] queryCurrencyByCorp(String pk_corp) throws DZFWarpException;
	
	
	
}
