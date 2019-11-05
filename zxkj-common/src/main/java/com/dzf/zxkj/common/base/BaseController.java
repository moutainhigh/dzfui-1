package com.dzf.zxkj.common.base;

import com.dzf.zxkj.common.constant.ISysConstants;
import com.dzf.zxkj.common.entity.Grid;
import com.dzf.zxkj.common.entity.Json;
import com.dzf.zxkj.common.enums.LogRecordEnum;
import com.dzf.zxkj.common.exception.BusinessException;
import com.dzf.zxkj.common.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;

@Slf4j
public class BaseController {

    @Autowired(required = false)
    private HttpServletRequest request;

    @Autowired(required = false)
    private IOperatorLogService operatorLogService;


    public void printErrorLog(Grid grid, Throwable e, String errorinfo){
        if(StringUtil.isEmpty(errorinfo))
            errorinfo = "操作失败";
        if(e instanceof BusinessException){
            grid.setMsg(e.getMessage());
        }else{
            grid.setMsg(errorinfo);
            log.error(errorinfo,e);
        }
        grid.setSuccess(false);
    }


    public void printErrorLog(Json json, Throwable e, String errorinfo){
        if(StringUtil.isEmpty(errorinfo))
            errorinfo = "操作失败";
        if(e instanceof BusinessException){
            json.setMsg(e.getMessage());
        }else{
            json.setMsg(errorinfo);
            log.error(errorinfo,e);
        }
        json.setSuccess(false);
    }

    public void writeLogRecord(LogRecordEnum recordEnum, String msg) {
        writeLogRecord(recordEnum, msg, ISysConstants.SYS_2);
    }

    public void writeLogRecord(LogRecordEnum recordEnum, String msg, Integer ident) {
        try {
//            String login_corp = request.getHeader("pk_corp");
//            String login_userid = request.getHeader("userId");
//            operatorLogService.saveLog(login_corp, null, IpUtil.getIpAddr(request), recordEnum.getValue(), msg, ident, login_userid);
        } catch (Exception e) {
            log.error("错误", e);
        }
    }


}
