package com.dzf.zxkj.platform.service.glic.impl;

import com.dzf.zxkj.base.dao.SingleObjectBO;
import com.dzf.zxkj.base.exception.BusinessException;
import com.dzf.zxkj.base.exception.DZFWarpException;
import com.dzf.zxkj.base.framework.SQLParameter;
import com.dzf.zxkj.base.framework.processor.BeanListProcessor;
import com.dzf.zxkj.base.framework.processor.ColumnListProcessor;
import com.dzf.zxkj.common.constant.AuxiliaryConstant;
import com.dzf.zxkj.common.utils.SqlUtil;
import com.dzf.zxkj.common.utils.StringUtil;
import com.dzf.zxkj.platform.model.bdset.AuxiliaryAccountBVO;
import com.dzf.zxkj.platform.model.glic.InventoryAliasVO;
import com.dzf.zxkj.platform.service.bdset.IAuxiliaryAccountService;
import com.dzf.zxkj.platform.service.glic.IInvAccAliasService;
import com.dzf.zxkj.platform.util.LetterNumberSortUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

@Service("ic_invtoryaliasserv")
public class InvAccAliasServiceImpl implements IInvAccAliasService {

	@Autowired
	private SingleObjectBO singleObjectBO;
	@Autowired
	private IAuxiliaryAccountService gl_fzhsserv;
	@Override
	public InventoryAliasVO[] query(String pk_corp, String pk_inventory) throws DZFWarpException {
		SQLParameter sp = new SQLParameter();
		sp.addParam(pk_corp);
		sp.addParam(pk_inventory);

		StringBuffer sf = new StringBuffer();
		sf.append(" select ali.*,re.name unitname from ynt_icalias ali");
		sf.append(" left join ynt_measure re on ali.unit = re.pk_measure ");
		sf.append(" where nvl(ali.dr,0) = 0 and ali.pk_corp = ? and ali.pk_inventory=? ");
		List<InventoryAliasVO> list = (List<InventoryAliasVO>) singleObjectBO.executeQuery(sf.toString(), sp,
				new BeanListProcessor(InventoryAliasVO.class));
		if (list == null || list.isEmpty()) {
			return new InventoryAliasVO[0];
		} else {
			list.sort(Comparator.comparing(InventoryAliasVO::getAliasname,Comparator.nullsFirst(LetterNumberSortUtil.letterNumberOrder())));
			return list.toArray(new InventoryAliasVO[list.size()]);
		}
	}

	@Override
	public InventoryAliasVO save(InventoryAliasVO vo1) throws DZFWarpException {
		check(vo1);
		singleObjectBO.saveObject(vo1.getPk_corp(), vo1);
		return vo1;
	}

