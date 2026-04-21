package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Autowired
    private IFollowService followService;

    /**
     *
     * 判断当前用户是否已经关注博主
     *
     * @param id 博主ID
     * @return {@link Result }
     */
    @GetMapping("/or/not/{id}")
    public Result orNot(@PathVariable Long id) {
        return followService.orNot(id);
    }

    /**
     *
     * 关注博主
     *
     * @param id
     * @param isFollow
     * @return {@link Result }
     */
    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable Long id, @PathVariable Boolean isFollow) {
        return followService.follow(id, isFollow);
    }


    /**
     *
     * 共同关注
     * @param id 博主ID
     * @return {@link Result }
     */
    @GetMapping("/common/{id}")
    public Result followCommons(@PathVariable Long id) {
        return followService.followCommons(id);
    }
}
