package com.dzf.zxkj.platform.service.bdset;

import com.dzf.zxkj.base.exception.DZFWarpException;
import com.dzf.zxkj.common.lang.DZFDouble;
import com.dzf.zxkj.platform.model.bdset.IncomeWarningVO;

public interface IIncomeWarningService {


    void save(String isLoginRemind, String isInputRemind, IncomeWarningVO vo, String pk_corp)
            throws DZFWarpException;

    IncomeWarningVO[] query(String pk_corp) throws DZFWarpException;

    /**
     * 根据名称+项目名称获取预警条目
     * @param pk_corp
     * @param name
     * @return
     * @throws DZFWarpException
     */
    public IncomeWarningVO queryByXm(String pk_corp,String name) throws DZFWarpException;

    IncomeWarningVO[] queryByPrimaryKey(String primaryKey) throws DZFWarpException;

    void delete(IncomeWarningVO vo) throws DZFWarpException;

    IncomeWarningVO[] queryFseInfo(IncomeWarningVO[] ivos, String pk_corp, String enddate);

    IncomeWarningVO[] queryIncomeWaringVos(String pk_corp, String period, String filflg) throws DZFWarpException;

    DZFDouble getSpecFsValue(String beginPeriod, String endPeriod,
                             String pk_corp, IncomeWarningVO vo) throws DZFWarpException;
}
