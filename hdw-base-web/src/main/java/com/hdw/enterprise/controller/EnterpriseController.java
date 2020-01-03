package com.hdw.enterprise.controller;


import com.google.common.collect.Lists;
import com.hdw.common.base.entity.LoginUser;
import com.hdw.common.result.CommonResult;
import com.hdw.common.result.PageParam;
import com.hdw.common.result.SelectNode;
import com.hdw.enterprise.entity.Enterprise;
import com.hdw.enterprise.entity.vo.EnterpriseVo;
import com.hdw.enterprise.param.EnterpriseParam;
import com.hdw.enterprise.service.IEnterpriseService;
import com.hdw.system.controller.UpLoadController;
import com.hdw.system.entity.SysFile;
import com.hdw.system.service.ISysDicService;
import com.hdw.system.service.ISysFileService;
import com.hdw.system.shiro.ShiroKit;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.*;

/**
 * @Description 企业Controller
 * @Author TuMinglong
 * @Date 2018/12/17 11:14
 */
@Slf4j
@Api(value = "企业接口", tags = {"企业接口"})
@RestController
@RequestMapping("/enterprise")
public class EnterpriseController extends UpLoadController {
    @Reference
    private ISysDicService sysDicService;

    @Reference
    private IEnterpriseService enterpriseService;

    @Reference
    private ISysFileService sysFileService;

    private Map<String, List<Map<String, String>>> uploadFileUrls = new HashMap<String, List<Map<String, String>>>();

    /**
     * 企业列表
     *
     * @return
     */
    @ApiOperation(value = "企业列表", notes = "企业列表")
    @GetMapping("/list")
    @RequiresPermissions("enterprise/enterprise/list")
    public CommonResult<PageParam<EnterpriseVo>> treeGrid(EnterpriseParam enterpriseParam) {
        LoginUser loginUser = ShiroKit.getUser();
        // 不是管理员
        if (loginUser.getUserType() != 0) {
            enterpriseParam.setUserId(ShiroKit.getUser().getId());
        }
        PageParam<EnterpriseVo> page = enterpriseService.pageList(enterpriseParam);
        return CommonResult.ok().data(page);
    }

