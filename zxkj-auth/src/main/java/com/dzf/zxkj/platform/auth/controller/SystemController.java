package com.dzf.zxkj.platform.auth.controller;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.CreateCache;
import com.dzf.auth.api.model.platform.PlatformVO;
import com.dzf.zxkj.common.constant.IcCostStyle;
import com.dzf.zxkj.common.entity.Grid;
import com.dzf.zxkj.common.entity.ReturnData;
import com.dzf.zxkj.platform.auth.config.ZxkjPlatformAuthConfig;
import com.dzf.zxkj.platform.auth.entity.FunNode;
import com.dzf.zxkj.platform.auth.entity.LoginUser;
import com.dzf.zxkj.platform.auth.mapper.FunNodeMapper;
import com.dzf.zxkj.platform.auth.model.sys.CorpModel;
import com.dzf.zxkj.platform.auth.service.ILoginService;
import com.dzf.zxkj.platform.auth.service.ISysService;
import com.dzf.zxkj.platform.auth.util.PermissionFilter;
import com.dzf.zxkj.platform.auth.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class SystemController {

    @CreateCache(name = "zxkj-platform-user", cacheType = CacheType.LOCAL, expire = 5, timeUnit = TimeUnit.DAYS)
    private Cache<String, LoginUser> platformUserCache;
    @Autowired
    private ILoginService loginService;
    @Autowired
    private ZxkjPlatformAuthConfig zxkjPlatformAuthConfig;
    @Autowired
    private FunNodeMapper funNodeMapper;
    @Autowired
    private ISysService sysService;

    /**
     * 跳去别处
     *
     * @param request
     * @param response
     * @param platformTag
     * @param userid
     */
    @GetMapping(value = "/to/{platform}/{userid}")
    public void jumpToOther(HttpServletRequest request,
                            HttpServletResponse response,
                            @PathVariable(value = "platform") String platformTag, @PathVariable(value = "userid") String userid) {
        LoginUser vo = platformUserCache.get(userid);

        Optional<PlatformVO> platformOptional = vo.getPlatformVOSet()
                .stream()
                .filter(e -> e.getPlatformTag().equals(platformTag))
                .findFirst();
        PlatformVO platform = platformOptional.get();
        StringBuilder sb = new StringBuilder(platform.getPlatformDomain())
                .append(platform.getPlatformIndexPage())
                .append("?token=")
                .append(vo.getDzfAuthToken());
        try {
            response.sendRedirect(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @ResponseBody
    @GetMapping("queryFunNode")
    public ReturnData<Grid> queryFunNode() {
        List<FunNode> funNodeList = funNodeMapper.getFunNodeByUseridAndPkCorp(SystemUtil.getLoginUserId(), SystemUtil.getLoginCorpId());

        CorpModel corpModel = sysService.queryCorpByPk(SystemUtil.getLoginCorpId());
        //资产是否开启
        if (!corpModel.getHoldflag().booleanValue()) {
            funNodeList = funNodeList.stream().filter(v -> !StringUtils.equalsAny(PermissionFilter.ZC_FUN_NODE_PK, v.getPk_funnode(), v.getPk_parent())).collect(Collectors.toList());
        }
        //根据会计公司制度显示税表
        funNodeList = funNodeList.stream().filter(v -> !PermissionFilter.nodeNames.contains(v.getPk_funnode()) || (PermissionFilter.nodeNames.contains(v.getPk_funnode()) && PermissionFilter.nodesFillterByCorpType.containsKey(corpModel.getCorptype()) && PermissionFilter.nodesFillterByCorpType.get(corpModel.getCorptype()).contains(v.getPk_funnode()))).collect(Collectors.toList());
        //库存
        if (IcCostStyle.IC_ON.equals(corpModel.getBbuildic())) {//启用进销存
            if (corpModel.getIbuildicstyle() != null && corpModel.getIbuildicstyle() == 1) {
                funNodeList = funNodeList.stream().filter(v -> !StringUtils.equalsAny(PermissionFilter.KCGL_FUN_NODE_PK, v.getPk_funnode(), v.getPk_parent()) ).collect(Collectors.toList());
            } else {
                funNodeList = funNodeList.stream().filter(v -> !StringUtils.equalsAny(PermissionFilter.KCGL1_FUN_NODE_PK, v.getPk_funnode(), v.getPk_parent()) ).collect(Collectors.toList());
            }
            funNodeList = funNodeList.stream().filter(v -> !StringUtils.equalsAny(PermissionFilter.KCGL2_FUN_NODE_PK, v.getPk_funnode(), v.getPk_parent()) ).collect(Collectors.toList());
        } else if(IcCostStyle.IC_INVTENTORY.equals(corpModel.getBbuildic())){
            funNodeList = funNodeList.stream().filter(v -> !StringUtils.equalsAny(PermissionFilter.KCGL_FUN_NODE_PK, v.getPk_funnode(), v.getPk_parent()) ).collect(Collectors.toList());
            funNodeList = funNodeList.stream().filter(v -> !StringUtils.equalsAny(PermissionFilter.KCGL1_FUN_NODE_PK, v.getPk_funnode(), v.getPk_parent()) ).collect(Collectors.toList());
        }else {
            funNodeList = funNodeList.stream().filter(v -> !StringUtils.equalsAny(PermissionFilter.KCGL_FUN_NODE_PK, v.getPk_funnode(), v.getPk_parent()) ).collect(Collectors.toList());
            funNodeList = funNodeList.stream().filter(v -> !StringUtils.equalsAny(PermissionFilter.KCGL1_FUN_NODE_PK, v.getPk_funnode(), v.getPk_parent()) ).collect(Collectors.toList());
            funNodeList = funNodeList.stream().filter(v -> !StringUtils.equalsAny(PermissionFilter.KCGL2_FUN_NODE_PK, v.getPk_funnode(), v.getPk_parent()) ).collect(Collectors.toList());
        }

        List<String> routerNames = funNodeList.stream().map(v -> v.getRouter()).collect(Collectors.toList());
        Grid grid = new Grid();
        grid.setRows(routerNames);
        grid.setSuccess(true);
        return ReturnData.ok().data(grid);
    }
}
