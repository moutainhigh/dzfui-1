package com.dzf.zxkj.app.controller;

import com.dzf.admin.dzfapp.model.econtract.AppSealVO;
import com.dzf.admin.dzfapp.model.result.AppResult;
import com.dzf.admin.dzfapp.service.econtract.IDzfAppSealService;
import com.dzf.zxkj.app.service.pub.IUserPubService;
import com.dzf.zxkj.app.utils.AppkeyUtil;
import com.dzf.zxkj.platform.model.sys.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/dzfapp/seal")
public class ElectronicSealController {

    @Reference(version = "1.0.0", protocol = "dubbo", timeout = Integer.MAX_VALUE, retries = 0)
    private IDzfAppSealService dzfAppSealService;
    @Autowired
    private IUserPubService userPubService;
    @RequestMapping("/haveSealStatus")
    public AppResult<Boolean> haveSealStatus(@RequestParam Map<String,Object> param) {
        try {
            return dzfAppSealService.haveSealStatus(changeParamvo(param));
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            return  new AppResult(-100,null,e.getMessage());
        }
    }

    @RequestMapping("/updateSealStatus")
    public AppResult updateSealStatus(@RequestParam Map<String,Object> param) {
        try {
            return dzfAppSealService.confirmSealStatus(changeParamvo(param));
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            return  new AppResult(-100,null,e.getMessage());
        }
        
    }

    @RequestMapping("/getCorpkSeals")
    public AppResult getCorpkSeals(@RequestParam Map<String,Object> param) {
        try {
            return dzfAppSealService.getCorpkSeals(changeParamvo(param));
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            return  new AppResult(-100,null,e.getMessage());
        }
        
    }

    @RequestMapping("/savePersonSign")
    public AppResult savePersonSign(@RequestParam Map<String,Object> param) {
        try {
            return dzfAppSealService.confirmPersonSign(changeParamvo(param), null);
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            return  new AppResult(-100,null,e.getMessage());
        }
        
    }

    @RequestMapping("/getSealImg")
    public AppResult<byte[]> getSealImg(@RequestParam Map<String,Object> param) {
        try {
            return dzfAppSealService.getSealImg(changeParamvo(param));
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            return  new AppResult(-100,null,e.getMessage());
        }
        
    }
    private AppSealVO changeParamvo(Map<String,Object> param){
        AppSealVO pamVO= new AppSealVO();
        AppkeyUtil.setAppValue(param,pamVO );
        UserVO uservo = userPubService.queryUserVOId((String)param.get("account_id"));
        pamVO.setCuserid(uservo.getCuserid());
        return pamVO;
    }
}