    /**
     * 企业信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "企业信息", notes = "企业信息")
    @ApiImplicitParam(paramType = "path", name = "id", value = "企业ID", dataType = "String", required = true)
    @GetMapping("/info/{id}")
    @RequiresPermissions("enterprise/enterprise/info")
    public CommonResult<Enterprise> info(@PathVariable("id") String id) {
        Enterprise enterprise = enterpriseService.getById(id);
        return CommonResult.ok().data(enterprise);
    }

    /**
     * 保存企业信息
     *
     * @param
     * @return
     */
    @ApiOperation(value = "保存企业信息", notes = "保存企业信息")
    @PostMapping("/save")
    @RequiresPermissions("enterprise/enterprise/save")
    public CommonResult save(@Valid @RequestBody Enterprise enterprise) {
        try {
            enterprise.setCreateTime(new Date());
            enterprise.setCreateUser(ShiroKit.getUser().getLoginName());
            boolean b = enterpriseService.save(enterprise);
            saveFile(enterprise.getId());
            if (b) {
                return CommonResult.ok().msg("添加成功");
            } else {
                return CommonResult.fail().msg("添加失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return CommonResult.fail().msg("运行异常，请联系管理员");
        }

    }

    /**
     * 修改企业信息
     *
     * @param enterprise
     * @return
     */
    @ApiOperation(value = "修改企业信息", notes = "修改企业信息")
    @PostMapping("/update")
    @RequiresPermissions("enterprise/enterprise/update")
    public CommonResult update(@Valid @RequestBody Enterprise enterprise) {
        try {
            enterprise.setUpdateTime(new Date());
            enterprise.setUpdateUser(ShiroKit.getUser().getLoginName());
            boolean b = enterpriseService.updateById(enterprise);
            saveFile(enterprise.getId());
            if (b) {
                return CommonResult.ok().msg("修改成功");
            } else {
                return CommonResult.fail().msg("修改失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return CommonResult.fail().msg("运行异常，请联系管理员");
        }
    }

    /**
     * 删除企业信息
     *
     * @param ids
     * @return
     */
    @ApiOperation(value = "删除企业信息", notes = "删除企业信息")
    @ApiImplicitParam(paramType = "query", name = "ids", value = "企业ID数组", dataType = "String", required = true, allowMultiple = true)
    @PostMapping("/delete")
    @RequiresPermissions("enterprise/enterprise/delete")
    public CommonResult deleteBatchIds(@RequestBody String[] ids) {
        enterpriseService.removeByIds(Arrays.asList(ids));
        return CommonResult.ok().msg("删除成功");
    }

    /**
     * 企业选择
     *
     * @param areaCode
     * @param industryCode
     * @return
     */
    @ApiOperation(value = "企业选择", notes = "企业选择")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "areaCode", value = "区域ID", required = false, dataType = "Integer", paramType = "query"),
            @ApiImplicitParam(name = "industryCode", value = "行业ID", required = false, dataType = "Integer", paramType = "query")
    })
    @GetMapping("/getEnterpriseTree")
    public CommonResult getEnterpriseTree(@RequestParam(required = false, value = "areaCode") Long areaCode,
                                          @RequestParam(required = false, value = "industryCode") Long industryCode) {
        try {
            List<SelectNode> nodeList = Lists.newArrayList();
            Map<String, Object> params = new HashMap<>();
            LoginUser loginUser = ShiroKit.getUser();
            // 不是管理员
            if (loginUser.getUserType() != 0) {
                params.put("userId", ShiroKit.getUser().getId());
            }
            params.put("industryCode", industryCode);
            List<Map<String, Object>> list = enterpriseService.selectEnterpriseList(params);
            if (!list.isEmpty()) {
                list.forEach(map -> {
                    SelectNode selectNode = new SelectNode();
                    selectNode.setValue(map.get("id").toString());
                    selectNode.setLabel(map.get("enterpriseName").toString());
                    nodeList.add(selectNode);
                });
            }
            return CommonResult.ok().data(nodeList);
        } catch (Exception e) {
            e.printStackTrace();
            return CommonResult.fail().msg("运行异常，请联系管理员");
        }
    }

    /**
     * 上传附件
     */
    @ApiOperation(value = "上传附件", notes = "上传附件")
    @PostMapping("/uploadFile")
    public CommonResult uploadFile(@RequestParam("file") MultipartFile[] files) {
        Map<String,Object> param=new HashMap<>();
        try {
            List<Map<String, String>> uploadFileUrl = uploads(files, "enterprise");
            if (!uploadFileUrl.isEmpty()) {
                for (Map<String, String> map : uploadFileUrl) {
                    String name = map.get("fileName");
                    String url = map.get("filePath");
                    param.put("name", name);
                    param.put("url", url);
                }
                setUploadFile(uploadFileUrl);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return CommonResult.ok().data(param);
    }


    /**
     * 删除文件
     *
     * @param name 文件名
     * @param url  文件路径
     * @return
     */
    @GetMapping("/deleteFile")
    public CommonResult deleteFile(@RequestParam(value = "id") String id,
                                   @RequestParam(value = "name", required = false) String name,
                                   @RequestParam(value = "url", required = false) String url) {
        try {
            sysFileService.deleteFile("t_app_emergency_case", "", "", name, url);
            if (StringUtils.isNotBlank(url)) {
                deleteFileFromFastDFS(url);
            }
            resetUploadFile();
            return CommonResult.ok().msg("删除文件成功");
        } catch (Exception e) {
            e.printStackTrace();
            return CommonResult.fail().msg("运行异常，请联系管理员");
        }
    }


    /**
     * 列示文件
     *
     * @param id
     * @return
     */
    @GetMapping("/lookFile/{id}")
    public CommonResult listFile(@PathVariable("id") String id) {
        List<Map<String, String>> list = new ArrayList<>();
        HashMap<String, Object> param = new HashMap<String, Object>();
        param.put("tableId", "t_enterprise");
        param.put("recordId", id);
        List<SysFile> files = sysFileService.selectFileListByTableIdAndRecordId(param);
        if (!files.isEmpty()) {
            for (SysFile sysFile : files) {
                Map<String, String> fileMap = new HashMap<>();
                fileMap.put("name", sysFile.getAttachmentName());
                fileMap.put("url", sysFile.getAttachmentPath());
                list.add(fileMap);
            }
        }
        return CommonResult.ok().data(list);
    }

    public CommonResult saveFile(String id) {
        try {
            if (getUploadFile() != null) {
                LoginUser user = ShiroKit.getUser();
                for (Map<String, String> uploadFileUrl : getUploadFile()) {
                    String fileName = uploadFileUrl.get("fileName");
                    String filePah = uploadFileUrl.get("filePath");
                    SysFile sysFile = new SysFile();
                    sysFile.setRecordId(id);
                    sysFile.setTableId("t_enterprise");
                    sysFile.setAttachmentGroup("企业");
                    sysFile.setAttachmentName(fileName);
                    sysFile.setAttachmentPath(filePah);
                    //附件类型(0-word,1-excel,2-pdf,3-jpg,png,4-其他等)
                    String fileType = fileName.substring(fileName.lastIndexOf(".") + 1);
                    if ("doc".equals(fileType.toLowerCase()) || "docx".equals(fileType.toLowerCase())) {
                        sysFile.setAttachmentType(0);
                    } else if ("xls".equals(fileType.toLowerCase()) || "xlsx".equals(fileType.toLowerCase())) {
                        sysFile.setAttachmentType(1);
                    } else if ("pdf".equals(fileType.toLowerCase())) {
                        sysFile.setAttachmentType(2);
                    } else if ("jpg".equals(fileType.toLowerCase()) || "png".equals(fileType.toLowerCase()) || "gif".equals(fileType.toLowerCase())) {
                        sysFile.setAttachmentType(3);
                    } else {
                        sysFile.setAttachmentType(4);
                    }
                    sysFile.setSaveType(0);
                    sysFile.setIsSync(0);
                    sysFile.setCreateTime(new Date());
                    sysFile.setCreateUser(ShiroKit.getUser().getLoginName());
                    sysFileService.save(sysFile);
                }
                resetUploadFile();
            }
            return CommonResult.ok().msg("保存成功");
        } catch (Exception e) {
            e.printStackTrace();
            return CommonResult.fail().msg("运行异常，请联系管理员");
        }
    }

    private void setUploadFile(List<Map<String, String>> uploadFileUrl) {
        LoginUser user = ShiroKit.getUser();
        Object o = uploadFileUrls.get(user.getId().toString());
        if (o == null) {
            uploadFileUrls.put(user.getId().toString(), new ArrayList<>());
        }
        uploadFileUrls.get(user.getId().toString()).addAll(uploadFileUrl);
    }

    private List<Map<String, String>> getUploadFile() {
        LoginUser user = ShiroKit.getUser();
        return uploadFileUrls.get(user.getId().toString());
    }

    private void resetUploadFile() {
        LoginUser user = ShiroKit.getUser();
        uploadFileUrls.remove(user.getId().toString());
    }
}
