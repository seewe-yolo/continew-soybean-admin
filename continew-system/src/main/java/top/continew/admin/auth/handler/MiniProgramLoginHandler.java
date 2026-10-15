/*
 * Copyright (c) 2022-present Charles7c Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package top.continew.admin.auth.handler;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import top.continew.admin.auth.AbstractLoginHandler;
import top.continew.admin.auth.client.MiniProgramApiClient;
import top.continew.admin.auth.enums.AuthTypeEnum;
import top.continew.admin.auth.model.req.MiniProgramLoginReq;
import top.continew.admin.auth.model.resp.LoginResp;
import top.continew.admin.common.enums.DisEnableStatusEnum;
import top.continew.admin.common.enums.RoleCodeEnum;
import top.continew.admin.system.enums.OptionCategoryEnum;
import top.continew.admin.system.model.entity.DeptDO;
import top.continew.admin.system.model.entity.user.UserDO;
import top.continew.admin.system.model.resp.ClientResp;
import top.continew.admin.system.service.DeptService;
import top.continew.admin.system.service.UserRoleService;
import top.continew.starter.core.util.validation.ValidationUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 微信小程序手机号授权登录处理器
 *
 * @author seewe-yolo
 * @since 4.2.0
 */
@Component
@RequiredArgsConstructor
public class MiniProgramLoginHandler extends AbstractLoginHandler<MiniProgramLoginReq> {

    private static final String APP_ID_OPTION = "MINI_PROGRAM_APP_ID";
    private static final String APP_SECRET_OPTION = "MINI_PROGRAM_APP_SECRET";
    private static final String EXTERNAL_DEPT_NAME = "外部部门";

    private final MiniProgramApiClient miniProgramApiClient;
    private final DeptService deptService;
    private final UserRoleService userRoleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResp login(MiniProgramLoginReq req, ClientResp client, HttpServletRequest request,
        HttpServletResponse response) {
        Map<String, String> loginOptions = optionService.getByCategory(OptionCategoryEnum.LOGIN);
        String appId = loginOptions.get(APP_ID_OPTION);
        String appSecret = loginOptions.get(APP_SECRET_OPTION);
        ValidationUtils.throwIf(StrUtil.hasBlank(appId, appSecret), "微信小程序登录尚未配置");
        String phone = miniProgramApiClient.getPhoneNumber(req.getCode(), appId, appSecret);
        UserDO user = userService.getByPhone(phone);
        if (user == null) {
            user = this.register(phone);
        }
        return super.authenticate(user, client, request, response);
    }

    @Override
    public AuthTypeEnum getAuthType() {
        return AuthTypeEnum.MINI_PROGRAM;
    }

    private UserDO register(String phone) {
        DeptDO rootDept = deptService.getOne(new LambdaQueryWrapper<DeptDO>()
            .eq(DeptDO::getIsSystem, true)
            .eq(DeptDO::getParentId, 0));
        ValidationUtils.throwIfNull(rootDept, "未查询到系统内置根部门");
        DeptDO externalDept = deptService.getOne(new LambdaQueryWrapper<DeptDO>()
            .eq(DeptDO::getParentId, rootDept.getId())
            .eq(DeptDO::getName, EXTERNAL_DEPT_NAME));
        ValidationUtils.throwIf(externalDept == null
            || externalDept.getStatus() != DisEnableStatusEnum.ENABLE, "未查询到启用的外部部门");
        Long roleId = roleService.getIdByCode(RoleCodeEnum.MINI_PROGRAM_USER.getCode());
        ValidationUtils.throwIfNull(roleId, "未查询到小程序用户角色");

        UserDO user = new UserDO();
        user.setUsername("mp" + IdUtil.fastSimpleUUID());
        user.setNickname("小程序用户" + RandomUtil.randomString(RandomUtil.BASE_CHAR, 5));
        user.setPhone(phone);
        user.setDeptId(externalDept.getId());
        user.setStatus(DisEnableStatusEnum.ENABLE);
        userService.save(user);
        userRoleService.assignRolesToUser(List.of(roleId), user.getId());
        return user;
    }
}