	private void check(InventoryAliasVO vo1) throws DZFWarpException {
		if (vo1 == null)
			throw new BusinessException("保存别名数据为空");
		if (StringUtil.isEmpty(vo1.getPk_inventory()))
			throw new BusinessException("保存别名指向的存货为空");
		if (StringUtil.isEmpty(vo1.getPk_corp())) {
			throw new BusinessException("保存别名指向的公司为空");
		}
		// 校验 别名+ 规格 + 计量单位 + pk_corp 是否数据库唯一。
		vo1.setAliasname(StringUtil.replaceBlank(vo1.getAliasname()));
		vo1.setSpec(StringUtil.replaceBlank(vo1.getSpec()));
		vo1.setUnit(StringUtil.replaceBlank(vo1.getUnit()));
		SQLParameter sp = new SQLParameter();
		StringBuffer sf = new StringBuffer();
		sf.setLength(0);
		sp.clearParams();
		sf.append(" select b.name from ynt_icalias s ");
		sf.append(" join ynt_inventory b on s.pk_inventory = b.pk_inventory ");
		sf.append(" where nvl(s.dr,0) = 0 and nvl(b.dr,0) = 0  and  s.aliasname = ? ");
		sp.addParam(vo1.getAliasname());
		if (!StringUtil.isEmpty(vo1.getSpec())) {
			sf.append("  and s.spec  = ? ");
			sp.addParam(vo1.getSpec());
		} else {
			sf.append("  and s.spec is null ");
		}
		if (!StringUtil.isEmpty(vo1.getUnit())) {
			sf.append("  and s.unit  = ? ");
			sp.addParam(vo1.getUnit());
		} else {
			sf.append("  and s.unit is null ");
		}

		if (!StringUtil.isEmpty(vo1.getInvtype())) {
			sf.append("  and s.invtype  = ? ");
			sp.addParam(vo1.getInvtype());
		} else {
			sf.append("  and s.invtype is null ");
		}

		sf.append(" and s.pk_corp = ? ");

		sp.addParam(vo1.getPk_corp());
		if (!StringUtil.isEmpty(vo1.getPk_alias())) {
			sf.append(" and s.pk_alias <> ? ");
			sp.addParam(vo1.getPk_alias());
		}
		List<Object> list = (List<Object>) singleObjectBO.executeQuery(sf.toString(), sp, new ColumnListProcessor());
		if (list != null && list.size() > 0) {
			throw new BusinessException("[" + list.get(0) + "]里存在该别名，保存失败");
		}
		AuxiliaryAccountBVO[] bvos = gl_fzhsserv.queryB(AuxiliaryConstant.ITEM_INVENTORY, vo1.getPk_corp(),null);
		if(bvos != null && bvos.length>0){
			HashSet<String> nameInfoSet = new HashSet<String>();
			for (AuxiliaryAccountBVO vo : bvos) {
				nameInfoSet.add(getNameInfoKey(vo));
			}
			String nameInfoKey = getNameInfoKey(vo1);
			if (nameInfoSet.contains(nameInfoKey)) {
				StringBuffer namemsg = new StringBuffer();
				if (!StringUtil.isEmpty(vo1.getAliasname())) {
					namemsg.append("存货名称[" + vo1.getAliasname() + "]、");
				}
				if (!StringUtil.isEmpty(vo1.getSpec())) {
					namemsg.append("规格(型号)[" + vo1.getSpec() + "]、");
				} else {
					namemsg.append("规格(型号)、");
				}
				if (!StringUtil.isEmpty(vo1.getUnit())) {
					namemsg.append("计量单位[" + vo1.getUnit() + "]");
				} else {
					namemsg.append("计量单位");
				}
				throw new BusinessException(namemsg.toString() + "与该别名重复，保存失败");
			}
		}
	}

	private String getNameInfoKey(AuxiliaryAccountBVO invo) {
		StringBuffer strb = new StringBuffer();
		strb.append(appendIsNull(StringUtil.replaceBlank(invo.getName())));
		strb.append(appendIsNull(StringUtil.replaceBlank(invo.getSpec())));
		strb.append(appendIsNull(StringUtil.replaceBlank(invo.getInvtype())));
		strb.append(appendIsNull(StringUtil.replaceBlank(invo.getUnit())));
		return strb.toString();
	}

	@Override
	public void delete(String pk_icalias, String pk_corp) throws DZFWarpException {
		deleteByPks(new String[] { pk_icalias }, pk_corp);
	}

	@Override
	public InventoryAliasVO[] insertAliasVOS(InventoryAliasVO[] vos, String pk_corp) throws DZFWarpException {

		if (vos == null || vos.length == 0)
			return null;

		String error = checkBeforeSave(vos, pk_corp);
		StringBuffer msg = new StringBuffer();
		if (StringUtil.isEmpty(error)) {
		} else {
			msg.append("<font color = 'red'>" + error + "</font>");
		}
		if (!StringUtil.isEmpty(msg.toString()))
			throw new BusinessException(msg.toString());

		singleObjectBO.insertVOArr(pk_corp, vos);
		return vos;
	}

	private String checkBeforeSave(InventoryAliasVO[] vos, String pk_corp) {

		if (vos == null || vos.length == 0)
			return null;

		HashSet<String> nameZjbmSet = new HashSet<String>();
		InventoryAliasVO[] qbvos = query(pk_corp);
		StringBuffer message = new StringBuffer();
		List<String> slist = new ArrayList<>();
		for (InventoryAliasVO nbvo : vos) {
			if (!StringUtil.isEmpty(nbvo.getPk_alias())) {
				slist.add(nbvo.getPk_alias());
			}
		}

		if (qbvos != null && qbvos.length > 0) {
			for (InventoryAliasVO bvo : qbvos) {
				if (slist.contains(bvo.getPk_alias())) {
					continue;
				}
				String namezjbm = getNameInfoKey(bvo);
				if (!StringUtil.isEmpty(namezjbm)) {
					nameZjbmSet.add(namezjbm);
				}
			}
		}

		for (InventoryAliasVO nbvo : vos) {
			check(message, nbvo, nameZjbmSet);
		}
		return message.toString();
	}

