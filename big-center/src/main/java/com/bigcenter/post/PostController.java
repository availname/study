package com.bigcenter.post;

import com.bigcenter.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 帖子接口：发帖（进审核管道）/ 详情 / 热榜 TopN */
@RestController
@RequestMapping("/api/post")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> publish(@RequestBody Map<String, String> body) {
        long id = postService.publish(body.get("title"), body.get("content"));
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("postId", id);
        view.put("状态", "待审核（AI 审核中，通过后可见）");
        return ApiResponse.ok(view);
    }

    @GetMapping("/{id}")
    public ApiResponse<Post> get(@PathVariable long id) {
        return ApiResponse.ok(postService.get(id));
    }

    @GetMapping("/hot")
    public ApiResponse<List<Long>> hot(@RequestParam(defaultValue = "10") int n) {
        return ApiResponse.ok(postService.hotTopN(n));
    }
}
