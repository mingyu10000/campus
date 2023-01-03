package com.oddfar.campus.admin.controller.system;

import com.oddfar.campus.common.annotation.ApiResource;
import com.oddfar.campus.common.domain.PageResult;
import com.oddfar.campus.common.domain.R;
import com.oddfar.campus.common.domain.entity.SysRoleEntity;
import com.oddfar.campus.common.domain.entity.SysUserEntity;
import com.oddfar.campus.common.enums.ResBizTypeEnum;
import com.oddfar.campus.common.utils.SecurityUtils;
import com.oddfar.campus.common.utils.StringUtils;
import com.oddfar.campus.framework.service.SysRoleService;
import com.oddfar.campus.framework.service.SysUserService;
import com.oddfar.campus.framework.web.service.SysPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理
 *
 * @author 致远
 */
@RestController
@RequestMapping("/system/user")
@ApiResource(name = "用户管理", resBizType = ResBizTypeEnum.SYSTEM)
public class SysUserController {
    @Autowired
    private SysUserService userService;
    @Autowired
    private SysRoleService roleService;
    @Autowired
    private SysPermissionService permissionService;

    @GetMapping("list")
    @Operation(summary = "分页")
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    public R page(SysUserEntity sysUserEntity) {
        PageResult<SysUserEntity> page = userService.page(sysUserEntity);

        return R.ok().put(page);
    }

    @GetMapping({"{userId}", "/"})
    @Operation(summary = "信息")
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    public R getInfo(@PathVariable(value = "userId", required = false) Long userId) {
        R res = R.ok();
        List<SysRoleEntity> roles = roleService.selectRoleAll();
        res.put("roles", SysUserEntity.isAdmin(userId) ? roles : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        if (StringUtils.isNotNull(userId)) {
            SysUserEntity sysUser = userService.selectUserById(userId);
            res.put("data", sysUser);
            res.put("roleIds", sysUser.getRoles().stream().map(SysRoleEntity::getRoleId).collect(Collectors.toList()));
        }

        return res;
    }

    @PostMapping
    @Operation(summary = "新增用户")
    @PreAuthorize("@ss.hasPermi('system:user:add')")
    public R save(@RequestBody SysUserEntity sysUserEntity) {
        userService.insertUser(sysUserEntity);

        return R.ok();
    }

    @PutMapping
    @Operation(summary = "修改")
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    public R update(@RequestBody SysUserEntity sysUserEntity) {
        userService.updateUser(sysUserEntity);

        return R.ok();
    }

    @DeleteMapping("/{userIds}")
    @Operation(summary = "删除")
    @PreAuthorize("@ss.hasPermi('system:user:remove')")
    public R remove(@PathVariable Long[] userIds) {
        if (ArrayUtils.contains(userIds, SecurityUtils.getUserId())) {
            return R.error("当前用户不能删除");
        }
        return R.ok(userService.deleteUserByIds(userIds));
    }

    /**
     * 根据用户编号获取授权角色
     */
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping("/authRole/{userId}")
    public R authRole(@PathVariable("userId") Long userId) {
        R res = R.ok();
        SysUserEntity user = userService.selectUserById(userId);
        List<SysRoleEntity> roles = roleService.selectRolesByUserId(userId);
        res.put("user", user);
        res.put("roles", SysUserEntity.isAdmin(userId) ? roles : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        return res;
    }

    /**
     * 用户授权角色
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @PutMapping("/authRole")
    public R insertAuthRole(Long userId, Long[] roleIds) {
        if (!SysUserEntity.isAdmin(userId)) {
            userService.insertUserAuth(userId, roleIds);
            return R.ok();
        } else {
            return R.error("不可操作超级管理员");
        }


    }

    /**
     * 重置密码
     */
    @PreAuthorize("@ss.hasPermi('system:user:resetPwd')")
    @PutMapping("/resetPwd")
    public R resetPwd(@RequestBody SysUserEntity user) {

        userService.checkUserAllowed(user);
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        return R.ok(userService.resetPwd(user));
    }

    /**
     * 状态修改
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @PutMapping("/changeStatus")
    public R changeStatus(@RequestBody SysUserEntity user) {

        userService.checkUserAllowed(user);
        userService.updateUserStatus(user);
        permissionService.resetUserRoleAuthCache(user.getUserId());
        return R.ok();
    }


}