	// 存货校验
	private void check(StringBuffer message, InventoryAliasVO bvo, HashSet<String> nameSet) {
		String nameInfoKey = getNameInfoKey(bvo);
		if (!StringUtil.isEmpty(nameInfoKey)) {
			if (nameSet.contains(nameInfoKey)) {
				String aliasname = StringUtil.isEmpty(bvo.getAliasname())?"":bvo.getAliasname();
				String spec = StringUtil.isEmpty(bvo.getSpec())?"":bvo.getSpec();
				String unit = StringUtil.isEmpty(bvo.getUnit())?"":bvo.getUnit();
				dealMessage(message, "别名[" + aliasname + "]、规格(型号)[" + spec+ "]、计量单位["
						+ unit + "]至少有一项不同！");
			} else {
				nameSet.add(nameInfoKey);
			}
		} else {
			dealMessage(message, "别名不能为空！");
		}
	}

	private void dealMessage(StringBuffer message, String errinfo) {
		message.append(errinfo + "<br>");
	}

	private String getNameInfoKey(InventoryAliasVO invo) {
		StringBuffer strb = new StringBuffer();
		strb.append(appendIsNull(StringUtil.replaceBlank(invo.getAliasname())));
		strb.append(appendIsNull(StringUtil.replaceBlank(invo.getSpec())));
		strb.append(appendIsNull(StringUtil.replaceBlank(invo.getInvtype())));
		strb.append(appendIsNull(StringUtil.replaceBlank(invo.getUnit())));
		return strb.toString();

	}

	private String appendIsNull(String info) {
		StringBuffer strb = new StringBuffer();
		if (StringUtil.isEmpty(info)) {
			strb.append("null");
		} else {
			strb.append(info);
		}
		return strb.toString();
	}

	@Override
	public InventoryAliasVO[] updateAliasVOS(InventoryAliasVO[] vos, String pk_corp, String[] fields)
			throws DZFWarpException {
		singleObjectBO.updateAry(vos, fields);
		return vos;
	}

	@Override
	public void deleteByPks(String[] pk_icaliass, String pk_corp) throws DZFWarpException {
		String part = SqlUtil.buildSqlForIn("pk_alias", pk_icaliass);
		StringBuffer sf = new StringBuffer();
		sf.append(" delete from ynt_icalias where pk_corp = ? and ");
		sf.append(part);
		SQLParameter sp = new SQLParameter();
		sp.addParam(pk_corp);
		singleObjectBO.executeUpdate(sf.toString(), sp);
	}

	@Override
	public void deleteByInvs(String[] pk_inventorys, String pk_corp) throws DZFWarpException {
		String part = SqlUtil.buildSqlForIn("pk_inventory", pk_inventorys);
		StringBuffer sf = new StringBuffer();
		sf.append(" delete from ynt_icalias where pk_corp = ? and ");
		sf.append(part);
		SQLParameter sp = new SQLParameter();
		sp.addParam(pk_corp);
		singleObjectBO.executeUpdate(sf.toString(), sp);
	}

	private InventoryAliasVO[] query(String pk_corp) throws DZFWarpException {
		SQLParameter sp = new SQLParameter();
		sp.addParam(pk_corp);

		StringBuffer sf = new StringBuffer();
		sf.append(" select b.name from ynt_icalias s ");
		sf.append(" join ynt_inventory b on s.pk_inventory = b.pk_inventory ");
		sf.append(" where nvl(s.dr,0) = 0 and nvl(b.dr,0) = 0 ");
		sf.append(" and s.pk_corp = ? ");

		List<InventoryAliasVO> list = (List<InventoryAliasVO>) singleObjectBO.executeQuery(sf.toString(), sp,
				new BeanListProcessor(InventoryAliasVO.class));
		InventoryAliasVO[] bodyvos = null;
		if (list != null && list.size() > 0) {
			bodyvos = list.toArray(new InventoryAliasVO[list.size()]);
		}
		return bodyvos;
	}

}