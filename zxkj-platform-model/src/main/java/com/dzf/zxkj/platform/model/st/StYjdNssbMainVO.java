package com.dzf.zxkj.platform.model.st;

import com.dzf.zxkj.common.lang.DZFDouble;

/**
 * 纳税申报表主表
 * @author WJX
 * */
public class StYjdNssbMainVO extends StBaseVO{

	private String pk_nssbmain;//主键
//	private String vno;//序号
//	private String vprojectname;//项目名称
	private DZFDouble vbqmny;//本期金额
	private DZFDouble vljmny;//累计金额
	private String citemclass;//类别
	private String rp_vbqmny;//本期金额（报表用金额）
	private String rp_vljmny;//累计金额（报表用金额）
//	private String pk_corp;//公司主键
//	private String pk_project;//纳税项目主键
	private String vdef1;
	private String vdef2;
	private String vdef3;
	private String vdef4;
	private String vdef5;
//	private String cyear;//会计年度
	
	
	

//	public String getPk_corp() {
//		return pk_corp;
//	}
//
//	public void setPk_corp(String pk_corp) {
//		this.pk_corp = pk_corp;
//	}
//
//	public String getPk_project() {
//		return pk_project;
//	}
//
//	public void setPk_project(String pk_project) {
//		this.pk_project = pk_project;
//	}

	
	@Override
	public String getParentPKFieldName() {
		// TODO Auto-generated method stub
		return null;
	}





	public String getPk_nssbmain() {
		return pk_nssbmain;
	}





	public void setPk_nssbmain(String pk_nssbmain) {
		this.pk_nssbmain = pk_nssbmain;
	}





	public DZFDouble getVbqmny() {
		return vbqmny;
	}

	public void setVbqmny(DZFDouble vbqmny) {
		this.vbqmny = vbqmny;
	}

	public DZFDouble getVljmny() {
		return vljmny;
	}

	public void setVljmny(DZFDouble vljmny) {
		this.vljmny = vljmny;
	}

	public String getCitemclass() {
		return citemclass;
	}

	public void setCitemclass(String citemclass) {
		this.citemclass = citemclass;
	}




	public String getRp_vbqmny() {
		return rp_vbqmny;
	}





	public void setRp_vbqmny(String rp_vbqmny) {
		this.rp_vbqmny = rp_vbqmny;
	}





	public String getRp_vljmny() {
		return rp_vljmny;
	}





	public void setRp_vljmny(String rp_vljmny) {
		this.rp_vljmny = rp_vljmny;
	}





	public String getVdef1() {
		return vdef1;
	}

	public void setVdef1(String vdef1) {
		this.vdef1 = vdef1;
	}

	public String getVdef2() {
		return vdef2;
	}

	public void setVdef2(String vdef2) {
		this.vdef2 = vdef2;
	}

	public String getVdef3() {
		return vdef3;
	}

	public void setVdef3(String vdef3) {
		this.vdef3 = vdef3;
	}

	public String getVdef4() {
		return vdef4;
	}

	public void setVdef4(String vdef4) {
		this.vdef4 = vdef4;
	}

	public String getVdef5() {
		return vdef5;
	}

	public void setVdef5(String vdef5) {
		this.vdef5 = vdef5;
	}

	@Override
	public String getPKFieldName() {
		// TODO Auto-generated method stub
		return "pk_nssbmain";
	}

	@Override
	public String getTableName() {
		// TODO Auto-generated method stub
		return "ynt_st_yjdnssbmain";
	}

}