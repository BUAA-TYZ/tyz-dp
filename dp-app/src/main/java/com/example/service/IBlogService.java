package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dto.Result;
import com.example.dto.ScrollResult;
import com.example.dto.UserDTO;
import com.example.entity.Blog;

import java.util.List;

public interface IBlogService extends IService<Blog> {

    Result<List<Blog>> queryHotBlog(Integer current);

    Result<Blog> queryBlogById(Long id);

    Result likeBlog(Long id);

    Result<List<UserDTO>> queryBlogLikes(Long id);

    Result saveBlog(Blog blog);

    Result<ScrollResult> queryBlogOfFollow(Long max, Integer offset);

}
